package com.browserstack.automate.ci.jenkins.util;

import com.browserstack.automate.AutomateClient;
import com.browserstack.automate.AutomateJenkinsClient;
import com.browserstack.client.exception.BrowserStackException;
import com.browserstack.client.model.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.browserstack.client.BrowserStackClient.Product.AUTOMATE;

public enum BrowserListingInfo {
    INSTANCE;

    private static final Pattern PATTERN_BROWSER_NAME = Pattern.compile("^(.*)\\s+\\d+(\\.\\d+)*$");

    private static final List<String> orderOs = Arrays.asList("windows 10", "windows 8.1", "windows 8", "windows 7", "windows xp", "os x el capitan", "os x yosemite", "os x mavericks", "os x mountain lion", "os x lion", "os x snow leopard", "ios", "android", "winphone");
    private static final List<String> orderBrowsersWin = Arrays.asList("edge", "ie", "firefox", "chrome", "opera", "yandex", "safari");
    private static final List<String> orderBrowsersMac = Arrays.asList("safari", "firefox", "chrome", "opera", "yandex");
    private Map<String, Platform> osMap = new HashMap<String, Platform>();
    private Map<String, BrowserStackObject> browserMap = new HashMap<String, BrowserStackObject>();
    private Map<String, List<String>> osBrowserMap = new HashMap<String, List<String>>();
    private Map<String, Browser> automateBrowsersMap = new HashMap<String, Browser>();
    private boolean isLoaded;


    BrowserListingInfo() {
        try {
            loadOsBrowserMaps();
        } catch (IOException localIOException) {
            // ignore
        }
    }

