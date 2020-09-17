package com.browserstack.automate.jenkins.helpers;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.apache.commons.io.IOUtils;
import org.jvnet.hudson.test.TestExtension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Builder step that copies the given resource to the specified folder under the <strong>target</strong> directory.
 * It creates the directories if they don't exist.
 *
 * @author Anirudha Khanna
 */
public class CopyResourceFileToWorkspaceTarget extends Builder {

    private static final String TARGET_DIR = "target";

    private final String finalReportDir;
    private final String resourceFileToCopy;

    public CopyResourceFileToWorkspaceTarget(String reportDir, String resourceFileToCopy) {
        this.resourceFileToCopy = resourceFileToCopy;
        this.finalReportDir = TARGET_DIR + File.separator + reportDir + File.separator;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        FilePath workspace = build.getWorkspace();
        workspace.child(this.finalReportDir).mkdirs();
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = workspace.child(this.finalReportDir + this.resourceFileToCopy).write();
            inputStream = this.getClass().getClassLoader().getResourceAsStream(this.resourceFileToCopy);
            IOUtils.copy(inputStream, outputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
        return true;
    }

    @TestExtension
    public static final class CopyBrowserStackReportBuilderDescriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Copy BrowserStack Test Report";
        }
    }
}
