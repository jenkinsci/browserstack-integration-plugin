package com.browserstack.automate.ci.common;

import java.util.Map;
import java.util.Map.Entry;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildVariableContributor;

/**
 * Description : For injecting environment variables to be accessed by next
 * build step.
 *
 * @author Hitesh Raghuvanshi
 */
@Extension
public class BrowserStackBuildVariableContributor extends BuildVariableContributor {

    @Override
    public void buildVariablesFor(AbstractBuild build, Map<String, String> variables) {
        VariableInjectorAction action = build.getAction(VariableInjectorAction.class);
        if (action != null) {
            Map<String, String> envMap = action.getEnvMap();
            for (Entry<String, String> entry : envMap.entrySet()) {
                variables.put(entry.getKey(), entry.getValue());
            }

        }

    }

}
