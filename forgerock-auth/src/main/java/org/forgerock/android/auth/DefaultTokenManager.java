/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.WorkerThread;

import org.forgerock.android.auth.exception.AuthenticationRequiredException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Builder;
import lombok.NonNull;

import static org.forgerock.android.auth.OAuth2.ACCESS_TOKEN;

/**
 * Default implementation for {@link TokenManager}. By default this class uses {@link SecuredSharedPreferences} to persist
 * the token locally on the device. However, it can be overridden by the {@link DefaultTokenManager#builder#sharedPreferences}
 * For example, it can replaced with Android JetPack {@link androidx.security.crypto.EncryptedSharedPreferences}
 * <p>
 * <p>
 * This {@link TokenManager} supports {@link AccessToken} in-memory caching. To control the caching interval use
 * {@link DefaultTokenManager#builder#cacheIntervalMillis}. with the encryption/decryption, disk and Keystore IO, it may drains
 * device CPU and Battery resources. If Application intensively with network operation, setting the
 * this attribute may improve performance.
 */
class DefaultTokenManager implements TokenManager {

    private static final String TAG = "DefaultTokenManager";

    //Alias to store keys
    static final String ORG_FORGEROCK_V_1_KEYS = "org.forgerock.v1.KEYS";

    //File name to store tokens
    static final String ORG_FORGEROCK_V_1_TOKENS = "org.forgerock.v1.TOKENS";

    /**
     * The {@link SharedPreferences} to store the tokens
     */
    private SharedPreferences sharedPreferences;

    /**
     * The {@link OAuth2Client} to auto refresh {@link AccessToken}
     */
    private final OAuth2Client oAuth2Client;
    private final AtomicReference<AccessToken> accessTokenRef;
    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();

    private final long cacheIntervalMillis;

    /**
     * Threshold to refresh the {@link AccessToken}
     */
    private long threshold;


    @Builder
    public DefaultTokenManager(@NonNull Context context,
                               OAuth2Client oAuth2Client,
                               SharedPreferences sharedPreferences,
                               Long cacheIntervalMillis,
                               Long threshold) {

        Config config = Config.getInstance(context);
        this.sharedPreferences = config.applyDefaultIfNull(sharedPreferences, context, new Function<Context, SharedPreferences>() {
            @Override
            public SharedPreferences apply(Context var) {
                return new SecuredSharedPreferences(var
                        , ORG_FORGEROCK_V_1_TOKENS, ORG_FORGEROCK_V_1_KEYS);
            }
        });

        Logger.debug(TAG, "Using SharedPreference: %s", this.sharedPreferences.getClass().getSimpleName());

        this.oAuth2Client = config.applyDefaultIfNull(oAuth2Client);
        this.accessTokenRef = new AtomicReference<>();
        this.cacheIntervalMillis = config.applyDefaultIfNull(cacheIntervalMillis);
        this.threshold = config.applyDefaultThresholdIfNull(threshold);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void persist(@NonNull AccessToken accessToken) {
        cache(accessToken);
        sharedPreferences.edit()
                .putString(ACCESS_TOKEN, accessToken.toJson())
                .commit();
    }

    /**
     * Refresh the AccessToken if expired
     *
     * @return The AccessToken
     */
    @Override
    @WorkerThread
    public AccessToken getAccessToken() throws AuthenticationRequiredException {
        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        getAccessToken(future);
        try {
            return future.get();
        } catch (Exception e) {
            throw new AuthenticationRequiredException(e);
        }
    }

    @Override
    public void getAccessToken(FRListener<AccessToken> tokenListener) {
        AccessToken accessToken = getAccessTokenLocally();
        if (accessToken != null) {
            accessToken.setPersisted(true);
            if (accessToken.isExpired(threshold)) {
                Logger.debug(TAG, "Access Token Expired!");
                refresh(tokenListener);
            } else {
                Listener.onSuccess(tokenListener, accessToken);
            }
        } else {
            Listener.onException(tokenListener,
                    new AuthenticationRequiredException("No Access Token, authentication is required."));
        }
    }

    @Override
    public boolean hasToken() {
        //Consider null if Access token does not exists
        return sharedPreferences.getString(ACCESS_TOKEN, null) != null;
    }

    @WorkerThread
    @Override
    public AccessToken refresh() throws AuthenticationRequiredException {
        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        refresh(future);
        try {
            return future.get();
        } catch (Exception e) {
            throw new AuthenticationRequiredException(e);
        }
    }

    @Override
    public void refresh(final FRListener<AccessToken> listener) {
        AccessToken accessToken = getAccessTokenLocally();
        if (accessToken == null) {
            Listener.onException(listener, new AuthenticationRequiredException("Refresh Token does not exists."));
            return;
        }

        String refreshToken = accessToken.getRefreshToken();
        if (refreshToken == null) {
            Listener.onException(listener, new AuthenticationRequiredException("Refresh Token does not exists."));
            return;
        }
        clear();
        oAuth2Client.refresh(refreshToken, new FRListener<AccessToken>() {
            @Override
            public void onSuccess(AccessToken token) {
                persist(token);
                token.setPersisted(true);
                Listener.onSuccess(listener, token);
            }

            @Override
            public void onException(Exception e) {
                Listener.onException(listener, new AuthenticationRequiredException(e));
            }
        });
    }

    /**
     * Retrieve {@link AccessToken} from cache or from storage.
     *
     * @return The Access Token
     */
    private AccessToken getAccessTokenLocally() {

        if (accessTokenRef.get() != null) {
            Logger.debug(TAG, "Retrieving Access Token from cache");
            return accessTokenRef.get();
        }

        //Consider null if Access token does not exists
        String value = sharedPreferences.getString(ACCESS_TOKEN, null);
        if (value == null) {
            return null;
        }
        return AccessToken.fromJson(value);
    }

    /**
     * Cache the {@link AccessToken} in memory and setup a worker thread to clear it after.
     *
     * @param accessToken The AccessToken
     */
    private void cache(AccessToken accessToken) {
        if (cacheIntervalMillis > 0) {
            accessTokenRef.set(accessToken);
            worker.schedule(() -> {
                Logger.debug(TAG, "Removing Access Token from cache.");
                accessTokenRef.set(null);
            }, cacheIntervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void clear() {
        accessTokenRef.set(null);
        sharedPreferences.edit().clear().commit();
    }

    @Override
    public void revoke(FRListener<Void> listener) {
        AccessToken accessToken = getAccessTokenLocally();
        //No matter success revoke or not, clear the token locally.
        clear();
        if (accessToken == null) {
            Listener.onException(listener, new IllegalStateException("Access Token Not found!"));
            return;
        }
        oAuth2Client.revoke(accessToken, listener);

    }

}
