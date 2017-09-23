package org.graylog2.plugins.slack;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugins.slack.configuration.SlackConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class SlackClient {

    private static final Logger LOG = LoggerFactory.getLogger(SlackClient.class);

    private final String webhookUrl;
    private final String proxyURL;

    public SlackClient(Configuration configuration) {
        this.webhookUrl = configuration.getString(SlackConfiguration.CK_WEBHOOK_URL);
        this.proxyURL = configuration.getString(SlackConfiguration.CK_PROXY_ADDRESS);
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
            if (!StringUtils.isEmpty(proxyURL)) {
                final URI proxyUri = new URI(proxyURL);
                InetSocketAddress sockAddress = new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort());
                final Proxy proxy = new Proxy(Proxy.Type.HTTP, sockAddress);
                conn = (HttpURLConnection) url.openConnection(proxy);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
        } catch (URISyntaxException | IOException e) {
            throw new SlackClientException("Could not open connection to Slack API", e);
        }

        try (final Writer writer = new OutputStreamWriter(conn.getOutputStream())) {
            writer.write(message.getJsonString());
            writer.flush();

            final int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                if(LOG.isDebugEnabled()){
                    try (final InputStream responseStream = conn.getInputStream()) {
                        final byte[] responseBytes = ByteStreams.toByteArray(responseStream);
                        final String response = new String(responseBytes, Charsets.UTF_8);
                        LOG.debug("Received HTTP response body:\n{}", response);
                    }
                }
                throw new SlackClientException("Unexpected HTTP response status " + responseCode);
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
