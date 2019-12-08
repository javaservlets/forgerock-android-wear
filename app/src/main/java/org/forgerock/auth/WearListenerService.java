package org.forgerock.auth;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.forgerock.android.auth.ui.SimpleLoginActivity;
import org.forgerock.android.auth.FRAuth;
import org.forgerock.android.auth.FRDevice;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.UserInfo;
import org.forgerock.android.auth.ui.SimpleLoginActivity;
import org.forgerock.android.auth.ui.SimpleRegisterActivity;
import org.json.JSONException;
import org.json.JSONObject;

import static org.forgerock.auth.MainActivity.*;

public class WearListenerService extends WearableListenerService {
    String TAG = "+++ mobile listner";
    public static final int AUTH_REQUEST_CODE = 100;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "mobile listner started");
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.v(TAG, "msg received" + messageEvent.getPath());
        try {
            Intent intent = new Intent(getApplicationContext().getPackageManager().getLaunchIntentForPackage("org.forgerock.auth"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("wear_command", messageEvent.getPath());
            startActivity(intent);
            //Log.v(TAG, "opened mobile");
        } catch (Exception e) {
            e.getMessage();
        }
    }


}

