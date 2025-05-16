package com.browserstack.automate.ci.jenkins.integrationService;

import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import hudson.model.Action;
import hudson.model.Run;
import org.json.JSONObject;
import okhttp3.*;

import javax.xml.bind.annotation.XmlType;
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
    if (reportHtml == null || reportHtml.equals(REPORT_IN_PROGRESS) || reportHtml.equals(RETRY_REPORT)) {
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
      String ciReportUrlWithParams = requestsUtil.buildQueryParams(reportUrl, params);
      log(logger, "Fetching browserstack report " + reportName);
      Response response = requestsUtil.makeRequest(ciReportUrlWithParams, credentials);
      if (response.isSuccessful()) {
        assert response.body() != null;
        JSONObject reportResponse = new JSONObject(response.body().string());
        String reportStatus = reportResponse.optString("reportStatus");
        if (reportStatus.equalsIgnoreCase(String.valueOf(BrowserStackReportStatus.COMPLETED)) ||
                reportStatus.equalsIgnoreCase(String.valueOf(BrowserStackReportStatus.TEST_AVAILABLE)) ||
                reportStatus.equalsIgnoreCase(String.valueOf(BrowserStackReportStatus.NOT_AVAILABLE))) {

          String defaultHTML = "<h1>No Report Found</h1>";
          JSONObject report = reportResponse.optJSONObject("report");
          reportHtml = report != null ? report.optString("report_html", defaultHTML) : defaultHTML;
          reportStyle = report != null ? report.optString("report_style", "") : "";

        } else if (reportStatus.equalsIgnoreCase(String.valueOf(BrowserStackReportStatus.IN_PROGRESS))) {

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
    return this.reportName;
  }

  @Override
  public String getUrlName() {
    return this.urlName;
  }

}
