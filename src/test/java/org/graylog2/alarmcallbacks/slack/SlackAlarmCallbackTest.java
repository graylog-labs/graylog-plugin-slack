package org.graylog2.alarmcallbacks.slack;

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
            .put("api_token", "test_api_token")
            .put("channel", "test_channel")
            .put("user_name", "test_user_name")
            .put("add_attachment", true)
            .put("link_names", true)
            .put("unfurl_links", true)
            .put("icon_url", "http://example.com")
            .put("icon_emoji", "test_icon_emoji")
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
        assertThat(attributes.keySet(), hasItems("api_token", "channel", "user_name", "add_attachment",
                "link_names", "unfurl_links", "icon_url", "icon_emoji"));
        assertThat((String) attributes.get("api_token"), equalTo("****"));
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
        alarmCallback.initialize(validConfigurationWithout("api_token"));
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
                .put("api_token", "TEST_api_token")
                .put("channel", "NO_UPPER_CASE")
                .build();

        alarmCallback.initialize(new Configuration(configSource));
        alarmCallback.checkConfiguration();
    }

    @Test(expected = ConfigurationException.class)
    public void checkConfigurationFailsIfUserNameContainsInvalidCharacters()
            throws AlarmCallbackConfigurationException, ConfigurationException {
        final Map<String, Object> configSource = ImmutableMap.<String, Object>builder()
                .put("api_token", "TEST_api_token")
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
                .put("api_token", "TEST_api_token")
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
                .put("api_token", "TEST_api_token")
                .put("channel", "TEST_channel")
                .put("icon_url", "ftp://example.net")
                .build();

        alarmCallback.initialize(new Configuration(configSource));
        alarmCallback.checkConfiguration();
    }

    @Test
    public void testGetRequestedConfiguration() {
        assertThat(alarmCallback.getRequestedConfiguration().asList().keySet(),
                hasItems("api_token", "channel", "user_name", "add_attachment", "link_names",
                        "unfurl_links", "icon_url", "icon_emoji"));
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
