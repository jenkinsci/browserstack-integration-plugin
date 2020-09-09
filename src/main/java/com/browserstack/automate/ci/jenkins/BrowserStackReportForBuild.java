package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.model.Build;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;
import com.browserstack.appautomate.AppAutomateClient;
import com.browserstack.automate.AutomateClient;
import com.browserstack.automate.ci.common.enums.ProjectType;
import com.browserstack.automate.exception.BuildNotFound;
import com.browserstack.automate.model.Session;
import com.browserstack.client.BrowserStackClient;
import com.browserstack.client.exception.BrowserStackException;

import org.json.JSONObject;

import javax.annotation.Nonnull;

public class BrowserStackReportForBuild extends AbstractBrowserStackReportForBuild {
    /*
     * Holds info about the Selenium Test
     */
    private String buildName;
    private Build browserStackBuild;
    private List<Session> browserStackSessions;
    private List<JSONObject> result;
    private List<JSONObject> resultMeta;
    private ProjectType projectType;
    private static PrintStream logger;
    private static int RESULT_META_MAX_SIZE = 5;

    BrowserStackReportForBuild(ProjectType projectType, final String buildName, final PrintStream logger) {
        super();
        this.projectType = projectType;
        this.buildName = buildName;
        this.result = new ArrayList<JSONObject>();
        this.resultMeta = new ArrayList<JSONObject>();
        BrowserStackReportForBuild.logger = logger;
    }

    public Boolean generateBrowserStackReport() {
        BrowserStackBuildAction browserStackBuildAction = getBuild().getAction(BrowserStackBuildAction.class);
        if (browserStackBuildAction == null) {
            log(logger, "Error: No BrowserStackBuildAction found");
            return false;
        }

        BrowserStackCredentials credentials = browserStackBuildAction.getBrowserStackCredentials();
        if (credentials == null) {
            log(logger, "Error: BrowserStack credentials could not be fetched");
            return false;
        }

        BrowserStackClient client = null;
        if (projectType.equals(ProjectType.APP_AUTOMATE)) {
            client = new AppAutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
        } else {
            client = new AutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
        }

        this.browserStackBuild = fetchBrowserStackBuild(client, this.buildName);
        if (this.browserStackBuild == null) return false;

        this.browserStackSessions = fetchBrowserStackSessions(client, this.browserStackBuild.getId());
        this.result.addAll(generateSessionsCollection(this.browserStackSessions));

        if (this.result.size() > 0) {
            this.result.sort(new SessionsSortingComparator());
            this.resultMeta.addAll(this.result.subList(0, Math.min(this.result.size(), RESULT_META_MAX_SIZE)));
            return true;
        }

        return false;
    }

    private static Build fetchBrowserStackBuild(@Nonnull BrowserStackClient client, @Nonnull String buildName) {
        Build build = null;
        try {
            build = client.getBuildByName(buildName);
        } catch (BuildNotFound bnfException) {
            log(logger, "No build found by name: " + buildName);
        } catch (BrowserStackException bstackException) {
            log(logger, "BrowserStackException occurred while fetching build: " + bstackException.toString());
        }

        return build;
    }

    private static List<Session> fetchBrowserStackSessions(@Nonnull BrowserStackClient client, @Nonnull String buildId) {
        List<Session> browserStackSessions = new ArrayList<Session>();
        try {
            browserStackSessions.addAll(client.getSessions(buildId));
        } catch (BuildNotFound bnfException) {
            log(logger, "No build found while fetching sessions for the buildId: " + buildId);
        } catch (BrowserStackException bstackException) {
            log(logger, "BrowserStackException occurred while fetching sessions: " + bstackException.toString());
        }

        return browserStackSessions;
    }

    private static List<JSONObject> generateSessionsCollection(List<Session> browserStackSessions) {
        List<JSONObject> sessionsCollection = new ArrayList<JSONObject>();
        for (Session session: browserStackSessions) {
            JSONObject sessionJSON = new JSONObject();

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
            if (session.getStatus().equals("running")) {
                sessionJSON.put("duration", "-");
            } else {
                sessionJSON.put("duration", session.getDuration());
            }

            sessionJSON.put("createdAt", session.getCreatedAt());
            sessionJSON.put("url", session.getPublicUrl() + "&source=jenkins");
            sessionsCollection.add(sessionJSON);
        }

        return sessionsCollection;
    }

    private static class SessionsSortingComparator implements Comparator<JSONObject> {

        @Override
        public int compare(JSONObject sessionOne, JSONObject sessionTwo) {
            // possible values for user_marked: failed, passed and UNMARKED, thus changing all to lowercase
            String sessionOneUserMarked = sessionOne.getString("userMarked").toLowerCase();
            String sessionTwoUserMarked = sessionTwo.getString("userMarked").toLowerCase();
            int userMarkedStatusComparator = sessionOneUserMarked.compareTo(sessionTwoUserMarked);

            // ascending with `user marked status` but descending with `created at`
            if (userMarkedStatusComparator == 0) {
                Date sessionOneDate = (Date) sessionOne.get("createdAt");
                Date sessionTwoDate = (Date) sessionTwo.get("createdAt");
                int createdAtComparator = sessionOneDate.compareTo(sessionTwoDate);

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

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

}
