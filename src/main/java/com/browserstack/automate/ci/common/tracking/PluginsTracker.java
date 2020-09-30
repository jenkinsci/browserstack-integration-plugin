package com.browserstack.automate.ci.common.tracking;

import com.browserstack.automate.ci.common.Tools;
import com.browserstack.automate.ci.common.constants.Constants;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

public class PluginsTracker {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String URL = "https://api.browserstack.com/ci_plugins/track";
    private static final OkHttpClient client = new OkHttpClient();
    private final String trackingId;
    private String username;
    private String accessKey;

    public PluginsTracker(final String username, final String accessKey) {
        this.username = username;
        this.accessKey = accessKey;
        this.trackingId = Tools.getUniqueString(true, true);
    }

    public PluginsTracker() {
        this.username = null;
        this.accessKey = null;
        this.trackingId = Tools.getUniqueString(true, true);
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
