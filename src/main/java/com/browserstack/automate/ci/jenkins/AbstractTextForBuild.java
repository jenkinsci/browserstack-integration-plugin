package com.browserstack.automate.ci.jenkins;

import hudson.model.Action;
import hudson.model.Run;

public abstract class AbstractTextForBuild implements Action {
    private Run<?, ?> build;

    protected String displayName;
    protected String iconFileName;
    protected String testUrl;

    @Override
    public String getIconFileName() {
        return iconFileName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getUrlName() {
        return testUrl;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public void setBuild(Run<?, ?> build) {
        this.build = build;
    }

    public void setIconFileName(String iconFileName) {
        this.iconFileName = iconFileName;
    }

    public void setDisplayName(String dn) {
        // the displayname does not wrap after 46 characters
        // so it will bleed into the view
        int maxCharactersViewable = 46;
        if (dn.length() > maxCharactersViewable - 3) {
            // going to cut the string down and add "..."
            dn = dn.substring(0, maxCharactersViewable - 3);
            dn += "...";
        }
        displayName = dn;
    }

    public void setTestUrl(String testUrl) {
        this.testUrl = testUrl.replaceAll("[:.()|/ ]", "").toLowerCase();
    }

}
