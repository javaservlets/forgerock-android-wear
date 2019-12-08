/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.net.Uri;

import okhttp3.Response;

import org.forgerock.android.auth.exception.ApiException;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.Objects;

import static org.forgerock.android.auth.OAuth2.*;

/**
 * Implementation for handling {@link OAuth2Client} response, and provide feedback to the registered {@link FRListener}
 */
class OAuth2ResponseHandler implements ResponseHandler {

    /**
     * Handle Authorization response.
     *
     * @param response The response from /authorize endpoint
     * @param listener Listener for receiving OAuth APIs related changes
     */
    void handleAuthorizeResponse(Response response, FRListener<String> listener) {
        if (response.isRedirect()) {
            String location = response.header("Location");
            Uri redirect = Uri.parse(location);
            String code = redirect.getQueryParameter("code");
            if (code != null) {
                Listener.onSuccess(listener, code);
            } else {
                String errorDescription = redirect.getQueryParameter("error_description");
                Listener.onException(listener, new ApiException(response.code(), response.message(), errorDescription));
            }
        } else {
            handleError(response, listener);
        }
    }

    /**
     * Handle Token response
     *
     * @param response The response from /token endpoint
     */
    void handleTokenResponse(Response response, FRListener<AccessToken> listener) {
        if (response.isSuccessful()) {
            try {
                JSONObject jsonObject = new JSONObject(Objects.requireNonNull(response.body()).string());
                Listener.onSuccess(listener, AccessToken.builder()
                        .idToken(jsonObject.optString(ID_TOKEN, null))
                        .value(jsonObject.getString(ACCESS_TOKEN))
                        .refreshToken(jsonObject.optString(REFRESH_TOKEN, null))
                        .scope(AccessToken.Scope.parse(jsonObject.optString(SCOPE, null)))
                        .tokenType(jsonObject.optString(TOKEN_TYPE, null))
                        .expiresIn(jsonObject.optLong(EXPIRES_IN, 0))
                        .build());
            } catch (Exception e) {
                Listener.onException(listener, e);
            }
        } else {
            handleError(response, listener);
        }
    }

    /**
     * Handle revoke token response
     *
     * @param response The response from the API
     * @param listener The Listener to listen for events
     */
    void handleRevokeResponse(Response response, FRListener<Void> listener) {
        if (response.isSuccessful()) {
            Listener.onSuccess(listener, null);
        } else {
            handleError(response, listener);
        }
    }

}

