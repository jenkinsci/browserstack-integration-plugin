package com.browserstack.automate.ci.jenkins;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapperDescriptor;

@Extension
public class BuildTextDescriptor extends BuildWrapperDescriptor {
    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "this is bs descriptor";
    }

    public BuildTextDescriptor() {
        super(BuildTextPublisher.class);
        load();
    }
}
