package com.browserstack.automate.ci.jenkins;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public class RequestsUtil {
  OkHttpClient client = new OkHttpClient();

  public Response makeRequest(String getUrl, BrowserStackCredentials browserStackCredentials) throws Exception {
    try {
      Request request = new Request.Builder()
              .url(getUrl)
              .header("Authorization", Credentials.basic(browserStackCredentials.getUsername(), browserStackCredentials.getDecryptedAccesskey()))
              .build();
      Response response = client.newCall(request).execute();
      return response;
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
}

