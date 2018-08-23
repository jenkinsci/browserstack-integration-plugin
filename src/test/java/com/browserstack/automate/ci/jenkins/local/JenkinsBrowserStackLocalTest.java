package com.browserstack.automate.ci.jenkins.local;

import org.junit.runner.RunWith;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import static org.assertj.core.api.Assertions.assertThat;
import org.apache.commons.lang.ArrayUtils;
import hudson.EnvVars;
import org.junit.Assert;
import org.junit.Test;


@RunWith(JUnitQuickcheck.class)
public class JenkinsBrowserStackLocalTest {

    public final String ACCESS_KEY = "dummy";
    private final String PROXY_HOST = "test.proxy.host";
    private final String PROXY_PORT = "1234";
    private final String PROXY_USER = "testUser";
    private final String PROXY_PASSWORD = "testPassword";
    private final String LOCAL_IDENTIFIER = "dummyIdentifier";

    @Property
    public void testLocalIdentifierGeneration(String buildTag) {
        LocalConfig localConfig = new LocalConfig();
        JenkinsBrowserStackLocal jenkinsBSLocal = new JenkinsBrowserStackLocal(ACCESS_KEY, localConfig, buildTag, null, null);
        String localIdentifier = jenkinsBSLocal.getLocalIdentifier();

        assertThat(localIdentifier).matches("^[a-zA-Z0-9_\\-\\.]+$");
    }
    
    @Test
    public void testLocalIdentifierOverride() {
        LocalConfig localConfig = new LocalConfig();
        localConfig.setLocalOptions("--local-identifier " + LOCAL_IDENTIFIER);
        JenkinsBrowserStackLocal jenkinsBSLocal = new JenkinsBrowserStackLocal(ACCESS_KEY, localConfig, "buildTag", null, null);
        String localIdentifier = jenkinsBSLocal.getLocalIdentifier();
        Assert.assertEquals("Local identifier should be overriden when passed through localOption", LOCAL_IDENTIFIER, localIdentifier);
    }
  
    @Test
    public void testLocalIdentifierNotOverride() {
        LocalConfig localConfig = new LocalConfig();
        localConfig.setLocalOptions("--local-identifier ");
        JenkinsBrowserStackLocal jenkinsBSLocal = new JenkinsBrowserStackLocal(ACCESS_KEY, localConfig, "buildTag", null, null);
        String localIdentifier = jenkinsBSLocal.getLocalIdentifier();
        Assert.assertTrue("Local identifier should not overriden when blank", !localIdentifier.equals(LOCAL_IDENTIFIER));
    }
  
    @Test
    public void testEnvLocalOptions(){
       LocalConfig localConfig = new LocalConfig();
       localConfig.setLocalOptions("--force-proxy --proxy-host $proxy_host --proxy-port $proxy_port --proxy-user $proxy_user --proxy-password $proxy_password");
       String buildTag = "tag";
       EnvVars envVars = new EnvVars();
       envVars.put("proxy_host", PROXY_HOST);
       envVars.put("proxy_port", PROXY_PORT);
       envVars.put("proxy_user", PROXY_USER);
       envVars.put("proxy_password", PROXY_PASSWORD);
       JenkinsBrowserStackLocal jenkinsBSLocal = new JenkinsBrowserStackLocal(ACCESS_KEY, localConfig, buildTag, envVars, System.out);
       String[] arguments = jenkinsBSLocal.getArguments();
      
       Assert.assertTrue("Local arguments should contain $proxy_host ", ArrayUtils.contains(arguments, PROXY_HOST));
       Assert.assertTrue("Local arguments should contain $proxy_port ", ArrayUtils.contains(arguments, PROXY_PORT));
       Assert.assertTrue("Local arguments should contain $proxy_user ", ArrayUtils.contains(arguments, PROXY_USER));
       Assert.assertTrue("Local arguments should contain $proxy_password ", ArrayUtils.contains(arguments, PROXY_PASSWORD));
    }

}
