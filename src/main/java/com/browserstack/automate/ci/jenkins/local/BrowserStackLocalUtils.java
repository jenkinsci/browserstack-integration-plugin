package com.browserstack.automate.ci.jenkins.local;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;
import java.io.PrintStream;
import hudson.Launcher;

public class BrowserStackLocalUtils {

  public static void stopBrowserStackLocal(JenkinsBrowserStackLocal browserStackLocal,
      Launcher launcher, PrintStream logger) throws Exception {
    if (browserStackLocal != null) {
      log(logger, "Local: Stopping BrowserStack Local...");
      try {
        browserStackLocal.stop(launcher);
        log(logger, "Local: Stopped");
      } catch (Exception e) {
        log(logger, "Local: ERROR: " + e.getMessage());
      }
    }
  }

}
