package com.browserstack.automate.ci.jenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;

public class BuildTextPublisher extends BuildWrapper implements Serializable {
    @DataBoundConstructor
    public BuildTextPublisher(String someText) {

    }

    @Override
    public BuildTextDescriptor getDescriptor() {
        return (BuildTextDescriptor) super.getDescriptor();
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
//        AbstractTextForBuild lfsBuildAction = new TextForBuild("<div>its the new style</div>", "operatingSystem",
//                "browserName", "browserVersion", "resolution");
//        lfsBuildAction.setBuild(build);
//        ((TextForBuild) lfsBuildAction).setBuildName("buildname");
//        ((TextForBuild) lfsBuildAction).setBuildNumber("buildnumber");
//        ((TextForBuild) lfsBuildAction).setIframeLink("https://browserstack.com");
//        build.addAction(lfsBuildAction);

        return new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                return super.tearDown(build, listener);
            }
        };
    }
}
