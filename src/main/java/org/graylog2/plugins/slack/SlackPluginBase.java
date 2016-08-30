package org.graylog2.plugins.slack;

import com.google.common.base.CharMatcher;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.streams.Stream;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

public class SlackPluginBase {

    private static final CharMatcher NAME_MATCHER = CharMatcher.inRange('a', 'z')
            .or(CharMatcher.inRange('A', 'Z'))
            .or(CharMatcher.inRange('0', '9'))
            .or(CharMatcher.anyOf("_-#@"))
            .precomputed();

    public static final String CK_WEBHOOK_URL = "webhook_url";
    public static final String CK_CHANNEL = "channel";
    public static final String CK_USER_NAME = "user_name";
    public static final String CK_NOTIFY_CHANNEL = "notify_channel";
    public static final String CK_ADD_ATTACHMENT = "add_attachment";
    public static final String CK_SHORT_MODE = "short_mode";
    public static final String CK_LINK_NAMES = "link_names";
    public static final String CK_ICON_URL = "icon_url";
    public static final String CK_ICON_EMOJI = "icon_emoji";
    public static final String CK_GRAYLOG2_URL = "graylog2_url";
    public static final String CK_PROXY_ADDRESS = "proxy_address";
    public static final String CK_COLOR = "color";

    public static ConfigurationRequest configuration() {
        final ConfigurationRequest configurationRequest = new ConfigurationRequest();

        configurationRequest.addField(new TextField(
                        CK_WEBHOOK_URL, "Webhook URL", "", "Slack \"Incoming Webhook\" URL",
                        ConfigurationField.Optional.NOT_OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                        CK_CHANNEL, "Channel", "#channel", "Name of Slack #channel or @user for a direct message.",
                        ConfigurationField.Optional.NOT_OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                        CK_USER_NAME, "User name", "Graylog",
                        "User name of the sender in Slack",
                        ConfigurationField.Optional.OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                        CK_COLOR, "Color", "#FF0000",
                        "Color to use for Slack message",
                        ConfigurationField.Optional.NOT_OPTIONAL)
        );
        configurationRequest.addField(new BooleanField(
                        CK_ADD_ATTACHMENT, "Include more information", true,
                        "Add structured information as message attachment")
        );
        configurationRequest.addField(new BooleanField(
                        CK_SHORT_MODE, "Short mode", false,
                        "Enable short mode? This strips down the Slack message to the bare minimum to take less space in the chat room. " +
                                "Not used in alarm callback but only in the message output module.")
        );
        configurationRequest.addField(new BooleanField(
                        CK_NOTIFY_CHANNEL, "Notify Channel", false,
                        "Notify all users in channel by adding @channel to the message.")
        );
        configurationRequest.addField(new BooleanField(
                        CK_LINK_NAMES, "Link names", true,
                        "Find and link channel names and user names")
        );
        configurationRequest.addField(new TextField(
                        CK_ICON_URL, "Icon URL", null,
                        "Image to use as the icon for this message",
                        ConfigurationField.Optional.OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                        CK_ICON_EMOJI, "Icon Emoji", null,
                        "Emoji to use as the icon for this message (overrides Icon URL)",
                        ConfigurationField.Optional.OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                CK_GRAYLOG2_URL, "Graylog URL", null,
                "URL to your Graylog web interface. Used to build links in alarm notification.",
                ConfigurationField.Optional.OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                CK_PROXY_ADDRESS, "Proxy", null,
                "Please insert the proxy information in the follwoing format: <ProxyAddress>:<Port>",
                ConfigurationField.Optional.OPTIONAL)
        );

        return configurationRequest;
    }

    public static void checkConfiguration(Configuration configuration) throws ConfigurationException {
        if (!configuration.stringIsSet(CK_WEBHOOK_URL)) {
            throw new ConfigurationException(CK_WEBHOOK_URL + " is mandatory and must not be empty.");
        }

        if (!configuration.stringIsSet(CK_CHANNEL)) {
            throw new ConfigurationException(CK_CHANNEL + " is mandatory and must not be empty.");
        }

        if (!configuration.stringIsSet(CK_COLOR)) {
            throw new ConfigurationException(CK_COLOR + " is mandatory and must not be empty.");
        }

        if (!configuration.stringIsSet(CK_USER_NAME)) {
            throw new ConfigurationException(CK_USER_NAME + " is mandatory and must not be empty.");
        }
        if (configuration.stringIsSet(CK_PROXY_ADDRESS)) {
        	try{
        		String[] url_and_port = configuration.getString(CK_PROXY_ADDRESS).split(":");
        		InetSocketAddress sockAddress = new InetSocketAddress(url_and_port[0], Integer.valueOf(url_and_port[1]));
        		if (sockAddress.isUnresolved()) {
        			throw new ConfigurationException("Couldn't resolve " + CK_PROXY_ADDRESS +".");
        		}
        	} catch(Exception e) {
        		throw new ConfigurationException("Couldn't parse " + CK_PROXY_ADDRESS + " correctly.", e);
        	}
        	
        }

        // work around for "null" string bug in graylog-server v1.2
        if (configuration.stringIsSet(CK_ICON_URL) && !configuration.getString(CK_ICON_URL).equals("null")) {
            try {
                final URI iconUri = new URI(configuration.getString(CK_ICON_URL));

                if (!"http".equals(iconUri.getScheme()) && !"https".equals(iconUri.getScheme())) {
                    throw new ConfigurationException(CK_ICON_URL + " must be a valid HTTP or HTTPS URL.");
                }
            } catch (URISyntaxException e) {
                throw new ConfigurationException("Couldn't parse " + CK_ICON_URL + " correctly.", e);
            }
        }

        // work around for "null" string bug in graylog-server v1.2
        if (configuration.stringIsSet(CK_GRAYLOG2_URL) && !configuration.getString(CK_GRAYLOG2_URL).equals("null")) {
            try {
                final URI graylog2Uri = new URI(configuration.getString(CK_GRAYLOG2_URL));

                if (!"http".equals(graylog2Uri.getScheme()) && !"https".equals(graylog2Uri.getScheme())) {
                    throw new ConfigurationException(CK_GRAYLOG2_URL + " must be a valid HTTP or HTTPS URL.");
                }
            } catch (URISyntaxException e) {
                throw new ConfigurationException("Couldn't parse " + CK_GRAYLOG2_URL + " correctly.", e);
            }
        }
    }

    protected String buildStreamLink(String baseUrl, Stream stream) {
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        return baseUrl + "streams/" + stream.getId() + "/messages?q=*&rangetype=relative&relative=3600";
    }

}
