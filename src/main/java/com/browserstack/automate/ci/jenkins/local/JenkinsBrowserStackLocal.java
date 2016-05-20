package com.browserstack.automate.ci.jenkins.local;

import com.browserstack.local.Local;
import hudson.Launcher;
import hudson.Proc;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JenkinsBrowserStackLocal extends Local {

    private final Launcher launcher;
    private final String accesskey;
    private final String binaryHome;
    private final String[] arguments;

    public JenkinsBrowserStackLocal(Launcher launcher, String accesskey, String binaryHome, String[] arguments) {
        this.launcher = launcher;
        this.accesskey = accesskey;
        this.binaryHome = binaryHome;
        this.arguments = arguments;
    }

    public void start() throws Exception {
        Map<String, String> localOptions = new HashMap<String, String>();
        localOptions.put("key", accesskey);

        if (StringUtils.isNotBlank(binaryHome)) {
            localOptions.put("binarypath", binaryHome);
        }

        super.start(localOptions);
    }

    @Override
    protected LocalProcess runCommand(List<String> command) throws IOException {
        if (isStartOrStop(command)) {
            for (String arg : arguments) {
                if (StringUtils.isNotBlank(arg)) {
                    command.add(arg.trim());
                }
            }
        }

        final Proc process = launcher.launch()
                .cmds(command)
                .readStdout()
                .readStderr()
                .start();

        return new LocalProcess() {
            public InputStream getInputStream() {
                return process.getStdout();
            }

            public InputStream getErrorStream() {
                return process.getStderr();
            }

            public int waitFor() throws Exception {
                return process.join();
            }
        };
    }

    private static boolean isStartOrStop(List<String> command) {
        return (command.size() > 2 && command.get(2).toLowerCase().matches("start|stop"));
    }
}
