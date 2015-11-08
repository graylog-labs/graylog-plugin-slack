package org.graylog2.plugins.slack.callback;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

public class SlackAlarmCallbackTest {
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
    private SlackAlarmCallback alarmCallback;

    @Before
    public void setUp() {
        alarmCallback = new SlackAlarmCallback();
    }

    @Test
    public void testInitialize() throws AlarmCallbackConfigurationException {
        final Configuration configuration = new Configuration(VALID_CONFIG_SOURCE);
        alarmCallback.initialize(configuration);
    }

    @Test
    public void testGetAttributes() throws AlarmCallbackConfigurationException {
        final Configuration configuration = new Configuration(VALID_CONFIG_SOURCE);
        alarmCallback.initialize(configuration);

        final Map<String, Object> attributes = alarmCallback.getAttributes();
        assertThat(attributes.keySet(), hasItems("webhook_url", "channel", "user_name", "add_attachment",
                "notify_channel", "link_names", "icon_url", "icon_emoji", "graylog2_url", "color"));
    }

    @Test
    public void checkConfigurationSucceedsWithValidConfiguration()
            throws AlarmCallbackConfigurationException, ConfigurationException {
        alarmCallback.initialize(new Configuration(VALID_CONFIG_SOURCE));
        alarmCallback.checkConfiguration();
    }

    @Test(expected = ConfigurationException.class)
    public void checkConfigurationFailsIfApiTokenIsMissing()
            throws AlarmCallbackConfigurationException, ConfigurationException {
        alarmCallback.initialize(validConfigurationWithout("webhook_url"));
        alarmCallback.checkConfiguration();
    }

    @Test(expected = ConfigurationException.class)
    public void checkConfigurationFailsIfChannelIsMissing()
            throws AlarmCallbackConfigurationException, ConfigurationException {
        alarmCallback.initialize(validConfigurationWithout("channel"));
        alarmCallback.checkConfiguration();
    }

    @Test(expected = ConfigurationException.class)
    public void checkConfigurationFailsIfChannelContainsInvalidCharacters()
            throws AlarmCallbackConfigurationException, ConfigurationException {
        final Map<String, Object> configSource = ImmutableMap.<String, Object>builder()
                .put("webhook_url", "TEST_api_token")
                .put("channel", "NO_UPPER_CASE")
                .build();

        alarmCallback.initialize(new Configuration(configSource));
        alarmCallback.checkConfiguration();
    }

    @Test(expected = ConfigurationException.class)
    public void checkConfigurationFailsIfUserNameContainsInvalidCharacters()
            throws AlarmCallbackConfigurationException, ConfigurationException {
        final Map<String, Object> configSource = ImmutableMap.<String, Object>builder()
                .put("webhook_url", "TEST_api_token")
                .put("channel", "test_channel")
                .put("user_name", "NO_UPPER_CASE")
                .build();

        alarmCallback.initialize(new Configuration(configSource));
        alarmCallback.checkConfiguration();
    }

    @Test(expected = ConfigurationException.class)
    public void checkConfigurationFailsIfIconUrlIsInvalid()
            throws AlarmCallbackConfigurationException, ConfigurationException {
        final Map<String, Object> configSource = ImmutableMap.<String, Object>builder()
                .put("webhook_url", "TEST_api_token")
                .put("channel", "TEST_channel")
                .put("icon_url", "Definitely$$Not#A!!URL")
                .build();

        alarmCallback.initialize(new Configuration(configSource));
        alarmCallback.checkConfiguration();
    }

    @Test(expected = ConfigurationException.class)
    public void checkConfigurationFailsIfIconUrlIsNotHttpOrHttps()
            throws AlarmCallbackConfigurationException, ConfigurationException {
        final Map<String, Object> configSource = ImmutableMap.<String, Object>builder()
                .put("webhook_url", "TEST_api_token")
                .put("channel", "TEST_channel")
                .put("icon_url", "ftp://example.net")
                .build();

        alarmCallback.initialize(new Configuration(configSource));
        alarmCallback.checkConfiguration();
    }

    @Test(expected = ConfigurationException.class)
    public void checkConfigurationFailsIfGraylog2UrlIsInvalid()
            throws AlarmCallbackConfigurationException, ConfigurationException {
        final Map<String, Object> configSource = ImmutableMap.<String, Object>builder()
                .put("webhook_url", "TEST_api_token")
                .put("channel", "TEST_channel")
                .put("graylog2_url", "Definitely$$Not#A!!URL")
                .build();

        alarmCallback.initialize(new Configuration(configSource));
        alarmCallback.checkConfiguration();
    }

    @Test(expected = ConfigurationException.class)
    public void checkConfigurationFailsIfGraylog2UrlIsNotHttpOrHttps()
            throws AlarmCallbackConfigurationException, ConfigurationException {
        final Map<String, Object> configSource = ImmutableMap.<String, Object>builder()
                .put("webhook_url", "TEST_api_token")
                .put("channel", "TEST_channel")
                .put("graylog2_url", "ftp://example.net")
                .build();

        alarmCallback.initialize(new Configuration(configSource));
        alarmCallback.checkConfiguration();
    }

    @Test
    public void testGetRequestedConfiguration() {
        assertThat(alarmCallback.getRequestedConfiguration().asList().keySet(),
                hasItems("webhook_url", "channel", "user_name", "add_attachment", "notify_channel", "link_names",
                        "icon_url", "icon_emoji", "graylog2_url"));
    }

    @Test
    public void testGetName() {
        assertThat(alarmCallback.getName(), equalTo("Slack alarm callback"));
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
