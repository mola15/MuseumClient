package it.polimi.molinaroli.museumclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import it.polimi.molinaroli.museumclient.Logic.IntentConverter;
import it.polimi.molinaroli.museumclient.Logic.LiquidAndroidService;
import it.polimi.molinaroli.museumclient.Logic.NsdHelper;
import xdroid.toaster.Toaster;

public class MainActivity extends AppCompatActivity {

    int myServerPort;
    Context c;
    Button discover;
    Button display;
    Button start;
    Button forward;
    NsdHelper helper;
    ListView serviceList;
    LiquidAndroidService mService;
    boolean mBound = false;
    Intent arrivalIntent;

    Button url;
    Button video;
    Button image;
    Button play;
    Button pause;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LiquidAndroidService.LocalBinder binder = (LiquidAndroidService.LocalBinder) service;
            mService = binder.getService();
            Log.d("Activity","service connected");
            mBound = true;
            myServerPort = mService.getPort();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d("Activity","service disconnected");
            mBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        arrivalIntent = getIntent();
        if(getIntent().getAction().equals("OPEN")){
            //qui è stato avviato dalla notifica e quindi sicuramente il service sta andando
            final Intent intent = new Intent(this, LiquidAndroidService.class);
            bindService(intent, mConnection, 0);
            Log.d("bound", "" + mBound);
            Log.d("helperinit",""+mService.getHelper().getInit());
        } else if ((arrivalIntent.getAction().equals("android.media.action.IMAGE_CAPTURE")) || (arrivalIntent.getAction().equals(Intent.ACTION_VIEW)) || (arrivalIntent.getAction().equals(Intent.ACTION_SEND)) || (arrivalIntent.getAction().equals(Intent.ACTION_SENDTO))){
            //voglio che sia partito il servizio e quindi dico che mbound è true;
            final Intent intent = new Intent(this, LiquidAndroidService.class);
            bindService(intent, mConnection, 0);
            Log.d("bound", "" + mBound);
        }

        setContentView(R.layout.activity_main);
        Log.e("azione intent",getIntent().getAction());

        try {
            Log.e("intento arrivato", IntentConverter.intentToJSON(getIntent()).toString());
        }catch(Exception e){
            e.printStackTrace();
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {


        final Intent intent = new Intent(this, LiquidAndroidService.class);


        c = this;
        start = (Button) findViewById(R.id.startservice) ;
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("bindstate",""+ mBound);
                if(!mBound) {
                    Intent myIntent = new Intent(getApplicationContext(), LiquidAndroidService.class);
                    startService(myIntent);
                    bindService(intent, mConnection, 0);
                    Log.d("bound", "" + mBound);
                } else{
                    Toaster.toast("Service already Started");
                }
            }
        });


        super.onStart();

    }


    @Override
    protected void onDestroy() {
        /*
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        */
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        super.onStop();
    }

}
