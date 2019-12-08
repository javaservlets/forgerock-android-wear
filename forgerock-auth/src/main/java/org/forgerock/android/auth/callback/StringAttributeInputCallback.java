/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Implements a Callback for collection of a single identity object attribute from a user.
 */
@NoArgsConstructor
@Getter
public class StringAttributeInputCallback extends AttributeInputCallback {

    /**
     * The attribute Value
     */
    private String value;

    /**
     * Constructor for this Callback.
     */
    @Keep
    public StringAttributeInputCallback(JSONObject jsonObject, int index) throws JSONException {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        if ("value".equals(name)) {
            this.value = (String) value;
        }
    }

    @Override
    public String getType() {
        return "StringAttributeInputCallback";
    }
}
