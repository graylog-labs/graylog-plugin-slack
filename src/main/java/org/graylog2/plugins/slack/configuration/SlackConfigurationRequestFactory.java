package org.graylog2.plugins.slack.configuration;

import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;

public class SlackConfigurationRequestFactory {

    public static ConfigurationRequest createSlackMessageOutputConfigurationRequest() {
        final ConfigurationRequest configurationRequest = createSlackAlarmCallbackConfigurationRequest();
        configurationRequest.addField(new BooleanField(
                SlackConfiguration.CK_SHORT_MODE, "Short mode", false,
                "Enable short mode? This strips down the Slack message to the bare minimum to take less space in the chat room. " +
                        "Not used in alarm callback but only in the message output module.")
        );

        return configurationRequest;
    }

    public static ConfigurationRequest createSlackAlarmCallbackConfigurationRequest() {
        final ConfigurationRequest configurationRequest = new ConfigurationRequest();

        configurationRequest.addField(new TextField(
                SlackConfiguration.CK_CUSTOM_MESSAGE, "Custom Message",
                "##########\n" +
                        "Alert Description: ${check_result.resultDescription}\n" +
                        "Date: ${check_result.triggeredAt}\n" +
                        "Stream ID: ${stream.id}\n" +
                        "Stream title: ${stream.title}\n" +
                        "Stream description: ${stream.description}\n" +
                        "Alert Condition Title: ${alert_condition.title}\n" +
                        "${if stream_url}Stream URL: ${stream_url}${end}\n" +
                        "\n" +
                        "Triggered condition: ${check_result.triggeredCondition}\n" +
                        "##########\n" +
                        "\n" +
                        "${if backlog}Last messages accounting for this alert:\n" +
                        "${foreach backlog message}${message}\n" +
                        "\n" +
                        "${end}${else}<No backlog>\n" +
                        "${end}\n",
                "Custom message to be appended below the alert title. " +
                        "The following properties are available for template building: " +
                        "\"stream\", " +
                        "\"check_result\"," +
                        " \"stream_url\"," +
                        " \"alert_condition\"," +
                        " \"backlog\"," +
                        " \"backlog_size\"." +
                        "See http://docs.graylog.org/en/2.3/pages/streams/alerts.html#email-alert-notification for more details.",
                ConfigurationField.Optional.OPTIONAL,
                TextField.Attribute.TEXTAREA)
        );

        configurationRequest.addField(new TextField(
                SlackConfiguration.CK_WEBHOOK_URL, "Webhook URL", "", "Slack \"Incoming Webhook\" URL",
                ConfigurationField.Optional.NOT_OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                SlackConfiguration.CK_CHANNEL, "Channel", "#channel", "Name of Slack #channel or @user for a direct message.",
                ConfigurationField.Optional.NOT_OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                SlackConfiguration.CK_USER_NAME, "User name", "Graylog",
                "User name of the sender in Slack",
                ConfigurationField.Optional.OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                SlackConfiguration.CK_COLOR, "Color", "#FF0000",
                "Color to use for Slack message",
                ConfigurationField.Optional.NOT_OPTIONAL)
        );
        configurationRequest.addField(new NumberField(
                SlackConfiguration.CK_ADD_BLITEMS, "Backlog items", 5,
                "Number of backlog item descriptions to attach")
        );

        configurationRequest.addField(new BooleanField(
                SlackConfiguration.CK_NOTIFY_CHANNEL, "Notify Channel", false,
                "Notify all users in channel by adding @channel to the message.")
        );
        configurationRequest.addField(new BooleanField(
                SlackConfiguration.CK_LINK_NAMES, "Link names", true,
                "Find and link channel names and user names")
        );
        configurationRequest.addField(new TextField(
                SlackConfiguration.CK_ICON_URL, "Icon URL", null,
                "Image to use as the icon for this message",
                ConfigurationField.Optional.OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                SlackConfiguration.CK_ICON_EMOJI, "Icon Emoji", null,
                "Emoji to use as the icon for this message (overrides Icon URL)",
                ConfigurationField.Optional.OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                SlackConfiguration.CK_GRAYLOG2_URL, "Graylog URL", null,
                "URL to your Graylog web interface. Used to build links in alarm notification.",
                ConfigurationField.Optional.OPTIONAL)
        );
        configurationRequest.addField(new TextField(
                SlackConfiguration.CK_PROXY_ADDRESS, "Proxy", null,
                "Please insert the proxy information in the follwoing format: <ProxyAddress>:<Port>",
                ConfigurationField.Optional.OPTIONAL)
        );

        return configurationRequest;
    }

}
