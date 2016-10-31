package org.graylog2.plugins.slack.callback;

import org.graylog2.plugins.slack.SlackClient;
import org.graylog2.plugins.slack.SlackMessage;
import org.graylog2.plugins.slack.SlackPluginBase;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;

import com.google.common.collect.Lists;

import java.util.Map;
import java.util.Collections;
import java.util.List;


import static com.google.common.base.Strings.isNullOrEmpty;

public class SlackAlarmCallback extends SlackPluginBase implements AlarmCallback {

    private Configuration configuration;

    @Override
    public void initialize(final Configuration config) throws AlarmCallbackConfigurationException {
        this.configuration = config;

        try {
            checkConfiguration(config);
        } catch (ConfigurationException e) {
            throw new AlarmCallbackConfigurationException("Configuration error. " + e.getMessage());
        }
    }

    @Override
    public void call(Stream stream, AlertCondition.CheckResult result) throws AlarmCallbackException {
        final SlackClient client = new SlackClient(configuration.getString(CK_WEBHOOK_URL));

        SlackMessage message = new SlackMessage(
                configuration.getString(CK_COLOR),
                configuration.getString(CK_ICON_EMOJI),
                configuration.getString(CK_ICON_URL),
                buildMessage(stream, result),
                configuration.getString(CK_USER_NAME),
                configuration.getString(CK_CHANNEL),
                configuration.getBoolean(CK_LINK_NAMES)
        );

        // Add attachments if requested.
        final List<Message> backlogItems = getAlarmBacklog(result);

        if(configuration.getBoolean(CK_ADD_ATTACHMENT)) {
            message.addAttachment(new SlackMessage.AttachmentField("Stream ID", stream.getId(), true));
            message.addAttachment(new SlackMessage.AttachmentField("Stream Title", stream.getTitle(), false));
            message.addAttachment(new SlackMessage.AttachmentField("Stream Description", stream.getDescription(), false));
            
            int count = configuration.getInt(CK_ADD_BLITEMS);
            if(count < 1){
                count = 5; //Default items to show
            }
            final int blSize = backlogItems.size();
            if(blSize < count){
                count = blSize;
            }
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                sb.append(backlogItems.get(i).getMessage()).append("\n\n");
            }
            String attachmentName = "Backlog Items ("+Integer.toString(count)+")";
            message.addAttachment(new SlackMessage.AttachmentField(attachmentName, sb.toString(), false));
        }

        try {
            client.send(message);
        } catch (SlackClient.SlackClientException e) {
            throw new RuntimeException("Could not send message to Slack.", e);
        }
    }

    protected List<Message> getAlarmBacklog(AlertCondition.CheckResult result) {
        final AlertCondition alertCondition = result.getTriggeredCondition();
        final List<MessageSummary> matchingMessages = result.getMatchingMessages();

        final int effectiveBacklogSize = Math.min(alertCondition.getBacklog(), matchingMessages.size());

        if (effectiveBacklogSize == 0) {
            return Collections.emptyList();
        }

        final List<MessageSummary> backlogSummaries = matchingMessages.subList(0, effectiveBacklogSize);

        final List<Message> backlog = Lists.newArrayListWithCapacity(effectiveBacklogSize);

        for (MessageSummary messageSummary : backlogSummaries) {
            backlog.add(messageSummary.getRawMessage());
        }

        return backlog;
    }

    public String buildMessage(Stream stream, AlertCondition.CheckResult result) {
        String graylogUri = configuration.getString(CK_GRAYLOG2_URL);
        boolean notifyChannel = configuration.getBoolean(CK_NOTIFY_CHANNEL);

        String titleLink;
        if (isSet(graylogUri)) {
            titleLink = "<" + buildStreamLink(graylogUri, stream) + "|" + stream.getTitle() + ">";
        } else {
            titleLink = "_" + stream.getTitle() + "_";
        }

        final StringBuilder message = new StringBuilder(notifyChannel ? "@channel " : "");
        message.append("*Alert for Graylog stream ").append(titleLink).append("*:\n").append("> ").append(result.getResultDescription());

        return message.toString();
    }

    private final boolean isSet(String x) {
        // Bug in graylog-server v1.2: Empty values are stored as "null" String. This is a dirty workaround.
        return !isNullOrEmpty(x) && !x.equals("null");
    }

    @Override
    public Map<String, Object> getAttributes() {
        return configuration.getSource();
    }

    @Override
    public void checkConfiguration() throws ConfigurationException {
        /* Never actually called by graylog-server */
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        return configuration();
    }

    @Override
    public String getName() {
        return "Slack alarm callback";
    }
}