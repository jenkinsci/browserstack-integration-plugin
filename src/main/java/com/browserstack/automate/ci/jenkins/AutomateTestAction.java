package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.AutomateClient;
import com.browserstack.automate.ci.common.analytics.Analytics;
import com.browserstack.automate.ci.jenkins.BrowserStackBuildWrapper.BuildWrapperItem;
import com.browserstack.automate.exception.AutomateException;
import com.browserstack.automate.model.Session;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestAction;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.Exported;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutomateTestAction extends TestAction {
    public final CaseResult caseResult;
    private final List<String> sessionIds;
    private final Run<?, ?> run;
    private transient AutomateException lastException;

    public AutomateTestAction(Run<?, ?> run, CaseResult caseResult) {
        this.run = run;
        this.caseResult = caseResult;
        this.sessionIds = new ArrayList<String>();
    }

    public void addSession(final String sessionId) {
        if (sessionId != null) {
            sessionIds.add(sessionId);
        }
    }

    @Exported
    public String getLastError() {
        return (lastException != null) ? lastException.getMessage() : null;
    }

    @Exported
    public List<Session> getSessions() {
        if (!sessionIds.isEmpty() && run != null) {
            BuildWrapperItem<BrowserStackBuildWrapper> wrapperItem = BrowserStackBuildWrapper.findBrowserStackBuildWrapper(run.getParent());
            if (wrapperItem != null && wrapperItem.buildWrapper != null) {
                BrowserStackCredentials credentials = BrowserStackCredentials.getCredentials(wrapperItem.buildItem, wrapperItem.buildWrapper.getCredentialsId());
                if (credentials != null) {
                    AutomateClient automateClient = new AutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
                    List<Session> activeSessions = new ArrayList<Session>();

                    for (String sessionId : sessionIds) {
                        try {
                            activeSessions.add(automateClient.getSession(sessionId));
                        } catch (AutomateException e) {
                            lastException = e;
                            return null;
                        } catch (Exception e) {
                            // ignore
                        }
                    }

                    if (!activeSessions.isEmpty()) {
                        Analytics.trackIframeRequest();
                    }

                    return Collections.unmodifiableList(activeSessions);
                }
            }
        }

        return null;
    }

    @Exported
    public String getLogs(Session session) {
        try {
            return session.getLogs();
        } catch (Exception e) {
            // ignore
        }

        return "View Logs: " + session.getLogUrl();
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
