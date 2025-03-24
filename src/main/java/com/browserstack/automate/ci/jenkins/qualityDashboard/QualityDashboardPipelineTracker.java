package com.browserstack.automate.ci.jenkins.qualityDashboard;

import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import io.jenkins.cli.shaded.org.apache.commons.lang.StringUtils;
import jenkins.model.Jenkins;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;

@Extension
public class QualityDashboardPipelineTracker extends RunListener<Run> {

    QualityDashboardAPIUtil apiUtil = new QualityDashboardAPIUtil();

    @Override
    public void onCompleted(Run run, TaskListener listener) {
        super.onCompleted(run, listener);
        BrowserStackCredentials browserStackCredentials = QualityDashboardUtil.getBrowserStackCreds();
        if(browserStackCredentials!=null) {
            WorkflowRun workflowRun = (WorkflowRun) run;
            WorkflowJob workflowJob = workflowRun.getParent();
            String jobName = workflowJob.getFullName();
            int buildNumber = run.getNumber();
            try {
                if(isQDEnabled(browserStackCredentials) && isPipelineEnabledForQD(browserStackCredentials, jobName)) {
                    Result overallResult = run.getResult();
                    if(overallResult != null) {
                        String qdS3Url = null;
                        String finalPathToZip = getFinalZipPath(run, browserStackCredentials);
                        apiUtil.logToQD(browserStackCredentials, "Final Computed Zip Path for jobName: " + jobName + " and buildNumber: " + buildNumber + " is: " + finalPathToZip);
                        if(StringUtils.isNotEmpty(finalPathToZip)) {
                            apiUtil.logToQD(browserStackCredentials, "Found artifacts in configured path for jobName: " + jobName + " and buildNumber: " + buildNumber);
                            copyDirectoryToParentIfRequired(run, finalPathToZip, browserStackCredentials);
                            qdS3Url = zipArtifactsAndUploadToQD(finalPathToZip, browserStackCredentials, jobName, buildNumber);
                        } else if(run.getHasArtifacts()) {
                            apiUtil.logToQD(browserStackCredentials, "No artifacts in configured path but found archive artifacts for jobName: " + jobName + " and buildNumber: " + buildNumber);
                            finalPathToZip = run.getArtifactsDir().getAbsolutePath();
                            apiUtil.logToQD(browserStackCredentials, "Got artifact path for jobName: " + jobName + " and buildNumber: " + buildNumber + " as: " + finalPathToZip);
                            qdS3Url = zipArtifactsAndUploadToQD(finalPathToZip, browserStackCredentials, jobName, buildNumber);
                        } else {
                            apiUtil.logToQD(browserStackCredentials, "Finally no artifacts found for jobName: " + jobName + " and buildNumber: " + buildNumber);
                        }
                        sendBuildDataToQD(run, overallResult, qdS3Url, browserStackCredentials);
                    } else {
                        apiUtil.logToQD(browserStackCredentials, "Null Result Captured for jobName: " + jobName + " and buildNumber: " + buildNumber);
                    }
                }
            } catch (IOException e) {
                try {
                    apiUtil.logToQD(browserStackCredentials, "Global Exception for jobName: " + jobName + " and buildNumber: " + buildNumber + " is: " + e.toString());
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
                throw new RuntimeException(e);
            }
        }
    }

    private String zipArtifactsAndUploadToQD (String finalPathToZip, BrowserStackCredentials browserStackCredentials, String jobName, int buildNumber) throws IOException {
        String finalZipFilePath = packZip(finalPathToZip, jobName, browserStackCredentials);
        apiUtil.logToQD(browserStackCredentials, "Final zip file's path for jobName: " + jobName + " and buildNumber: " + buildNumber + " is:" + finalZipFilePath);
        String qdS3Url = uploadZipToQd(finalZipFilePath, browserStackCredentials, jobName, buildNumber);
        if(StringUtils.isNotEmpty(finalZipFilePath)) {
            Files.deleteIfExists(Paths.get(finalZipFilePath));
            apiUtil.logToQD(browserStackCredentials, "Deleted file from server after upload for jobName: " + jobName + " and buildNumber: " + buildNumber);
        } else {
            apiUtil.logToQD(browserStackCredentials, "No zip file to delete for jobName: " + jobName + " and buildNumber: " + buildNumber);
        }
        return qdS3Url;
    }

    private void sendBuildDataToQD(Run run, Result overallResult, String finalZipPath, BrowserStackCredentials browserStackCredentials) {
        Long pipelineDuration = getPipelineDuration(run);
        try {
            String jobName = run.getParent().getFullName();
            int buildNumber = run.getNumber();
            long endTimeInMillis = run.getTimeInMillis();

            Jenkins jenkins = Jenkins.getInstanceOrNull();
            String rootUrl = jenkins !=null ? jenkins.getRootUrl() : null;
            String jobUrl = null;
            if(rootUrl != null) {
                jobUrl = rootUrl + run.getUrl();
            }

            Timestamp endTime = new Timestamp(endTimeInMillis);
            PipelineResults pipelineResultsReqObj = new PipelineResults(buildNumber, pipelineDuration, overallResult.toString(), finalZipPath, jobName, endTime, jobUrl);
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(pipelineResultsReqObj);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
            apiUtil.logToQD(browserStackCredentials, "Sending Final Results for jobName: " + jobName + " and buildNumber: " + buildNumber);
            apiUtil.makePostRequestToQd(Constants.QualityDashboardAPI.getStorePipelineResultsEndpoint(), browserStackCredentials, requestBody);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private Long getPipelineDuration(Run build) {
        long startTime = build.getStartTimeInMillis();
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime) / 1000;
        return duration;
    }

    private boolean checkIfPathIsFound(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path) ? true : false;
    }
    private String getFinalZipPath(Run run, BrowserStackCredentials browserStackCredentials) throws JsonProcessingException {
        String finalZipPath = null;
        String currentResultDir = getResultDirForPipeline(getUrlForPipeline(run), browserStackCredentials, run.getNumber());
        if(StringUtils.isNotEmpty(currentResultDir) && checkIfPathIsFound(currentResultDir)) {
            finalZipPath = currentResultDir;
        } else {
            String defaultWorkspaceDir = getDefaultWorkspaceDirectory(run);
            if(StringUtils.isNotEmpty(defaultWorkspaceDir)) {
                String jobName = run.getParent().getName();
                defaultWorkspaceDir = defaultWorkspaceDir + "/workspace/" + jobName + "/browserstack-artifacts";
                finalZipPath = checkIfPathIsFound(defaultWorkspaceDir) ? defaultWorkspaceDir : null;
            }
        }
        return finalZipPath;
    }

    private String getDefaultWorkspaceDirectory(Run run) {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        String workspacePath = jenkins != null && jenkins.getRootDir() != null ? jenkins.getRootDir().getAbsolutePath() : null;
        return StringUtils.isNotEmpty(workspacePath) ? workspacePath : null;
    }

    private String getUrlForPipeline(Run<?, ?> build) {
        return build.getParent().getFullName();
    }

    private boolean isQDEnabled(BrowserStackCredentials browserStackCredentials) throws IOException {
        Response response = apiUtil.makeGetRequestToQd(Constants.QualityDashboardAPI.getIsQdEnabledEndpoint(), browserStackCredentials);
        if (response != null &&  response.code() == HttpURLConnection.HTTP_OK) {
            ResponseBody responseBody = response.body();
            if(responseBody != null && Boolean.parseBoolean(response.body().string())) {
                apiUtil.logToQD(browserStackCredentials, "QD enabled check passed");
                return true;
            }
        }
        apiUtil.logToQD(browserStackCredentials, "QD enabled check failed");
        return false;
    }

    private boolean isPipelineEnabledForQD(BrowserStackCredentials browserStackCredentials, String pipelineName) throws IOException {
        QualityDashboardGetDetailsForPipeline getPipelineEnabledObj = new QualityDashboardGetDetailsForPipeline(pipelineName);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = objectMapper.writeValueAsString(getPipelineEnabledObj);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
        Response response = apiUtil.makePostRequestToQd(Constants.QualityDashboardAPI.getIsPipelineEnabledEndpoint(), browserStackCredentials, requestBody);
        if (response != null &&  response.code() == HttpURLConnection.HTTP_OK) {
            ResponseBody responseBody = response.body();
            if(responseBody != null && Boolean.parseBoolean(response.body().string())) {
                apiUtil.logToQD(browserStackCredentials, "Pipeline enabled - pipelineName: " + pipelineName);
                return true;
            }
        }
        apiUtil.logToQD(browserStackCredentials, "Pipeline disabled - pipelineName: " + pipelineName);
        return false;
    }

    private String getResultDirForPipeline(String pipelineUrl, BrowserStackCredentials browserStackCredentials, int buildNumber) throws JsonProcessingException {
        String resultDir = null;
        try {
            QualityDashboardGetDetailsForPipeline getResultDirReqObj = new QualityDashboardGetDetailsForPipeline(pipelineUrl);
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(getResultDirReqObj);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
            Response response = apiUtil.makePostRequestToQd(Constants.QualityDashboardAPI.getResultDirectoryEndpoint(), browserStackCredentials, requestBody);
            if (response != null && response.code() == HttpURLConnection.HTTP_OK) {
                String responseBody = response.body() !=null ? response.body().string() : null;
                resultDir = responseBody;
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        resultDir = resultDir !=null && resultDir.contains("%build_number%") ? resultDir.replace("%build_number%", String.valueOf(buildNumber)) : resultDir;
        apiUtil.logToQD(browserStackCredentials, "Result Directory for jobName: " + pipelineUrl + " and buildNumber: " + buildNumber + " is resultDir: " + resultDir);
        return resultDir;
    }

    private String packZip(String sourceDirPath, String jobName, BrowserStackCredentials browserStackCredentials) throws JsonProcessingException {
        Path zipPath = Paths.get(sourceDirPath).getParent();
        String zipFile = zipPath.toString() + "/browserstack-artifacts.zip";
        Path zipFilePath = Paths.get(zipFile);
        apiUtil.logToQD(browserStackCredentials, "zipFilePath for jobName: " + jobName + " is:" + zipFilePath);
        try {
            Files.deleteIfExists(zipFilePath);
            ZipUtil.pack(new File(sourceDirPath), new File(zipFile));
            apiUtil.logToQD(browserStackCredentials, "zipFile size for jobName: " + jobName + " is:" + Files.size(zipFilePath));
        } catch (IOException e) {
            String exceptionString = exceptionToString(e);
            apiUtil.logToQD(browserStackCredentials, "Error creating zip for jobName: " + jobName + " is:" + exceptionString);
        }
        return zipFile;
    }

    private String exceptionToString(Throwable throwable) {
        return throwable.toString();
    }

    private String uploadZipToQd(String pathToZip, BrowserStackCredentials browserStackCredentials, String jobName, int buildNumber) throws IOException {
        String qdS3Url = null;
        File fileToUpload = new File(pathToZip);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileToUpload.getName(),
                        RequestBody.create(MediaType.parse("application/octet-stream"), fileToUpload))
                .addFormDataPart("jobName", jobName)
                .addFormDataPart("buildNumber", String.valueOf(buildNumber))
                .build();

        Response response = apiUtil.makePostRequestToQd(Constants.QualityDashboardAPI.getUploadResultZipEndpoint(), browserStackCredentials, requestBody);
        if (response != null && response.code() == HttpURLConnection.HTTP_OK) {
            qdS3Url = response.body() !=null ? response.body().string() : null;
        }
        return qdS3Url;
    }

    private void copyDirectoryToParentIfRequired(Run run, String finalParentPathFrom, BrowserStackCredentials browserStackCredentials) throws IOException {
        String finalParentPathTo = null;
        String upStreamProj = upStreamPipelineUrl(run);
        if(StringUtils.isNotEmpty(upStreamProj)) {
            String parentResultDir = getResultDirForPipeline(upStreamProj, browserStackCredentials, run.getNumber());
            if(StringUtils.isNotEmpty(parentResultDir) && checkIfPathIsFound(parentResultDir)) {
                finalParentPathTo = parentResultDir;
            } else {
                String defaultWorkspaceDir = getDefaultWorkspaceDirectory(run);
                if(StringUtils.isNotEmpty(defaultWorkspaceDir) && checkIfPathIsFound(defaultWorkspaceDir)) {
                    defaultWorkspaceDir = defaultWorkspaceDir + "/workspace/" + upStreamProj + "/browserstack-artifacts";
                    boolean pathAlreadyExists = checkIfPathIsFound(defaultWorkspaceDir);
                    if(!pathAlreadyExists) {
                        Files.createDirectory(Paths.get(defaultWorkspaceDir));
                    }
                    finalParentPathTo = defaultWorkspaceDir;
                }
            }
            if(StringUtils.isNotEmpty(finalParentPathTo)) {
                FileUtils.copyDirectoryToDirectory(new File(finalParentPathFrom), new File(finalParentPathTo));
                int buildNum = run.getNumber();
                File finalParentFromFile = new File(finalParentPathFrom);
                File newZipDir = new File(finalParentPathTo + "/" + finalParentFromFile.getName() + "_" + buildNum);
                FileUtils.moveDirectory(new File(finalParentPathTo + "/" + finalParentFromFile.getName()), newZipDir);
            }
        }
    }

    private String upStreamPipelineUrl(Run run) {
        String upstreamProjectName = null;
        List<Cause> causes = run.getCauses();
        for (Cause cause : causes) {
            if (cause instanceof Cause.UpstreamCause) {
                Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) cause;
                upstreamProjectName = upstreamCause.getUpstreamProject();
            }
        }
        return upstreamProjectName;
    }
}

class QualityDashboardGetDetailsForPipeline implements Serializable {
    @JsonProperty("url")
    private String pipeline;
    public QualityDashboardGetDetailsForPipeline(String pipeline) {
        this.pipeline = pipeline;
    }
}

class PipelineResults implements Serializable {

    @JsonProperty("buildNumber")
    private Integer buildNumber;

    @JsonProperty("pipelineName")
    private String pipelineName;
    @JsonProperty("buildDuration")
    private Long buildDuration;

    @JsonProperty("jobUrl")
    private String jobUrl;

    @JsonProperty("endTime")
    private Timestamp endTime;
    @JsonProperty("buildStatus")
    private String buildStatus;

    @JsonProperty("zipFile")
    private String zipFile;

    public PipelineResults(Integer buildNumber, Long buildDuration, String buildStatus, String zipFile, String pipelineName, Timestamp endTime, String jobUrl) {
        this.buildNumber = buildNumber;
        this.buildDuration = buildDuration;
        this.buildStatus = buildStatus;
        this.zipFile = zipFile;
        this.pipelineName = pipelineName;
        this.endTime = endTime;
        this.jobUrl = jobUrl;
    }
}
