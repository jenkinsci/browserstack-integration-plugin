package com.browserstack.automate.ci.jenkins.integrationService;

import com.browserstack.automate.ci.common.BrowserStackEnvVars;
import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.jenkins.BrowserStackBuildAction;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.FilePath;
import hudson.Launcher;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;
import static com.browserstack.automate.ci.common.logger.PluginLogger.logError;

public class BrowserStackTestReportPublisher extends Recorder implements SimpleBuildStep {
  private static final Logger LOGGER = Logger.getLogger(BrowserStackTestReportPublisher.class.getName());
  private static final int MAX_ATTEMPTS = 3;
  private static final int RETRY_DELAY_SECONDS = 5;
  private final Map<String, String> customEnvVars;

  RequestsUtil requestsUtil;

  @DataBoundConstructor
  public BrowserStackTestReportPublisher(Map<String, String> customEnvVars) {
    this.customEnvVars = customEnvVars != null && !customEnvVars.isEmpty() ? new ConcurrentHashMap<>(customEnvVars) : new ConcurrentHashMap<>();
    requestsUtil = new RequestsUtil();
  }

  @Override
  public void perform(Run<?, ?> build, @NonNull FilePath workspace, @NonNull Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
    final PrintStream logger = listener.getLogger();
    log(logger, "Adding BrowserStack Report");

    EnvVars parentEnvs = build.getEnvironment(listener);
    parentEnvs.putAll(getCustomEnvVars());

    String browserStackBuildName = Optional.ofNullable(parentEnvs.get(BrowserStackEnvVars.BROWSERSTACK_BUILD_NAME))
            .orElse(parentEnvs.get(Constants.JENKINS_BUILD_TAG));
    String projectName = parentEnvs.get(BrowserStackEnvVars.BROWSERSTACK_PROJECT_NAME);

    BrowserStackBuildAction buildAction = build.getAction(BrowserStackBuildAction.class);
    if (buildAction == null || buildAction.getBrowserStackCredentials() == null) {
      logError(logger, "No BrowserStackBuildAction or credentials found");
      return;
    }

    BrowserStackCredentials credentials = buildAction.getBrowserStackCredentials();

    LOGGER.info("Adding BrowserStack Report Action");

    String configDetailsEndpoint = Constants.INTEGRATE_BASE_URL + Constants.BROWSERSTACK_CONFIG_DETAILS_ENDPOINT;
    JSONObject reportConfigDetailsResponse = fetchConfigDetails(logger, configDetailsEndpoint, credentials);
    if (reportConfigDetailsResponse == null) {
      logError(logger, "Could not fetch the report config details");
      return;
    }

    JSONArray configDetails = reportConfigDetailsResponse.getJSONArray("config_details");
    String lookUpURL = reportConfigDetailsResponse.getString("lookup_endpoint");
    Date buildTimestamp = new Date(build.getStartTimeInMillis());

    // Format the timestamp (e.g., YYYY-MM-DD HH:MM:SS)
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    String formattedTime = sdf.format(buildTimestamp);

    // Encode the timestamp to make it URL-safe
    String encodedTimestamp = URLEncoder.encode(formattedTime, "UTF-8");

    String UUID = fetchBuildUUID(logger, lookUpURL, credentials, browserStackBuildName, projectName, encodedTimestamp);
    if (UUID == null) {
      logError(logger, "Cannot find a build with name " + browserStackBuildName);
    }

    for (int i = 0; i < configDetails.length(); i++) {
      JSONObject config = configDetails.getJSONObject(i);

      String reportUrl = config.getString("report_fetch_url");
      String reportName = config.getString("report_name");
      String reportTabUrl = config.getString("report_tab_url");
      build.addAction(new BrowserStackTestReportAction(build, credentials, reportUrl, UUID, reportName, reportTabUrl, logger));
    }

  }

  private String fetchBuildUUID(PrintStream logger, String lookUpURL, BrowserStackCredentials credentials, String buildName, String projectName, String encodedTimestamp) throws InterruptedException {

    Map<String, String> params = new HashMap<>();
    params.put("build_name", buildName);
    params.put("pipeline_timestamp", encodedTimestamp);
    params.put("tool", Constants.INTEGRATIONS_TOOL_KEY);
    if (projectName != null) {
      params.put("project_name", projectName);
    }

    log(logger, "Fetching build....");
    String lookUpURLWithParams;

    //constructing build params
    try {
      lookUpURLWithParams = requestsUtil.buildQueryParams(lookUpURL, params);
    } catch (URISyntaxException uriSyntaxException) {
      logError(logger, "Could not build look up url with params" + Arrays.toString(uriSyntaxException.getStackTrace()));
      return null;
    }

    //making attempts
    for (int attempts = 0; attempts < MAX_ATTEMPTS; attempts++) {
      try {
        Response response = requestsUtil.makeRequest(lookUpURLWithParams, credentials);
        if (response.isSuccessful()) {
          assert response.body() != null;
          JSONObject lookUpResponse = new JSONObject(response.body().string());
          String UUID = lookUpResponse.optString("UUID", null);

          if (UUID != null) {
            log(logger, "build found with " + buildName + " and project name " + projectName);
            return UUID;
          }

          LOGGER.info("build not found will retry in sometime.." + lookUpResponse.getString("message"));
        }
      } catch (Exception e) {
        logError(logger, "Attempt " + (attempts + 1) + " failed: " + Arrays.toString(e.getStackTrace()));
      }
      TimeUnit.SECONDS.sleep(RETRY_DELAY_SECONDS);
    }

    LOGGER.info("build not found even after " + MAX_ATTEMPTS + "attempts");
    return null;
  }

  private JSONObject fetchConfigDetails(PrintStream logger, String configDetailsURL, BrowserStackCredentials credentials) {
    log(logger, "Fetching config details for reports");
    Map<String, String> params = new HashMap<>();
    params.put("tool", Constants.INTEGRATIONS_TOOL_KEY);
    params.put("operation", Constants.REPORT_CONFIG_OPERATION_NAME);
    try {
      String configDetailsURLWithParams = requestsUtil.buildQueryParams(configDetailsURL, params);
      Response response = requestsUtil.makeRequest(configDetailsURLWithParams, credentials);
      if (response.isSuccessful()) {
        assert response.body() != null;
        return new JSONObject(response.body().string());
      }
      logError(logger, "Failed to fetch config details: " + response.code());
    } catch (Exception e) {
      logError(logger, "Exception occurred while fetching config details: " + Arrays.toString(e.getStackTrace()));
    }
    return null;
  }

  public Map<String, String> getCustomEnvVars() {
    return customEnvVars;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  @Symbol("browserStackBuildTestReports")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      // indicates that this builder can be used with all kinds of project types
      return true;
    }

    @Override
    public String getDisplayName() {
      return Constants.BROWSERSTACK_TEST_REPORT_DISPLAY_NAME;
    }

  }
}
