package com.browserstack.automate.ci.jenkins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.browserstack.automate.ci.common.VariableInjectorAction;
import com.browserstack.automate.ci.common.logger.PluginLogger;
import com.browserstack.automate.ci.common.uploader.AppUploader;
import com.browserstack.automate.ci.jenkins.BrowserStackBuildWrapper.BuildWrapperItem;
import com.browserstack.automate.exception.AppAutomateException;
import com.browserstack.automate.exception.InvalidFileExtensionException;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

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
    public boolean perform(Build<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        BuildWrapperItem<BrowserStackBuildWrapper> wrapperItem = BrowserStackBuildWrapper
                .findBrowserStackBuildWrapper(build.getParent());
        if (wrapperItem == null || wrapperItem.buildWrapper == null) {
            PluginLogger.logDebug(listener.getLogger(), "Was not able to fetch build wrapper item.");
            return false;
        }

        BrowserStackCredentials credentials = BrowserStackCredentials.getCredentials(wrapperItem.buildItem,
                wrapperItem.buildWrapper.getCredentialsId());
        if (credentials == null) {
            PluginLogger.logDebug(listener.getLogger(), "Was not able to fetch credentials in AppUploadBuilder.");
            return false;
        }

        AppUploader appUploader = new AppUploader(buildFilePath, credentials, listener);

        try {
            PluginLogger.log(listener.getLogger(), "Uploading app " + this.buildFilePath + " to Browserstack.");
            String appId = appUploader.uploadFile();
            PluginLogger.log(listener.getLogger(), this.buildFilePath + " uploaded successfully to Browserstack with app_url : " + appId);
            addAppIdToEnvironment(build, appId);
            PluginLogger.log(listener.getLogger(), "Environment variable BROWSERSTACK_APP_ID set with value : " + appId);
            return true;
        } catch (AppAutomateException e) {
            PluginLogger.log(listener.getLogger(), "App upload failed.");
            PluginLogger.log(listener.getLogger(), e.getMessage());
        } catch (InvalidFileExtensionException e) {
            PluginLogger.log(listener.getLogger(), e.getMessage());
        } catch (FileNotFoundException e) {
            PluginLogger.log(listener.getLogger(), e.getMessage());
        }
        return false;
    }

    // This method is for injecting appId so that next build step can use it.
    private void addAppIdToEnvironment(Build<?, ?> build, String appId) {
        VariableInjectorAction variableInjectorAction = build.getAction(VariableInjectorAction.class);
        if (variableInjectorAction == null) {
            variableInjectorAction = new VariableInjectorAction(new HashMap<String, String>());
            build.addAction(variableInjectorAction);
        }
        Map<String, String> envVariables = new HashMap<String, String>();
        envVariables.put("BROWSERSTACK_APP_ID", appId);
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
            if (value == null || value.isEmpty()) {
                return FormValidation.error("Please enter absolute path to your app.");
            }
            File file = new File(value);
            if (!file.exists()) {
                return FormValidation.error("File not found : " + value);
            }

            if (!value.endsWith(".apk") && !value.endsWith(".ipa")) {
                return FormValidation.error("File extension should be only .apk or .ipa.");
            }

            return FormValidation.ok();
        }
    }
}
