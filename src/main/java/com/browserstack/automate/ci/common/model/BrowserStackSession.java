package com.browserstack.automate.ci.common.model;

import org.json.JSONObject;

import com.browserstack.automate.ci.common.enums.ProjectType;

/**
 * Description : For storing info of a browserstack session.
 *
 * @author Hitesh Raghuvanshi
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

    public BrowserStackSession(JSONObject jsonObject) {
        this.sessionId = jsonObject.getString(KEY_SESSION_ID);
        this.projectType = getProjectTypeFromString(jsonObject.optString(KEY_PROJECT_TYPE, ""));
    }

    public BrowserStackSession(String jsonString) {
        this(new JSONObject(jsonString));
    }

    private ProjectType getProjectTypeFromString(String projectType) {
        try {
            return ProjectType.valueOf(projectType);
        } catch (Exception e) {
            return ProjectType.AUTOMATE; // default project type for backward compatibility.
        }
    }

    public JSONObject getAsJSONObject() {
        JSONObject sessionObj = new JSONObject();
        sessionObj.put(KEY_SESSION_ID, this.sessionId);
        sessionObj.put(KEY_PROJECT_TYPE, this.projectType.toString());
        return sessionObj;
    }

    public String getSessionId() {
        return sessionId;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

}

