package org.graylog2.alarmcallbacks.slack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SlackClient {
    private static final Logger LOG = LoggerFactory.getLogger(SlackClient.class);

    private final String webhookUrl;
    private final String channel;
    private final String userName;
    private final boolean addAttachment;
    private final boolean notifyChannel;
    private final boolean linkNames;
    private final String iconUrl;
    private final String iconEmoji;
    private final String graylogUri;
    private final String color;

    private final ObjectMapper objectMapper;

    public SlackClient(final String webhookUrl,
                       final String channel,
                       final String userName,
                       final boolean addAttachment,
                       final boolean notifyChannel,
                       final boolean linkNames,
                       final String iconUrl,
                       final String iconEmoji,
                       final String graylogUri,
                       final String color) {
        this(webhookUrl, channel, userName, addAttachment, notifyChannel, linkNames, iconUrl, iconEmoji, graylogUri, color, new ObjectMapper());
    }

    @VisibleForTesting
    SlackClient(final String webhookUrl,
                final String channel,
                final String userName,
                final boolean addAttachment,
                final boolean notifyChannel,
                final boolean linkNames,
                final String iconUrl,
                final String iconEmoji,
                final String graylogUri,
                final String color,
                final ObjectMapper objectMapper) {
        this.webhookUrl = webhookUrl;
        this.channel = channel;
        this.userName = userName;
        this.addAttachment = addAttachment;
        this.notifyChannel = notifyChannel;
        this.linkNames = linkNames;
        this.iconUrl = iconUrl;
        this.iconEmoji = iconEmoji;
        this.graylogUri = graylogUri;
        this.color = color;
        this.objectMapper = objectMapper;
    }

    public void trigger(AlertCondition.CheckResult checkResult, Stream stream) throws AlarmCallbackException {
        final URL url;
        try {
            url = new URL(webhookUrl);
        } catch (MalformedURLException e) {
            throw new AlarmCallbackException("Error while constructing webhook URL.", e);
        }

        final HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
        } catch (IOException e) {
            throw new AlarmCallbackException("Could not open connection to Slack API", e);
        }

        try (final Writer writer = new OutputStreamWriter(conn.getOutputStream())) {
            writer.write(buildPostParametersFromAlertCondition(checkResult, stream));
            writer.flush();

            if (conn.getResponseCode() != 200) {
                throw new AlarmCallbackException("Unexpected HTTP response status " + conn.getResponseCode());
            }
        } catch (IOException e) {
            throw new AlarmCallbackException("Could not POST event trigger to Slack API", e);
        }

        try (final InputStream responseStream = conn.getInputStream()) {
            final byte[] responseBytes = ByteStreams.toByteArray(responseStream);

            final String response = new String(responseBytes, Charsets.UTF_8);
            if (response.equals("ok")) {
                LOG.debug("Successfully sent message to Slack.");
            } else {
                LOG.warn("Message couldn't be successfully sent. Response was: {}", response);
            }
        } catch (IOException e) {
            throw new AlarmCallbackException("Could not read response body from Slack API", e);
        }
    }

    private String buildPostParametersFromAlertCondition(AlertCondition.CheckResult checkResult, Stream stream)
            throws UnsupportedEncodingException {

        String titleLink;
        if (isSet(graylogUri)) {
            titleLink = "<" + buildStreamLink(graylogUri, stream) + "|" + stream.getTitle() + ">";
        } else {
            titleLink = "_" + stream.getTitle() + "_";
        }

        final StringBuilder message = new StringBuilder(notifyChannel ? "@channel " : "");
        message.append("*Alert for Graylog stream " + titleLink + "*:\n" + "> " + checkResult.getResultDescription());


        // See https://api.slack.com/methods/chat.postMessage for valid parameters
        final Map<String, Object> params = new HashMap<String, Object>(){{
                put("channel", ensureChannelName(channel));
                put("text", message.toString());
                put("link_names", linkNames ? "1" : "0");
                put("parse", "none");
        }};

        if (isSet(userName)) {
           params.put("username", userName);
        }

        if (isSet(iconUrl)) {
            params.put("icon_url", iconUrl);
        }

        if (isSet(iconEmoji)) {
            params.put("icon_emoji", ensureEmojiSyntax(iconEmoji));
        }

        if (addAttachment) {
            final ImmutableList.Builder<AttachmentField> fields = ImmutableList.<AttachmentField>builder()
                    .add(new AttachmentField("Stream ID", stream.getId(), true))
                    .add(new AttachmentField("Stream Title", stream.getTitle(), false))
                    .add(new AttachmentField("Stream Description", stream.getDescription(), false));
            final Attachment attachment = new Attachment("Alert details", null, "Details:", color, fields.build());
            final List<Attachment> attachments = ImmutableList.of(attachment);

            params.put("attachments", attachments);
        }

        try {
            return new ObjectMapper().writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not build payload JSON.", e);
        }
    }

    private String ensureEmojiSyntax(final String x) {
        String emoji = x.trim();

        if (!emoji.isEmpty() && !emoji.startsWith(":")) {
            emoji = ":" + emoji;
        }

        if (!emoji.isEmpty() && !emoji.endsWith(":")) {
            emoji = emoji + ":";
        }

        return emoji;
    }

    private String buildStreamLink(String baseUrl, Stream stream) {
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        return baseUrl + "streams/" + stream.getId() + "/messages?q=*&rangetype=relative&relative=3600";
    }

    private String ensureChannelName(String x) {
        if(x.startsWith("#")) {
            return x;
        } else {
            return "#" + x;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attachment {
        @JsonProperty
        public String fallback;
        @JsonProperty
        public String text;
        @JsonProperty
        public String pretext;
        @JsonProperty
        public String color = "good";
        @JsonProperty
        public List<AttachmentField> fields;

        @JsonCreator
        public Attachment(String fallback, String text, String pretext, String color, List<AttachmentField> fields) {
            this.fallback = fallback;
            this.text = text;
            this.pretext = pretext;
            this.color = color;
            this.fields = fields;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttachmentField {
        @JsonProperty
        public String title;
        @JsonProperty
        public String value;
        @JsonProperty("short")
        public boolean isShort = false;

        @JsonCreator
        public AttachmentField(String title, String value, boolean isShort) {
            this.title = title;
            this.value = value;
            this.isShort = isShort;
        }
    }

    private final boolean isSet(String x) {
        // Bug in graylog-server v1.2: Empty values are stored as "null" String. This is a dirty workaround.
        return !isNullOrEmpty(x) && !x.equals("null");
    }

}


