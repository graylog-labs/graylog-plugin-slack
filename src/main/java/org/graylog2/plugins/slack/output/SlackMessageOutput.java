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
import org.graylog2.plugins.slack.configuration.SlackConfiguration;
import org.graylog2.plugins.slack.configuration.SlackConfigurationRequestFactory;
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
    public SlackMessageOutput(
            @Assisted Stream stream,
            @Assisted Configuration configuration)
            throws MessageOutputConfigurationException {
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
        boolean shortMode = configuration.getBoolean(SlackConfiguration.CK_SHORT_MODE);
        String message = shortMode ? buildShortMessageBody(msg) : buildFullMessageBody(stream, msg);
        SlackMessage slackMessage = createSlackMessage(configuration, message);

        try {
            client.send(slackMessage);
        } catch (SlackClient.SlackClientException e) {
            throw new RuntimeException("Could not send message to Slack.", e);
        }
    }

    private String buildFullMessageBody(Stream stream, Message msg) {
        String graylogUri = configuration.getString(SlackConfiguration.CK_GRAYLOG2_URL);
        boolean notifyChannel = configuration.getBoolean(SlackConfiguration.CK_NOTIFY_CHANNEL);

        String titleLink;
        if (!isNullOrEmpty(graylogUri)) {
            titleLink = "<" + buildStreamLink(graylogUri, stream) + "|" + stream.getTitle() + ">";
        } else {
            titleLink = "_" + stream.getTitle() + "_";
        }

        String audience = notifyChannel ? "@channel " : "";
        return String.format("%s*New message in Graylog stream %s*:\n> %s",
                audience, titleLink, msg.getMessage());
    }

    private String buildShortMessageBody(Message msg) {
        return String.format("%s: %s", msg.getTimestamp()
                .toDateTime(DateTimeZone.getDefault())
                .toString(DateTimeFormat.shortTime()), msg.getMessage()
        );
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
            return SlackConfigurationRequestFactory.createSlackMessageOutputConfigurationRequest();
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Slack Output", false, "", "Writes messages to a Slack chat room.");
        }
    }
}
