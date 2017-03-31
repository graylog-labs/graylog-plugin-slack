package org.graylog2.plugins.slack.output;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugins.slack.SlackClient;
import org.graylog2.plugins.slack.SlackMessage;
import org.graylog2.plugins.slack.SlackPluginBase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SlackMessageOutput extends SlackPluginBase implements MessageOutput {
    private AtomicBoolean running = new AtomicBoolean(false);

    private final Configuration configuration;
    private final Stream stream;

    private final SlackClient client;

    @Inject
    public SlackMessageOutput(@Assisted Stream stream, @Assisted Configuration configuration) throws MessageOutputConfigurationException {
        this.configuration = configuration;
        this.stream = stream;

        // Check configuration.
        try {
            checkConfiguration(configuration);
        } catch (ConfigurationException e) {
            throw new MessageOutputConfigurationException("Missing configuration: " + e.getMessage());
        }

        this.client = new SlackClient(configuration);

        running.set(true);
    }

    @Override
    public void stop() {
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void write(Message msg) throws Exception {
        // load timestamp
        final String tsField = configuration.getString(CK_FOOTER_TS_FIELD);
        Long tsValue = null;
        if (!isNullOrEmpty(tsField)) {
            try {
                DateTime timestamp = null;
                if ("timestamp".equals(tsField)) { // timestamp is reserved field in org.graylog2.notifications.NotificationImpl
                    timestamp = msg.getTimestamp();
                }
                else {
                    Object value = msg.getField(tsField);
                    if (value instanceof DateTime) {
                        timestamp = (DateTime)value;
                    } else {
                        timestamp = new DateTime(value, DateTimeZone.UTC);
                    }
                }
                tsValue = timestamp.getMillis() / 1000;
            } catch (NullPointerException | IllegalArgumentException e) {
                // ignore
            }
        }      
        
        SlackMessage slackMessage = new SlackMessage(
                configuration.getString(CK_COLOR),
                configuration.getString(CK_MESSAGE_ICON),
                buildMessage(stream, msg),
                configuration.getString(CK_USER_NAME),
                configuration.getString(CK_CHANNEL),
                configuration.getBoolean(CK_LINK_NAMES),
                configuration.getString(CK_FOOTER_TEXT),
                configuration.getString(CK_FOOTER_ICON_URL),
                tsValue
        );

        // Add attachments if requested.
        if (!configuration.getBoolean(CK_SHORT_MODE) && configuration.getBoolean(CK_ADD_STREAM_INFO)) {
            slackMessage.addAttachment(new SlackMessage.AttachmentField("Stream Description", stream.getDescription(), false));
            slackMessage.addAttachment(new SlackMessage.AttachmentField("Source", msg.getSource(), true));
        }

        int count = configuration.getInt(CK_ADD_BLITEMS);
        if (count > 0) {
            for (Map.Entry<String, Object> field : msg.getFields().entrySet()) {
                if (Message.RESERVED_FIELDS.contains(field.getKey())) {
                    continue;
                }
                slackMessage.addAttachment(new SlackMessage.AttachmentField(field.getKey(), field.getValue().toString(), true));
            }
        }

        try {
            client.send(slackMessage);
        } catch (SlackClient.SlackClientException e) {
            throw new RuntimeException("Could not send message to Slack.", e);
        }
    }

    public String buildMessage(Stream stream, Message msg) {
        if (configuration.getBoolean(CK_SHORT_MODE)) {
            return msg.getTimestamp().toDateTime(DateTimeZone.getDefault()).toString(DateTimeFormat.shortTime()) + ": " + msg.getMessage();
        }

        String graylogUri = configuration.getString(CK_GRAYLOG2_URL);
        boolean notifyChannel = configuration.getBoolean(CK_NOTIFY_CHANNEL);

        String titleLink;
        if (!isNullOrEmpty(graylogUri)) {
            titleLink = "<" + buildStreamLink(graylogUri, stream) + "|" + stream.getTitle() + ">";
        } else {
            titleLink = "_" + stream.getTitle() + "_";
        }

        return (notifyChannel ? "@channel " : "") + "*New message in Graylog stream " + titleLink + "*:\n" + "> " + msg.getMessage();
    }

    @Override
    public void write(List<Message> list) throws Exception {
        for (Message message : list) {
            write(message);
        }
    }

    public Map<String, Object> getConfiguration() {
        return configuration.getSource();
    }

    @FactoryClass
    public interface Factory extends MessageOutput.Factory<SlackMessageOutput> {
        @Override
        SlackMessageOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return configuration();
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Slack Output", false, "", "Writes messages to a Slack chat room.");
        }
    }

}
