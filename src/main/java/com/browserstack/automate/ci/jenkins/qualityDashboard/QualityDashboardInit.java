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
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import java.sql.Timestamp;
import java.time.Instant;
import java.io.Serializable;
import jenkins.model.Jenkins;
import okhttp3.*;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import java.time.temporal.ChronoUnit;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Extension
public class QualityDashboardInit {

    private static final Logger LOGGER = Logger.getLogger(QualityDashboardInit.class.getName());
    static QualityDashboardAPIUtil apiUtil = new QualityDashboardAPIUtil();

    @Initializer(after = InitMilestone.JOB_LOADED)
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
            if(browserStackCredentials != null) {
                apiUtil.logToQD(browserStackCredentials,"Starting plugin data export to QD");
                checkQDIntegrationAndDumpMetaData(browserStackCredentials);
            } else {
                LOGGER.warning("BrowserStack credentials not found. Skipping Quality Dashboard initialization.");
            }
        } catch (Exception e) {
            try {
                apiUtil.logToQD(browserStackCredentials, "Global exception in data export is:" + exceptionToString(e));
            } catch (Exception ex) {
                String exceptionString = exceptionToString(ex);
                apiUtil.logToQD(browserStackCredentials, "Global exception in exception data export is:" + exceptionString);
            }

        }

    }

    private static void checkQDIntegrationAndDumpMetaData(BrowserStackCredentials browserStackCredentials) throws JsonProcessingException {
        if(initialQDSetupRequired(browserStackCredentials)) {
            List<PipelineInfo> allPipelines = getAllPipelines(browserStackCredentials);
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

    private static List<PipelineInfo> getAllPipelines(BrowserStackCredentials browserStackCredentials) {
        List<PipelineInfo> allPipelines = new ArrayList<>();
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        Integer totalPipelines = 0;

        if (jenkins != null) {
            totalPipelines = jenkins.getAllItems().size();
            jenkins.getAllItems().forEach(job -> {
                try {
                    String itemType = QualityDashboardUtil.getItemTypeModified(job);
                    boolean isWorkflowJob = job instanceof WorkflowJob;
                    
                    // Logging job details
                    apiUtil.logToQD(
                        browserStackCredentials,
                        String.format(
                            "Job name: %s, instance type: %s, and is_workflow_job: %s",
                            job.getName(),
                            itemType,
                            isWorkflowJob ? "yes" : "no"
                        )
                    );
                    if (itemType != null && !itemType.equals("FOLDER")) {
                        String pipelineName = job.getFullName();
                        allPipelines.add(new PipelineInfo(pipelineName, itemType));
                    }
                    else{
                        apiUtil.logToQD(browserStackCredentials, "Skipping job or Folder: " + job.getName() + " as it is not a Job or Folder instance");
                    }
                    
                } catch (JsonProcessingException e) {
                    // Handling the exception and logging an error
                    LOGGER.warning("Error processing JSON for job: " + job.getName() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            try {
                apiUtil.logToQD(browserStackCredentials, "Issue getting Jenkins Instance");
            } catch (JsonProcessingException e) {
                LOGGER.warning("Error logging issue with Jenkins instance.");
                e.printStackTrace();
            }
        }

        try {
            apiUtil.logToQD(browserStackCredentials,"Total Pipelines on the jenkins side : " + totalPipelines);
            apiUtil.logToQD(browserStackCredentials,"Total Pipelines detected : " + allPipelines.size());
        } catch (JsonProcessingException e) {
            // Handling the exception and logging an error
            LOGGER.warning("Error processing JSON for total pipelines: " + e.getMessage());
            e.printStackTrace();
        }
        // Returning the list of filtered pipelines
        return allPipelines;
    }

    private static boolean sendPipelinesPaginated(BrowserStackCredentials browserStackCredentials, List<PipelineInfo> allPipelines) {
        boolean isSuccess = true;
        int pageSize = getProjectPageSize(browserStackCredentials);
        List<List<PipelineInfo>> pipelinesInSmallerBatches = Lists.partition(allPipelines, pageSize);
        int totalPages = !pipelinesInSmallerBatches.isEmpty() ? pipelinesInSmallerBatches.size() : 0;
        int page = 0;
        for(List<PipelineInfo> singlePagePipelineList : pipelinesInSmallerBatches) {
            try {
                page++;
                ObjectMapper objectMapper = new ObjectMapper();
                PipelinesPaginated pipelinesPaginated = new PipelinesPaginated(page, totalPages, singlePagePipelineList);
                String jsonBody = objectMapper.writeValueAsString(pipelinesPaginated);
                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
                Response response = apiUtil.makePostRequestToQd(Constants.QualityDashboardAPI.getSavePipelinesEndpoint(), browserStackCredentials, requestBody);
                apiUtil.logToQD(browserStackCredentials, "Sending page " + page + " with " + singlePagePipelineList.size() + " pipelines");
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
            jenkins.getAllItems().forEach(item -> {
                // Support both WorkflowJob and Matrix projects (and potentially other job types)
                if (item instanceof Job) {
                    Job<?, ?> job = (Job<?, ?>) item;
                    String pipelineName = job.getFullName();
                    List<? extends Run<?, ?>> allBuilds = job.getBuilds();
                    if(!allBuilds.isEmpty()) {
                        allBuilds.stream().filter(build -> Instant.ofEpochMilli(build.getTimeInMillis()).isAfter(thresholdInstant)).forEach(
                                build -> {
                                    int buildNumber = build.getNumber();
                                    long duration = build.getDuration();
                                    Result overallResult = build.getResult();
                                    long endTimeInMillis = build.getTimeInMillis();
                                    Timestamp endTime = new Timestamp(endTimeInMillis);
                                    String result = overallResult != null ? overallResult.toString() : null;
                                    
                                    // Get root upstream project information for QEI with build number (returns in format "project#build")
                                    String rootUpstreamProject = "";
                                    String immediateParentProject = "";
                                    try {
                                        rootUpstreamProject = UpstreamPipelineResolver.resolveRootUpstreamProject(build, browserStackCredentials);
                                        immediateParentProject = UpstreamPipelineResolver.resolveImmediateUpstreamProjectForQEI(build, browserStackCredentials);
                                    } catch (Exception e) {          
                                        LOGGER.warning("Error resolving upstream project for " + pipelineName + " build number " + buildNumber + ": " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                    PipelineDetails pipelineDetail = new PipelineDetails(pipelineName, buildNumber, duration, result,
                                            endTime, rootUpstreamProject, immediateParentProject);
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
        }
        return no_of_days;
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
        }
        return projectPageSize;
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
        }
        return resultPageSize;
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
    
    @JsonProperty("rootProject")
    private String rootProject;
    
    @JsonProperty("immediateParentProject")
    private String immediateParentProject;

    public PipelineDetails(String pipelineName, Integer buildNumber, Long buildDuration, String buildStatus, 
                          Timestamp endTime, String rootProject, String immediateParentProject) {
        this.pipelineName = pipelineName;
        this.buildNumber = buildNumber;
        this.buildDuration = buildDuration;
        this.buildStatus = buildStatus;
        this.endTime = endTime;
        this.rootProject = rootProject;
        this.immediateParentProject = immediateParentProject;
    }
}

class PipelineInfo implements Serializable {
    @JsonProperty("pipelineName")
    private String pipelineName;

    @JsonProperty("jobType")
    private String jobType;

    public PipelineInfo(String pipelineName, String jobType) {
        this.pipelineName = pipelineName;
        this.jobType = jobType;
    }
}

class PipelinesPaginated {
    @JsonProperty("page")
    private int page;

    @JsonProperty("totalPages")
    private int totalPages;

    @JsonProperty("pipelines")
    private List<PipelineInfo> pipelines;

    public PipelinesPaginated(int page, int totalPages, List<PipelineInfo> pipelines) {
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
