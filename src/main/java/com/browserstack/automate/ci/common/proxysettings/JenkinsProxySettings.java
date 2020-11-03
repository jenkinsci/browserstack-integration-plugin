package com.browserstack.automate.ci.common.proxysettings;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import okhttp3.Credentials;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class JenkinsProxySettings {
    static final ProxyConfiguration p = Jenkins.getInstanceOrNull()!=null ? Jenkins.getInstanceOrNull().proxy : null;
    public static Proxy getJenkinsProxy(){
        String proxyHost = p.name;
        int proxyPort = p.port;
        return (proxyHost!=null && proxyPort!=0) ? new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)): null;
    }
    public static String getBasicCredentials(){
        String proxyusername = p.getUserName();
        String proxyPassword = p.getPassword();
        if(proxyusername!=null && proxyPassword!=null){
            String credential = Credentials.basic(proxyusername, proxyPassword);
            return credential;
        }
        return null;
    }
    public static String getHost(){
        if(p==null) return null;
        return p.name;
    }
    public static int getPort(){
        if(p==null) return 0;
        return p.port;
    }
    public static String getUsername(){
        if(p==null) return null;
        return p.getUserName();
    }
    public static String getPassword(){
        if(p==null) return null;
        return p.getPassword();
    }
    public static String getNoProxyHost(){
        if(p==null) return null;
        return p.noProxyHost;
    }
    public static ProxyConfiguration getProxyConfig(){
        return p;
    }
    public static boolean hasProxy(){
        return getHost()!=null && getPort()!=0;
    }

}
