package com.browserstack.automate.ci.common.proxysettings;

import com.browserstack.automate.ci.common.Tools;
import hudson.ProxyConfiguration;
import hudson.Util;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;
import static com.browserstack.automate.ci.common.logger.PluginLogger.logError;

public class JenkinsProxySettings {

    private static final ProxyConfiguration jenkinsProxy = Jenkins.getInstanceOrNull() != null ? Jenkins.getInstanceOrNull().proxy : null;

    private static final String jarProxyHost = System.getProperty("https.proxyHost");
    private static final int jarProxyPort = Integer.parseInt(System.getProperty("https.proxyPort", "443"));
    private static final String jarProxyUser = System.getProperty("https.proxyUser");
    private static final String jarProxyPassword = System.getProperty("https.proxyPassword");

    private static final String systemHttpProxyEnv = System.getenv("http_proxy");
    private static final String systemHttpsProxyEnv = System.getenv("https_proxy");

    private String finalProxyHost;
    private int finalProxyPort;
    private String finalProxyUsername;
    private String finalProxyPassword;

    private boolean hasProxy;

    private transient PrintStream logger;

    /**
     * Constructor for JenkinsProxySettings with Priority for Custom Proxy
     *
     * @param customProxy Custom Proxy String
     * @param logger      Logger
     */
    public JenkinsProxySettings(@Nonnull final String customProxy, @Nullable PrintStream logger) {
        this.logger = logger;
        decideJenkinsProxy(customProxy);
    }

    /**
     * Constructor for JenkinsProxySettings
     *
     * @param logger Logger
     */
    public JenkinsProxySettings(@Nullable PrintStream logger) {
        this.logger = logger;
        decideJenkinsProxy(null);
    }

    /**
     * Verifies the format of the Proxy String
     *
     * @param proxyString String
     * @param proxyType   Type/Source of Proxy
     * @return
     */
    private URL verifyAndGetProxyURL(final String proxyString, final String proxyType) {
        try {
            final URL proxyUrl = new URL(proxyString);
            if (proxyUrl.getHost() != null && proxyUrl.getHost().length() == 0)
                throw new Error("Empty host in proxy");

            String userInfo = proxyUrl.getUserInfo();
            if (userInfo != null) {
                if (userInfo.split(":").length != 2) {
                    throw new Error("Invalid authentication params in proxy");
                }
            }

            return proxyUrl;
        } catch (Exception e) {
            // TODO: Change to logDebug
            if (logger != null)
                logError(logger, String.format("Invalid Proxy String: %s, Proxy Type: %s. Error: %s", proxyString, proxyType, e.toString()));
            return null;
        }
    }

    /**
     * Decides Proxy for the Plugin. Priority:
     * 0. Custom Proxy passed as input
     * 1. `https_proxy` Environment Variable
     * 2. `http_proxy` Environment Variable
     * 3. Jenkins Proxy Configuration
     * 4. JAR Proxy arguments, i.e. `https.proxyHost` etc.
     *
     * @param customProxyString Custom Proxy String
     */
    private void decideJenkinsProxy(final String customProxyString) {
        URL proxyUrl = null;

        // Verifies the custom proxy string
        if (customProxyString != null) {
            proxyUrl = verifyAndGetProxyURL(customProxyString, "ENV_VAR");
        }

        // Looks for System level `https_proxy`. If not, looks for `http_proxy`
        if (proxyUrl == null && getSystemProxyString() != null) {
            proxyUrl = verifyAndGetProxyURL(getSystemProxyString(), "SYSTEM_ENV_VAR");
        }

        // Looks for Jenkins Proxy
        if (proxyUrl == null && jenkinsProxy != null) {
            this.finalProxyHost = jenkinsProxy.name;
            this.finalProxyPort = jenkinsProxy.port;

            if (Util.fixEmpty(jenkinsProxy.getUserName()) != null && Util.fixEmpty(jenkinsProxy.getPassword()) != null) {
                this.finalProxyUsername = jenkinsProxy.getUserName();
                this.finalProxyPassword = jenkinsProxy.getPassword();
            }
        }

        // Looks for JAR Proxy Arguments
        if (proxyUrl == null && this.finalProxyHost == null && jarProxyHost != null) {
            this.finalProxyHost = jarProxyHost;
            this.finalProxyPort = jarProxyPort;

            if (Util.fixEmpty(jarProxyUser) != null && Util.fixEmpty(jarProxyPassword) != null) {
                this.finalProxyUsername = jarProxyUser;
                this.finalProxyPassword = jarProxyPassword;
            }

            if (this.finalProxyUsername == null)
                System.out.println("Username is null in case of JAR proxy...");
        }

        // Utilises the proxyUrl set by Env Vars if Jenkins & Jar Proxy are absent
        if (proxyUrl != null) {
            this.finalProxyHost = proxyUrl.getHost();
            this.finalProxyPort = proxyUrl.getPort() == -1 ? proxyUrl.getDefaultPort() : proxyUrl.getPort();

            final String userInfo = proxyUrl.getUserInfo();

            if (userInfo != null) {
                String[] userInfoArray = userInfo.split(":");
                this.finalProxyUsername = userInfoArray[0];
                this.finalProxyPassword = userInfoArray[1];
            }
        }

        // Logging and final boolean set
        if (this.finalProxyHost != null && this.finalProxyPort != 0) {
            this.hasProxy = true;

            String proxyDataToLog = "\nHost: " + this.getHost() + "\nPort: " + this.getPort();
            if (this.hasAuth()) {
                proxyDataToLog += "\nUsername: " + this.getUsername() + "\nPassword: " + Tools.maskString(this.getPassword());
            }

            if (logger != null) log(logger, "Proxy Selected for BrowserStack Plugin: " + proxyDataToLog);
        } else {
            this.hasProxy = false;

            if (logger != null) log(logger, "No Proxy Selected for BrowserStack Plugin");
        }
    }


    /**
     * Returns the proxy string from System level env vars. Priority:
     * 1. `https_proxy`
     * 2. `http_proxy`
     * If no value exists, returns null
     *
     * @return String/null
     */
    private String getSystemProxyString() {
        return systemHttpsProxyEnv == null ? systemHttpProxyEnv : systemHttpsProxyEnv;
    }

    public Proxy getJenkinsProxy() {
        if (this.hasProxy()) {
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.finalProxyHost, this.finalProxyPort));
        }

        return Proxy.NO_PROXY;
    }

    public String getHost() {
        return this.finalProxyHost;
    }

    public int getPort() {
        return this.finalProxyPort;
    }

    public String getUsername() {
        if (this.finalProxyUsername != null && this.finalProxyUsername.length() != 0)
            return this.finalProxyUsername;

        return null;
    }

    public String getPassword() {
        if (this.finalProxyPassword != null && this.finalProxyPassword.length() != 0)
            return this.finalProxyPassword;

        return null;
    }

    public boolean hasAuth() {
        return ((this.getUsername() != null) && (this.getPassword() != null));
    }

    public boolean hasProxy() {
        return this.hasProxy;
    }
}
