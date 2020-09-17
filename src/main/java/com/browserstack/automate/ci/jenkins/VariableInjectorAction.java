package com.browserstack.automate.ci.jenkins;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Run;

import javax.annotation.CheckForNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * Description : This class is for injecting environment variables.
 */
public class VariableInjectorAction implements EnvironmentContributingAction {

  protected transient @CheckForNull Map<String, String> envMap = new HashMap<String, String>();;
  private transient @CheckForNull Run<?, ?> build;

  public VariableInjectorAction(Map<String, String> envMap) {
    this.envMap = envMap;
  }

  @Override
  public String getIconFileName() {
    return null;
  }

  @Override
  public String getDisplayName() {
    return null;
  }

  @Override
  public String getUrlName() {
    return null;
  }

  public void overrideAll(Map<String, String> newEnvMap) {
    if (envMap == null) {
      envMap = new HashMap<String, String>();
    }
    envMap.putAll(newEnvMap);
  }

  public Set<String> getEnvMapKeys() {
    if (envMap == null) {
      return null;
    }
    return envMap.keySet();
  }

  @Override
  public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
    if (this.envMap != null && env != null) {
      env.putAll(this.envMap);
    }
  }
}
