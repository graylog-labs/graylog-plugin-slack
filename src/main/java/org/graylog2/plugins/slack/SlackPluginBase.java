package org.graylog2.plugins.slack;

import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugins.slack.configuration.SlackConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

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

    protected static void checkConfiguration(Configuration configuration) throws ConfigurationException {
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
                final URI uri = new URI(configuration.getString(settingName));
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

        return baseUrl + "streams/" + stream.getId() + "/messages?q=*&rangetype=relative&relative=3600";
    }

    protected static SlackMessage createSlackMessage(Configuration configuration, String message) {
        return new SlackMessage(
                configuration.getString(SlackConfiguration.CK_COLOR),
                configuration.getString(SlackConfiguration.CK_ICON_EMOJI),
                configuration.getString(SlackConfiguration.CK_ICON_URL),
                message,
                configuration.getString(SlackConfiguration.CK_USER_NAME),
                configuration.getString(SlackConfiguration.CK_CHANNEL),
                configuration.getBoolean(SlackConfiguration.CK_LINK_NAMES)
        );
    }

    protected String buildMessageLink(String baseUrl, String index, String id) {
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        return baseUrl + "messages/" + index + "/" + id;
    }

}
