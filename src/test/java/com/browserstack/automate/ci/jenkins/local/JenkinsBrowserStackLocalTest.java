package com.browserstack.automate.ci.jenkins.local;

import org.junit.runner.RunWith;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import static org.assertj.core.api.Assertions.assertThat;


@RunWith(JUnitQuickcheck.class)
public class JenkinsBrowserStackLocalTest {

    public final String ACCESS_KEY = "dummy";

    @Property
    public void testLocalIdentifierGeneration(String buildTag) {
        LocalConfig localConfig = new LocalConfig();
        JenkinsBrowserStackLocal jenkinsBSLocal = new JenkinsBrowserStackLocal(ACCESS_KEY, localConfig, buildTag);

        String localIdentifier = jenkinsBSLocal.getLocalIdentifier();

        assertThat(localIdentifier).matches("^[a-zA-Z0-9_\\-\\.]+$");
    }

}
