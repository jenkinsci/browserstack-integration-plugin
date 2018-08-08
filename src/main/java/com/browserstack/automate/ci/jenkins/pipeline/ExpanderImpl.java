package com.browserstack.automate.ci.jenkins.pipeline;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import hudson.EnvVars;

public class ExpanderImpl extends EnvironmentExpander{

  private static final long serialVersionUID = 1;
  private final Map<String, String> overrides;

  ExpanderImpl(HashMap<String, String> overrides) {
      this.overrides = overrides;
  }

  @Override
  public void expand(@Nonnull EnvVars env) throws IOException, InterruptedException {
      env.overrideAll(overrides);
  }
  

}
