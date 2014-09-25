package org.graylog2.alarmcallbacks.slack;

import com.google.common.collect.Maps;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.streams.Stream;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class SlackAlarmCallback implements AlarmCallback {
    private static final String CK_API_TOKEN = "api_token";
    private static final String CK_CHANNEL = "channel";
    private static final String CK_USER_NAME = "user_name";
    private static final String CK_ADD_ATTACHMENT = "add_attachment";
    private static final String CK_LINK_NAMES = "link_names";
    private static final String CK_UNFURL_LINKS = "unfurl_links";
    private static final String CK_ICON_URL = "icon_url";
    private static final String CK_ICON_EMOJI = "icon_emoji";

    private Configuration configuration;

    @Override
    public void initialize(final Configuration config) throws AlarmCallbackConfigurationException {
        this.configuration = config;
    }

    @Override
    public void call(Stream stream, AlertCondition.CheckResult result) throws AlarmCallbackException {
        final SlackClient client = new SlackClient(
                configuration.getString(CK_API_TOKEN),
                configuration.getString(CK_CHANNEL),
                configuration.getString(CK_USER_NAME),
                configuration.getBoolean(CK_ADD_ATTACHMENT),
                configuration.getBoolean(CK_LINK_NAMES),
                configuration.getBoolean(CK_UNFURL_LINKS),
                configuration.getString(CK_ICON_URL),
                configuration.getString(CK_ICON_EMOJI));
        client.trigger(result.getTriggeredCondition());
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Maps.transformEntries(configuration.getSource(), new Maps.EntryTransformer<String, Object, Object>() {
            @Override
            public Object transformEntry(String key, Object value) {
                if (CK_API_TOKEN.equals(key)) {
                    return "****";
                }
                return value;
            }
        });
    }

    @Override
    public void checkConfiguration() throws ConfigurationException {
        if (!configuration.stringIsSet(CK_API_TOKEN)) {
            throw new ConfigurationException(CK_API_TOKEN + " is mandatory and must not be empty.");
        }

        if (!configuration.stringIsSet(CK_CHANNEL)) {
            throw new ConfigurationException(CK_CHANNEL + " is mandatory and must not be empty.");
        }

        // Names must be lower case and cannot contain spaces or periods.

        if (configuration.stringIsSet(CK_ICON_URL)) {
            try {
                final URI iconUri = new URI(configuration.getString(CK_ICON_URL));

                if(!"http".equals(iconUri.getScheme()) && !"https".equals(iconUri.getScheme())) {
                    throw new ConfigurationException(CK_ICON_URL + " must be a valid HTTP or HTTPS URL.");
                }
            } catch (URISyntaxException e) {
                throw new ConfigurationException("Couldn't parse " + CK_ICON_URL + " correctly.", e);
            }
        }
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        final ConfigurationRequest configurationRequest = new ConfigurationRequest();

        configurationRequest.addField(new TextField(
                        CK_API_TOKEN, "API Token", "", "Slack API authentication token",
                        ConfigurationField.Optional.NOT_OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                        CK_CHANNEL, "Channel", "", "ID or name of Slack channel",
                        ConfigurationField.Optional.NOT_OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                        CK_USER_NAME, "User name", "Graylog2",
                        "User name of the sender in Slack",
                        ConfigurationField.Optional.OPTIONAL)
        );
        configurationRequest.addField(new BooleanField(
                        CK_ADD_ATTACHMENT, "Add Attachment", true,
                        "Add structured information as message attachment")
        );
        configurationRequest.addField(new BooleanField(
                        CK_LINK_NAMES, "Link names", true,
                        "Find and link channel names and usernames")
        );
        configurationRequest.addField(new BooleanField(
                        CK_UNFURL_LINKS, "Unfurl links", true,
                        "Enable unfurling of primarily text-based content")
        );
        configurationRequest.addField(new TextField(
                        CK_ICON_URL, "Icon URL", null,
                        "Name of the sender in Slack",
                        ConfigurationField.Optional.OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                        CK_ICON_EMOJI, "Icon Emoji", null,
                        "Emoji to use as the icon for this message (overrides Icon URL)",
                        ConfigurationField.Optional.OPTIONAL)
        );

        return configurationRequest;
    }

    public String getName() {
        return "Slack alarm callback";
    }
}