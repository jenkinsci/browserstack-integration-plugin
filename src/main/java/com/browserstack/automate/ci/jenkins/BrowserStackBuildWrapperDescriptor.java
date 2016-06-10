package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.jenkins.local.LocalConfig;
import com.browserstack.automate.ci.jenkins.util.BrowserListingInfo;
import com.browserstack.client.model.DesktopPlatform;
import com.browserstack.client.model.Platform;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.browserstack.automate.ci.jenkins.util.BrowserListingInfo.getBrowserMacOrderIndex;
import static com.browserstack.automate.ci.jenkins.util.BrowserListingInfo.getBrowserWinOrderIndex;

@Extension
public final class BrowserStackBuildWrapperDescriptor extends BuildWrapperDescriptor {
    private static final String NAMESPACE = "browserStack";

    private String credentialsId;
    private LocalConfig localConfig;

    public BrowserStackBuildWrapperDescriptor() {
        super(BrowserStackBuildWrapper.class);
        load();
    }

    @Override
    public String getDisplayName() {
        return "BrowserStack";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        if (formData.has(NAMESPACE)) {
            req.bindJSON(this, formData.getJSONObject(NAMESPACE));
            save();
        }

        return true;
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
        return true;
    }

    public ListBoxModel doFillOsItems() {
        ListBoxModel osList = new ListBoxModel();
        BrowserListingInfo browserListingInfo = BrowserListingInfo.getInstance();
        if (browserListingInfo == null) {
            return osList;
        }

        Map<String, Platform> osMap = browserListingInfo.getOsMap();
        if (osMap != null) {
            for (Map.Entry<String, Platform> entry : osMap.entrySet()) {
                String displayName = BrowserListingInfo.getDisplayOs(entry.getValue().getOsDisplayName());
                osList.add(displayName, displayName);
            }

            Collections.sort(osList, new Comparator<ListBoxModel.Option>() {
                public int compare(ListBoxModel.Option o1, ListBoxModel.Option o2) {
                    return compareIntegers(
                            BrowserListingInfo.getOsOrderIndex(o1.name.toLowerCase()),
                            BrowserListingInfo.getOsOrderIndex(o2.name.toLowerCase()));
                }
            });
        }

        osList.add(0, new ListBoxModel.Option("-- Select OS --", "null"));
        return osList;
    }

    public ListBoxModel doFillBrowserItems(@QueryParameter final String os) {
        ListBoxModel browserList = new ListBoxModel();
        String osName = (os != null && !os.equals("null")) ? os.toLowerCase() : null;
        BrowserListingInfo browserListingInfo = BrowserListingInfo.getInstance();

        if (osName != null && browserListingInfo != null) {
            Map<String, List<String>> osBrowserMap = browserListingInfo.getOsBrowserMap();
            if (osBrowserMap != null && osBrowserMap.containsKey(osName)) {
                for (String browser : osBrowserMap.get(osName)) {
                    browserList.add(browser, browser);
                }

                Map<String, Platform> osMap = browserListingInfo.getOsMap();
                if (osMap != null && osMap.containsKey(osName) && osMap.get(osName) instanceof DesktopPlatform) {
                    final boolean isWindows = osName.contains("windows");

                    Collections.sort(browserList, new Comparator<ListBoxModel.Option>() {
                        public int compare(ListBoxModel.Option o1, ListBoxModel.Option o2) {
                            String b1 = BrowserListingInfo.extractBrowserName(o1.name).toLowerCase();
                            String b2 = BrowserListingInfo.extractBrowserName(o2.name).toLowerCase();

                            return compareIntegers(isWindows ?
                                    getBrowserWinOrderIndex(b1) :
                                    getBrowserMacOrderIndex(b1), isWindows ?
                                    getBrowserWinOrderIndex(b2) :
                                    getBrowserMacOrderIndex(b2));
                        }
                    });
                }
            }
        }

        return browserList;
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath final Item context) {
        if (context != null && !context.hasPermission(Item.CONFIGURE)) {
            return new StandardListBoxModel();
        }

        return new StandardListBoxModel()
                .withMatching(CredentialsMatchers.anyOf(
                        CredentialsMatchers.instanceOf(BrowserStackCredentials.class)),
                        CredentialsProvider.lookupCredentials(
                                BrowserStackCredentials.class,
                                context,
                                ACL.SYSTEM,
                                new ArrayList<DomainRequirement>()));
    }

    public FormValidation doCheckLocalPath(@AncestorInPath final AbstractProject project,
                                           @QueryParameter final String localPath) {
        final String path = Util.fixEmptyAndTrim(localPath);
        if (StringUtils.isBlank(path)) {
            return FormValidation.ok();
        }

        try {
            File f = resolvePath(project, localPath);
            if (f != null) {
                return FormValidation.ok();
            }
        } catch (IOException e) {
            // ignore
        } catch (InterruptedException e) {
            return FormValidation.error(e.getMessage());
        }

        return FormValidation.error("Invalid path.");
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public LocalConfig getLocalConfig() {
        return localConfig;
    }

    public void setLocalConfig(LocalConfig localConfig) {
        this.localConfig = localConfig;
    }

    private static int compareIntegers(int x, int y) {
        return (x == y) ? 0 : (x < y) ? -1 : 1;
    }

    public File resolvePath(final AbstractProject project, final String path) throws IOException, InterruptedException {
        File f = new File(path);
        if (f.isAbsolute() && (!f.isFile() || !f.canExecute())) {
            return null;
        }

        // For absolute paths
        FormValidation validateExec = FormValidation.validateExecutable(path);
        if (validateExec.kind == FormValidation.Kind.OK) {
            return f;
        }

        // Ant style path definitions
        FilePath workspace = project.getSomeWorkspace();
        if (workspace != null) {
            File workspaceRoot = new File(workspace.toURI());
            FileSet fileSet = Util.createFileSet(workspaceRoot, path);
            FileScanner fs = fileSet.getDirectoryScanner();
            fs.setIncludes(new String[]{path});
            fs.scan();

            String[] includedFiles = fs.getIncludedFiles();
            if (includedFiles.length > 0) {
                File includedFile = new File(workspaceRoot, includedFiles[0]);
                if (includedFile.exists() && includedFile.isFile() && includedFile.canExecute()) {
                    return includedFile;
                }
            }
        }

        return null;
    }
}
