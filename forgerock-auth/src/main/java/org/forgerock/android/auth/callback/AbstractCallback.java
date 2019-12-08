/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import lombok.Getter;
import lombok.NoArgsConstructor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Abstract Callback that provides the raw content of the Callback, and allow sub classes to access
 * Callback's input and output
 */
@NoArgsConstructor
@Getter
public abstract class AbstractCallback implements Callback {

    //The content is as JSON representation, JSONObject is not Serializable
    protected static final String VALUE = "value";
    protected String content;
    protected int id;

    protected JSONObject getContentAsJson() throws JSONException {
        return new JSONObject(content);
    }

    public AbstractCallback(JSONObject raw, int index) {
        setContent(raw);
        id = raw.optInt("_id", index);

        JSONArray output = raw.optJSONArray("output");
        for (int i = 0; i < output.length(); i++) {
            try {
                JSONObject elm = output.getJSONObject(i);
                setAttribute(getName(elm), elm.get(VALUE));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected abstract void setAttribute(String name, Object value);

    protected String getName(JSONObject jsonObject) {
        return jsonObject.optString("name");
    }

    /**
     * Sets the value of the Callback
     *
     * @param jsonObject The Json Object to represent the Callback
     */
    protected void setContent(JSONObject jsonObject) {
        content = jsonObject.toString();

    }

    /**
     * Set the value for the input.
     *
     * @param value The input value
     * @param index The index of the element.
     */
    public void setValue(Object value, int index) {
        try {
            JSONObject json = getContentAsJson();
            JSONObject input = getInput(json, index);
            input.put(VALUE, value);
            setContent(json);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the first value for input
     *
     * @param value The input value
     */
    public void setValue(Object value) {
        setValue(value, 0);
    }

    private JSONObject getInput(JSONObject content, int index) throws JSONException {
        return content
                .getJSONArray("input")
                .getJSONObject(index);

    }

}
