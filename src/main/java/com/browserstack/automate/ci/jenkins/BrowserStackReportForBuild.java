package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.Tools;
import com.browserstack.automate.model.Build;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;
import com.browserstack.appautomate.AppAutomateClient;
import com.browserstack.automate.AutomateClient;
import com.browserstack.automate.ci.common.enums.ProjectType;
import com.browserstack.automate.exception.BuildNotFound;
import com.browserstack.automate.model.Session;
import com.browserstack.client.BrowserStackClient;
import com.browserstack.client.exception.BrowserStackException;

import hudson.model.Run;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class BrowserStackReportForBuild extends AbstractBrowserStackReportForBuild {
    /*
     * Holds info about the Selenium Test
     */
    private final String buildName;
    private Build browserStackBuild;
    private final List<Session> browserStackSessions;
    private final List<JSONObject> result;
    private final List<JSONObject> resultMeta;
    private final JSONObject resultAggregation;
    private final ProjectType projectType;
    private final PrintStream logger;
    private static final int RESULT_META_MAX_SIZE = 5;

    public BrowserStackReportForBuild(final Run<?, ?> build, final ProjectType projectType, final String buildName, final PrintStream logger) {
        super();
        setBuild(build);
        this.buildName = buildName;
        this.browserStackSessions = new ArrayList<>();
        this.result = new ArrayList<>();
        this.resultMeta = new ArrayList<>();
        this.resultAggregation = new JSONObject();
        this.projectType = projectType;
        this.logger = logger;
        fetchBuildAndSessions();
    }

    private void fetchBuildAndSessions() {
        final BrowserStackBuildAction browserStackBuildAction = getBuild().getAction(BrowserStackBuildAction.class);
        if (browserStackBuildAction == null) {
            log(logger, "Error: No BrowserStackBuildAction found");
            return;
        }

        final BrowserStackCredentials credentials = browserStackBuildAction.getBrowserStackCredentials();
        if (credentials == null) {
            log(logger, "Error: BrowserStack credentials could not be fetched");
            return;
        }

        BrowserStackClient client;
        if (projectType == ProjectType.APP_AUTOMATE) {
            client = new AppAutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
        } else {
            client = new AutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
        }

        this.browserStackBuild = fetchBrowserStackBuild(client, this.buildName);
        if (this.browserStackBuild != null) {
            this.browserStackSessions.addAll(fetchBrowserStackSessions(client, this.browserStackBuild.getId()));
        }
    }

    public boolean generateBrowserStackReport() {
        if (this.result.size() == 0) {
            this.result.addAll(generateSessionsCollection(this.browserStackSessions));

            if (this.result.size() > 0) {
                this.result.sort(new SessionsSortingComparator());
                this.resultMeta.addAll(this.result.subList(0, Math.min(this.result.size(), RESULT_META_MAX_SIZE)));
                generateAggregationInfo();
                return true;
            }
            return false;
        }
        return true;
    }

    private Build fetchBrowserStackBuild(@Nonnull BrowserStackClient client, @Nonnull String buildName) {
        Build build = null;
        try {
            build = client.getBuildByName(buildName);
        } catch (BuildNotFound bnfException) {
            log(this.logger, "No build found by name: " + buildName);
        } catch (BrowserStackException bstackException) {
            log(this.logger, "BrowserStackException occurred while fetching build: " + bstackException.toString());
        }

        return build;
    }

    private List<Session> fetchBrowserStackSessions(@Nonnull BrowserStackClient client, @Nonnull String buildId) {
        final List<Session> browserStackSessions = new ArrayList<Session>();
        try {
            browserStackSessions.addAll(client.getSessions(buildId));
        } catch (BuildNotFound bnfException) {
            log(this.logger, "No build found while fetching sessions for the buildId: " + buildId);
        } catch (BrowserStackException bstackException) {
            log(this.logger, "BrowserStackException occurred while fetching sessions: " + bstackException.toString());
        }

        return browserStackSessions;
    }

    private List<JSONObject> generateSessionsCollection(List<Session> browserStackSessions) {
        return browserStackSessions.stream().map(this::convertSessionToJsonObject).collect(Collectors.toList());
    }

    private JSONObject convertSessionToJsonObject(Session session) {
        final JSONObject sessionJSON = new JSONObject();

        if (session.getName() == null || session.getName().isEmpty()) {
            sessionJSON.put("name", session.getId());
        } else {
            sessionJSON.put("name", session.getName());
        }

        if (session.getDevice() == null || session.getDevice().isEmpty()) {
            sessionJSON.put("browser", session.getBrowser());
        } else {
            sessionJSON.put("browser", session.getDevice());
        }
        sessionJSON.put("os", session.getOs());
        sessionJSON.put("osVersion", session.getOsVersion());
        sessionJSON.put("status", session.getBrowserStackStatus());

        if (session.getBrowserStackStatus().equals(session.getStatus())) {
            sessionJSON.put("userMarked", "UNMARKED");
        } else {
            sessionJSON.put("userMarked", session.getStatus());
        }


        // Condition which shouldn't occur if the build is not being reused elsewhere.
        // But if it happens, the following condition will handle the scenario where
        // duration is null or empty (running session)
        if ("running".equals(session.getStatus())) {
            sessionJSON.put("duration", "-");
        } else {
            sessionJSON.put("duration", Tools.durationToHumanReadable(session.getDuration()));
        }

        sessionJSON.put("createdAt", session.getCreatedAt());
        sessionJSON.put("url", session.getPublicUrl() + "&source=jenkins");
        return sessionJSON;
    }

    private void generateAggregationInfo() {
        final int totalSessions = this.result.size();
        int totalErrors = 0;
        for(JSONObject session: this.result) {
            if (session.getString("status").equals("error")
                    || session.getString("userMarked").equals("failed")) {
                totalErrors++;
            }
        }

        this.resultAggregation.put("totalSessions", totalSessions);
        this.resultAggregation.put("totalErrors", totalErrors);
        this.resultAggregation.put("buildDuration", Tools.durationToHumanReadable(this.browserStackBuild.getDuration()));
    }

    private static class SessionsSortingComparator implements Comparator<JSONObject> {

        @Override
        public int compare(JSONObject sessionOne, JSONObject sessionTwo) {
            // possible values for user_marked: failed, passed and UNMARKED, thus changing all to lowercase
            final String sessionOneUserMarked = sessionOne.getString("userMarked").toLowerCase();
            final String sessionTwoUserMarked = sessionTwo.getString("userMarked").toLowerCase();
            final int userMarkedStatusComparator = sessionOneUserMarked.compareTo(sessionTwoUserMarked);

            // ascending with `user marked status` but descending with `created at`
            if (userMarkedStatusComparator == 0) {
                final Date sessionOneDate = (Date) sessionOne.get("createdAt");
                final Date sessionTwoDate = (Date) sessionTwo.get("createdAt");
                final int createdAtComparator = sessionOneDate.compareTo(sessionTwoDate);

                return createdAtComparator == 0
                        ? userMarkedStatusComparator
                        : (createdAtComparator > 0 ? -1 : 1);
            }
            return userMarkedStatusComparator;
        }
    }

    public List<JSONObject> getResult() {
        return result;
    }

    public List<JSONObject> getResultMeta() {
        return resultMeta;
    }

    public JSONObject getResultAggregation() {
        return resultAggregation;
    }

    public String getBuildName() {
        return buildName;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

}
