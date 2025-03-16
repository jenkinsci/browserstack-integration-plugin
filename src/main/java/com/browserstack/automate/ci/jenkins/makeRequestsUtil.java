package com.browserstack.automate.ci.jenkins;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class makeRequestsUtil {
  OkHttpClient client = new OkHttpClient();

  public Response makeRequest(String getUrl, BrowserStackCredentials browserStackCredentials) throws Exception{
    try {
      Request request = new Request.Builder()
              .url(getUrl)
              .header("Authorization", Credentials.basic(browserStackCredentials.getUsername(), browserStackCredentials.getDecryptedAccesskey()))
              .build();
      Response response = client.newCall(request).execute();
      return response;
    } catch(IOException e) {
      e.printStackTrace();
      throw e;
    }
  }
}
