package com.browserstack.automate.ci.common.tracking;

import com.browserstack.automate.ci.common.Tools;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Optional;

public class PluginsTracker {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String URL = "http://apidev.bsstag.com/plugins/track";
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
            }
        });
    }

    public void trackOperation(String operationType, JSONObject data) {
        JSONObject requestData = new JSONObject();
        requestData.put("data", data);
        requestData.put("track_operation_type", operationType);
        requestData.put("tracking_id", trackingId);

        Optional.ofNullable(username)
                .ifPresent(userName -> requestData.put("username", userName));
        Optional.ofNullable(accessKey)
                .ifPresent(accessKey -> requestData.put("access_key", accessKey));

        asyncPostRequestSilent(URL, requestData.toString());
    }

    public void setCredentials(String username, String accessKey) {
        this.username = Optional.ofNullable(this.username)
                .orElse(username);
        this.accessKey = Optional.ofNullable(this.accessKey)
                .orElse(accessKey);
    }
}
