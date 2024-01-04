package com.browserstack.automate.ci.jenkins.qualityDashboard;

import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.fasterxml.jackson.annotation.JsonProperty;
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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Extension
public class QualityDashboardPipelineTracker extends RunListener<Run> {

    QualityDashboardAPIUtil apiUtil = new QualityDashboardAPIUtil();

    //check if this is the final step of the build
    // if yes, get the result dir for the pipeline
    // compose an object which contains, status, duration and zip fo test directory (if configured)
    // check if this was called by upstream proj. if so, get result dir for the parent pipeline
    // if parent pipeline configured then, copy the test results in that dir. If result dir not configured for parent, create a browserstack-artifacts dir if not present and copy the result there
    @Override
    public void onCompleted(Run run, TaskListener listener) {
        super.onCompleted(run, listener);
        BrowserStackCredentials browserStackCredentials = QualityDashboardUtil.getBrowserStackCreds();
        if(browserStackCredentials!=null) {
            try {
                WorkflowRun workflowRun = (WorkflowRun) run;
                WorkflowJob workflowJob = workflowRun.getParent();
                String jobName = workflowJob.getFullName();
                if(isQDEnabled(browserStackCredentials) && isPipelineEnabledForQD(browserStackCredentials, jobName)) {
                    Result overallResult = run.getResult();
                    if(overallResult != null) {
                        String finalZipFilePath = null; String qdS3Url = null;
                        String finalPathToZip = getFinalZipPath(run, browserStackCredentials);
                        if(StringUtils.isNotEmpty(finalPathToZip)) {
                            int buildNumber = run.getNumber();
                            copyDirectoryToParentIfRequired(run, finalPathToZip, browserStackCredentials);
                            finalZipFilePath = packZip(finalPathToZip, run, jobName);
                            qdS3Url = uploadZipToQd(finalZipFilePath, browserStackCredentials, jobName, buildNumber);
                        }
                        sendBuildDataToQD(run, overallResult, qdS3Url, browserStackCredentials);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendBuildDataToQD(Run run, Result overallResult, String finalZipPath, BrowserStackCredentials browserStackCredentials) {
        Long pipelineDuration = getPipelineDuration(run);
        try {
            String jobName = run.getParent().getFullName();
            int buildNumber = run.getNumber();
            long endTimeInMillis = run.getTimeInMillis();
            Timestamp endTime = new Timestamp(endTimeInMillis);
            PipelineResults pipelineResultsReqObj = new PipelineResults(buildNumber, pipelineDuration, overallResult.toString(), finalZipPath, jobName, endTime);
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(pipelineResultsReqObj);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
            apiUtil.makePostRequestToQd(Constants.QualityDashboardAPI.STORE_PIPELINE_RESULTS, browserStackCredentials, requestBody);
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
    private String getFinalZipPath(Run run, BrowserStackCredentials browserStackCredentials) {
        String finalZipPath = null;
        String currentResultDir = getResultDirForPipeline(getUrlForPipeline(run), browserStackCredentials);
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
        String pipelineDirectory = null;
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        String workspacePath = jenkins.getRootDir().getAbsolutePath();
        if (StringUtils.isNotEmpty(workspacePath)) {
            pipelineDirectory = workspacePath;
        }
        return pipelineDirectory;
    }

    private String getUrlForPipeline(Run<?, ?> build) {
        return build.getParent().getFullName();
    }

    private boolean isQDEnabled(BrowserStackCredentials browserStackCredentials) throws IOException {
        Response response = apiUtil.makeGetRequestToQd(Constants.QualityDashboardAPI.IS_QD_ENABLED, browserStackCredentials);
        if (response != null &&  response.code() == HttpURLConnection.HTTP_OK) {
            ResponseBody responseBody = response.body();
            if(responseBody != null && Boolean.parseBoolean(response.body().string())) {
                return true;
            }
        }
        return false;
    }

    private boolean isPipelineEnabledForQD(BrowserStackCredentials browserStackCredentials, String pipelineName) throws IOException {
        QualityDashboardGetDetailsForPipeline getPipelineEnabledObj = new QualityDashboardGetDetailsForPipeline(pipelineName);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = objectMapper.writeValueAsString(getPipelineEnabledObj);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
        Response response = apiUtil.makePostRequestToQd(Constants.QualityDashboardAPI.IS_PIPELINE_ENABLED, browserStackCredentials, requestBody);
        if (response != null &&  response.code() == HttpURLConnection.HTTP_OK) {
            ResponseBody responseBody = response.body();
            if(responseBody != null && Boolean.parseBoolean(response.body().string())) {
                return true;
            }
        }
        return false;
    }

    private String getResultDirForPipeline(String pipelineUrl, BrowserStackCredentials browserStackCredentials) {
        String resultDir = null;
        try {
            QualityDashboardGetDetailsForPipeline getResultDirReqObj = new QualityDashboardGetDetailsForPipeline(pipelineUrl);
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(getResultDirReqObj);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
            Response response = apiUtil.makePostRequestToQd(Constants.QualityDashboardAPI.GET_RESULT_DIRECTORY, browserStackCredentials, requestBody);
            if (response != null && response.code() == HttpURLConnection.HTTP_OK) {
                String responseBody = response.body() !=null ? response.body().string() : null;
                resultDir = responseBody;
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        return resultDir;
    }

    private String packZip(String sourceDirPath, Run run, String jobName) {
        String defaultWorkspaceDir = getDefaultWorkspaceDirectory(run);
        String zipFilePath = defaultWorkspaceDir + "/workspace/" + jobName + "/browserstack-artifacts.zip";
        try {
            Files.deleteIfExists(Paths.get(zipFilePath));
            Path p = Files.createFile(Paths.get(zipFilePath));
            try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
                Path pp = Paths.get(sourceDirPath);
                Files.walk(pp)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                            try {
                                zs.putNextEntry(zipEntry);
                                Files.copy(path, zs);
                                zs.closeEntry();
                            } catch (IOException e) {
                                System.err.println(e);
                            }
                        });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return zipFilePath;
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

        Response response = apiUtil.makePostRequestToQd(Constants.QualityDashboardAPI.UPLOAD_RESULT_ZIP, browserStackCredentials, requestBody);
        if (response != null && response.code() == HttpURLConnection.HTTP_OK) {
            qdS3Url = response.body() !=null ? response.body().string() : null;
        }
        return qdS3Url;
    }

    private void copyDirectoryToParentIfRequired(Run run, String finalParentPathFrom, BrowserStackCredentials browserStackCredentials) throws IOException {
        String finalParentPathTo = null;
        String upStreamProj = upStreamPipelineUrl(run);
        if(StringUtils.isNotEmpty(upStreamProj)) {
            String parentResultDir = getResultDirForPipeline(upStreamProj, browserStackCredentials);
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

    @JsonProperty("endTime")
    private Timestamp endTime;
    @JsonProperty("buildStatus")
    private String buildStatus;

    @JsonProperty("zipFile")
    private String zipFile;

    public PipelineResults(Integer buildNumber, Long buildDuration, String buildStatus, String zipFile, String pipelineName, Timestamp endTime) {
        this.buildNumber = buildNumber;
        this.buildDuration = buildDuration;
        this.buildStatus = buildStatus;
        this.zipFile = zipFile;
        this.pipelineName = pipelineName;
        this.endTime = endTime;
    }
}
