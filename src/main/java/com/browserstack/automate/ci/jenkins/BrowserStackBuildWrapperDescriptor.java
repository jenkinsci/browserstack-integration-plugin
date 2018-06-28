package com.browserstack.automate.ci.jenkins;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.browserstack.automate.ci.common.BrowserStackBuildWrapperOperations;
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
  // By default usage stats are enabled. But user's can choose to disable through Jenkin's
  // configuration.
  private boolean usageStatsEnabled = true;

  public BrowserStackBuildWrapperDescriptor() {
    super(BrowserStackBuildWrapper.class);
    load();

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
      if (config.has("usageStatsEnabled")) {
        setEnableUsageStats(config.getBoolean("usageStatsEnabled"));
      }
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

  public boolean getEnableUsageStats() {
    return usageStatsEnabled;
  }

  public void setEnableUsageStats(boolean usageStatsEnabled) {
    this.usageStatsEnabled = usageStatsEnabled;
    Analytics.setEnabled(this.usageStatsEnabled);
    // We track an install if one has not been done before.
    // Since a user could have chosen to disable the plugin and then chosen to re-enable it,
    // before installing a newer version of the plugin.
    if (this.usageStatsEnabled) {
      Analytics.trackInstall();
    }
  }

  private static int compareIntegers(int x, int y) {
    return (x == y) ? 0 : (x < y) ? -1 : 1;
  }
}
