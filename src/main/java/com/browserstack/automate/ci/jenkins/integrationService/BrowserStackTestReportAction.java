package com.browserstack.automate.ci.jenkins.integrationService;

import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.google.gson.Gson;
import hudson.model.Action;
import hudson.model.Run;
import org.json.JSONObject;
import okhttp3.*;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;
import static com.browserstack.automate.ci.common.logger.PluginLogger.logError;

public class BrowserStackTestReportAction implements Action {

  private Run<?, ?> run;
  private BrowserStackCredentials credentials;
  private String reportHtml;

  private String buildName;
  private String buildCreatedAt;
  private final transient PrintStream logger;
  private String reportStyle;
  public String reportName;
  public String urlName;

  private int maxRetryReportAttempt;

  private static final String DEFAULT_REPORT_TIMEOUT = "120";
  private static final String REPORT_IN_PROGRESS = "REPORT_IN_PROGRESS";
  private static final String REPORT_FAILED = "REPORT_FAILED";

  private static final String RETRY_REPORT = "RETRY_REPORT";

  private static final String RATE_LIMIT = "RATE_LIMIT";

  private static final int MAX_ATTEMPTS = 3;
  private RequestsUtil requestsUtil;

  public BrowserStackTestReportAction(Run<?, ?> run, BrowserStackCredentials credentials, String buildName, String builldCreatedAt, final PrintStream logger) {
    super();
    setBuild(run);
    this.credentials = credentials;
    this.buildName = buildName;
    this.buildCreatedAt = builldCreatedAt;
    this.reportHtml = null;
    this.reportStyle = "";
    maxRetryReportAttempt = MAX_ATTEMPTS;
    this.logger = logger;

    this.requestsUtil = new RequestsUtil();
  }


  public String getReportHtml() {
    fetchReportConditions();
    return reportHtml;
  }

  public String getReportStyle() {
    fetchReportConditions();
    return reportStyle;
  }

  private void fetchReportConditions() {
    if (reportHtml == null || reportHtml.equals(REPORT_IN_PROGRESS) || reportHtml.equals(RETRY_REPORT) || reportHtml.equals(RATE_LIMIT)) {
      fetchReport();
    }
  }

  private void fetchReport() {
    Map<String, String> params = new HashMap<>();
    String pollValue = "FIRST";
    params.put("buildCreatedAt", buildCreatedAt);
    params.put("buildName", buildName);
    params.put("requestingCi", Constants.INTEGRATIONS_TOOL_KEY);
    params.put("reportFormat", Arrays.asList(Constants.REPORT_FORMAT).toString());
    params.put("requestType", pollValue);
    params.put("userTimeout", DEFAULT_REPORT_TIMEOUT);

    try {
      String reportUrl = Constants.CAD_BASE_URL + Constants.BROWSERSTACK_CONFIG_DETAILS_ENDPOINT;
      Gson gson = new Gson();
      String json = gson.toJson(params);
      RequestBody ciReportBody = RequestBody.create(MediaType.parse("application/json"), json);
      log(logger, "Fetching browserstack report " + reportName);
      Response response = requestsUtil.makeRequest(reportUrl, credentials, ciReportBody);
      if (response.isSuccessful()) {
        assert response.body() != null;
        JSONObject reportResponse = new JSONObject(response.body().string());
        String reportStatus = reportResponse.optString("reportStatus");
        if (reportStatus.equalsIgnoreCase(String.valueOf(BrowserStackReportStatus.COMPLETED)) ||
                reportStatus.equalsIgnoreCase(String.valueOf(BrowserStackReportStatus.TEST_AVAILABLE)) ||
                reportStatus.equalsIgnoreCase(String.valueOf(BrowserStackReportStatus.NOT_AVAILABLE))) {

          String defaultHTML = "<h1>No Report Found</h1>";
          JSONObject report = reportResponse.optJSONObject("report");
          reportHtml = report != null ? report.optString("richHtml", defaultHTML) : defaultHTML;
          reportStyle = report != null ? report.optString("richCss", "") : "";

        } else if (reportStatus.equalsIgnoreCase(String.valueOf(BrowserStackReportStatus.IN_PROGRESS))) {

          reportHtml = REPORT_IN_PROGRESS;

        } else {
          reportHtml = REPORT_FAILED;
        }
      } else if (response.code() == 429) {
        reportHtml = RATE_LIMIT;
      } else {
        reportHtml = REPORT_FAILED;
        logError(logger, "Received Non success response while fetching report" + response.code());
      }
    } catch (Exception e) {
      reportHtml = RETRY_REPORT;
      this.maxRetryReportAttempt--;
      if (this.maxRetryReportAttempt < 0) {
        reportHtml = REPORT_FAILED;
      }
      logError(logger, "Exception while fetching the report" + Arrays.toString(e.getStackTrace()));
    }
  }

  public boolean isReportInProgress() {
    return reportHtml.equals(REPORT_IN_PROGRESS);
  }

  public boolean isReportFailed() {
    return reportHtml.equals(REPORT_FAILED);
  }

  public boolean reportRetryRequired() {
    return reportHtml.equals(RETRY_REPORT);
  }

  public boolean isUserRateLimited() { return  reportHtml.equals(RATE_LIMIT); }

  public boolean isReportAvailable() {
    if (reportHtml != null && !reportHtml.equals(REPORT_IN_PROGRESS) && !reportHtml.equals(REPORT_FAILED) && !reportHtml.equals(RETRY_REPORT)) {
      return true;
    }
    return false;
  }

  public boolean reportHasStatus() {
    if (reportHtml == null) return false;
    return reportHtml.equals(REPORT_IN_PROGRESS) || reportHtml.equals(REPORT_FAILED);
  }

  public Run<?, ?> getBuild() {
    return run;
  }

  public void setBuild(Run<?, ?> build) {
    this.run = build;
  }

  @Override
  public String getIconFileName() {
    return Constants.BROWSERSTACK_LOGO;
  }

  @Override
  public String getDisplayName() {
    return Constants.BROWSERSTACK_CAD_REPORT_DISPLAY_NAME;
  }

  @Override
  public String getUrlName() {
    return Constants.BROWSERSTACK_TEST_REPORT_URL;
  }

}
