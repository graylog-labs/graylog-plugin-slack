package org.graylog2.plugins.slack.callback;

import com.floreysoft.jmte.Engine;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
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
import org.graylog2.plugins.slack.*;
import org.graylog2.plugins.slack.configuration.SlackConfiguration;
import org.graylog2.plugins.slack.configuration.SlackConfigurationRequestFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SlackAlarmCallback extends SlackPluginBase implements AlarmCallback {
    private static final String DELIMITER = " | ";
    private Configuration configuration;
    private final Engine templateEngine;

    @Inject
    public SlackAlarmCallback(Engine templateEngine) {
        this.templateEngine = templateEngine;
    }

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
        SlackMessage message = createSlackMessage(configuration, buildMessageBody(stream, result));

        // Add attachments if requested.
        if (configuration.getBoolean(SlackConfiguration.CK_ADD_ATTACHMENT)) {
            addAttachments(result, message, stream);
        }

        // Add custom fields
        final String customFields = configuration.getString(SlackConfiguration.CK_FIELDS);
        if (!isNullOrEmpty(customFields)) {
            addCustomFields(result, message, customFields);
        }

        try {
            client.send(message);
        } catch (SlackClient.SlackClientException e) {
            throw new RuntimeException("Could not send message to Slack.", e);
        }
    }

    private void addCustomFields(AlertCondition.CheckResult result, SlackMessage message, String customFields) {
        final String[] fields = customFields.split(",");
        for (MessageSummary messageSummary : result.getMatchingMessages()) {
            final String value = Arrays.stream(fields)
                    .map(String::trim)
                    .map(messageSummary::getField)
                    .map(String::valueOf)
                    .collect(Collectors.joining(DELIMITER));

            final String title = String.join(DELIMITER, (CharSequence[]) fields);
            message.addAttachment(new SlackMessage.AttachmentField(title, value, false));
        }
    }

    private void addAttachments(AlertCondition.CheckResult result, SlackMessage message, Stream stream) {
        final List<Message> backlogItems = getAlarmBacklog(result);
        message.addAttachment(new SlackMessage.AttachmentField("Stream ID", stream.getId(), true));
        message.addAttachment(new SlackMessage.AttachmentField("Stream Title", stream.getTitle(), false));
        message.addAttachment(new SlackMessage.AttachmentField("Stream Description", stream.getDescription(), false));

        int count = configuration.getInt(SlackConfiguration.CK_ADD_BLITEMS);
        if (count < 1) {
            count = 5; //Default items to show
        }

        final int blSize = backlogItems.size();
        if (blSize < count) {
            count = blSize;
        }

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(backlogItems.get(i).getMessage()).append("\n\n");
        }

        String attachmentName = "Backlog Items (" + Integer.toString(count) + ")";
        message.addAttachment(new SlackMessage.AttachmentField(attachmentName, sb.toString(), false));
    }

    private List<Message> getAlarmBacklog(AlertCondition.CheckResult result) {
        final AlertCondition alertCondition = result.getTriggeredCondition();
        final List<MessageSummary> matchingMessages = result.getMatchingMessages();
        final int effectiveBacklogSize = Math.min(alertCondition.getBacklog(), matchingMessages.size());

        if (effectiveBacklogSize == 0) return Collections.emptyList();
        final List<MessageSummary> backlogSummaries = matchingMessages.subList(0, effectiveBacklogSize);
        final List<Message> backlog = Lists.newArrayListWithCapacity(effectiveBacklogSize);
        for (MessageSummary messageSummary : backlogSummaries) {
            backlog.add(messageSummary.getRawMessage());
        }

        return backlog;
    }

    private String buildMessageBody(Stream stream, AlertCondition.CheckResult result) {
        String graylogUri = configuration.getString(SlackConfiguration.CK_GRAYLOG2_URL);
        boolean notifyChannel = configuration.getBoolean(SlackConfiguration.CK_NOTIFY_CHANNEL);

        String titleLink;
        if (!isNullOrEmpty(graylogUri)) {
            titleLink = "<" + buildStreamLink(graylogUri, stream) + "|" + stream.getTitle() + ">";
        } else {
            titleLink = "_" + stream.getTitle() + "_";
        }

        // Build custom message
        StringBuilder message = new StringBuilder(result.getResultDescription()).append("\n");
        String template = configuration.getString(SlackConfiguration.CK_CUSTOM_MESSAGE);
        if (!isNullOrEmpty(template)) {
            String customMessage = buildCustomMessage(stream, result, template);
            message.append("\n").append(customMessage);
        }

        String audience = notifyChannel ? "@channel " : "";
        return String.format("%s*Alert for Graylog stream %s*:\n> %s",
                audience, titleLink, message.toString());
    }

    private String buildCustomMessage(Stream stream, AlertCondition.CheckResult result, String template) {
        List<Message> backlog = getAlarmBacklog(result);
        Map<String, Object> model = getModel(stream, result, backlog);
        try {
            return templateEngine.transform(template, model);
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    private Map<String, Object> getModel(Stream stream, AlertCondition.CheckResult result, List<Message> backlog) {
        Map<String, Object> model = new HashMap<>();
        String graylogUri = configuration.getString(SlackConfiguration.CK_GRAYLOG2_URL);
        model.put("stream", stream);
        model.put("check_result", result);
        model.put("alert_condition", result.getTriggeredCondition());
        model.put("backlog", backlog);
        model.put("backlog_size", backlog.size());
        if (!isNullOrEmpty(graylogUri)) {
            model.put("stream_url", buildStreamLink(graylogUri, stream));
        }

        return model;
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
        return SlackConfigurationRequestFactory.createSlackAlarmCallbackConfigurationRequest();
    }

    @Override
    public String getName() {
        return "Slack alarm callback";
    }
}