    public static BrowserListingInfo getInstance() {
        if (!INSTANCE.isLoaded) {
            try {
                INSTANCE.loadOsBrowserMaps();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return INSTANCE;
    }

    public static int getOsOrderIndex(String name) {
        return normalizeIndex(orderOs.indexOf(name), orderOs.size());
    }

    public static int getBrowserWinOrderIndex(String name) {
        return normalizeIndex(orderBrowsersWin.indexOf(name), orderBrowsersWin.size());
    }

    public static int getBrowserMacOrderIndex(String name) {
        return normalizeIndex(orderBrowsersMac.indexOf(name), orderBrowsersMac.size());
    }

    public static String getDisplayOs(String os) {
        if (os.equals("ios"))
            return "iOS";
        if (os.equals("android"))
            return "Android";
        if (os.equals("winphone")) {
            return "WinPhone";
        }
        return os;
    }

    public static String getDisplayKey(String os, String browser) {
        return (("" + os).trim() + "-" + ("" + browser).trim()).toLowerCase();
    }

    public static String extractBrowserName(String displayName) {
        Matcher matcher = PATTERN_BROWSER_NAME.matcher(displayName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return displayName;
    }

    private static int normalizeIndex(int index, int max) {
        return index < 0 ? max : index;
    }

    private static String getKey(BrowserStackObject browserStackObject) {
        if (browserStackObject instanceof Browser) {
            Browser browser = (Browser) browserStackObject;
            return (browser.getBrowser() + "-" + browser.getBrowserVersion() + "-" + browser.getOs() + "-" + browser.getOsVersion()).toLowerCase();

        } else if (browserStackObject instanceof Device) {
            Device device = (Device) browserStackObject;
            return (device.getDevice() + "-" + device.getAdditionalProperties().get("os") + "-" + device.getOsVersion()).toLowerCase();
        } else {
            return null;
        }
    }

    public Map<String, Platform> getOsMap() {
        return this.osMap;
    }

    public BrowserStackObject getDisplayBrowser(String os, String browser) {
        String key = getDisplayKey(os, browser);
        return this.browserMap.containsKey(key) ? this.browserMap.get(key) : null;
    }

    public Map<String, List<String>> getOsBrowserMap() {
        return this.osBrowserMap;
    }

    public void init(String username, String accessKey) throws IOException {
        if (!this.automateBrowsersMap.isEmpty()) {
            return;
        }

        try {
            AutomateClient client = new AutomateClient(username, accessKey);

            for (Browser browser : client.getBrowsers()) {
                String browserKey = getKey(browser);
                if (browserKey != null) {
                    this.automateBrowsersMap.put(browserKey, browser);
                }
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public BrowserStackObject getAutomateBrowser(BrowserStackObject displayBrowser) {
        Map<String, Object> browserProperties = null;

        if (displayBrowser instanceof Browser) {
            browserProperties = ((Browser) displayBrowser).getAdditionalProperties();
        } else if (displayBrowser instanceof Device) {
            browserProperties = ((Device) displayBrowser).getAdditionalProperties();
        }

        if (browserProperties != null) {
            String browserKey = browserProperties.containsKey("_key") ? browserProperties.get("_key").toString() : getKey(displayBrowser);

            if (browserKey != null && this.automateBrowsersMap.containsKey(browserKey)) {
                return this.automateBrowsersMap.get(browserKey);
            }
        }

        return null;
    }

    private void loadOsBrowserMaps() throws IOException {
        this.isLoaded = false;
        this.osMap.clear();
        this.browserMap.clear();
        this.osBrowserMap.clear();

        AutomateJenkinsClient client = new AutomateJenkinsClient();

        try {
            BrowserListing browserListing = client.getBrowsersForProduct(AUTOMATE);
            if (browserListing != null) {
                populateDesktopMaps(browserListing.getDesktopPlatforms());
                populateMobileMaps(browserListing.getMobilePlatforms());
                this.isLoaded = !this.osMap.isEmpty();
            }
        } catch (BrowserStackException e) {
            throw new IOException(e.getMessage());
        }
    }

    private void populateDesktopMaps(List<DesktopPlatform> desktopPlatforms) {
        for (DesktopPlatform desktopPlatform : desktopPlatforms) {

            String osDisplayName = desktopPlatform.getOsDisplayName().toLowerCase();
            this.osMap.put(osDisplayName, desktopPlatform);

            List<String> browserPerOsList = new ArrayList<String>();
            for (Browser browser : desktopPlatform.getBrowsers()) {
                if (browser.getOs() == null) {
                    browser.setOs(desktopPlatform.getOs());
                }

                if (browser.getOsVersion() == null) {
                    browser.setOsVersion(desktopPlatform.getOsVersion());
                }

                String browserKey = getKey(browser);
                if (browserKey != null) {
                    browser.setAdditionalProperty("_key", browserKey);
                }

                String browserDisplayName = browser.getDisplayName();
                this.browserMap.put(getDisplayKey(osDisplayName, browserDisplayName), browser);
                browserPerOsList.add(browserDisplayName);
            }

            this.osBrowserMap.put(osDisplayName, browserPerOsList);
        }
    }

    private void populateMobileMaps(List<MobilePlatform> mobilePlatforms) {
        for (MobilePlatform mobilePlatform : mobilePlatforms) {

            String osDisplayName = mobilePlatform.getOsDisplayName().toLowerCase();
            this.osMap.put(osDisplayName, mobilePlatform);

            List<String> devicePerOsList = new ArrayList<String>();
            for (Device device : mobilePlatform.getDevices()) {
                if (device.getAdditionalProperties().containsKey("os")) {
                    device.getAdditionalProperties().put("os", mobilePlatform.getOs());
                }

                String browserKey = getKey(device);
                if (browserKey != null) {
                    device.setAdditionalProperty("_key", browserKey);
                }

                String browserDisplayName = device.getDisplayName();
                this.browserMap.put(getDisplayKey(osDisplayName, browserDisplayName), device);
                devicePerOsList.add(browserDisplayName);
            }

            this.osBrowserMap.put(osDisplayName, devicePerOsList);
        }
    }
}