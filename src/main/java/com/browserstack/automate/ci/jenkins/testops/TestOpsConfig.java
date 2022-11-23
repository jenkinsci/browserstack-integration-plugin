package com.browserstack.automate.ci.jenkins.testops;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

public class TestOpsConfig implements Serializable {
    private String tests;
    private String reRun;

    public TestOpsConfig() {
    }

    @DataBoundConstructor
    public TestOpsConfig(String tests, String reRun) {
        this.tests = tests;
        this.reRun = reRun;
    }

    public String getTests() {
        return this.tests;
    }

    public void setTests(String tests) {
        this.tests = tests;
    }

    public String getReRun() {
        return this.reRun;
    }

    public void setReRun(String reRun) {
        this.reRun = reRun;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        TestOpsConfig that = (TestOpsConfig) o;
        if (this.tests != null ? !this.tests.equals(that.tests) : that.tests != null) return false;
        return this.reRun != null ? this.reRun.equals(that.reRun) : that.reRun == null;
    }

    public int hashCode() {
        int result = this.tests != null ? this.tests.hashCode() : 0;
        result = 31 * result + (this.reRun != null ? this.reRun.hashCode() : 0);
        return result;
    }
}
