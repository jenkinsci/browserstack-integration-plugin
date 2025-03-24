package com.browserstack.automate.ci.jenkins.qualityDashboard;

import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Result;
import java.sql.Timestamp;
import java.time.Instant;
import jenkins.model.Jenkins;
import okhttp3.*;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import java.time.temporal.ChronoUnit;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

@Extension
public class QualityDashboardInit {

    static QualityDashboardAPIUtil apiUtil = new QualityDashboardAPIUtil();

    @Initializer(after = InitMilestone.PLUGINS_PREPARED)
    public static void postInstall() {
        try {
            initQDSetupIfRequired();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void pluginConfiguredNotif() {
        try {
            initQDSetupIfRequired();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static String exceptionToString(Throwable throwable) {
        return throwable.toString();
    }

    private static void initQDSetupIfRequired() throws JsonProcessingException {
        BrowserStackCredentials browserStackCredentials = QualityDashboardUtil.getBrowserStackCreds();
        try {
            if(browserStackCredentials!=null) {
                apiUtil.logToQD(browserStackCredentials,"Starting plugin data export to QD");
                checkQDIntegrationAndDumpMetaData(browserStackCredentials);
            }
        } catch (Exception e) {
            try {
                apiUtil.logToQD(browserStackCredentials, "Global exception in data export is:");
            } catch (Exception ex) {
                String exceptionString = exceptionToString(ex);
                apiUtil.logToQD(browserStackCredentials, "Global exception in exception data export is:" + exceptionString);
            }

        }

    }

    private static void checkQDIntegrationAndDumpMetaData(BrowserStackCredentials browserStackCredentials) throws JsonProcessingException {
        if(initialQDSetupRequired(browserStackCredentials)) {
            List<String> allPipelines = getAllPipelines(browserStackCredentials);
            if(!allPipelines.isEmpty()){
                boolean projectsSavedSuccessfully = sendPipelinesPaginated(browserStackCredentials, allPipelines);
                if(projectsSavedSuccessfully) {
                    List<PipelineDetails> allBuilds = getAllBuilds(browserStackCredentials);
                    if(!allBuilds.isEmpty()){
                        sendBuildsPaginated(browserStackCredentials, allBuilds);
                    } else {
                        apiUtil.logToQD(browserStackCredentials,"No Build Results data found");
                    }
                } else {
                    apiUtil.logToQD(browserStackCredentials,"Projects import failed, so not importing build results");
                }
            } else {
                apiUtil.logToQD(browserStackCredentials,"No pipelines detected");
            }
        }
    }

    private static boolean initialQDSetupRequired(BrowserStackCredentials browserStackCredentials) throws JsonProcessingException {
        try {
            Response response = apiUtil.makeGetRequestToQd(Constants.QualityDashboardAPI.getIsInitSetupRequiredEndpoint(), browserStackCredentials);
            if (response != null && response.code() == HttpURLConnection.HTTP_OK) {
                ResponseBody responseBody = response.body();
                if(responseBody != null && responseBody.string().equals("REQUIRED")) {
                    apiUtil.logToQD(browserStackCredentials,"Initial QD setup is required");
                    return true;
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        apiUtil.logToQD(browserStackCredentials,"Initial QD setup is not required");
        return false;
    }

    private static List<String> getAllPipelines(BrowserStackCredentials browserStackCredentials) throws JsonProcessingException {
        List<String> allPipelines = new ArrayList<>();
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            jenkins.getAllItems().forEach(job -> {
                if(job instanceof WorkflowJob) {
                    String pipelineName = job.getFullName();
                    allPipelines.add(pipelineName);
                }
            });
        } else {
            apiUtil.logToQD(browserStackCredentials,"Issue getting Jenkins Instance");
        }
        return allPipelines;
    }

    private static boolean sendPipelinesPaginated(BrowserStackCredentials browserStackCredentials, List<String> allPipelines) {
        boolean isSuccess = true;
        int pageSize = getProjectPageSize(browserStackCredentials);
        List<List<String>> pipelinesInSmallerBatches = Lists.partition(allPipelines, pageSize);
        int totalPages = !pipelinesInSmallerBatches.isEmpty() ? pipelinesInSmallerBatches.size() : 0;
        int page = 0;
        for(List<String> singlePagePipelineList : pipelinesInSmallerBatches) {
            try {
                page++;
                ObjectMapper objectMapper = new ObjectMapper();
                PipelinesPaginated pipelinesPaginated = new PipelinesPaginated(page, totalPages, singlePagePipelineList);
                String jsonBody = objectMapper.writeValueAsString(pipelinesPaginated);
                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
                Response response = apiUtil.makePostRequestToQd(Constants.QualityDashboardAPI.getSavePipelinesEndpoint(), browserStackCredentials, requestBody);
                if (response == null ||  response.code() != HttpURLConnection.HTTP_OK) {
                    apiUtil.logToQD(browserStackCredentials,"Got Non 200 response while saving projects");
                    isSuccess = false;
                    break;
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return isSuccess;
    }

    private static List<PipelineDetails> getAllBuilds(BrowserStackCredentials browserStackCredentials) {
        List<PipelineDetails> allBuildResults = new ArrayList<>();
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        Instant thresholdInstant = Instant.now().minus(getHistoryForDays(browserStackCredentials), ChronoUnit.DAYS);
        if (jenkins != null) {
            jenkins.getAllItems().forEach(job -> {
                if (job instanceof WorkflowJob) {
                    String pipelineName = job.getFullName();
                    List<WorkflowRun> allBuilds = ((WorkflowJob) job).getBuilds();
                    if(!allBuilds.isEmpty()) {
                        allBuilds.stream().filter(build -> Instant.ofEpochMilli(build.getTimeInMillis()).isAfter(thresholdInstant)).forEach(
                                build -> {
                                    int buildNumber = build.getNumber();
                                    long duration = build.getDuration();
                                    Result overallResult = build.getResult();
                                    long endTimeInMillis = build.getTimeInMillis();
                                    Timestamp endTime = new Timestamp(endTimeInMillis);
                                    String result = overallResult != null ? overallResult.toString() : null;
                                    PipelineDetails pipelineDetail = new PipelineDetails(pipelineName, buildNumber, duration, result, endTime);
                                    allBuildResults.add(pipelineDetail);
                                }
                        );
                    }
                }
            });
        }
        return allBuildResults;
    }

    private static void sendBuildsPaginated(BrowserStackCredentials browserStackCredentials, List<PipelineDetails> allBuilds) {
        int pageSize = getResultPageSize(browserStackCredentials);
        List<List<PipelineDetails>> buildResultsInSmallerBatches = Lists.partition(allBuilds, pageSize);
        int totalPages = !buildResultsInSmallerBatches.isEmpty() ? buildResultsInSmallerBatches.size() : 0;
        int page = 0;
        for(List<PipelineDetails> buildResultList : buildResultsInSmallerBatches) {
            try {
                page++;
                ObjectMapper objectMapper = new ObjectMapper();
                BuildResultsPaginated buildResultsPaginated = new BuildResultsPaginated(page, totalPages, buildResultList);
                String jsonBody = objectMapper.writeValueAsString(buildResultsPaginated);
                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
                Response response = apiUtil.makePostRequestToQd(Constants.QualityDashboardAPI.getSavePipelineResultsEndpoint(), browserStackCredentials, requestBody);
                if (response == null ||  response.code() != HttpURLConnection.HTTP_OK) {
                    apiUtil.logToQD(browserStackCredentials,"Got Non 200 response while saving projects");
                    break;
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static int getHistoryForDays(BrowserStackCredentials browserStackCredentials) {
        int no_of_days = 90;
        try {
            Response response = apiUtil.makeGetRequestToQd(Constants.QualityDashboardAPI.getHistoryForDaysEndpoint(), browserStackCredentials);
            if (response != null &&  response.code() == HttpURLConnection.HTTP_OK) {
                ResponseBody responseBody = response.body();
                if(responseBody != null) {
                    String responseBodyStr = responseBody.string();
                    if(responseBodyStr!=null)
                        no_of_days = Integer.parseInt(responseBodyStr);
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            return no_of_days;
        }
    }

    private static int getProjectPageSize(BrowserStackCredentials browserStackCredentials) {
        int projectPageSize = 2000;
        try {
            Response response = apiUtil.makeGetRequestToQd(Constants.QualityDashboardAPI.getProjectsPageSizeEndpoint(), browserStackCredentials);
            if (response != null &&  response.code() == HttpURLConnection.HTTP_OK) {
                ResponseBody responseBody = response.body();
                if(responseBody != null) {
                    String responseBodyStr = responseBody.string();
                    if(responseBodyStr!=null)
                        projectPageSize = Integer.parseInt(responseBodyStr);
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            return projectPageSize;
        }
    }

    private static int getResultPageSize(BrowserStackCredentials browserStackCredentials) {
        int resultPageSize = 1000;
        try {
            Response response = apiUtil.makeGetRequestToQd(Constants.QualityDashboardAPI.getResultsPageSizeEndpoint(), browserStackCredentials);
            if (response != null &&  response.code() == HttpURLConnection.HTTP_OK) {
                ResponseBody responseBody = response.body();
                if(responseBody != null) {
                    String responseBodyStr = responseBody.string();
                    if(responseBodyStr!=null)
                        resultPageSize = Integer.parseInt(responseBodyStr);
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            return resultPageSize;
        }
    }
}

class PipelineDetails {

    @JsonProperty("pipelineName")
    private String pipelineName;

    @JsonProperty("buildNumber")
    private Integer buildNumber;
    @JsonProperty("buildDuration")
    private Long buildDuration;
    @JsonProperty("buildStatus")
    private String buildStatus;

    @JsonProperty("endTime")
    private Timestamp endTime;

    public PipelineDetails(String pipelineName, Integer buildNumber, Long buildDuration, String buildStatus, Timestamp endTime) {
        this.pipelineName = pipelineName;
        this.buildNumber = buildNumber;
        this.buildDuration = buildDuration;
        this.buildStatus = buildStatus;
        this.endTime = endTime;
    }
}

class PipelinesPaginated {
    @JsonProperty("page")
    private int page;

    @JsonProperty("totalPages")
    private int totalPages;

    @JsonProperty("pipelines")
    private List<String> pipelines;

    public PipelinesPaginated(int page, int totalPages, List<String> pipelines) {
        this.page = page;
        this.totalPages = totalPages;
        this.pipelines = pipelines;
    }
}

class BuildResultsPaginated {
    @JsonProperty("page")
    private int page;

    @JsonProperty("totalPages")
    private int totalPages;

    @JsonProperty("builds")
    private List<PipelineDetails> builds;

    public BuildResultsPaginated(int page, int totalPages, List<PipelineDetails> builds) {
        this.page = page;
        this.totalPages = totalPages;
        this.builds = builds;
    }
}
