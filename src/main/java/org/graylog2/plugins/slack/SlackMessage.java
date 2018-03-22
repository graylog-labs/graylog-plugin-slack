package org.graylog2.plugins.slack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import java.util.ArrayList;
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
    private final List<AttachmentField> detailFields;
    private String customMessage;

    public SlackMessage(
            String color,
            String iconEmoji,
            String iconUrl,
            String message,
            String userName,
            String channel,
            boolean linkNames
    ) {
        this.color = color;
        this.iconEmoji = iconEmoji;
        this.iconUrl = iconUrl;
        this.message = message;
        this.userName = userName;
        this.channel = channel;
        this.linkNames = linkNames;
        this.detailFields = Lists.newArrayList();
        this.customMessage = null;
    }

    public String getJsonString() {
        // See https://api.slack.com/methods/chat.postMessage for valid parameters
        final Map<String, Object> params = new HashMap<String, Object>() {{
            put("channel", channel);
            put("text", message);
            put("link_names", linkNames);
        }};

        if (!isNullOrEmpty(userName)) {
            params.put("username", userName);
        }

        if (!isNullOrEmpty(iconUrl)) {
            params.put("icon_url", iconUrl);
        }

        if (!isNullOrEmpty(iconEmoji)) {
            params.put("icon_emoji", ensureEmojiSyntax(iconEmoji));
        }

        final List<Attachment> attachments = new ArrayList<>();
        if (!isNullOrEmpty(customMessage)) {
            final Attachment attachment = new Attachment(
                    color,
                    customMessage,
                    "Custom Message",
                    "Custom Message:",
                    null
            );
            attachments.add(attachment);
        }

        if (!detailFields.isEmpty()) {
            final Attachment attachment = new Attachment(
                    color,
                    null,
                    "Alert details",
                    "Alert Details:",
                    detailFields
            );
            attachments.add(attachment);
        }

        if (attachments.size() > 0) {
            params.put("attachments", attachments);
        }

        try {
            return new ObjectMapper().writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not build payload JSON.", e);
        }
    }

    public void addDetailsAttachmentField(AttachmentField attachmentField) {
        this.detailFields.add(attachmentField);
    }

    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
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
        public String color;
        @JsonProperty
        public List<AttachmentField> fields;

        @JsonCreator
        public Attachment(String color, String text, String fallback, String pretext, List<AttachmentField> fields) {
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
        public boolean isShort;

        @JsonCreator
        public AttachmentField(String title, String value, boolean isShort) {
            this.title = title;
            this.value = value;
            this.isShort = isShort;
        }
    }

}
