package com.browserstack.automate.ci.jenkins.qualityDashboard;

import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
}
