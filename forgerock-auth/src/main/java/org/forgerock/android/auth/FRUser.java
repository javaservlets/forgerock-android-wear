/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import androidx.annotation.WorkerThread;
import lombok.RequiredArgsConstructor;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;

public class FRUser {

    //Hold the current login user.
    private static FRUser current;

    private SessionManager sessionManager;

    private FRUser() {
        sessionManager = SessionManager.builder()
                .build();
    }

    /**
     * Retrieve the existing FRUser instance,
     * <p>
     * If user session does not exist return null, otherwise return the existing FRUser with the associated user session,
     * this cannot guarantee the existing user session is valid.
     *
     * @return The existing FRUser instance, or null if there is no user session.
     */
    public static FRUser getCurrentUser() {
        if (current != null) {
            return current;
        }

        FRUser user = new FRUser();
        if (user.sessionManager.hasSession()) {
            current = user;
        }
        return current;
    }

    /**
     * Logout the user
     */
    public void logout() {
        current = null;
        sessionManager.close();
    }


    /**
     * Retrieve the {@link AccessToken} asynchronously,
     *
     * <p>
     * If the stored {@link AccessToken} is expired, auto refresh the token.
     *
     * @param listener Listener to listen get Access Token event.
     */

    public void getAccessToken(FRListener<AccessToken> listener) {
        sessionManager.getAccessToken(listener);
    }

    @WorkerThread
    public AccessToken getAccessToken() throws AuthenticationRequiredException {
        return sessionManager.getAccessToken();
    }

    /**
     * Handles REST requests to the OpenId Connect userinfo endpoint for retrieving information about the user who granted
     * the authorization for the token.
     *
     * @param listener Listener to listen get UserInfo event.
     */
    public void getUserInfo(final FRListener<UserInfo> listener) {

        UserService.builder()
                .serverConfig(Config.getInstance().getServerConfig())
                .sessionManager(sessionManager)
                .build()
                .userinfo(new FRListener<UserInfo>() {
                    @Override
                    public void onSuccess(UserInfo result) {
                        Listener.onSuccess(listener, result);
                    }

                    @Override
                    public void onException(Exception e) {
                        Listener.onException(listener, e);
                    }
                });
    }

    /**
     * Trigger the user login process, the login service name can be defined under <b>string.xml</b> file with
     * <b>forgerock_auth_service</b>
     *
     * @param context  The Application Context
     * @param listener Listener to listen login event.
     */
    public static void login(Context context, final NodeListener<FRUser> listener) {
        createFRAuth(context, context.getString(R.string.forgerock_auth_service))
                .next(context, listener);
    }

    private static FRAuth createFRAuth(Context context, String serviceName) {
        return FRAuth.builder()
                .serviceName(serviceName)
                .context(context)
                .interceptor(new UserInterceptor())
                .build();
    }

    /**
     * Trigger the user login process, the registration service name can be defined under <b>string.xml</b> file with
     * <b>forgerock_registration_service</b>
     *
     * @param context  The Application Context
     * @param listener Listener to listen login event.
     */
    public static void register(Context context, NodeListener<FRUser> listener) {
        createFRAuth(context, context.getString(R.string.forgerock_registration_service))
                .start(context, listener);
    }

    @RequiredArgsConstructor
    private static class UserInterceptor implements Interceptor {

        @Override
        public void intercept(Chain chain, Object any) {
            current = new FRUser();
            chain.proceed(current);
        }
    }

}
