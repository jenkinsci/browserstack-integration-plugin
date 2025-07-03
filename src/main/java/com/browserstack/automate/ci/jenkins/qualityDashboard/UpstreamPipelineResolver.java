package com.browserstack.automate.ci.jenkins.qualityDashboard;

import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.fasterxml.jackson.core.JsonProcessingException;
import hudson.model.*;
import jenkins.model.Jenkins;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class UpstreamPipelineResolver {
    
    private static final QualityDashboardAPIUtil apiUtil = new QualityDashboardAPIUtil();

    /**
     * Centralized method to handle JsonProcessingException logging.
     * This reduces duplication of exception handling code throughout the class.
     * 
     * @param browserStackCredentials The credentials required for logging
     * @param message The error message to log
     */
    private static void logError(BrowserStackCredentials browserStackCredentials, String message) {
        try {
            apiUtil.logToQD(browserStackCredentials, message);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to log error: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Resolves the root upstream project with its build number in the format "project#build".
     * This method returns both the project name and build number, which is useful for
     * identifying the root project in the pipeline chain.
     * 
     * @param run The current build run
     * @param browserStackCredentials The BrowserStack credentials for API access
     * @return The root upstream project with build number in format "project#build", or null if none found
     */
    public static String resolveRootUpstreamProject(Run<?, ?> run, BrowserStackCredentials browserStackCredentials) {
        try {
            Set<String> visitedProjects = new HashSet<>();
            return findRootUpstreamProject(run, browserStackCredentials, visitedProjects);
        } catch (Exception e) {
            logError(browserStackCredentials, "Error resolving root upstream project for " + 
                     getProjectName(run) + "#" + run.getNumber() + ": " + e.getMessage());
            return null;
        }
    }
    
    private static String findRootUpstreamProject(Run<?, ?> run, BrowserStackCredentials browserStackCredentials, 
                                                 Set<String> visitedProjects) {
        if (run == null) {
            return null;
        }
        
        String currentProject = getProjectName(run);
        
        // Cycle detection - prevent infinite recursion
        if (visitedProjects.contains(currentProject)) {
            logError(browserStackCredentials, "Circular dependency detected in project: " + currentProject);
            return null;
        }
        
        visitedProjects.add(currentProject);
        
        List<Cause> causes = run.getCauses();
        if (causes == null || causes.isEmpty()) {
            // No causes found - this is likely a root project
            return null;
        }
        
        String rootProject = null;
        
        for (Cause cause : causes) {
            String upstreamProject = processUpstreamCause(cause, run, browserStackCredentials, visitedProjects);
            if (upstreamProject != null) {
                // If we found an upstream project, that becomes our root candidate
                rootProject = upstreamProject;
                break; // Use first valid upstream cause
            }
        }
        
        return rootProject;
    }
    
    private static String processUpstreamCause(Cause cause, Run<?, ?> run, 
                                             BrowserStackCredentials browserStackCredentials, 
                                             Set<String> visitedProjects) {
        try {
            if (cause instanceof Cause.UpstreamCause) {
                return handleUpstreamCause((Cause.UpstreamCause) cause, browserStackCredentials, visitedProjects);
            } else if (cause instanceof hudson.triggers.TimerTrigger.TimerTriggerCause) {
                apiUtil.logToQD(browserStackCredentials, "Build triggered by timer/schedule");
                return null;
            } else if (cause instanceof hudson.triggers.SCMTrigger.SCMTriggerCause) {
                apiUtil.logToQD(browserStackCredentials, "Build triggered by SCM change");
                return null;
            } else if (cause instanceof Cause.UserIdCause) {
                Cause.UserIdCause userCause = (Cause.UserIdCause) cause;
                apiUtil.logToQD(browserStackCredentials, "Build triggered manually by user: " + 
                         getUserDisplayName(userCause));
                return null;
            } else if (cause instanceof Cause.RemoteCause) {
                Cause.RemoteCause remoteCause = (Cause.RemoteCause) cause;
                apiUtil.logToQD(browserStackCredentials, "Build triggered remotely from: " + 
                         remoteCause.getAddr());
                return null;
            } else {
                // Handle unknown cause types
                apiUtil.logToQD(browserStackCredentials, "Unknown build cause type: " + 
                         cause.getClass().getSimpleName());
                return null;
            }
        } catch (JsonProcessingException e) {
            logError(browserStackCredentials, "Error processing cause: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Handles upstream cause by recursively finding the root upstream project and its build number.
     * This method always returns the project name and build number in the format "project#build".
     * 
     * @param upstreamCause The upstream cause to process
     * @param browserStackCredentials The BrowserStack credentials for API access
     * @param visitedProjects Set of already visited projects to detect cycles
     * @return The root upstream project with build number in format "project#build", or null if none found
     */
    private static String handleUpstreamCause(Cause.UpstreamCause upstreamCause, 
                                            BrowserStackCredentials browserStackCredentials, 
                                            Set<String> visitedProjects) {
        try {
            String upstreamProjectName = upstreamCause.getUpstreamProject();
            int upstreamBuildNumber = upstreamCause.getUpstreamBuild();
            
            if (upstreamProjectName == null || upstreamProjectName.trim().isEmpty()) {
                apiUtil.logToQD(browserStackCredentials, "Invalid upstream project name");
                return null;
            }
            
            apiUtil.logToQD(browserStackCredentials, "Found upstream: " + upstreamProjectName + 
                     "#" + upstreamBuildNumber);
        
        // Try to get the upstream run for recursive traversal
        Run<?, ?> upstreamRun = getUpstreamRun(upstreamProjectName, upstreamBuildNumber);
        if (upstreamRun != null) {
            // Recursively check if this upstream has its own upstream
            String rootProject = findRootUpstreamProject(upstreamRun, browserStackCredentials, visitedProjects);
            if (rootProject != null) {
                // Found a higher-level upstream, return that
                return rootProject;
            }
        }
        // Return in format projectName#buildNumber
        String formattedResult = upstreamProjectName + "#" + upstreamBuildNumber;
        apiUtil.logToQD(browserStackCredentials, "Resolved root upstream project: " + formattedResult);
        return formattedResult;
        } catch (JsonProcessingException e) {
            logError(browserStackCredentials, "Error processing upstream cause: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logError(browserStackCredentials, "Unexpected error while handling upstream cause: " + e.getMessage());
            return null;
        }
    }
    
    private static Run<?, ?> getUpstreamRun(String projectName, int buildNumber) {
        try {
            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins == null) {
                return null;
            }
            
            Job<?, ?> job = jenkins.getItemByFullName(projectName, Job.class);
            if (job == null) {
                return null;
            }
            
            return job.getBuildByNumber(buildNumber);
        } catch (Exception e) {
            // upstream build might not exist anymore
            return null;
        }
    }
    
    private static String getProjectName(Run<?, ?> run) {
        try {
            return run.getParent().getFullName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static String getUserDisplayName(Cause.UserIdCause userCause) {
        try {
            String userName = userCause.getUserName();
            String userId = userCause.getUserId();
            return userName != null ? userName : userId;
        } catch (Exception e) {
            return "unknown user";
        }
    }
    
    public static String resolveImmediateUpstreamProject(Run<?, ?> run, BrowserStackCredentials browserStackCredentials) {
        try {
            return findImmediateUpstreamProject(run, browserStackCredentials);
        } catch (Exception e) {
            logError(browserStackCredentials, "Error resolving immediate upstream project for " + 
                     getProjectName(run) + "#" + run.getNumber() + ": " + e.getMessage());
            return null;
        }
    }
    
    public static String resolveImmediateUpstreamProjectForQEI(Run<?, ?> run, BrowserStackCredentials browserStackCredentials) {
        try {
            return findImmediateUpstreamProjectForQEI(run, browserStackCredentials);
        } catch (Exception e) {
            logError(browserStackCredentials, "Error resolving immediate upstream project for QEI for " + 
                     getProjectName(run) + "#" + run.getNumber() + ": " + e.getMessage());
            return null;
        }
    }
    
    private static String findImmediateUpstreamProject(Run<?, ?> run, BrowserStackCredentials browserStackCredentials) throws JsonProcessingException {
        if (run == null) {
            return null;
        }
        
        List<Cause> causes = run.getCauses();
        if (causes == null || causes.isEmpty()) {
            return null;
        }
        
        for (Cause cause : causes) {
            if (cause instanceof Cause.UpstreamCause) {
                Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) cause;
                String upstreamProjectName = upstreamCause.getUpstreamProject();
                
                if (upstreamProjectName != null && !upstreamProjectName.trim().isEmpty()) {
                    apiUtil.logToQD(browserStackCredentials, "Found immediate upstream: " + upstreamProjectName);
                    return upstreamProjectName;
                }
            }
        }
        
        apiUtil.logToQD(browserStackCredentials, "No immediate upstream found for: " + getProjectName(run));
        return null;
    }
    
    private static String findImmediateUpstreamProjectForQEI(Run<?, ?> run, BrowserStackCredentials browserStackCredentials) throws JsonProcessingException {
        if (run == null) {
            return null;
        }
        
        List<Cause> causes = run.getCauses();
        if (causes == null || causes.isEmpty()) {
            return null;
        }
        
        for (Cause cause : causes) {
            if (cause instanceof Cause.UpstreamCause) {
                Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) cause;
                String upstreamProjectName = upstreamCause.getUpstreamProject();
                int upstreamBuildNumber = upstreamCause.getUpstreamBuild();
                
                if (upstreamProjectName != null && !upstreamProjectName.trim().isEmpty()) {
                    String formattedResult = upstreamProjectName + "#" + upstreamBuildNumber;
                    apiUtil.logToQD(browserStackCredentials, "Found immediate upstream for QEI: " + formattedResult);
                    return formattedResult;
                }
            }
        }
        
        apiUtil.logToQD(browserStackCredentials, "No immediate upstream found with BuildNumber: " + getProjectName(run));
        return null;
    }
    
}
