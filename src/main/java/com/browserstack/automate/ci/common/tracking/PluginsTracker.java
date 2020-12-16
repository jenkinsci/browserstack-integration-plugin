package com.browserstack.automate.ci.common.tracking;


import com.browserstack.automate.ci.common.Tools;
import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.common.proxysettings.JenkinsProxySettings;
import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Proxy;
import java.time.Instant;
import java.util.Optional;

public class PluginsTracker {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String URL = "https://api.browserstack.com/ci_plugins/track";
    private static OkHttpClient client;
    private final String trackingId;
    private String username;
    private String accessKey;
    private String customProxy;

    public PluginsTracker(final String username, final String accessKey, @Nullable final String customProxy) {
        this.username = username;
        this.accessKey = accessKey;
        this.customProxy = customProxy;
        this.trackingId = Tools.getUniqueString(true, true);
        initializeClient();
    }

    public PluginsTracker() {
        this(null);
    }

    public PluginsTracker(@Nullable final String customProxy) {
        this.username = null;
        this.accessKey = null;
        this.customProxy = customProxy;
        this.trackingId = Tools.getUniqueString(true, true);
        initializeClient();
    }

    private static void asyncPostRequestSilent(final String url, final String json) {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // closing the response body is important, else it will start leaking
                if (response != null && response.body() != null) {
                    response.body().close();
                }
            }
        });
    }

    private void initializeClient() {

        JenkinsProxySettings jenkinsProxy;
        if (customProxy != null) {
            System.out.println("Custom Proxy In Plugins Tracker: " + customProxy);
            jenkinsProxy = new JenkinsProxySettings(customProxy, null);
        } else {
            System.out.println("Without Custom Proxy In Plugins Tracker");
            jenkinsProxy = new JenkinsProxySettings(null);
        }

        final Proxy proxy = jenkinsProxy.getJenkinsProxy();
        if (proxy != Proxy.NO_PROXY) {
            System.out.println("Selected some proxy for plugins tracker. " + proxy.toString());
            if (jenkinsProxy.hasAuth()) {
                final String username = jenkinsProxy.getUsername();
                final String password = jenkinsProxy.getPassword();
                Authenticator proxyAuthenticator = new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {
                        final String credential = Credentials.basic(username, password);
                        return response.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build();
                    }
                };
                this.client = new OkHttpClient.Builder().proxy(proxy).proxyAuthenticator(proxyAuthenticator).build();
            } else {
                this.client = new OkHttpClient.Builder().proxy(proxy).build();
            }
        } else {
            this.client = new OkHttpClient.Builder().build();
        }
    }

    public void trackOperation(String operationType, JSONObject data) {
        JSONObject requestData = new JSONObject();
        requestData.put("source", Constants.JENKINS_CI_PLUGIN);
        requestData.put("product", Constants.AUTOMATE);
        requestData.put("team", Constants.AUTOMATE);
        requestData.put("data", data);
        requestData.put("event_timestamp", Instant.now().getEpochSecond());
        requestData.put("track_operation_type", operationType);
        requestData.put("tracking_id", trackingId);

        Optional.ofNullable(username)
                .ifPresent(userName -> requestData.put("username", userName));
        Optional.ofNullable(accessKey)
                .ifPresent(accessKey -> requestData.put("access_key", accessKey));

        asyncPostRequestSilent(URL, requestData.toString());
    }

    public void sendError(String errorMessage, boolean pipelineStatus, String phase) {
        JSONObject trackingData = new JSONObject();
        trackingData.put("error", errorMessage);
        trackingData.put("pipeline", pipelineStatus);
        trackingData.put("phase", phase);
        trackOperation(PluginsTrackerEvents.CI_PLUGIN_ERROR, trackingData);
    }

    public void pluginInitialized(String buildName, boolean localStatus, boolean pipelineStatus) {
        JSONObject trackingData = new JSONObject();
        trackingData.put("build_name", buildName);
        trackingData.put("local", localStatus);
        trackingData.put("pipeline", pipelineStatus);
        trackOperation(PluginsTrackerEvents.CI_PLUGIN_INITIALIZED, trackingData);
    }

    public void reportGenerationInitialized(String buildName, String product, boolean pipelineStatus) {
        JSONObject trackingData = new JSONObject();
        trackingData.put("build_name", buildName);
        trackingData.put("product", product);
        trackingData.put("pipeline", pipelineStatus);
        trackOperation(PluginsTrackerEvents.CI_PLUGIN_REPORT_GENERATION_STARTED, trackingData);
    }

    public void reportGenerationCompleted(String status, String product, boolean pipelineStatus, String buildName, String buildId) {
        JSONObject dataToTrack = new JSONObject();
        dataToTrack.put("status", status);
        dataToTrack.put("product", product);
        dataToTrack.put("pipeline", pipelineStatus);
        dataToTrack.put("build_name", buildName);
        dataToTrack.put("build_id", buildId);
        trackOperation(PluginsTrackerEvents.CI_PLUGIN_REPORT_PUBLISHED, dataToTrack);
    }

    public void setCredentials(String username, String accessKey) {
        this.username = Optional.ofNullable(this.username)
                .orElse(username);
        this.accessKey = Optional.ofNullable(this.accessKey)
                .orElse(accessKey);
    }
}
