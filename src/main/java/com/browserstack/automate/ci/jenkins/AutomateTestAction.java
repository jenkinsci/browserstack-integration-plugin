package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.AutomateClient;
import com.browserstack.automate.ci.jenkins.BrowserStackBuildWrapper.BuildWrapperItem;
import com.browserstack.automate.exception.AutomateException;
import com.browserstack.automate.model.Session;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestAction;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.export.Exported;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutomateTestAction extends TestAction {
    public final CaseResult caseResult;
    private final List<Session> sessions;
    private final Run<?, ?> run;
    private String lastError;

    public AutomateTestAction(Run<?, ?> run, CaseResult caseResult) {
        this.run = run;
        this.caseResult = caseResult;
        this.sessions = new ArrayList<Session>();
    }

    public void addSession(final Session session) {
        if (session != null) {
            sessions.add(session);
        }
    }

    public boolean isEmpty() {
        return sessions.isEmpty();
    }

    @Exported
    public String getLastError() {
        return lastError;
    }

    @Exported
    public List<Session> getSessions() {
        if (!sessions.isEmpty() && run != null) {
            BuildWrapperItem<BrowserStackBuildWrapper> wrapperItem = BrowserStackBuildWrapper.findBrowserStackBuildWrapper(run.getParent());
            if (wrapperItem != null && wrapperItem.buildWrapper != null) {
                BrowserStackCredentials credentials = BrowserStackCredentials.getCredentials(wrapperItem.buildItem, wrapperItem.buildWrapper.getCredentialsId());
                if (credentials != null) {
                    AutomateClient automateClient = new AutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
                    List<Session> activeSessions = new ArrayList<Session>();

                    for (Session session : sessions) {
                        try {
                            activeSessions.add(automateClient.getSession(session.getId()));
                        } catch (AutomateException e) {
                            lastError = (StringUtils.isNotBlank(e.getMessage())) ? e.getMessage() : null;
                        } catch (Exception e) {
                            // ignore
                        }
                    }

                    return Collections.unmodifiableList(activeSessions);
                }
            }
        }

        return sessions;
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
