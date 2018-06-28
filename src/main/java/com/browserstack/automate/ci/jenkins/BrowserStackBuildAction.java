package com.browserstack.automate.ci.jenkins;

import hudson.model.Action;

public class BrowserStackBuildAction implements Action {
  
  private BrowserStackCredentials browserStackCredentials;

  public BrowserStackBuildAction(BrowserStackCredentials browserStackCredentials) {
    super();
    this.browserStackCredentials = browserStackCredentials;
  }

  public BrowserStackCredentials getBrowserStackCredentials() {
    return browserStackCredentials;
  }

  public void setBrowserStackCredentials(BrowserStackCredentials browserStackCredentials) {
    this.browserStackCredentials = browserStackCredentials;
  }

  @Override
  public String getIconFileName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getDisplayName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getUrlName() {
    // TODO Auto-generated method stub
    return null;
  }

  
}
