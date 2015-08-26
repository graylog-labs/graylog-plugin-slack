# Slack alarm callback plugin for Graylog

![](https://s3.amazonaws.com/graylog2public/images/plugin-alarmcallback-slack-1.png)

[![Build Status](https://travis-ci.org/Graylog2/graylog-plugin-slack.svg)](https://travis-ci.org/Graylog2/graylog-plugin-alarmcallback-slack)

This plugin can notify [Slack](https://www.slack.com) channels about triggered alerts in Graylog.

## Instructions

#### Step 1: Create Slack API token

The plugin configuration asks for a Slack API token which can be created by visiting [https://api.slack.com/web](https://api.slack.com/web) and press "Create token". (make sure you are logged in)

#### Step 2: Install plugin

Copy the `.jar` file to the Graylog plugin directory which is configured in your `graylog.conf` configuration file. (`plugin_dir` variable)

Restart the `graylog-server` service so it will load the new plugin.

#### Step 3: Create alarm callback

Create a "Slack alarm callback" on the "Manage alerts" page of your stream. Enter the requested configuration and save. Make sure you also configured alert conditions for the stream so that the alerts are actually triggered.

## How to build

This project is using Maven and requires Java 7 or higher.

You can build a plugin (JAR) with `mvn package`. DEB and RPM packages can be build with `mvn jdeb:jdeb` and `mvn rpm:rpm` respectively.
