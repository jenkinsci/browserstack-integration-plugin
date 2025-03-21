package com.browserstack.automate.ci.jenkins.integrationService;

import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import hudson.model.Action;
import hudson.model.Run;
import org.json.JSONObject;
import okhttp3.*;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;
import static com.browserstack.automate.ci.common.logger.PluginLogger.logError;

public class BrowserStackTestReportAction implements Action {

  private Run<?, ?> run;
  private BrowserStackCredentials credentials;
  private String reportUrl;
  private final String UUID;
  private String reportHtml;
  private final transient PrintStream logger;
  private String reportStyle;
  private String reportName;
  private String urlName;

  private int maxRetryReportAttempt;
  private static final String REPORT_IN_PROGRESS = "REPORT_IN_PROGRESS";
  private static final String REPORT_FAILED = "REPORT_FAILED";

  private static final String RETRY_REPORT = "RETRY_REPORT";

  private static final int MAX_ATTEMPTS = 3;
  private static final OkHttpClient client = new OkHttpClient();
  RequestsUtil requestsUtil;

  public BrowserStackTestReportAction(Run<?, ?> run, BrowserStackCredentials credentials, String reportUrl, String UUID, String reportName, String tabUrl, final PrintStream logger) {
    super();
    setBuild(run);
    this.credentials = credentials;
    this.reportUrl = reportUrl;
    this.UUID = UUID;
    this.reportHtml = null;
    this.reportStyle = "";
    this.logger = logger;
    this.reportName = reportName;
    this.urlName = tabUrl;
    this.maxRetryReportAttempt = MAX_ATTEMPTS;

    requestsUtil = new RequestsUtil();
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
    if (reportHtml == null || reportHtml.equals(REPORT_IN_PROGRESS) || reportHtml.equals(RETRY_REPORT)) {
      fetchReport();
    }
  }

  private void fetchReport() {
    if (UUID == null) {
      reportHtml = REPORT_FAILED;
      return;
    }
    Map<String, String> params = new HashMap<>();
    params.put("UUID", UUID);
    params.put("report_name", reportName);
    params.put("tool", Constants.INTEGRATIONS_TOOL_KEY);

    try {
      String ciReportUrlWithParams = requestsUtil.buildQueryParams(reportUrl, params);
      log(logger, "Fetching browserstack report " + reportName);
      Response response = requestsUtil.makeRequest(ciReportUrlWithParams, credentials);
      if (response.isSuccessful()) {
        JSONObject reportResponse = new JSONObject(response.body().string());
        String reportStatus = reportResponse.optString("report_status");
        if (reportStatus.equalsIgnoreCase(String.valueOf(Constants.REPORT_STATUS.COMPLETED))) {
          String defaultHTML = "<h1>No Report Found</h1>";
          reportHtml = reportResponse.optString("report_html", defaultHTML);
          reportStyle = reportResponse.optString("report_style", "");
        } else if (reportStatus.equalsIgnoreCase(String.valueOf(Constants.REPORT_STATUS.IN_PROGRESS))) {
          reportHtml = REPORT_IN_PROGRESS;
        } else {
          reportHtml = REPORT_FAILED;
        }
        logError(logger, "Received Non success response while fetching report" + response.code());
      }
    } catch (Exception e) {
      reportHtml = RETRY_REPORT;
      this.maxRetryReportAttempt--;
      if (this.maxRetryReportAttempt < 0) {
        reportHtml = REPORT_FAILED;
      }
      logError(logger, "Exception while fetching the report" + e.getMessage());
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

  public boolean isReportAvailable() {
    if (reportHtml != null && !reportHtml.equals(REPORT_IN_PROGRESS) && !reportHtml.equals(REPORT_FAILED) && !reportHtml.equals(RETRY_REPORT)) {
      return true;
    }
    return false;
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
    return this.reportName;
  }

  @Override
  public String getUrlName() {
    return this.urlName;
  }

}
