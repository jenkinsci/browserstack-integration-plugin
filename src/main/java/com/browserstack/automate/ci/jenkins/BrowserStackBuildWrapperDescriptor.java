package com.browserstack.automate.ci.jenkins;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.browserstack.automate.ci.common.analytics.Analytics;
import com.browserstack.automate.ci.jenkins.local.LocalConfig;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@Extension
public final class BrowserStackBuildWrapperDescriptor extends BuildWrapperDescriptor {
    private static final String NAMESPACE = "browserStack";

    private String credentialsId;
    private LocalConfig localConfig;
    private boolean usageStatsEnabled;

    public BrowserStackBuildWrapperDescriptor() {
        super(BrowserStackBuildWrapper.class);
        load();

        Analytics.setEnabled(usageStatsEnabled);
        if (usageStatsEnabled) {
            Analytics.trackInstall();
        }
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
            Analytics.setEnabled(!config.has("usageStatsEnabled") || config.getBoolean("usageStatsEnabled"));
        }

        return true;
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
        return true;
    }


    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath final Item context) {
        if (context != null && !context.hasPermission(Item.CONFIGURE)) {
            return new StandardListBoxModel();
        }

        return new StandardListBoxModel()
                .withMatching(CredentialsMatchers.anyOf(
                        CredentialsMatchers.instanceOf(BrowserStackCredentials.class)),
                        CredentialsProvider.lookupCredentials(
                                BrowserStackCredentials.class,
                                context,
                                ACL.SYSTEM,
                                new ArrayList<DomainRequirement>()));
    }

    public FormValidation doCheckLocalPath(@AncestorInPath final AbstractProject project,
                                           @QueryParameter final String localPath) {
        final String path = Util.fixEmptyAndTrim(localPath);
        if (StringUtils.isBlank(path)) {
            return FormValidation.ok();
        }

        try {
            File f = resolvePath(project, localPath);
            if (f != null) {
                return FormValidation.ok();
            }
        } catch (Exception e) {
            return FormValidation.error(e.getMessage());
        }

        return FormValidation.error("Invalid path.");
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

    public boolean getEnableUsageStats() {
        return usageStatsEnabled;
    }

    public void setEnableUsageStats(boolean usageStatsEnabled) {
        this.usageStatsEnabled = usageStatsEnabled;
    }

    private static int compareIntegers(int x, int y) {
        return (x == y) ? 0 : (x < y) ? -1 : 1;
    }

    public File resolvePath(final AbstractProject project, final String path) throws IOException, InterruptedException {
        File f = new File(path);
        if (f.isAbsolute() && (!f.isFile() || !f.canExecute())) {
            return null;
        }

        // For absolute paths
        FormValidation validateExec = FormValidation.validateExecutable(path);
        if (validateExec.kind == FormValidation.Kind.OK) {
            return f;
        }

        // Ant style path definitions
        FilePath workspace = project.getSomeWorkspace();
        if (workspace != null) {
            File workspaceRoot = new File(workspace.toURI());
            FileSet fileSet = Util.createFileSet(workspaceRoot, path);
            FileScanner fs = fileSet.getDirectoryScanner();
            fs.setIncludes(new String[]{path});
            fs.scan();

            String[] includedFiles = fs.getIncludedFiles();
            if (includedFiles.length > 0) {
                File includedFile = new File(workspaceRoot, includedFiles[0]);
                if (includedFile.exists() && includedFile.isFile() && includedFile.canExecute()) {
                    return includedFile;
                }
            }
        }

        return null;
    }
}
