/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.forgerock.android.auth.callback.ConfirmationCallback;
import org.forgerock.android.auth.ui.R;

/**
 * UI representation for {@link ConfirmationCallback}
 */
public class ConfirmationCallbackFragment extends CallbackFragment<ConfirmationCallback> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_confirmation_callback, container, false);
        TextView prompt = view.findViewById(R.id.prompt);
        prompt.setText(callback.getPrompt());
        LinearLayout confirmation = view.findViewById(R.id.confirmation);
        for (int i = 0; i < callback.getOptions().size(); i++) {
            Button button = new Button(getContext());
            button.setText(callback.getOptions().get(i));
            final int finalI = i;
            button.setOnClickListener(v -> {
                callback.setSelectedIndex(finalI);
                next();
            });
            confirmation.addView(button, i);
        }
        return view;
    }

}
