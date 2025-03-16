package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.constants.Constants;
import hudson.model.Run;
import hudson.model.Action;

public abstract class AbstractBrowserStackTestReportForBuild implements Action  {
  private Run<?, ?> run;


  @Override
  public String getIconFileName() {
    return Constants.BROWSERSTACK_LOGO;
  }

  @Override
  public abstract  String getDisplayName();

  @Override
  public abstract String getUrlName();

  public Run<?, ?> getBuild() {
    return run;
  }

  public void setBuild(Run<?, ?> build) {
    this.run = build;
  }
}
