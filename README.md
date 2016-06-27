# BrowserStack Jenkins Plugin
==============================

Git Branch | Build | Coverage
-----------|-------|---------
master | [![Build Status](https://travis-ci.org/browserstack/jenkins-plugin.svg?branch=master)](https://travis-ci.org/browserstack/jenkins-plugin) | [![codecov](https://codecov.io/gh/browserstack/jenkins-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/browserstack/jenkins-plugin)
develop | [![Build Status](https://travis-ci.org/browserstack/jenkins-plugin.svg?branch=develop)](https://travis-ci.org/browserstack/jenkins-plugin) | [![codecov](https://codecov.io/gh/browserstack/jenkins-plugin/branch/develop/graph/badge.svg)](https://codecov.io/gh/browserstack/jenkins-plugin)

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [BrowserStack Jenkins Plugin](#browserstack-jenkins-plugin)
  - [Features](#features)
  - [Prerequisites](#prerequisites)
  - [Building the Plugin](#building-the-plugin)
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

To build the plugin *hpi* package, run  `mvn clean package`. This will compile the code, run the unit tests and build the *hpi* package as well.

## Reporting Issues

Before creating an Issue please read the [JIRA guidelines](https://wiki.jenkins-ci.org/display/JENKINS/How+to+report+an+issue).

* Create a [Bug](https://issues.jenkins-ci.org/secure/CreateIssue.jspa?pid=10172&issuetype=1&components=19622).
* Request for a [New Feature](https://issues.jenkins-ci.org/secure/CreateIssue.jspa?pid=10172&issuetype=2&components=19622).
* Request for an [Improvement](https://issues.jenkins-ci.org/secure/CreateIssue.jspa?pid=10172&issuetype=4&components=19622) in current functionality.
