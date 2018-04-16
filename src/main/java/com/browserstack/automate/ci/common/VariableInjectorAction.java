package com.browserstack.automate.ci.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.kohsuke.stapler.StaplerProxy;
import hudson.model.Run;
import jenkins.model.RunAction2;

/**
 *
 * Description : This class is for injecting environment variables.
 */
public class VariableInjectorAction implements RunAction2, StaplerProxy {

  protected transient @CheckForNull Map<String, String> envMap;
  private transient @CheckForNull Run<?, ?> build;

  public VariableInjectorAction(Map<String, String> envMap) {
    this.envMap = envMap;
  }

  @Override
  public String getIconFileName() {
    return Constants.VariableInjectorAction.ICON_FILE_NAME;
  }

  @Override
  public String getDisplayName() {
    return Constants.VariableInjectorAction.DISPLAY_NAME;
  }

  @Override
  public String getUrlName() {
    return null;
  }

  @Override
  public Object getTarget() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void onAttached(Run<?, ?> r) {
    build = r;

  }

  @Override
  public void onLoad(Run<?, ?> r) {
    build = r;
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
}
