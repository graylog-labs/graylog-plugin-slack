Slack Plugin for Graylog
========================

[![Build Status](https://travis-ci.org/Graylog2/graylog-plugin-slack.svg)](https://travis-ci.org/Graylog2/graylog-plugin-slack)

![](https://github.com/Graylog2/graylog-plugin-slack/blob/master/screenshot.png)

This plugin can notify [Slack](https://www.slack.com) channels about triggered alerts in Graylog (Alarm Callback) and also forward each message routed into a stream (Message Output) in realtime.

**Required Graylog version:** 1.0 and later

## v2.0 upgrade notes

**Alarm callbacks created with previous plugin versions will be marked as invalid and should be removed using the web interface.** Please create completely new alarm callbacks. Besides that you can just replace the old .jar file in your plugins folder with the new one and restart graylog-server.

## Changes in v2.0

* We are now using the Slack Incoming Webhooks instead of API tokens. This way the plugin is no longer bound to a user.
* Now includes message output plugin. You can forward messages from a (low throughput) stream directly into Slack. For example for specific error messages or events that happen on your monitored devices and applications. (We are using to be notified about new [Graylog Marketplace](https://marketplace.graylog.org/) submissions for example.)
* You can now define a color to use. For example red for alerts and green for general notifications.
* Message can not only be sent to a channel but also as a private message to users. The syntax is *#channel* to send into a channel or *@username* for a private message.
* Several bugfixes and improvements

## Installation

[Download the plugin](https://github.com/Graylog2/graylog-plugin-slack/releases)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

## Usage

#### Step 1: Create Slack Incoming Webhook

[Create a new Slack Incoming Webhook](https://graylog.slack.com/services/new/incoming-webhook) and copy the URL it will present to you. It will ask you to select a Slack channel but you can override it in the plugin configuration later.

#### Step 2: Create alarm callback or message output

Create a "Slack alarm callback" on the "Manage alerts" page of your stream. Enter the requested configuration (use the Incoming Webhook URL you created in step 1) and save. Make sure you also configured alert conditions for the stream so that the alerts are actually triggered.

The same applies for message outputs which you can configure in *Stream* - > *Manage Outputs*.

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
