/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Callback to collect a username and validate it against IDM policy.
 */
@NoArgsConstructor
@Getter
public class ValidatedCreateUsernameCallback extends AbstractValidatedCallback {

    private String prompt;

    @Keep
    public ValidatedCreateUsernameCallback(@NotNull JSONObject raw, int index) throws JSONException {
        super(raw, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        if ("prompt".equals(name)) {
            this.prompt = (String) value;
        }
    }

    /**
     * Set the retrieved username.
     *
     * @param username the retrieved name (which may be null).
     */
    public void setUsername(String username) {
        setValue(username);
    }

    @Override
    public String getType() {
        return "ValidatedCreateUsernameCallback";
    }


}
