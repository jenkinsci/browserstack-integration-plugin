package com.browserstack.automate.ci.jenkins.local;

import com.browserstack.automate.ci.common.logger.PluginLogger;
import com.browserstack.local.Local;
import hudson.EnvVars;
import hudson.Launcher;
import jenkins.security.MasterToSlaveCallable;
import java.io.PrintStream;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JenkinsBrowserStackLocal extends Local implements Serializable {
    private static final String OPTION_LOCAL_IDENTIFIER = "localIdentifier";
    // local identifier doesn't override when user passes --local-identifier
    // Not replacing existing localIdentifier because of legacy reason 
    private static final String OPTION_LOCAL_IDENTIFIER_2 = "--local-identifier";

    private final String accesskey;
    private final String binarypath;
    private final String[] arguments;
    private String localIdentifier;
    private EnvVars envVars;
    private PrintStream logger;

    public JenkinsBrowserStackLocal(String accesskey, LocalConfig localConfig, String buildTag, EnvVars envVars, PrintStream logger) {
        this.accesskey = accesskey;
        this.binarypath = localConfig.getLocalPath();
        this.envVars = envVars;
        this.logger = logger;
        String localOptions = localConfig.getLocalOptions();
        this.arguments = processLocalArguments((localOptions != null) ? localOptions.trim() : "", buildTag);
    }

    private String[] processLocalArguments(final String argString, String buildTag) {
        String[] args = argString.split("\\s+");
        int localIdPos = 0;
        List<String> arguments = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].contains(OPTION_LOCAL_IDENTIFIER) || args[i].contains(OPTION_LOCAL_IDENTIFIER_2)) {
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
            
            // inject from environment variable if variable starts with $
            if (args[i].startsWith("$")) {
                String envVarName = args[i].substring(1);
            	PluginLogger.log(logger,
                        "Local: Replacing " + args[i] + " in local options with Environment variable "+ envVarName);
            	args[i] = envVars.get(envVarName);
            }

            arguments.add(args[i]);
        }

        localIdentifier = UUID.randomUUID().toString() + "-" + buildTag.replaceAll("[^\\w\\-\\.]", "_");

        arguments.add(localIdPos, localIdentifier);
        arguments.add(localIdPos, "-" + OPTION_LOCAL_IDENTIFIER);
        return arguments.toArray(new String[]{});
    }

    public void start() throws Exception {
        Map<String, String> localOptions = new HashMap<String, String>();
        localOptions.put("key", accesskey);
        if (binarypath != null && binarypath.length() > 0) localOptions.put("binarypath", binarypath);
        super.start(localOptions);
    }

    public void stop() throws Exception {
        Map<String, String> localOptions = new HashMap<String, String>();
        localOptions.put("key", accesskey);
        if (binarypath != null && binarypath.length() > 0) localOptions.put("binarypath", binarypath);
        super.stop(localOptions);
    }

    public void start(Launcher launcher) throws Exception {
        launcher.getChannel().call(new MasterToSlaveCallable<Void, Exception>() {
            @Override
            public Void call() throws Exception {
                JenkinsBrowserStackLocal.this.start();
                return null;
            }
        });
    }

    public void stop(Launcher launcher) throws Exception {
        launcher.getChannel().call(new MasterToSlaveCallable<Void, Exception>() {
            @Override
            public Void call() throws Exception {
                JenkinsBrowserStackLocal.this.stop();
                return null;
            }
        });
    }

    @Override
    protected LocalProcess runCommand(List<String> command) throws IOException {
        DaemonAction daemonAction = detectDaemonAction(command);
        if (daemonAction != null) {
            for (String arg : arguments) {
                if (StringUtils.isNotBlank(arg)) {
                    command.add(arg.trim());
                }
            }
        }

        return super.runCommand(command);
    }

    public String getLocalIdentifier() {
        return localIdentifier;
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
