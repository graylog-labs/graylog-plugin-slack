package org.graylog2.plugins.slack.callback;

import com.google.common.collect.Lists;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugins.slack.SlackClient;
import org.graylog2.plugins.slack.SlackMessage;
import org.graylog2.plugins.slack.SlackPluginBase;
import org.graylog2.plugins.slack.StringReplacement;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
        final SlackClient client = new SlackClient(configuration);
        // load timestamp
        final String tsField = configuration.getString(CK_FOOTER_TS_FIELD);
        Long tsValue = null;
        if (!isNullOrEmpty(tsField)) {
            for (MessageSummary messageSummary : result.getMatchingMessages()) {
                try {
                    DateTime timestamp = null;
                    if ("timestamp".equals(tsField)) { // timestamp is reserved field in org.graylog2.notifications.NotificationImpl
                        timestamp = messageSummary.getTimestamp();
                    }
                    else {
                        Object value = messageSummary.getField(tsField);
                        if (value instanceof DateTime) {
                            timestamp = (DateTime)value;
                        } else {
                            timestamp = new DateTime(value, DateTimeZone.UTC);
                        }
                    }
                    tsValue = timestamp.getMillis() / 1000;
                    break; // use first occurance of timestamp
                } catch (NullPointerException | IllegalArgumentException e) {
                    // ignore
                }
            }
        }    
        // load footer text
        String footerText = configuration.getString(CK_FOOTER_TEXT);
        if (!isNullOrEmpty(footerText)) {
            List<MessageSummary> messageList = result.getMatchingMessages();
            if (messageList.size() > 0) {
                for (MessageSummary messageSummary : messageList) {
                    footerText = StringReplacement.replace(footerText, messageSummary.getRawMessage().getFields());
                }
            }
            else {
                footerText = StringReplacement.replace(footerText, Collections.emptyMap());
            }
        }

        SlackMessage message = new SlackMessage(
                configuration.getString(CK_COLOR),
                configuration.getString(CK_MESSAGE_ICON),
                buildMessage(stream, result),
                configuration.getString(CK_USER_NAME),
                configuration.getString(CK_CHANNEL),
                configuration.getBoolean(CK_LINK_NAMES),
                footerText,
                configuration.getString(CK_FOOTER_ICON_URL),
                tsValue
        );

        if (configuration.getBoolean(CK_ADD_STREAM_INFO)) {
            message.addAttachment(new SlackMessage.AttachmentField("Stream ID", stream.getId(), true));
            message.addAttachment(new SlackMessage.AttachmentField("Stream Title", stream.getTitle(), false));
            message.addAttachment(new SlackMessage.AttachmentField("Stream Description", stream.getDescription(), false));
        }

        // Add attachments if requested.
        final List<Message> backlogItems = getAlarmBacklog(result);
        int count = configuration.getInt(CK_ADD_BLITEMS);
        if (count > 0) {
            final int blSize = backlogItems.size();
            if (blSize < count) {
                count = blSize;
            }
            for (int i = 0; i < count; i++) {
                StringBuilder attachmentName = new StringBuilder();
                if (count > 1)
                    attachmentName.append('(').append(i+1).append(") ");
                attachmentName.append("Backlog");
                message.addAttachment(new SlackMessage.AttachmentField(attachmentName.toString(), backlogItems.get(i).getMessage(), false));
            }
        }

        // Add custom fields from backlog list
        final String customFields = configuration.getString(SlackPluginBase.CK_FIELDS);
        // We don't care backlog item setting. We will list every fields from every backlog items
        final int totalItem = backlogItems.size();
        if (!isNullOrEmpty(customFields) &&  totalItem > 0) {
            boolean shortMode = configuration.getBoolean(CK_SHORT_MODE);
            final String[] fields = customFields.split(",");
            int backlogIndex = 0;
            for (Message msg : backlogItems) {
                final int index = ++backlogIndex;
                Arrays.stream(fields)
                        .map(String::trim)
                        .forEach(f -> addAttachment(index, f, shortMode, msg, message, totalItem));
            }
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

    public void addAttachment(int index, String name, boolean shortMode, Message message, SlackMessage slackMessage, int totalItem) {
        Object value = message.getField(name);
        if (value != null) {
            StringBuilder filedName = new StringBuilder();
            if (totalItem > 1) {
                filedName.append('(').append(index).append(") ");
            }
            filedName.append(name);
            final SlackMessage.AttachmentField attachment = new SlackMessage.AttachmentField(filedName.toString(), value.toString(), shortMode);
            slackMessage.addAttachment(attachment);
        }
    }

    public String buildMessage(Stream stream, AlertCondition.CheckResult result) {
        String graylogUri = configuration.getString(CK_GRAYLOG2_URL);
        String notifyUser = configuration.getString(CK_NOTIFY_USER);
        
        StringBuilder message = new StringBuilder();
        if (!isNullOrEmpty(notifyUser)) {
            List<MessageSummary> messageList = result.getMatchingMessages();
            if (messageList.size() > 0) {
                for (MessageSummary messageSummary : result.getMatchingMessages()) {
                    notifyUser = StringReplacement.replaceWithPrefix(notifyUser, "@", messageSummary.getRawMessage().getFields());
                }
            }
            else {
                notifyUser = StringReplacement.replace(notifyUser, Collections.emptyMap());
            }
            message.append(notifyUser).append(' ');
        }
        message.append("*Alert for Graylog stream ");
        if (!isNullOrEmpty(graylogUri)) {
            message.append('<').append(buildStreamLink(graylogUri, stream)).append('|').append(stream.getTitle()).append('>');
        } else {
            message.append('_').append(stream.getTitle()).append('_');
        }
        return message.append("*:\n> ").append(result.getResultDescription()).toString();
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
