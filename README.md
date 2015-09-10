Slack Plugin for Graylog
========================

[![Build Status](https://travis-ci.org/Graylog2/graylog-plugin-slack.svg)](https://travis-ci.org/Graylog2/graylog-plugin-slack)

![](https://s3.amazonaws.com/graylog2public/images/plugin-alarmcallback-slack-1.png)

This plugin can notify [Slack](https://www.slack.com) channels about triggered alerts in Graylog.

**Required Graylog version:** 1.0 and later

## Installation

[Download the plugin](https://github.com/Graylog2/graylog-plugin-hipchat/releases)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

## Usage

#### Step 1: Create Slack API token

The plugin configuration asks for a Slack API token which can be created by visiting [https://api.slack.com/web](https://api.slack.com/web) and press "Create token". (make sure you are logged in)

#### Step 2: Create alarm callback

Create a "Slack alarm callback" on the "Manage alerts" page of your stream. Enter the requested configuration and save. Make sure you also configured alert conditions for the stream so that the alerts are actually triggered.

## Build

This project is using Maven and requires Java 7 or higher.

You can build a plugin (JAR) with `mvn package`.

DEB and RPM packages can be build with `mvn jdeb:jdeb` and `mvn rpm:rpm` respectively.

## Plugin Release

We are using the maven release plugin:

```
$ mvn release:prepare
[...]
$ mvn release:perform
```

This sets the version numbers, creates a tag and pushes to GitHub. TravisCI will build the release artifacts and upload to GitHub automatically.
