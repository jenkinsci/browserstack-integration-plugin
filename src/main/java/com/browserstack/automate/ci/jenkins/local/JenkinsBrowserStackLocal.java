package com.browserstack.automate.ci.jenkins.local;

import com.browserstack.local.Local;
import hudson.Launcher;
import hudson.Proc;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JenkinsBrowserStackLocal extends Local {
    private static final String OPTION_LOCAL_IDENTIFIER = "localIdentifier";
    private static final Pattern PATTERN_PID = Pattern.compile("\"pid\":(\\d+)");

    private final Launcher launcher;
    private final String accesskey;
    private final String binaryHome;
    private final String[] arguments;
    private String localIdentifier;
    private int pid;

    public JenkinsBrowserStackLocal(Launcher launcher, String accesskey, String binaryHome, String argString) {
        this.launcher = launcher;
        this.accesskey = accesskey;
        this.binaryHome = binaryHome;
        this.arguments = processLocalArguments((argString != null) ? argString.trim() : "");
    }

    private String[] processLocalArguments(final String argString) {
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
        DaemonAction daemonAction = detectDaemonAction(command);
        if (pid > 0 && SystemUtils.IS_OS_WINDOWS && daemonAction == DaemonAction.STOP) {
            // temporary fix for daemon mode stop on Windows
            command = new ArrayList<String>();
            command.add("taskkill");
            command.add("/PID");
            command.add("" + pid);
            command.add("/F");
        } else if (daemonAction != null) {
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
                if (SystemUtils.IS_OS_WINDOWS) {
                    return processStdout(process.getStdout());
                }

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

    private InputStream processStdout(InputStream inputStream) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            Matcher matcher = PATTERN_PID.matcher(new String(baos.toByteArray()));
            if (matcher.find()) {
                pid = Integer.parseInt(matcher.group(1));
            }

            return new ByteArrayInputStream(baos.toByteArray());
        } catch (NumberFormatException e) {
            // invalid pid
        } catch (IOException e) {
            // ignore
        }

        return inputStream;
    }

    private static DaemonAction detectDaemonAction(List<String> command) {
        if (command.size() > 2) {
            String action = command.get(2).toLowerCase();
            if (action.equals("start")) {
                return DaemonAction.START;
            } else if (action.equals("stop")) {
                return DaemonAction.STOP;
            }
        }

        return null;
    }

    private enum DaemonAction {
        START, STOP
    }
}
