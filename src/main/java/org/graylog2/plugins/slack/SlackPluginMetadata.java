package org.graylog2.plugins.slack;

import org.graylog2.plugins.slack.callback.SlackAlarmCallback;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class SlackPluginMetadata implements PluginMetaData {
    @Override
    public String getUniqueId() {
        return SlackAlarmCallback.class.getCanonicalName();
    }

    @Override
    public String getName() {
        return "Slack";
    }

    @Override
    public String getAuthor() {
        return "Graylog, Inc.";
    }

    @Override
    public URI getURL() {
        return URI.create("https://www.graylog.org");
    }

    @Override
    public Version getVersion() {
        return new Version(2, 3, 0);
    }

    @Override
    public String getDescription() {
        return "Slack plugin to forward messages or write alarms to Slack chat rooms.";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(2, 0, 0);
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
