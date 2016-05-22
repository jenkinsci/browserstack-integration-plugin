package com.browserstack.automate.ci.jenkins.local;

import com.browserstack.local.Local;
import hudson.Launcher;
import hudson.Proc;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class JenkinsBrowserStackLocal extends Local {
    private static final String OPTION_LOCAL_IDENTIFIER = "localIdentifier";

    private final Launcher launcher;
    private final String accesskey;
    private final String binaryHome;
    private final String[] arguments;
    private String localIdentifier;

    public JenkinsBrowserStackLocal(Launcher launcher, String accesskey, String binaryHome, String argString) {
        this.launcher = launcher;
        this.accesskey = accesskey;
        this.binaryHome = binaryHome;
        this.arguments = processLocalArguments((argString != null) ? argString.trim() : "");
    }

    public String[] processLocalArguments(final String argString) {
        String[] args = argString.split("\\s+");
        int localIdPos = 0;
        List<String> arguments = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].contains(OPTION_LOCAL_IDENTIFIER)) {
                localIdPos = i;
                if (i < args.length - 1 && args[i + 1] != null && !args[i + 1].startsWith("-")) {
                    localIdentifier = args[i + 1];
                    if (StringUtils.isNotBlank(localIdentifier)) {
                        return args;
                    }

                    // skip next, since already processed
                    i += 1;
                }

                continue;
            }

            arguments.add(args[i]);
        }

        localIdentifier = UUID.randomUUID().toString().replaceAll("\\-", "");
        arguments.add(localIdPos, localIdentifier);
        arguments.add(localIdPos, "-" + OPTION_LOCAL_IDENTIFIER);
        return arguments.toArray(new String[]{});
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

    public String getLocalIdentifier() {
        return localIdentifier;
    }

    private static boolean isStartOrStop(List<String> command) {
        return (command.size() > 2 && command.get(2).toLowerCase().matches("start|stop"));
    }
}
