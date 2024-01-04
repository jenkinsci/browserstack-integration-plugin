package com.browserstack.automate.ci.jenkins.qualityDashboard;

import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        initQDSetupIfRequired();
    }

    public void pluginConfiguredNotif() {
        initQDSetupIfRequired();
    }

    private static void initQDSetupIfRequired() {
        BrowserStackCredentials browserStackCredentials = QualityDashboardUtil.getBrowserStackCreds();
        if(browserStackCredentials!=null) {
            checkQDIntegrationAndDumpMetaData(browserStackCredentials);
        }
    }

    private static void checkQDIntegrationAndDumpMetaData(BrowserStackCredentials browserStackCredentials) {
        if(initialQDSetupRequired(browserStackCredentials)) {
            List<PipelineDetailsMap> pipelineDetailsMapList = getExistingPipelineDump(browserStackCredentials);
            if(!pipelineDetailsMapList.isEmpty()){
                syncInitialDataWithQD(pipelineDetailsMapList, browserStackCredentials);
            }
        } 
    }

    private static boolean initialQDSetupRequired(BrowserStackCredentials browserStackCredentials) {
        try {
            Response response = apiUtil.makeGetRequestToQd(Constants.QualityDashboardAPI.IS_INIT_SETUP_REQUIRED, browserStackCredentials);
            if (response != null && response.code() == HttpURLConnection.HTTP_OK) {
                ResponseBody responseBody = response.body();
                if(responseBody != null && responseBody.string().equals("REQUIRED")) {
                    return true;
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static List<PipelineDetailsMap> getExistingPipelineDump(BrowserStackCredentials browserStackCredentials) {
        List<PipelineDetailsMap> pipelineDetailsMapList = new ArrayList<>();
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        Instant thresholdInstant = Instant.now().minus(getHistoryForDays(browserStackCredentials), ChronoUnit.DAYS);
        if (jenkins != null) {
            jenkins.getAllItems().forEach(job -> {
                if(job instanceof WorkflowJob) {
                    String pipelineName = job.getFullName();
                    List<PipelineDetails> pipelineDetailsList = new ArrayList<>();
                    List<WorkflowRun> allBuilds = ((WorkflowJob) job).getBuilds();
                    if(!allBuilds.isEmpty()) {
                        allBuilds.stream().filter(build -> Instant.ofEpochMilli(build.getTimeInMillis()).isAfter(thresholdInstant) ).forEach(
                            build -> {
                                int buildNumber = build.getNumber();
                                long duration = build.getDuration();
                                Result overallResult = build.getResult();
                                long endTimeInMillis = build.getTimeInMillis();
                                Timestamp endTime = new Timestamp(endTimeInMillis);
                                PipelineDetails pipelineDetail = new PipelineDetails(buildNumber, duration, overallResult.toString(), endTime );
                                pipelineDetailsList.add(pipelineDetail);
                            }
                        );
                    }
                    PipelineDetailsMap pipelineDetailsMap = new PipelineDetailsMap(pipelineName, pipelineDetailsList);
                    pipelineDetailsMapList.add(pipelineDetailsMap);
                }
            });
        }
        return pipelineDetailsMapList;
    }

    private static int getHistoryForDays(BrowserStackCredentials browserStackCredentials) {
        int no_of_days = 90;
        try {
            Response response = apiUtil.makeGetRequestToQd(Constants.QualityDashboardAPI.HISTORY_FOR_DAYS, browserStackCredentials);
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

    private static void syncInitialDataWithQD(List<PipelineDetailsMap> pipelineDetailsMapList, BrowserStackCredentials browserStackCredentials) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(pipelineDetailsMapList);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
            Response response = apiUtil.makePostRequestToQd(Constants.QualityDashboardAPI.ADD_ALL_PIPELINES, browserStackCredentials, requestBody);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}

class PipelineDetails {

    @JsonProperty("buildNumber")
    private Integer buildNumber;
    @JsonProperty("buildDuration")
    private Long buildDuration;
    @JsonProperty("buildStatus")
    private String buildStatus;

    @JsonProperty("endTime")
    private Timestamp endTime;

    public PipelineDetails(Integer buildNumber, Long buildDuration, String buildStatus, Timestamp endTime) {
        this.buildNumber = buildNumber;
        this.buildDuration = buildDuration;
        this.buildStatus = buildStatus;
        this.endTime = endTime;
    }
}

class PipelineDetailsMap {
    @JsonProperty("pipelineName")
    private String pipelineName;

    @JsonProperty("pipelineDetails")
    private List<PipelineDetails> pipelineDetails;

    public PipelineDetailsMap(String pipelineName, List<PipelineDetails> pipelineDetails) {
        this.pipelineName = pipelineName;
        this.pipelineDetails = pipelineDetails;
    }
}
