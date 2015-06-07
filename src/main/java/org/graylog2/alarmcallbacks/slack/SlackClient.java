package org.graylog2.alarmcallbacks.slack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class SlackClient {
    private static final Logger LOG = LoggerFactory.getLogger(SlackClient.class);

    private final String apiToken;
    private final String channel;
    private final String userName;
    private final boolean addAttachment;
    private final boolean notifyChannel;
    private final boolean linkNames;
    private final boolean unfurlLinks;
    private final String iconUrl;
    private final String iconEmoji;
    private final String graylogUri;

    private final ObjectMapper objectMapper;

    public SlackClient(final String apiToken,
                       final String channel,
                       final String userName,
                       final boolean addAttachment,
                       final boolean notifyChannel,
                       final boolean linkNames,
                       final boolean unfurlLinks,
                       final String iconUrl,
                       final String iconEmoji,
                       final String graylogUri) {
        this(apiToken, channel, userName, addAttachment, notifyChannel, linkNames, unfurlLinks, iconUrl, iconEmoji, graylogUri, new ObjectMapper());
    }

    @VisibleForTesting
    SlackClient(final String apiToken,
                final String channel,
                final String userName,
                final boolean addAttachment,
                final boolean notifyChannel,
                final boolean linkNames,
                final boolean unfurlLinks,
                final String iconUrl,
                final String iconEmoji,
                final String graylogUri,
                final ObjectMapper objectMapper) {
        this.apiToken = apiToken;
        this.channel = channel;
        this.userName = userName;
        this.addAttachment = addAttachment;
        this.notifyChannel = notifyChannel;
        this.linkNames = linkNames;
        this.unfurlLinks = unfurlLinks;
        this.iconUrl = iconUrl;
        this.iconEmoji = iconEmoji;
        this.graylogUri = graylogUri;
        this.objectMapper = objectMapper;
    }

    public void trigger(AlertCondition.CheckResult checkResult, Stream stream) throws AlarmCallbackException {
        final URL url;
        try {
            url = new URL("https://slack.com/api/chat.postMessage");
        } catch (MalformedURLException e) {
            throw new AlarmCallbackException("Error while constructing URL of Slack API.", e);
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

            final SlackResponse response = objectMapper.readValue(responseBytes, SlackResponse.class);
            if (response.ok) {
                LOG.debug("Successfully sent message to {}", response.channel);
            } else {
                LOG.warn("Message couldn't be successfully sent. Reason: {}", response.error);
            }
        } catch (IOException e) {
            throw new AlarmCallbackException("Could not read response body from Slack API", e);
        }
    }

    private String buildPostParametersFromAlertCondition(AlertCondition.CheckResult checkResult, Stream stream)
            throws UnsupportedEncodingException {
        String message = notifyChannel ? "@channel " : "";
        message += "*Alert for stream _" + stream.getTitle() + "_*:\n" + "> " + checkResult.getResultDescription();

        if (!isNullOrEmpty(graylogUri)) {
            message += "\n<" + buildStreamLink(graylogUri, stream) + "|Open stream in Graylog>";
        }

        // See https://api.slack.com/methods/chat.postMessage for valid parameters
        final ImmutableMap.Builder<String, String> paramBuilder = ImmutableMap.<String, String>builder()
                .put("token", URLEncoder.encode(apiToken, "UTF-8"))
                .put("channel", URLEncoder.encode(channel, "UTF-8"))
                .put("text", URLEncoder.encode(message, "UTF-8"))
                .put("link_names", linkNames ? "1" : "0")
                .put("unfurl_links", unfurlLinks ? "1" : "0")
                .put("parse", "none");

        if (!isNullOrEmpty(userName)) {
            paramBuilder.put("username", URLEncoder.encode(userName, "UTF-8"));
        }

        if (!isNullOrEmpty(iconUrl)) {
            paramBuilder.put("icon_url", URLEncoder.encode(iconUrl, "UTF-8"));
        }

        if (!isNullOrEmpty(iconEmoji)) {
            paramBuilder.put("icon_emoji", URLEncoder.encode(ensureEmojiSyntax(iconEmoji), "UTF-8"));
        }

        if (addAttachment) {
            final AlertCondition alertCondition = checkResult.getTriggeredCondition();
            final int matchingMessagesSize = checkResult.getMatchingMessages().size();
            final int searchHits = Math.min(alertCondition.getBacklog(), matchingMessagesSize);
            final ImmutableList.Builder<AttachmentField> fields = ImmutableList.<AttachmentField>builder()
                    .add(new AttachmentField("Backlog", String.valueOf(alertCondition.getBacklog()), true))
                    .add(new AttachmentField("Search hits", String.valueOf(searchHits), true))
                    .add(new AttachmentField("Stream ID", stream.getId(), true))
                    .add(new AttachmentField("Stream Title", stream.getTitle(), false))
                    .add(new AttachmentField("Stream Description", stream.getDescription(), false));
            final Attachment attachment = new Attachment("Alert details", null, "Alert details", "good", fields.build());
            final List<Attachment> attachments = ImmutableList.of(attachment);

            try {
                paramBuilder.put("attachments", URLEncoder.encode(objectMapper.writeValueAsString(attachments), "UTF-8"));
            } catch (JsonProcessingException e) {
                LOG.warn("Couldn't create Slack message attachment.", e);
            }
        }

        return Joiner.on('&')
                .withKeyValueSeparator("=")
                .join(paramBuilder.build());
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SlackResponse {
        @JsonProperty
        public boolean ok;
        @JsonProperty
        public String ts;
        @JsonProperty
        public String channel;
        @JsonProperty
        public String error;
    }

}
