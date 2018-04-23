package com.browserstack.automate.ci.jenkins;

import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.Exported;
import com.browserstack.appautomate.AppAutomateClient;
import com.browserstack.automate.AutomateClient;
import com.browserstack.automate.ci.common.analytics.Analytics;
import com.browserstack.automate.ci.common.enums.ProjectType;
import com.browserstack.automate.ci.common.model.BrowserStackSession;
import com.browserstack.automate.ci.jenkins.BrowserStackBuildWrapper.BuildWrapperItem;
import com.browserstack.automate.exception.AppAutomateException;
import com.browserstack.automate.exception.AutomateException;
import com.browserstack.automate.exception.SessionNotFound;
import com.browserstack.automate.model.Session;
import com.browserstack.client.BrowserStackClient;
import com.browserstack.client.exception.BrowserStackException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
  private final BrowserStackSession browserStackSession;
  private final Run<?, ?> run;
  private transient BrowserStackException lastException;

  public AutomateTestAction(Run<?, ?> run, CaseResult caseResult, String sessionStr) {
    this.run = run;
    this.caseResult = caseResult;

    // Generate BrowserStackSession object from jsonobject
    Gson gson = new GsonBuilder().create();
    this.browserStackSession = gson.fromJson(sessionStr, BrowserStackSession.class);
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
    if (this.browserStackSession.getSessionId() == null
        || this.browserStackSession.getSessionId().isEmpty() || run == null) {
      return null;
    }

    BuildWrapperItem<BrowserStackBuildWrapper> wrapperItem =
        BrowserStackBuildWrapper.findBrowserStackBuildWrapper(run.getParent());
    if (wrapperItem == null || wrapperItem.buildWrapper == null) {
      return null;
    }

    BrowserStackCredentials credentials = BrowserStackCredentials
        .getCredentials(wrapperItem.buildItem, wrapperItem.buildWrapper.getCredentialsId());
    if (credentials == null) {
      return null;
    }
    Session activeSession = getSession(credentials, this.browserStackSession.getProjectType());

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

  private Session getSession(BrowserStackCredentials credentials, ProjectType projectType) {
    Session activeSession = null;
    BrowserStackClient client = null;
    if (projectType.equals(ProjectType.APP_AUTOMATE)) {
      client =
          new AppAutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
    } else {
      client = new AutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
    }
    try {
      activeSession = client.getSession(this.browserStackSession.getSessionId());
      Analytics.trackIframeRequest();
    } catch (SessionNotFound snfEx) {
      lastException = snfEx;
      return null;
    } catch (BrowserStackException aex) {
      if (aex instanceof AppAutomateException) {
        lastException = new AppAutomateException(aex);
      } else {
        lastException = new AutomateException(aex);
      }
      return null;
    }
    return activeSession;
  }
}
