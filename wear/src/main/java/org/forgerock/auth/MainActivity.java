package org.forgerock.auth;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends WearableActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {
    private GoogleApiClient mGoogleApiClient;
    private String TAG = "+++ robbies wear";
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.open).setOnClickListener(this);
        findViewById(R.id.auth).setOnClickListener(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        Log.v(TAG, "wear app open");
    }

    @Override
    public void onClick(View view) {
        Log.v(TAG, " clicked on btn # " + view.getId());

        switch (view.getId()) {
            case R.id.open:
                sendMessage("open", view);
                break;
            case R.id.auth:
                sendMessage("auth", view);
                break;
        }
    }

    //    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "connecting to the Google API");
        mGoogleApiClient.connect();
        //rj only uncomment if u want 2 test node comm (ie, watch 2 phone) // sendMessage("open");
    }

    public void sendMessage(final String msg, final View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (String nodeId : getNodes()) {
                    Log.v(TAG, " sendMsg sendToDevice: " + msg);
                    sendToDevice(nodeId, "/" + msg, msg, view); //note mobile app itself may redirect
                }
            }
        }).start();
    }

    private void sendToDevice(final String nodeId, final String path, final String message, final View wearview) {
        final ResultCallback<MessageApi.SendMessageResult> resultCallback = new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(final MessageApi.SendMessageResult sendMessageResult) {
                try {
                    if (!sendMessageResult.getStatus().isSuccess()) {
                        Log.v(TAG, "sending message failed");
                        Toast.makeText(MainActivity.this, "failed message", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.v(TAG, "successfully sent: " + message);
                        Toast.makeText(MainActivity.this, "GA on 12.10.19", Toast.LENGTH_SHORT).show();
                        Snackbar.make(wearview, "GA:: 12.10.19",Snackbar.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Wearable.MessageApi
                .sendMessage(mGoogleApiClient, nodeId, path, message.getBytes())
                .setResultCallback(resultCallback, 10, TimeUnit.SECONDS);
    }


    @Override
    public void onConnected(final Bundle bundle) {
        //Log.v(TAG, "Google Api connected");
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        Log.v(TAG, "found " + results.size() + " nodes");
        return results;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }


}
