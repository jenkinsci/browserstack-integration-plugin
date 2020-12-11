package com.browserstack.automate.ci.common.proxysettings;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class JenkinsProxySettings {

    private static final ProxyConfiguration jenkinsProxy = Jenkins.getInstanceOrNull() != null ? Jenkins.getInstanceOrNull().proxy : null;
    private static final String protocol = "https";
    private static String proxyHost;
    private static int proxyPort;
    private static String proxyUser;
    private static String proxyPassword;
    static {
        final String systemProxyHost = System.getProperty(protocol + ".proxyHost");
        final int systemProxyPort = Integer.parseInt(System.getProperty(protocol + ".proxyPort", "0"));
        final String systemProxyUser = System.getProperty(protocol + ".proxyUser");
        final String systemProxyPassword = System.getProperty(protocol + ".proxyPassword");
        if(systemProxyHost!=null && systemProxyPort!=0){
            proxyHost = systemProxyHost;
            proxyPort = systemProxyPort;
            if(systemProxyUser!=null && systemProxyPassword!=null){
                proxyUser = systemProxyUser;
                proxyPassword = systemProxyPassword;
            }
        }

        if(proxyHost==null && proxyPort==0 && jenkinsProxy!=null){
            final String host = jenkinsProxy.name;
            final int port = jenkinsProxy.port;
            final String user = jenkinsProxy.getUserName();
            final String password = jenkinsProxy.getPassword();
            if(host!=null && port!=0){
                proxyHost = host;
                proxyPort = port;
                if(user!=null && password!=null){
                    proxyUser = user;
                    proxyPassword = password;
                }
            }
        }
        if(proxyHost==null && proxyPort==0){
            String proxyEnv = System.getenv("https_proxy");
            String authRegex = "(https:\\/\\/)(.+):(.+)@(.+):(\\d+)";
            String basicRegex = "(https:\\/\\/)(.+):(\\d+)";

            if(proxyEnv!=null && proxyEnv.matches(authRegex)){
                Pattern r = Pattern.compile(authRegex);
                Matcher m = r.matcher(proxyEnv);
                final String envHost = m.group(1);
                final int envPort = Integer.parseInt(m.group(2));
                final String envUser = m.group(3);
                final String envPassword = m.group(4);
                if(envHost!=null && envPort!=0){
                    proxyHost = envHost;
                    proxyPort = envPort;
                    if(envUser!=null && envPassword!=null){
                        proxyUser = envUser;
                        proxyPassword = envPassword;
                    }
                }
            }
            else if(proxyEnv!=null && proxyEnv.matches(basicRegex)) {
                Pattern r = Pattern.compile(authRegex);
                Matcher m = r.matcher(proxyEnv);
                final String envHost = m.group(1);
                final int envPort = Integer.parseInt(m.group(2));
                if(envHost!=null && envPort!=0){
                    proxyHost = envHost;
                    proxyPort = envPort;
                }
            }
        }
    }

    public static Proxy getJenkinsProxy() {
        return (proxyHost != null && proxyPort != 0) ? new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)) : null;
    }

    public static String getHost() {
        return proxyHost;
    }

    public static int getPort() {
        return proxyPort;
    }

    public static String getUsername() {
        return proxyUser;
    }

    public static String getPassword() {
        return proxyPassword;
    }

    public static ProxyConfiguration getProxyConfig() {
        return jenkinsProxy;
    }

    public static boolean hasProxy() {
        return getHost() != null && getPort() != 0;
    }

}
