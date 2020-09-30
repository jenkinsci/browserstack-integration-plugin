package com.browserstack.automate.ci.common.model;

import com.browserstack.automate.ci.common.enums.ProjectType;

/**
 * Description : For storing info of a browserstack session.
 */

public class BrowserStackSession {

    private static final String KEY_SESSION_ID = "sessionId";
    private static final String KEY_PROJECT_TYPE = "projectType";

    private String sessionId;
    private ProjectType projectType;

    public BrowserStackSession(String sessionId, String projectType) {
        this.sessionId = sessionId;
        this.projectType = getProjectTypeFromString(projectType);
    }

    private ProjectType getProjectTypeFromString(String projectType) {
        try {
            return ProjectType.valueOf(projectType);
        } catch (Exception e) {
            return ProjectType.AUTOMATE; // default project type for backward compatibility.
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    public ProjectType getProjectType() {
        return projectType;
    }
}
