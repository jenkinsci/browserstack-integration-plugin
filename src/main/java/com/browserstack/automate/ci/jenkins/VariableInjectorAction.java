package com.browserstack.automate.ci.jenkins;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.kohsuke.stapler.StaplerProxy;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Run;
import jenkins.model.RunAction2;

/**
 *
 * Description : This class is for injecting environment variables.
 */
public class VariableInjectorAction implements EnvironmentContributingAction {

  protected transient @CheckForNull Map<String, String> envMap;
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

  public String getEnvValue(String key) {
    return this.envMap.get(key);
  }

  @Override
  public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
    env.putAll(this.envMap);
  }
}
