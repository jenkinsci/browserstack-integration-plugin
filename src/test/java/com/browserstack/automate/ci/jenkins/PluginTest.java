package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.jenkins.local.LocalConfig;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.util.DescribableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.TouchBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;

/**
 * @author Shirish Kamath
 * @author Anirudha Khanna
 */
public class PluginTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private FreeStyleProject project;

    @Before
    public void setUp() throws Exception {
        j.recipeLoadCurrentPlugin();
        j.configRoundtrip();
        project = j.createFreeStyleProject("browserstack-test");
    }

    @Test
    public void test1() throws Exception {
        addBuildStep();
        project.getBuildersList().add(new TouchBuilder());
        project.getBuildersList().add(new SimpleArchive("A", 7, 0));

        JUnitResultArchiver a = new JUnitResultArchiver("report.xml");
        a.setTestDataPublishers(Collections.singletonList(new AutomateTestDataPublisher()));
        project.getPublishersList().add(a);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, build);

        DescribableList<Publisher, Descriptor<Publisher>> publishersList = project.getPublishersList();
        JUnitResultArchiver jUnitResultArchiver = publishersList.get(JUnitResultArchiver.class);

        if (jUnitResultArchiver != null) {
            System.out.println(jUnitResultArchiver.getTestResults());
        } else {
            System.out.println("jUnitResultArchiver: missing");
        }
    }

    public void addBuildStep() throws IOException {
        String credentialsId = "1";
        addCredentials("1", "shirishkamath1", "njBUNq7CcaEpZeyqj3zQ");
        LocalConfig localConfig = new LocalConfig();
        localConfig.setLocalOptions("-force");

        BrowserStackBuildWrapper buildWrapper = new BrowserStackBuildWrapper(credentialsId, null, localConfig);
        project.getBuildWrappersList().add(buildWrapper);
    }

    private static void addCredentials(String id, String username, String accessKey) throws IOException {
        BrowserStackCredentials credentials = new BrowserStackCredentials(
                CredentialsScope.GLOBAL,
                id,
                "browserstack-credentials-description",
                username,
                accessKey);
        addCredentials(credentials);
    }

    private static void addCredentials(BrowserStackCredentials credentials) throws IOException {
        CredentialsStore store = new SystemCredentialsProvider.UserFacingAction().getStore();
        store.addCredentials(Domain.global(), credentials);
    }

    public static final class SimpleArchive extends Builder {
        private final String name;
        private final int pass;
        private final int fail;

        public SimpleArchive(String name, int pass, int fail) {
            this.name = name;
            this.pass = pass;
            this.fail = fail;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            FilePath ws = build.getWorkspace();
            OutputStream os = ws.child(name + ".xml").write();
            try {
                PrintWriter pw = new PrintWriter(os);
                pw.println("<testsuite failures=\"" + fail + "\" errors=\"0\" skipped=\"0\" tests=\"" + (pass + fail) + "\" name=\"" + name + "\">");
                for (int i = 0; i < pass; i++) {
                    pw.println("<testcase classname=\"" + name + "\" name=\"passing" + i + "\"/>");
                }
                for (int i = 0; i < fail; i++) {
                    pw.println("<testcase classname=\"" + name + "\" name=\"failing" + i + "\"><error message=\"failure\"/></testcase>");
                }
                pw.println("</testsuite>");
                pw.flush();
            } finally {
                os.close();
            }
            new JUnitResultArchiver(name + ".xml").perform(build, ws, launcher, listener);
            return true;
        }

        @TestExtension
        public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
            @Override public boolean isApplicable(Class<? extends AbstractProject> jobType) {
                return true;
            }
            @Override public String getDisplayName() {
                return "Incremental JUnit result publishing";
            }
        }
    }
}
