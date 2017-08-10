package org.graylog2.plugins.slack;

import org.graylog2.plugins.slack.callback.SlackAlarmCallback;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class SlackPluginMetadata implements PluginMetaData {
    private static final String PLUGIN_PROPERTIES = "org.graylog.plugins.graylog-plugin-slack/graylog-plugin.properties";

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
        return Version.fromPluginProperties(this.getClass(), PLUGIN_PROPERTIES, "version", Version.from(2, 4, 0, "unknown"));
    }

    @Override
    public String getDescription() {
        return "Slack plugin to forward messages or write alarms to Slack chat rooms.";
    }

    @Override
    public Version getRequiredVersion() {
        return Version.fromPluginProperties(this.getClass(), PLUGIN_PROPERTIES, "graylog.version", Version..from(2, 0, 0, "unknown"));
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
