/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.application.connector.secrets;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESSingleNodeTestCase;
import org.elasticsearch.xpack.application.connector.secrets.action.GetConnectorSecretResponse;
import org.elasticsearch.xpack.application.connector.secrets.action.PostConnectorSecretRequest;
import org.elasticsearch.xpack.application.connector.secrets.action.PostConnectorSecretResponse;
import org.junit.Before;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class ConnectorSecretsIndexServiceTests extends ESSingleNodeTestCase {

    private static final int TIMEOUT_SECONDS = 10;

    private ConnectorSecretsIndexService connectorSecretsIndexService;

    @Before
    public void setup() throws Exception {
        this.connectorSecretsIndexService = new ConnectorSecretsIndexService(client());
    }

    public void testCreateAndGetConnectorSecret() throws Exception {
        PostConnectorSecretRequest createSecretRequest = ConnectorSecretsTestUtils.getRandomPostConnectorSecretRequest();
        PostConnectorSecretResponse createdSecret = awaitPostConnectorSecret(createSecretRequest);

        GetConnectorSecretResponse gotSecret = awaitGetConnectorSecret(createdSecret.id());

        assertThat(gotSecret.id(), equalTo(createdSecret.id()));
        assertThat(gotSecret.value(), notNullValue());
    }

    private PostConnectorSecretResponse awaitPostConnectorSecret(PostConnectorSecretRequest secretRequest) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        final AtomicReference<PostConnectorSecretResponse> responseRef = new AtomicReference<>(null);
        final AtomicReference<Exception> exception = new AtomicReference<>(null);

        connectorSecretsIndexService.createSecret(secretRequest, new ActionListener<>() {
            @Override
            public void onResponse(PostConnectorSecretResponse postConnectorSecretResponse) {
                responseRef.set(postConnectorSecretResponse);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                exception.set(e);
                latch.countDown();
            }
        });

        if (exception.get() != null) {
            throw exception.get();
        }

        boolean requestTimedOut = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        PostConnectorSecretResponse response = responseRef.get();

        assertTrue("Timeout waiting for post request", requestTimedOut);
        assertNotNull("Received null response from post request", response);

        return response;
    }

    private GetConnectorSecretResponse awaitGetConnectorSecret(String connectorSecretId) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<GetConnectorSecretResponse> resp = new AtomicReference<>(null);
        final AtomicReference<Exception> exc = new AtomicReference<>(null);

        connectorSecretsIndexService.getSecret(connectorSecretId, new ActionListener<GetConnectorSecretResponse>() {
            @Override
            public void onResponse(GetConnectorSecretResponse response) {
                resp.set(response);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                exc.set(e);
                latch.countDown();
            }
        });

        assertTrue("Timeout waiting for get request", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        if (exc.get() != null) {
            throw exc.get();
        }
        assertNotNull("Received null response from get request", resp.get());
        return resp.get();
    }
}
