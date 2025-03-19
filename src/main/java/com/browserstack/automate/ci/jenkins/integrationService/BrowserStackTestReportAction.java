package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.constants.Constants;
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
  private static final String REPORT_IN_PROGRESS = "REPORT_IN_PROGRESS";
  private static final String REPORT_FAILED = "REPORT_FAILED";
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
    if (reportHtml == null || reportHtml.equals(REPORT_IN_PROGRESS)) {
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
      String CIReportUrlWithParams = requestsUtil.buildQueryParams(reportUrl, params);
      log(logger, "Fetching browserstack report " + reportName);
      Response response = requestsUtil.makeRequest(CIReportUrlWithParams, credentials);
      if (response.isSuccessful()) {
        JSONObject reportResponse = new JSONObject(response.body().string());
        String reportStatus = reportResponse.optString("report_status");
        if (reportStatus.equalsIgnoreCase(String.valueOf(Constants.REPORT_STATUS.COMPLETED))) {
          reportHtml = reportResponse.optString("report_html", null);
          reportStyle = reportResponse.optString("report_style", "");
        } else if (reportStatus.equalsIgnoreCase(String.valueOf(Constants.REPORT_STATUS.IN_PROGRESS))) {
          reportHtml = REPORT_IN_PROGRESS;
        } else {
          reportHtml = REPORT_FAILED;
        }
        logError(logger, "Received Non success response while fetching report" + response.code());
      }
    } catch (Exception e) {
      reportHtml = REPORT_FAILED;
      logError(logger, "Exception while fetching the report" + e.getMessage());
    }
  }

  public boolean isReportInProgress() {
    return reportHtml.equals(REPORT_IN_PROGRESS);
  }

  public boolean isReportFailed() {
    return reportHtml.equals(REPORT_FAILED);
  }

  public boolean isReportAvailable() {
    if (reportHtml != null && !reportHtml.equals(REPORT_IN_PROGRESS) && !reportHtml.equals(REPORT_FAILED)) {
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
