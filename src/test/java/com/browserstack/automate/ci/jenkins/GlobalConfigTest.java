package com.browserstack.automate.ci.jenkins;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static junit.framework.TestCase.assertTrue;

/**
 * @author Shirish Kamath
 * @author Anirudha Khanna
 */
public class GlobalConfigTest {

    HtmlPage page;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        page = j.createWebClient().goTo("configure");
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testConfigElements() throws Exception {
        String pageText = page.asText();
        assertTrue("Missing: BrowserStack Global Config", pageText.contains("BrowserStack"));
    }


}
