# BrowserStack Jenkins Plugin
==============================

Git Branch | Build | Coverage
-----------|-------|---------
master | [![Build Status](https://travis-ci.org/browserstack/jenkins-plugin.svg?branch=master)](https://travis-ci.org/browserstack/jenkins-plugin) | [![codecov](https://codecov.io/gh/browserstack/jenkins-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/browserstack/jenkins-plugin)
develop | [![Build Status](https://travis-ci.org/browserstack/jenkins-plugin.svg?branch=develop)](https://travis-ci.org/browserstack/jenkins-plugin) | [![codecov](https://codecov.io/gh/browserstack/jenkins-plugin/branch/develop/graph/badge.svg)](https://codecov.io/gh/browserstack/jenkins-plugin)

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Building the Plugin](#building-the-plugin)
  - [For Testing](#for-testing)
  - [For Release](#for-release)
- [Reporting Issues](#reporting-issues)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

This plugin allows you to integrate your Selenium tests in Jenkins with BrowserStack Automate.  

## Features
1. Setup and teardown BrowserStackLocal for testing internal,dev or staging environments. 
2. Embed BrowserStack Automate Report in your Jenkins job results.
3. Manage BrowserStack credentials in a central location for all your BrowserStack builds.

## Prerequisites
1. Minimum Jenkins version supported is 1.509.
2. For viewing the BrowserStack Automate report in your Jenkins job results, JUnit plugin must be installed.
3. The build must have the BrowserStack build tool plugin. Currently there are plugins for the following build tools,
  * maven
  
## Building the Plugin

### For Testing

When building the plugin package for internal testing build the *hpi* package using `mvn clean package`. This will compile the code, run unit tests and build the *hpi* package.
The Google Analytics tracking id that will be used by default will be the one for testing.

### For Release

When building the plugin package for users to install in their Jenkins instance using the command `mvn clean package -Prelease`. 
This will do the same thing as when building the plugin **For Testing** but the production Google Analytics tracking id will be used for 
tracking analytics, if the user has it enabled.

## Reporting Issues

Before creating an Issue please read the [JIRA guidelines](https://wiki.jenkins-ci.org/display/JENKINS/How+to+report+an+issue).

* Create a [Bug](https://issues.jenkins-ci.org/secure/CreateIssue.jspa?pid=10172&issuetype=1&components=19622).
* Request for a [New Feature](https://issues.jenkins-ci.org/secure/CreateIssue.jspa?pid=10172&issuetype=2&components=19622).
* Request for an [Improvement](https://issues.jenkins-ci.org/secure/CreateIssue.jspa?pid=10172&issuetype=4&components=19622) in current functionality.

## For Code formatting
Code style files are their in code_formatter folder, for eclipse and intellij idea:
* For Eclipse : Under Window/Preferences select Java/Code Style/Formatter. Import the settings file by selecting Import.
* For Intellij Idea : Settings → Code Style → Java, click Manage, and import that XML file by simply clicking Import.

