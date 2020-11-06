package com.browserstack.automate.ci.common.proxysettings;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import okhttp3.Credentials;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class JenkinsProxySettings {

    static final ProxyConfiguration jenkinsProxy = Jenkins.getInstanceOrNull()!=null ? Jenkins.getInstanceOrNull().proxy : null;
    static final String protocol = "https";
    static final String systemProxyHost = System.getProperty(protocol + ".proxyHost");
    static final int systemProxyPort = Integer.parseInt(System.getProperty(protocol + ".proxyPort", "0"));
    static final String systemProxyUser = System.getProperty(protocol + ".proxyUser");
    static final String systemProxyPassword = System.getProperty(protocol + ".proxyPassword");

    public static Proxy getJenkinsProxy(){
        if(hasSystemProxy()){
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(systemProxyHost, systemProxyPort));
        }

        if(jenkinsProxy==null) return null;
        String proxyHost = jenkinsProxy.name;
        int proxyPort = jenkinsProxy.port;        
        return (proxyHost!=null && proxyPort!=0) ? new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)): null;
    }

    public static String getHost(){
        if(hasSystemProxy()){
            return systemProxyHost;
        }

        if(jenkinsProxy==null) return null;
        return jenkinsProxy.name;
    }

    public static int getPort(){
        if(hasSystemProxy()){
            return systemProxyPort;
        }

        if(jenkinsProxy==null) return 0;
        return jenkinsProxy.port;
    }

    public static String getUsername(){
        if(hasSystemProxy() && systemProxyUser!=null && systemProxyPassword!=null){
            return systemProxyUser;
        }

        if(jenkinsProxy==null) return null;
        return jenkinsProxy.getUserName();
    }

    public static String getPassword(){
        if(hasSystemProxy() && systemProxyUser!=null && systemProxyPassword!=null){
            return systemProxyPassword;
        }

        if(jenkinsProxy==null) return null;
        return jenkinsProxy.getPassword();
    }

    public static ProxyConfiguration getProxyConfig(){
        return jenkinsProxy;
    }

    public static boolean hasProxy(){
        return getHost()!=null && getPort()!=0;
    }

    public static boolean hasSystemProxy(){
        return systemProxyHost!=null && systemProxyPort!=0;
    }

}
