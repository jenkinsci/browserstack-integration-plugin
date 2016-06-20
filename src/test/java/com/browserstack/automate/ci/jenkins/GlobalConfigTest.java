package com.browserstack.automate.ci.jenkins;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * @author Shirish Kamath
 * @author Anirudha Khanna
 */
public class GlobalConfigTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testConfigElements() throws Exception {
        HtmlPage page = jenkinsRule.createWebClient().goTo("configure");
        String pageText = page.asText();
        Assert.assertTrue("Missing: BrowserStack Global Config", pageText.contains("BrowserStack"));
    }


}
