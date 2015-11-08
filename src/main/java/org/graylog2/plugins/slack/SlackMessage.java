package org.graylog2.plugins.slack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SlackMessage {

    private final String channel;
    private final String userName;
    private final String message;
    private final String iconUrl;
    private final String iconEmoji;
    private final String color;
    private final boolean linkNames;

    private final List<AttachmentField> attachments;

    public SlackMessage(String color, String iconEmoji, String iconUrl, String message, String userName, String channel, boolean linkNames) {
        this.color = color;
        this.iconEmoji = iconEmoji;
        this.iconUrl = iconUrl;
        this.message = message;
        this.userName = userName;
        this.channel = channel;
        this.linkNames = linkNames;

        this.attachments = Lists.newArrayList();
    }

    public String getJsonString() {
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

        if (!attachments.isEmpty()) {
            final Attachment attachment = new Attachment("Alert details", null, "Details:", color, attachments);
            final List<Attachment> attachments = ImmutableList.of(attachment);
            params.put("attachments", attachments);
        }

        try {
            return new ObjectMapper().writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not build payload JSON.", e);
        }
    }

    public void addAttachment(AttachmentField attachment) {
        this.attachments.add(attachment);
    }

    private String ensureChannelName(String x) {
        if(x.startsWith("#")) {
            return x;
        } else {
            return "#" + x;
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

    private final boolean isSet(String x) {
        // Bug in graylog-server v1.2: Empty values are stored as "null" String. This is a dirty workaround.
        return !isNullOrEmpty(x) && !x.equals("null");
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

}
