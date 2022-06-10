package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.common.enums.ProjectType;
import com.browserstack.automate.ci.common.tracking.PluginsTracker;
import hudson.model.Run;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

import static com.browserstack.automate.ci.common.logger.PluginLogger.logError;

public class BrowserStackCypressReportForBuild extends AbstractBrowserStackCypressReportForBuild {
    private static PrintStream logger;
    private final String buildName;
    private final transient JSONObject result;
    private final Map<String, String> resultAggregation;
    private final ProjectType projectType;
    // to make them available in jelly
    private final String passedConst = Constants.SessionStatus.PASSED;
    private final String failedConst = Constants.SessionStatus.FAILED;
    private final transient PluginsTracker tracker;
    private final boolean pipelineStatus;

    public BrowserStackCypressReportForBuild(final Run<?, ?> build,
                                             final ProjectType projectType,
                                             final String buildName,
                                             final PrintStream logger,
                                             final PluginsTracker tracker,
                                             final boolean pipelineStatus) {
        super();
        setBuild(build);
        this.buildName = buildName;
        this.result = new JSONObject();
        this.resultAggregation = new HashMap<>();
        this.projectType = projectType;
        this.logger = logger;
        this.tracker = tracker;
        this.pipelineStatus = pipelineStatus;
    }


    public JSONObject getCypressMatrix(String filepath) {
        JSONObject report = null;
        final String reportJSONPath = filepath + "/results/browserstack-cypress-report.json";

        try {
            InputStream is = new FileInputStream(reportJSONPath);
            String jsonTxt = IOUtils.toString(is, "UTF-8");
            report = new JSONObject(jsonTxt);
        } catch (FileNotFoundException e) {
            logError(logger, "Cypress report not found at " + reportJSONPath);
            tracker.sendError("BrowserStack Cypress Report Not Found", pipelineStatus, "CypressReportGeneration");
        } catch (IOException e) {
            logError(logger, "There was a problem while reading report files");
            tracker.sendError(e.toString(), pipelineStatus, "CypressReportGeneration");
        }
        return report;
    }

    public boolean generateBrowserStackCypressReport(String workspace) {
        if (result.length() == 0) {
            JSONObject matrix = getCypressMatrix(workspace);

            if(matrix == null) {
                return false;
            }

            String buildNameWithBuildNumber = matrix.optString("build_name");
            int indexOfBuildNumberSeparator = buildNameWithBuildNumber.lastIndexOf(": ") == -1 ? buildNameWithBuildNumber.length()
                    : buildNameWithBuildNumber.lastIndexOf(": ");
            String buildNameWithoutBuildNumber = buildNameWithBuildNumber.substring(0, indexOfBuildNumberSeparator);

            if (buildNameWithoutBuildNumber == null) {
                logError(logger, "BrowserStack Cypress Report not generated, result json may have been corrupted. Please retry.");
                tracker.sendError("Report not generated", pipelineStatus, "CypressReportGeneration");
                return false;
            }

            if (!buildNameWithoutBuildNumber.equalsIgnoreCase(this.buildName)) {
                logError(logger, "BrowserStack Cypress Report not generated, build name mismatch.");
                tracker.sendError("Report not generated", pipelineStatus, "CypressReportGeneration");
                return false;
            }

            generateResult(matrix);

            if (result.length() > 0) {
                generateAggregationInfo();
                return true;
            }
            return false;
        }
        return true;
    }

    private void generateResult(JSONObject matrix) {
        result.put("buildName", matrix.getString("build_name"));
        result.put("buildId", matrix.getString("build_id"));
        result.put("projectName", matrix.getString("project_name"));
        result.put("buildUrl", matrix.getString("build_url"));

        JSONArray specs = new JSONArray();
        JSONObject rows = matrix.getJSONObject("rows");
        rows.keySet().forEach(specName ->
        {
            JSONObject spec = new JSONObject();
            JSONObject specData = rows.getJSONObject(specName);
            JSONObject specMeta = specData.getJSONObject("meta");

            spec.put("name", specName);
            spec.put("path", specData.getString("path"));

            // Meta
            spec.put("total", specMeta.getInt("total"));
            spec.put("failed", specMeta.getInt("failed"));
            spec.put("passed", specMeta.getInt("passed"));

            // Sessions
            JSONArray sessions = specData.getJSONArray("sessions");
            spec.put("sessions", sessions);

            specs.put(spec);
        });

        result.put("specs", specs);
    }

    private void generateAggregationInfo() {
        int totalSpecs = 0, totalErrors = 0;

        JSONArray specs = result.getJSONArray("specs");

        for(int i = 0; i < specs.length(); i++){
            JSONObject spec = specs.getJSONObject(i);
            totalSpecs += spec.getInt("total");
            totalErrors += spec.getInt("failed");
        }

        resultAggregation.put("totalSpecs", String.valueOf(totalSpecs));
        resultAggregation.put("totalErrors", String.valueOf(totalErrors));
    }

    public JSONObject getResult() {
        return result;
    }

    public Map<String, String> getResultAggregation() {
        return resultAggregation;
    }

    public String getBuildName() {
        return buildName;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public String getPassedConst() {
        return passedConst;
    }

    public String getFailedConst() {
        return failedConst;
    }
}
