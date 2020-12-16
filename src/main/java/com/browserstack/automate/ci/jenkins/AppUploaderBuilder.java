package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.BrowserStackEnvVars;
import com.browserstack.automate.ci.common.logger.PluginLogger;
import com.browserstack.automate.ci.common.uploader.AppUploaderHelper;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class AppUploaderBuilder extends Builder {

    private final String buildFilePath;

    @DataBoundConstructor
    public AppUploaderBuilder(String buildFilePath) {
        this.buildFilePath = buildFilePath;
    }

    public String getBuildFilePath() {
        return buildFilePath;
    }

    @Override
    public boolean perform(@Nonnull AbstractBuild<?, ?> build, @Nonnull Launcher launcher,
                           @Nonnull BuildListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();

        String appId = AppUploaderHelper.uploadApp(build, logger, this.buildFilePath, null);

        if (StringUtils.isEmpty(appId)) {
            return false;
        } else {
            addAppIdToEnvironment(build, appId);
            PluginLogger.log(logger,
                    "Environment variable BROWSERSTACK_APP_ID set with value : " + appId);
            return true;
        }
    }

    // This method is for injecting appId so that next build step can use it.
    private void addAppIdToEnvironment(AbstractBuild<?, ?> build, String appId) {
        VariableInjectorAction variableInjectorAction = build.getAction(VariableInjectorAction.class);
        if (variableInjectorAction == null) {
            variableInjectorAction = new VariableInjectorAction(new HashMap<String, String>());
            build.addAction(variableInjectorAction);
        }
        Map<String, String> envVariables = new HashMap<String, String>();
        envVariables.put(BrowserStackEnvVars.BROWSERSTACK_APP_ID, appId);
        variableInjectorAction.overrideAll(envVariables);
    }

    @Extension
    public static class AppUploaderDescriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Upload App to BrowserStack";
        }

        // Check if buildFilePath and its extension is valid or not.
        public FormValidation doCheckBuildFilePath(@QueryParameter String value) {
            return AppUploaderHelper.validateAppPath(value);
        }
    }
}
