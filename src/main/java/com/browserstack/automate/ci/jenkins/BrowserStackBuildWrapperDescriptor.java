package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.BrowserStackBuildWrapperOperations;
import com.browserstack.automate.ci.jenkins.local.LocalConfig;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public final class BrowserStackBuildWrapperDescriptor extends BuildWrapperDescriptor {
    private static final String NAMESPACE = "browserStack";

    private String credentialsId;
    private LocalConfig localConfig;

    public BrowserStackBuildWrapperDescriptor() {
        super(BrowserStackBuildWrapper.class);
        load();

    }

    private static int compareIntegers(int x, int y) {
        return (x == y) ? 0 : (x < y) ? -1 : 1;
    }

    @Override
    public String getDisplayName() {
        return "BrowserStack";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        if (formData.has(NAMESPACE)) {
            JSONObject config = formData.getJSONObject(NAMESPACE);
            req.bindJSON(this, config);
            save();
        }

        return true;
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
        return true;
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath final Item context) {
        return BrowserStackBuildWrapperOperations.doFillCredentialsIdItems(context);
    }

    public FormValidation doCheckLocalPath(@AncestorInPath final AbstractProject project,
                                           @QueryParameter final String localPath) {
        return BrowserStackBuildWrapperOperations.doCheckLocalPath(project, localPath);
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public LocalConfig getLocalConfig() {
        return localConfig;
    }

    public void setLocalConfig(LocalConfig localConfig) {
        this.localConfig = localConfig;
    }

}
