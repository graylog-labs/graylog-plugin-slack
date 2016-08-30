package org.graylog2.plugins.slack;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SlackClient {

    private static final Logger LOG = LoggerFactory.getLogger(SlackClient.class);

    private final String webhookUrl;

    public SlackClient(Configuration configuration) {
        this.webhookUrl = configuration.getString(SlackPluginBase.CK_WEBHOOK_URL);
    }

    public void send(SlackMessage message) throws SlackClientException {
        final URL url;
        try {
            url = new URL(webhookUrl);
        } catch (MalformedURLException e) {
            throw new SlackClientException("Error while constructing webhook URL.", e);
        }

        final HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
        } catch (IOException e) {
            throw new SlackClientException("Could not open connection to Slack API", e);
        }

        try (final Writer writer = new OutputStreamWriter(conn.getOutputStream())) {
            writer.write(message.getJsonString());
            writer.flush();

            if (conn.getResponseCode() != 200) {
                throw new SlackClientException("Unexpected HTTP response status " + conn.getResponseCode());
            }
        } catch (IOException e) {
            throw new SlackClientException("Could not POST to Slack API", e);
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
            throw new SlackClientException("Could not read response body from Slack API", e);
        }
    }


    public class SlackClientException extends Exception {

        public SlackClientException(String msg) {
            super(msg);
        }

        public SlackClientException(String msg, Throwable cause) {
            super(msg, cause);
        }

    }

}
