package com.browserstack.automate.ci.common;

import java.util.Map;
import java.util.Set;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildVariableContributor;

/**
 * Description : For injecting environment variables to be accessed by next build step.
 *
 */
@Extension
public class BrowserStackBuildVariableContributor extends BuildVariableContributor {

  @Override
  public void buildVariablesFor(AbstractBuild build, Map<String, String> variables) {
    VariableInjectorAction action = build.getAction(VariableInjectorAction.class);
    if (action != null) {
      Set<String> envMapKeySet = action.getEnvMapKeys();
      if (envMapKeySet != null) {
        for (String key : envMapKeySet) {
          variables.put(key, action.getEnvValue(key));
        }
      }
    }
  }
}
