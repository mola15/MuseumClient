/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.polimi.molinaroli.museumclient.Logic;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.jaredrummler.android.device.DeviceName;

import java.util.ArrayList;

import it.polimi.molinaroli.museumclient.MainActivity;
import it.polimi.molinaroli.museumclient.R;

public class NsdHelper {
    private int init;
    Context mContext;
    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;
    public static final String SERVICE_TYPE = "_museum._tcp.";
    public static final String TAG = "NsdHelper";
    public String mServiceName = "Museum";
    NsdServiceInfo mService;
    String registeredName;
    private ArrayList<NsdServiceInfo> services;

    AlertDialog.Builder alertDialog;
    Context activity;
    ListView lv;
    Intent toF;

    int vicini;
    boolean[] acheck;

    public NsdHelper(Context context) {
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        services = new ArrayList<>();
        mServiceName ="Museum " + DeviceName.getDeviceName();
        setInit(2);
    }

    public void initializeNsd() {
        initializeResolveListener();
        //mNsdManager.init(mContext.getMainLooper(), this);
        setInit(2);
    }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(registeredName)) {
                    Log.d(TAG, "Same machine: " + registeredName);
                } else if (service.getServiceType().equals(SERVICE_TYPE)) {
                    mNsdManager.resolveService(service, new CustomResolveListener());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
                if (mService == service) {
                    mService = null;
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                boolean present = false;
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                if (serviceInfo.getServiceName().equals(registeredName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                mService = serviceInfo;
                //quando ne trovo uno lo aggiungo all array
                present = false;
                for (NsdServiceInfo s : services) {
                    if (s.getServiceName().equals(serviceInfo.getServiceName())) {
                        present = true;
                        break;
                    }
                }
                if (!present) {
                    getServices().add(serviceInfo);
                }
                //qui ho risolto i servizi devo fare il display
                //setto l'adapter
            }
        };
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                registeredName = NsdServiceInfo.getServiceName();
                Log.d(TAG, "Service registered: " + registeredName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
                Log.d(TAG, "Service registration failed: " + arg1);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                Log.d(TAG, "Service unregistered: " + arg0.getServiceName());
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "Service unregistration failed: " + errorCode);
            }
        };
    }

    public void registerService(int port) {
        tearDown();  // Cancel any previous registration request
        initializeRegistrationListener();
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public void discoverServices() {
        stopDiscovery();  // Cancel any eFxisting discovery request
        initializeDiscoveryListener();
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDiscovery() {
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } finally {
            }
            mDiscoveryListener = null;
        }
    }

    public NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }

    public void tearDown() {
        if (mRegistrationListener != null) {
            try {
                mNsdManager.unregisterService(mRegistrationListener);
            } finally {
            }
            mRegistrationListener = null;
        }
    }

    public ArrayList<NsdServiceInfo> getServices() {
        return services;
    }

    public void setServices(ArrayList<NsdServiceInfo> services) {
        this.services = services;
    }

    public int getInit() {
        return init;
    }

    public void setInit(int init) {
        this.init = init;
    }





    public void forwardIntent(Context c, Intent i, int myServerPort){
        stopDiscovery();
        services = new ArrayList<>();
        toF = i;
        //fa tutto il discover services una volta risolto il client viene lanciato l'intento
        // per questo bisogna salvare l'intent arrivato a livello globale
        discoverServices();

    }

    //INNER CLASSES


    public class CustomResolveListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed" + errorCode);
            switch (errorCode) {
                case NsdManager.FAILURE_ALREADY_ACTIVE:
                    Log.e(TAG, "FAILURE_ALREADY_ACTIVE");
                    // Just try again...
                    mNsdManager.resolveService(serviceInfo, new CustomResolveListener());
                    break;
                case NsdManager.FAILURE_INTERNAL_ERROR:
                    Log.e(TAG, "FAILURE_INTERNAL_ERROR");
                    break;
                case NsdManager.FAILURE_MAX_LIMIT:
                    Log.e(TAG, "FAILURE_MAX_LIMIT");
                    break;
            }
        }

        @Override
        public void onServiceResolved(final NsdServiceInfo serviceInfo) {
            boolean present = false;
            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
            if (serviceInfo.getServiceName().equals(registeredName)) {
                Log.d(TAG, "Same IP.");
                return;
            }
            mService = serviceInfo;
            //quando ne trovo uno lo aggiungo all array
            present = false;
            for (final NsdServiceInfo s : services) {
                if (s.getServiceName().equals(serviceInfo.getServiceName())) {
                    present = true;
                    break;
                }
            }

                if (!present) {
                    getServices().add(serviceInfo);
                }
        }
    }


    //innerclass adapter
    public class CustomAdapter extends ArrayAdapter<NsdServiceInfo> {
        ArrayList<CheckBox> cb = new ArrayList<>();

        public CustomAdapter(Context context, int textViewResourceId,
                             ArrayList<NsdServiceInfo> objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.serviceitem, null);
            TextView nome = (TextView)convertView.findViewById(R.id.name);
            TextView port = (TextView)convertView.findViewById(R.id.porta);
            TextView ip = (TextView)convertView.findViewById(R.id.ip);
            CheckBox s = (CheckBox) convertView.findViewById(R.id.selected);

            cb.add(position,s);

            NsdServiceInfo c = getItem(position);
            nome.setText(c.getServiceName().replaceAll("Museum ",""));
            port.setText("" + c.getPort());
            ip.setText(c.getHost().toString().replaceAll("/",""));
            return convertView;
        }

        public ArrayList<CheckBox> getCheckBoxes (){
            return cb;
        }
    }



}
