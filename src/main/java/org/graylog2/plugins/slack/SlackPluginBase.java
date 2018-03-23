package org.graylog2.plugins.slack;

import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugins.slack.configuration.SlackConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;

public class SlackPluginBase {

    protected Configuration configuration;

    public void setConfiguration(final Configuration config) throws ConfigurationException {
        this.configuration = config;

        try {
            checkConfiguration(config);
        } catch (ConfigurationException e) {
            throw new ConfigurationException("Configuration error. " + e.getMessage());
        }
    }

    protected void checkConfiguration(Configuration configuration) throws ConfigurationException {
        if (!configuration.stringIsSet(SlackConfiguration.CK_WEBHOOK_URL)) {
            throw new ConfigurationException(SlackConfiguration.CK_WEBHOOK_URL + " is mandatory and must not be empty.");
        }

        if (!configuration.stringIsSet(SlackConfiguration.CK_CHANNEL)) {
            throw new ConfigurationException(SlackConfiguration.CK_CHANNEL + " is mandatory and must not be empty.");
        }

        if (!configuration.stringIsSet(SlackConfiguration.CK_COLOR)) {
            throw new ConfigurationException(SlackConfiguration.CK_COLOR + " is mandatory and must not be empty.");
        }

        checkUri(configuration, SlackConfiguration.CK_PROXY_ADDRESS);
        checkUri(configuration, SlackConfiguration.CK_ICON_URL);
        checkUri(configuration, SlackConfiguration.CK_GRAYLOG2_URL);
    }

    private static void checkUri(Configuration configuration, String settingName) throws ConfigurationException {
        if (configuration.stringIsSet(settingName)) {
            try {
                final URI uri = new URI(Objects.requireNonNull(configuration.getString(settingName)));
                if (!isValidUriScheme(uri, "http", "https")) {
                    throw new ConfigurationException(settingName + " must be a valid HTTP or HTTPS URL.");
                }
            } catch (URISyntaxException e) {
                throw new ConfigurationException("Couldn't parse " + settingName + " correctly.", e);
            }
        }
    }

    private static boolean isValidUriScheme(URI uri, String... validSchemes) {
        return uri.getScheme() != null && Arrays.binarySearch(validSchemes, uri.getScheme(), null) >= 0;
    }

    protected String buildStreamLink(String baseUrl, Stream stream) {
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        return baseUrl + "streams/" + stream.getId() + "/messages?q=%2A&rangetype=relative&relative=3600";
    }

    protected static SlackMessage createSlackMessage(Configuration configuration, String message) {
        String color = configuration.getString(SlackConfiguration.CK_COLOR);
        String emoji = configuration.getString(SlackConfiguration.CK_ICON_EMOJI);
        String url = configuration.getString(SlackConfiguration.CK_ICON_URL);
        String user = configuration.getString(SlackConfiguration.CK_USER_NAME);
        String channel = configuration.getString(SlackConfiguration.CK_CHANNEL);

        //Note: Link names if notify channel or else the channel tag will be plain text.
        boolean linkNames = configuration.getBoolean(SlackConfiguration.CK_LINK_NAMES) ||
                configuration.getBoolean(SlackConfiguration.CK_NOTIFY_CHANNEL);

        return new SlackMessage(color, emoji, url, message, user, channel, linkNames);
    }

    protected String buildMessageLink(String baseUrl, String index, String id) {
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        return baseUrl + "messages/" + index + "/" + id;
    }
}
