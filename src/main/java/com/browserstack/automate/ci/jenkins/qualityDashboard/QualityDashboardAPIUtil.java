package com.browserstack.automate.ci.jenkins.qualityDashboard;

import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import java.io.IOException;
import java.io.Serializable;

public class QualityDashboardAPIUtil {

    OkHttpClient client = new OkHttpClient();

    public Response makeGetRequestToQd(String getUrl, BrowserStackCredentials browserStackCredentials) {
        try {
            Request request = new Request.Builder()
                    .url(getUrl)
                    .header("Authorization", Credentials.basic(browserStackCredentials.getUsername(), browserStackCredentials.getDecryptedAccesskey()))
                    .build();
            Response response = client.newCall(request).execute();
            return response;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Response makePostRequestToQd(String postUrl, BrowserStackCredentials browserStackCredentials, RequestBody requestBody) {
        try {
            Request request = new Request.Builder()
                    .url(postUrl)
                    .header("Authorization", Credentials.basic(browserStackCredentials.getUsername(), browserStackCredentials.getDecryptedAccesskey()))
                    .post(requestBody)
                    .build();
            Response response = client.newCall(request).execute();
            return response;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Response makePutRequestToQd(String postUrl, BrowserStackCredentials browserStackCredentials, RequestBody requestBody) {
        try {
            Request request = new Request.Builder()
                    .url(postUrl)
                    .header("Authorization", Credentials.basic(browserStackCredentials.getUsername(), browserStackCredentials.getDecryptedAccesskey()))
                    .put(requestBody)
                    .build();
            Response response = client.newCall(request).execute();
            return response;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Response makeDeleteRequestToQd(String postUrl, BrowserStackCredentials browserStackCredentials, RequestBody requestBody) {
        try {
            Request request = new Request.Builder()
                    .url(postUrl)
                    .header("Authorization", Credentials.basic(browserStackCredentials.getUsername(), browserStackCredentials.getDecryptedAccesskey()))
                    .delete(requestBody)
                    .build();
            Response response = client.newCall(request).execute();
            return response;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void logToQD(BrowserStackCredentials browserStackCredentials, String logMessage) throws JsonProcessingException {
        LogMessage logMessageObj = new LogMessage(logMessage);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = objectMapper.writeValueAsString(logMessageObj);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
        makePostRequestToQd(Constants.QualityDashboardAPI.getLogMessageEndpoint(), browserStackCredentials, requestBody);
    }
}

class LogMessage implements Serializable {

    @JsonProperty("message")
    private String message;

    public LogMessage(String message) {
        this.message = message;
    }
}
