package com.browserstack.automate.ci.jenkins;

import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.Exported;

import com.browserstack.automate.AutomateClient;
import com.browserstack.automate.ci.common.analytics.Analytics;
import com.browserstack.automate.ci.jenkins.BrowserStackBuildWrapper.BuildWrapperItem;
import com.browserstack.automate.exception.AutomateException;
import com.browserstack.automate.exception.SessionNotFound;
import com.browserstack.automate.model.Session;
import com.browserstack.client.exception.BrowserStackException;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestAction;

/**
 * A {@link TestAction} extension to display the BrowserStack Automate video for the session.
 *
 * @author Shirish Kamath
 * @author Anirudha Khanna
 */
public class AutomateTestAction extends TestAction {

    private final CaseResult caseResult;
    private final String sessionId;
    private final Run<?, ?> run;
    private transient BrowserStackException lastException;

    public AutomateTestAction(Run<?, ?> run, CaseResult caseResult, String sessionId) {
        this.run = run;
        this.caseResult = caseResult;
        this.sessionId = sessionId;
    }

    @Exported
    public String getLastError() {
        return (lastException != null) ? lastException.getMessage() : null;
    }

    // For testing only.
    BrowserStackException getLastException() {
        return this.lastException;
    }

    @Exported
    public Session getSession() {
        if (sessionId == null || sessionId.isEmpty() || run == null) {
            return null;
        }

        BuildWrapperItem<BrowserStackBuildWrapper> wrapperItem = BrowserStackBuildWrapper.findBrowserStackBuildWrapper(run.getParent());
        if (wrapperItem == null || wrapperItem.buildWrapper == null) {
            return null;
        }

        BrowserStackCredentials credentials = BrowserStackCredentials.getCredentials(wrapperItem.buildItem, wrapperItem.buildWrapper.getCredentialsId());
        if (credentials == null) {
            return null;
        }

        AutomateClient automateClient = new AutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
        Session activeSession = null;

        try {
            activeSession = automateClient.getSession(this.sessionId);
            Analytics.trackIframeRequest();
        } catch (AutomateException aex) {
            lastException = aex;
            return null;
        } catch (SessionNotFound snfEx) {
            lastException = snfEx;
            return null;
        }

        return activeSession;
    }

    @JavaScriptMethod
    public void iframeLoadTime(int time) {
        Analytics.trackIframeLoad(time);
    }

    @Override
    public String annotate(String text) {
        return text;
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }
}
