package com.browserstack.automate.ci.jenkins.integrationService;

import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import okhttp3.*;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public class RequestsUtil {
  private transient OkHttpClient client;


  public Response makeRequest(String url, BrowserStackCredentials browserStackCredentials, RequestBody body) throws Exception {
    try {
      Request request = new Request.Builder()
              .url(url)
              .header("Authorization", Credentials.basic(browserStackCredentials.getUsername(), browserStackCredentials.getDecryptedAccesskey()))
              .post(body)
              .build();
      return getClient().newCall(request).execute();
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    }
  }

  public String buildQueryParams(String url, Map<String, String> params) throws URISyntaxException {
    try {
      URIBuilder builder = new URIBuilder(url);
      for (String key : params.keySet()) {
        builder.addParameter(key, params.get(key));
      }
      String fullUrl = builder.build().toString();
      return fullUrl;
    } catch (URISyntaxException uriSyntaxException) {
      uriSyntaxException.printStackTrace();
      throw uriSyntaxException;
    }
  }

  private OkHttpClient getClient() {
    if (client == null) {
      client = new OkHttpClient();
    }
    return client;
  }
}

