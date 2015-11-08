package org.graylog2.plugins.slack.output;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

public class SlackMessageOutputTest {
    private static final ImmutableMap<String, Object> VALID_CONFIG_SOURCE = ImmutableMap.<String, Object>builder()
            .put("webhook_url", "https://www.example.org/")
            .put("channel", "test_channel")
            .put("user_name", "test_user_name")
            .put("add_attachment", true)
            .put("notify_channel", true)
            .put("link_names", true)
            .put("icon_url", "http://example.com")
            .put("icon_emoji", "test_icon_emoji")
            .put("graylog2_url", "http://graylog2.example.com")
            .put("color", "#FF0000")
            .build();

    @Test
    public void testGetAttributes() throws MessageOutputConfigurationException {
        SlackMessageOutput output = new SlackMessageOutput(null, new Configuration(VALID_CONFIG_SOURCE));

        final Map<String, Object> attributes = output.getConfiguration();
        assertThat(attributes.keySet(), hasItems("webhook_url", "channel", "user_name", "add_attachment",
                "notify_channel", "link_names", "icon_url", "icon_emoji", "graylog2_url", "color"));
    }

    @Test
    public void checkConfigurationSucceedsWithValidConfiguration() throws MessageOutputConfigurationException {
        new SlackMessageOutput(null, new Configuration(VALID_CONFIG_SOURCE));
    }

    @Test(expected = MessageOutputConfigurationException.class)
    public void checkConfigurationFailsIfApiTokenIsMissing() throws MessageOutputConfigurationException {
        new SlackMessageOutput(null, validConfigurationWithout("webhook_url"));
    }

    @Test(expected = MessageOutputConfigurationException.class)
    public void checkConfigurationFailsIfChannelIsMissing() throws MessageOutputConfigurationException {
        new SlackMessageOutput(null, validConfigurationWithout("channel"));

    }

    @Test(expected = MessageOutputConfigurationException.class)
    public void checkConfigurationFailsIfChannelContainsInvalidCharacters() throws MessageOutputConfigurationException {
        final Map<String, Object> configSource = ImmutableMap.<String, Object>builder()
                .put("webhook_url", "TEST_api_token")
                .put("channel", "NO_UPPER_CASE")
                .build();

        new SlackMessageOutput(null, new Configuration(configSource));
    }

    @Test(expected = MessageOutputConfigurationException.class)
    public void checkConfigurationFailsIfUserNameContainsInvalidCharacters() throws MessageOutputConfigurationException {
        final Map<String, Object> configSource = ImmutableMap.<String, Object>builder()
                .put("webhook_url", "TEST_api_token")
                .put("channel", "test_channel")
                .put("user_name", "NO_UPPER_CASE")
                .build();

        new SlackMessageOutput(null, new Configuration(configSource));
    }

    @Test(expected = MessageOutputConfigurationException.class)
    public void checkConfigurationFailsIfIconUrlIsInvalid() throws MessageOutputConfigurationException {
        final Map<String, Object> configSource = ImmutableMap.<String, Object>builder()
                .put("webhook_url", "TEST_api_token")
                .put("channel", "TEST_channel")
                .put("icon_url", "Definitely$$Not#A!!URL")
                .build();

        new SlackMessageOutput(null, new Configuration(configSource));
    }

    @Test(expected = MessageOutputConfigurationException.class)
    public void checkConfigurationFailsIfIconUrlIsNotHttpOrHttps() throws MessageOutputConfigurationException {
        final Map<String, Object> configSource = ImmutableMap.<String, Object>builder()
                .put("webhook_url", "TEST_api_token")
                .put("channel", "TEST_channel")
                .put("icon_url", "ftp://example.net")
                .build();

        new SlackMessageOutput(null, new Configuration(configSource));
    }

    @Test(expected = MessageOutputConfigurationException.class)
    public void checkConfigurationFailsIfGraylog2UrlIsInvalid() throws MessageOutputConfigurationException {
        final Map<String, Object> configSource = ImmutableMap.<String, Object>builder()
                .put("webhook_url", "TEST_api_token")
                .put("channel", "TEST_channel")
                .put("graylog2_url", "Definitely$$Not#A!!URL")
                .build();

        new SlackMessageOutput(null, new Configuration(configSource));
    }

    @Test(expected = MessageOutputConfigurationException.class)
    public void checkConfigurationFailsIfGraylog2UrlIsNotHttpOrHttps() throws MessageOutputConfigurationException {
        final Map<String, Object> configSource = ImmutableMap.<String, Object>builder()
                .put("webhook_url", "TEST_api_token")
                .put("channel", "TEST_channel")
                .put("graylog2_url", "ftp://example.net")
                .build();

        new SlackMessageOutput(null, new Configuration(configSource));
    }

    private Configuration validConfigurationWithout(final String key) {
        return new Configuration(Maps.filterEntries(VALID_CONFIG_SOURCE, new Predicate<Map.Entry<String, Object>>() {
            @Override
            public boolean apply(Map.Entry<String, Object> input) {
                return key.equals(input.getKey());
            }
        }));
    }

}
