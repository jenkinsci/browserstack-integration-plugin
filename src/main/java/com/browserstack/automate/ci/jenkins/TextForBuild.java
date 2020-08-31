package com.browserstack.automate.ci.jenkins;

import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.browserstack.appautomate.AppAutomateClient;
import com.browserstack.automate.AutomateClient;
import com.browserstack.automate.ci.common.enums.ProjectType;
import com.browserstack.automate.exception.BuildNotFound;
import com.browserstack.automate.exception.SessionNotFound;
import com.browserstack.automate.model.Build;
import com.browserstack.automate.model.Session;
import com.browserstack.client.BrowserStackClient;
import com.browserstack.client.exception.BrowserStackException;

public class TextForBuild extends AbstractTextForBuild {
    /*
     * Holds info about the Selenium Test
     */
    private String buildName;
    private List<String> browserStackBuilds = new ArrayList<String>();
    private List<Session> browserStackSessions;
    private String buildNumber;
    private ProjectType projectType;

    TextForBuild(ProjectType projectType, final String buildName) {
        super();
        this.projectType = projectType;
        this.buildName = buildName;
        setIconFileName(Jenkins.RESOURCE_PATH + "/plugin/browserstack-integration/images/logo.png");
    }

    public void generateBrowserStackReport() {
        System.out.println("INSIDE GENERATE BROWWSERSTACK REPORT...");
        BrowserStackBuildAction browserStackBuildAction = getBuild().getAction(BrowserStackBuildAction.class);
        if (browserStackBuildAction == null) {
            // TODO: add logging here of failure
            System.out.println("browserStackBuildAction not found....");
            return;
        }

        BrowserStackCredentials credentials = browserStackBuildAction.getBrowserStackCredentials();
        if (credentials == null) {
            // TODO: adding logging here
            System.out.println("Credentials not found...");
            return;
        }

        BrowserStackClient client = null;
        if (projectType.equals(ProjectType.APP_AUTOMATE)) {
            client = new AppAutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
        } else {
            System.out.println("Setting client as Automate........");
            client = new AutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
        }

        fetchBrowserStackBuilds(client);
        fetchBrowserStackSessions(client);
    }

    private void fetchBrowserStackBuilds(BrowserStackClient client) {
        // TODO: fetch the buildIDs using the buildName
        // insert the buildIds in the List browserStackBuilds
        System.out.println("Assigning random builds ids");
        this.browserStackBuilds.add("1fe259030726f0ff9a00ac3e49049682a24e00d6");
    }

    private void fetchBrowserStackSessions(BrowserStackClient client) {
        List<Session> browserStackSessions;
        Session a;
        try {
            System.out.println("Trying to fetch sessions........");
            // browserStackSessions = client.getSessions("1fe259030726f0ff9a00ac3e49049682a24e00d6");
            a = client.getSession("dac3ed1bac2d923978cb8a6f416d2fa5f75a1949");
            System.out.println("after fetching session...");
            System.out.println(a);
        } catch (BuildNotFound bnfException) {
            // TODO: add logging
            System.out.println("BuildNotFound exception....");
            System.out.println(bnfException);
            return;
        } catch (SessionNotFound snfException) {
            System.out.println("Some snfException exception");
            System.out.println(snfException);
            return;
        } catch (BrowserStackException bsException) {
            // TODO: add logging
            System.out.println("Some BSTACK exception");
            System.out.println(bsException);
            return;
        }

        // this.browserStackSessions = browserStackSessions;
        
        // System.out.println("CHECK THE SESSIONS HERE ___________________________________________________");
        // System.out.println(this.browserStackSessions);
        System.out.println("CHECK SESSION HERE...");
        System.out.println(a);
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
