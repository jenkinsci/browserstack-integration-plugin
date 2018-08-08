package com.browserstack.automate.jenkins.helpers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;

public class TempCredentialIdGenerator {
  
  public static String generateTempCredentialId(String username, String accessKey) throws IOException {
    String credentialsId = UUID.randomUUID().toString();
    BrowserStackCredentials browserStackCredentials = new BrowserStackCredentials(credentialsId, "",
        username, accessKey);

    final SystemCredentialsProvider credentialsProvider = SystemCredentialsProvider.getInstance();
    final Map<Domain, List<Credentials>> credentialsMap =
        credentialsProvider.getDomainCredentialsMap();

    final Domain domain = Domain.global();
    if (credentialsMap.get(domain) == null) {
      credentialsMap.put(domain, Collections.EMPTY_LIST);
    }
    credentialsMap.get(domain).add(browserStackCredentials);

    credentialsProvider.setDomainCredentialsMap(credentialsMap);
    credentialsProvider.save();
    return credentialsId;
  }

}
