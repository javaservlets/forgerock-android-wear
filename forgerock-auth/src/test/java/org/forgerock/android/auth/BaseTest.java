/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.ShadowLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BaseTest {

    protected MockWebServer server;

    public Context context = ApplicationProvider.getApplicationContext();
    public ServerConfig serverConfig;
    public OAuth2Client oAuth2Client;

    @Mock
    public Context mockContext;

    @Before
    public void startServer() throws Exception {

        ShadowLog.stream = System.out;
        Logger.set(Logger.Level.DEBUG);
        ShadowLog.clear();

        MockitoAnnotations.initMocks(this);
        server = new MockWebServer();
        server.start();
        serverConfig = getServerConfig();
        oAuth2Client = getOAuth2Client();

    }

    @After
    public void shutdown() throws IOException {
        server.shutdown();
        Config.reset();
    }

    protected String getUrl() {
        return "http://" + server.getHostName() + ":" + server.getPort();
    }


    protected String getJson(String path) {
        try {
            return IOUtils.toString(getClass().getResourceAsStream(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected ServerConfig getServerConfig() {
        return ServerConfig.builder()
                .context(context)
                .url(getUrl())
                .build();
    }

    protected OAuth2Client getOAuth2Client() {
        return OAuth2Client.builder()
                .clientId("andy_app")
                .scope("openid email address")
                .redirectUri("http://www.example.com:8080/callback")
                .serverConfig(serverConfig)
                .build();
    }

    protected void enqueue(String path, int statusCode) {
        server.enqueue(new MockResponse()
                .setResponseCode(statusCode)
                .addHeader("Content-Type", "application/json")
                .setBody(getJson(path)));
    }

}
