package com.browserstack.automate.ci.jenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;

public class BTPublisher extends Recorder implements SimpleBuildStep {
    private static final Log log = LogFactory.getLog(BTPublisher.class);

    private String someText;
    private String site;

    @DataBoundConstructor
    public BTPublisher(String someText, String site) {
        this.someText = someText;
        this.site = site;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }


    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {

        listener.getLogger().println("Generating Generic BrowserStack Reports");

        AbstractTextForBuild lfsBuildAction = new TextForBuild("<div>BT Publisher style</div>", "operatingSystem",
                "browserName", "browserVersion", "resolution");
        lfsBuildAction.setBuild(build);
        ((TextForBuild) lfsBuildAction).setBuildName("buildname");
        ((TextForBuild) lfsBuildAction).setBuildNumber(this.someText);
        ((TextForBuild) lfsBuildAction).setIframeLink(this.site);
        build.addAction(lfsBuildAction);

        listener.getLogger().println("Generated Report for BrowserStack");
        return;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public FormValidation doCheckPublishText(@AncestorInPath Job<?,?> project, @QueryParameter String value) throws IOException, ServletException {
            return FormValidation.ok();
        }

        @Override
        @SuppressWarnings("rawtypes")
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "BTPublisher";
        }
    }
}
