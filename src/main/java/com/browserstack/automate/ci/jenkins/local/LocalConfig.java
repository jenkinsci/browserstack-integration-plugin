package com.browserstack.automate.ci.jenkins.local;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

public class LocalConfig implements Serializable {
    private String localPath;
    private String localOptions;

    public LocalConfig() {
    }

    @DataBoundConstructor
    public LocalConfig(String localPath, String localOptions) {
        this.localPath = localPath;
        this.localOptions = localOptions;
    }

    public String getLocalPath() {
        return this.localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getLocalOptions() {
        return this.localOptions;
    }

    public void setLocalOptions(String localOptions) {
        this.localOptions = localOptions;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        LocalConfig that = (LocalConfig) o;
        if (this.localPath != null ? !this.localPath.equals(that.localPath) : that.localPath != null) return false;
        return this.localOptions != null ? this.localOptions.equals(that.localOptions) : that.localOptions == null;
    }

    public int hashCode() {
        int result = this.localPath != null ? this.localPath.hashCode() : 0;
        result = 31 * result + (this.localOptions != null ? this.localOptions.hashCode() : 0);
        return result;
    }
}