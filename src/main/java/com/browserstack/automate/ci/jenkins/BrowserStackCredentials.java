package com.browserstack.automate.ci.jenkins;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

import com.browserstack.automate.AutomateClient;
import com.browserstack.automate.ci.common.analytics.Analytics;
import com.browserstack.automate.exception.AutomateException;
import com.cloudbees.plugins.credentials.BaseCredentials;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractItem;
import hudson.model.Item;
import hudson.model.ModelObject;
import hudson.model.User;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;


@NameWith(value = BrowserStackCredentials.NameProvider.class)
public class BrowserStackCredentials extends BaseCredentials implements StandardCredentials {

    private static final String CREDENTIAL_DISPLAY_NAME = "BrowserStack";
    private static final String OK_VALID_AUTH = "Success";
    private static final String ERR_INVALID_AUTH = "Invalid username or access key!";
    private static final String ERR_HUB_UNREACHABLE = "Unable to connect to BrowserStack!";

    private final String id;

    private final String description;

    private final String username;

    private final Secret accesskey;


    @DataBoundConstructor
    public BrowserStackCredentials(String id, String description, String username, String accesskey) {
        super(CredentialsScope.GLOBAL);
        this.id = IdCredentials.Helpers.fixEmptyId(id);
        this.description = Util.fixNull(description);
        this.username = Util.fixNull(username);
        this.accesskey = Secret.fromString(accesskey);
        Analytics.trackInstall();
    }

    @Exported
    public String getUsername() {
        return username;
    }

    public boolean hasUsername() {
        return StringUtils.isNotBlank(username);
    }

    @Exported
    public Secret getAccesskey() {
        return accesskey;
    }

    public String getDecryptedAccesskey() {
        return accesskey.getPlainText();
    }

    public boolean hasAccesskey() {
        return (accesskey != null);
    }

    @NonNull
    @Exported
    public String getDescription() {
        return description;
    }

    @NonNull
    @Exported
    public String getId() {
        return id;
    }

    @Override
    public final boolean equals(Object o) {
        return IdCredentials.Helpers.equals(this, o);
    }

    @Override
    public final int hashCode() {
        return IdCredentials.Helpers.hashCode(this);
    }

    public static FormValidation testAuthentication(final String username, final String accesskey) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(accesskey)) {
            return FormValidation.ok();
        }

        try {
            AutomateClient client = new AutomateClient(username, accesskey);
            if (client.getAccountUsage() != null) {
                return FormValidation.ok(OK_VALID_AUTH);
            }
        } catch (AutomateException e) {
            if (e.getStatusCode() == 401) {
                return FormValidation.error(ERR_INVALID_AUTH);
            }
        } catch (Exception e) {
            return FormValidation.error(e.getMessage());
        }

        return FormValidation.error(ERR_HUB_UNREACHABLE);
    }

    public static BrowserStackCredentials getCredentials(final AbstractItem buildItem, final String credentialsId) {
        List<BrowserStackCredentials> available = availableCredentials(buildItem);
        if (available.isEmpty()) {
            return null;
        }

        CredentialsMatcher matcher;
        if (credentialsId != null) {
            matcher = CredentialsMatchers.allOf(CredentialsMatchers.withId(credentialsId));
        } else {
            matcher = CredentialsMatchers.always();
        }

        return CredentialsMatchers.firstOrDefault(
                available,
                matcher,
                available.get(0));
    }

    public static List<BrowserStackCredentials> availableCredentials(final AbstractItem abstractItem) {
        return CredentialsProvider.lookupCredentials(
                BrowserStackCredentials.class,
                abstractItem,
                null,
                new ArrayList<DomainRequirement>());
    }

    @Extension(ordinal = 1.0D)
    public static class DescriptorImpl extends CredentialsDescriptor {

        public DescriptorImpl() {
            clazz.asSubclass(BrowserStackCredentials.class);
        }

        public DescriptorImpl(Class<? extends BaseStandardCredentials> clazz) {
            super(clazz);
        }

        public final FormValidation doAuthenticate(@QueryParameter("username") String username,
                                                   @QueryParameter("accesskey") String accesskey) {
            return testAuthentication(username, accesskey);
        }

        @Override
        public String getDisplayName() {
            return CREDENTIAL_DISPLAY_NAME;
        }

        /**
         * @return always returns false since the scope of Local credentials are always Global.
         */
        @Override
        public boolean isScopeRelevant() {
            return false;
        }

        /**
         * @return always returns false since the scope of Local credentials are always Global.
         */
        @SuppressWarnings("unused") // used by stapler
        public boolean isScopeRelevant(ModelObject object) {
            return false;
        }

        @CheckForNull
        private static FormValidation checkForDuplicates(String value, ModelObject context, ModelObject object) {
            for (CredentialsStore store : CredentialsProvider.lookupStores(object)) {
                if (!store.hasPermission(CredentialsProvider.VIEW)) {
                    continue;
                }
                ModelObject storeContext = store.getContext();
                for (Domain domain : store.getDomains()) {
                    if (CredentialsMatchers.firstOrNull(store.getCredentials(domain), CredentialsMatchers.withId(value))
                            != null) {
                        if (storeContext == context) {
                            return FormValidation.error("This ID is already in use");
                        } else {
                            return FormValidation.warning("The ID ‘%s’ is already in use in %s", value,
                                    storeContext instanceof Item
                                            ? ((Item) storeContext).getFullDisplayName()
                                            : storeContext.getDisplayName());
                        }
                    }
                }
            }
            return null;
        }

        public final FormValidation doCheckId(@QueryParameter String value, @AncestorInPath ModelObject context) {
            if (value.isEmpty()) {
                return FormValidation.ok();
            }
            if (!value.matches("[a-zA-Z0-9_.-]+")) { // anything else considered kosher?
                return FormValidation.error("Unacceptable characters");
            }
            FormValidation problem = checkForDuplicates(value, context, context);
            if (problem != null) {
                return problem;
            }
            if (!(context instanceof User)) {
                User me = User.current();
                if (me != null) {
                    problem = checkForDuplicates(value, context, me);
                    if (problem != null) {
                        return problem;
                    }
                }
            }
            if (!(context instanceof Jenkins)) {
                // CredentialsProvider.lookupStores(User) does not return SystemCredentialsProvider.
                Jenkins j = Jenkins.getInstance();
                if (j != null) {
                    problem = checkForDuplicates(value, context, j);
                    if (problem != null) {
                        return problem;
                    }
                }
            }
            return FormValidation.ok();
        }
    }

    public static class NameProvider extends CredentialsNameProvider<BrowserStackCredentials> {

        @Override
        public String getName(BrowserStackCredentials credentials) {
            String description = Util.fixEmptyAndTrim(credentials.getDescription());
            return credentials.getUsername() + "/******" + (description != null ? " (" + description + ")" : "");
        }
    }
}
