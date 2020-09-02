package com.browserstack.automate.ci.jenkins;

import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;

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
    private String browserStackBuildId;
    private List<Session> browserStackSessions;
    private List<JSONObject> result;
    private String buildNumber;
    private ProjectType projectType;

    BrowserStackReportForBuild(ProjectType projectType, final String buildName) {
        super();
        this.projectType = projectType;
        this.buildName = buildName;
        setIconFileName(Jenkins.RESOURCE_PATH + "/plugin/browserstack-integration/images/logo.png");
    }

    public void generateBrowserStackReport() {
        BrowserStackBuildAction browserStackBuildAction = getBuild().getAction(BrowserStackBuildAction.class);
        if (browserStackBuildAction == null) {
            // TODO: add logging here of failure
            return;
        }

        BrowserStackCredentials credentials = browserStackBuildAction.getBrowserStackCredentials();
        if (credentials == null) {
            // TODO: adding logging here
            return;
        }

        BrowserStackClient client = null;
        if (projectType.equals(ProjectType.APP_AUTOMATE)) {
            client = new AppAutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
        } else {
            client = new AutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
        }

        this.browserStackBuildId = fetchBrowserStackBuild(client);
        this.browserStackSessions = fetchBrowserStackSessions(client, this.browserStackBuildId);
        generateSessionsCollection();
    }

    static private String fetchBrowserStackBuild(BrowserStackClient client) {
        // TODO: fetch the buildIDs using the buildName
        // insert the buildIds in the List browserStackBuilds
         return "225e89fdb45eba8a5d6e288be77f894c338c4aed";
    }

    static private List<Session> fetchBrowserStackSessions(BrowserStackClient client, @Nonnull String buildId) {
        List<Session> browserStackSessions;
        try {
            browserStackSessions = client.getSessions(buildId, 1000);
        } catch (BuildNotFound bnfException) {
            // TODO: add logging
            browserStackSessions = new ArrayList<Session>();
        } catch (BrowserStackException bsException) {
            // TODO: add logging
            browserStackSessions = new ArrayList<Session>();
        }

        return browserStackSessions;
    }

    private void generateSessionsCollection() {
        List<JSONObject> sessionsCollection = new ArrayList<JSONObject>();
        for (int i = 0; i < this.browserStackSessions.size(); i++) {
            Session session = this.browserStackSessions.get(i);
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
            sessionJSON.put("duration", session.getDuration());
            sessionJSON.put("osVersion", session.getOsVersion());
            sessionJSON.put("status", session.getStatus());
            sessionJSON.put("url", session.getBrowserUrl());
            sessionsCollection.add(sessionJSON);
        }

        this.result = sessionsCollection;
    }

    public List<JSONObject> getResult() {
        return result;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

}
