package com.mdsol.serialaisfinder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import dk.tbsalling.aismessages.AISInputStreamReader;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.PositionReport;

import com.google.common.eventbus.Subscribe;
import dk.tbsalling.ais.tracker.AisTrack;
import dk.tbsalling.ais.tracker.AisTracker;
import dk.tbsalling.ais.tracker.events.AisTrackCreatedEvent;
import dk.tbsalling.ais.tracker.events.AisTrackDeletedEvent;
import dk.tbsalling.ais.tracker.events.AisTrackDynamicsUpdatedEvent;
import dk.tbsalling.ais.tracker.events.AisTrackUpdatedEvent;

public class MainActivity extends AppCompatActivity {

    public static ArrayList<String> aisList = new ArrayList<>();

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private TextView display;
    private EditText editText;
    private MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new MyHandler(this);

        display = (TextView) findViewById(R.id.textView1);




        /*InputStream inputStream = new ByteArrayInputStream(demoNmeaStrings.getBytes());
        AisReader reader = AisReaders.createReaderFromInputStream(inputStream);
        reader.registerHandler(new Consumer<AisMessage>() {

            @Override
            public void accept(AisMessage aisMessage) {
                // Handle AtoN message
                if (aisMessage instanceof AisMessage21) {
                    AisMessage21 msg21 = (AisMessage21) aisMessage;
                    display.append("AtoN name: " + msg21.getName());
                }
                // Handle position messages 1,2 and 3 (class A) by using their shared parent
                if (aisMessage instanceof AisPositionMessage) {
                    AisPositionMessage posMessage = (AisPositionMessage) aisMessage;
                    display.append(posMessage.toString());
                }
                // Handle position messages 1,2,3 and 18 (class A and B)
                if (aisMessage instanceof IVesselPositionMessage) {
                    IVesselPositionMessage posMessage = (IVesselPositionMessage) aisMessage;
                    display.append(posMessage.toString());
                }
                // Handle static reports for both class A and B vessels (msg 5 + 24)
                if (aisMessage instanceof AisStaticCommon) {
                    AisStaticCommon staticMessage = (AisStaticCommon) aisMessage;
                    display.append("vessel name: " + staticMessage.getName());
                }
            }
        });
        reader.start();*/


    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
        callDecodeTask();
    }


    public void callDecodeTask() {
        Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsyncTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            decodeAis task = new decodeAis();
                            task.execute(aisList);
                        } catch (Exception e) {

                        }
                    }
                });
            }
        };
        timer.schedule(doAsyncTask,0, 10000);
    }

    private class decodeAis extends AsyncTask<ArrayList<String>, Void, ArrayList<String>>{

        @Override
        protected ArrayList<String> doInBackground(ArrayList<String>... arrayLists) {
            StringBuilder builder = new StringBuilder();
            for(String s : aisList)
            {
                builder.append(s);
            }
            InputStream inputStream = new ByteArrayInputStream(builder.toString().getBytes());
            AISInputStreamReader streamReader = new AISInputStreamReader(inputStream, new Consumer<AISMessage>() {
                @Override
                public void accept(AISMessage aisMessage) {
                    display.append(String.valueOf(aisMessage.getMessageType().getValue()));
                    if(aisMessage instanceof PositionReport){
                        PositionReport msg123 = (PositionReport) aisMessage;
                        display.append(msg123.toString());
                    }
                }
            });

            try {
                streamReader.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
                   /* AisReader reader = AisReaders.createReaderFromInputStream(inputStream);
            reader.registerHandler(new Consumer<AisMessage>() {

                @Override
                public void accept(AisMessage aisMessage) {
                    // Handle AtoN message
                    display.append(String.valueOf(aisMessage.getMsgId()));
                    if (aisMessage instanceof AisMessage21) {
                        AisMessage21 msg21 = (AisMessage21) aisMessage;
                        display.append("AtoN name: " + msg21.getName());
                    }
                    // Handle position messages 1,2 and 3 (class A) by using their shared parent
                    if (aisMessage instanceof AisPositionMessage) {
                        AisPositionMessage posMessage = (AisPositionMessage) aisMessage;
                        display.append("speed over ground: " + posMessage.getSog());
                    }
                    // Handle position messages 1,2,3 and 18 (class A and B)
                    if (aisMessage instanceof IVesselPositionMessage) {
                        IVesselPositionMessage posMessage = (IVesselPositionMessage) aisMessage;
                        display.append("course over ground: " + posMessage.getCog());
                    }
                    // Handle static reports for both class A and B vessels (msg 5 + 24)
                    if (aisMessage instanceof AisStaticCommon) {
                        AisStaticCommon staticMessage = (AisStaticCommon) aisMessage;
                        display.append("vessel name: " + staticMessage.getName());
                    }
                }
            });

            reader.start();*/
            return null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }



    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    aisList.add(data);
                    mActivity.get().display.append(data);

                    /*InputStream inputStream = new ByteArrayInputStream(data.getBytes());
                    AISTracker tracker = new AISTracker();
                    try {
                        tracker.update (inputStream);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Set tracks = tracker.getAisTracks();
                    mActivity.get().display.append(String.valueOf(tracker.getNumberOfAisTracks()));
                    while (tracks.iterator().hasNext())
                    {
                        mActivity.get().display.append("decoding ais");
                        AISTrack track = (AISTrack) tracks.iterator().next();
                        mActivity.get().display.append(track.toString());
                    }*/
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
                    break;
            }
        }


        public class EventTracker {
            // Create the tracker
            AisTracker tracker = new AisTracker();

            // Register event listeners
            tracker.registerSubscriber(new Object() {
                @Subscribe
                public void handleEvent(AisTrackCreatedEvent event) {
                    System.out.println("CREATED: " + event.getAisTrack());
                }

                @Subscribe
                public void handleEvent(AisTrackUpdatedEvent event) {
                    System.out.println("UPDATED: " + event.getAisTrack());
                }

                @Subscribe
                public void handleEvent(AisTrackDynamicsUpdatedEvent event) {
                    System.out.println("UPDATED DYNAMICS: " + event.getAisTrack());
                }

                @Subscribe
                public void handleEvent(AisTrackDeletedEvent event) {
                    System.out.println("DELETED: " + event.getAisTrack());
                }
            });

            // Feed AIS Data into tracker
            InputStream aisInputStream = ...;
            tracker.update(aisInputStream);
        }


    }




}
