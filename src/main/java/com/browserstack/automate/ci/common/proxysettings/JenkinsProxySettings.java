package com.browserstack.automate.ci.common.proxysettings;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class JenkinsProxySettings {

    private static final ProxyConfiguration jenkinsProxy = Jenkins.getInstanceOrNull() != null ? Jenkins.getInstanceOrNull().proxy : null;
    private static final String protocol = "https";
    private static final String systemProxyHost = System.getProperty(protocol + ".proxyHost");
    private static final int systemProxyPort = Integer.parseInt(System.getProperty(protocol + ".proxyPort", "0"));
    private static final String systemProxyUser = System.getProperty(protocol + ".proxyUser");
    private static final String systemProxyPassword = System.getProperty(protocol + ".proxyPassword");

    public static Proxy getJenkinsProxy() {
        if (hasSystemProxy()) {
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(systemProxyHost, systemProxyPort));
        }

        if (jenkinsProxy == null) return null;
        final String proxyHost = jenkinsProxy.name;
        final int proxyPort = jenkinsProxy.port;
        return (proxyHost != null && proxyPort != 0) ? new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)) : null;
    }

    public static String getHost() {
        if (hasSystemProxy()) {
            return systemProxyHost;
        }

        if (jenkinsProxy == null) return null;
        return jenkinsProxy.name;
    }

    public static int getPort() {
        if (hasSystemProxy()) {
            return systemProxyPort;
        }

        if (jenkinsProxy == null) return 0;
        return jenkinsProxy.port;
    }

    public static String getUsername() {
        if (hasSystemProxy() && systemProxyUser != null && systemProxyPassword != null) {
            return systemProxyUser;
        }

        if (jenkinsProxy == null) return null;
        return jenkinsProxy.getUserName();
    }

    public static String getPassword() {
        if (hasSystemProxy() && systemProxyUser != null && systemProxyPassword != null) {
            return systemProxyPassword;
        }

        if (jenkinsProxy == null) return null;
        return jenkinsProxy.getPassword();
    }

    public static ProxyConfiguration getProxyConfig() {
        return jenkinsProxy;
    }

    public static boolean hasProxy() {
        return getHost() != null && getPort() != 0;
    }

    public static boolean hasSystemProxy() {
        return systemProxyHost != null && systemProxyPort != 0;
    }

}
