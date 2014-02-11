
package com.bitalino.bitalinodroid;

import com.google.gson.Gson;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import com.bitalino.BITalinoDevice;
import com.bitalino.BITalinoFrame;
import java.util.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import android.widget.Toast;
import java.util.Date;
import java.text.SimpleDateFormat;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    /*
     * http://developer.android.com/reference/android/bluetooth/BluetoothDevice.html
     * #createRfcommSocketToServiceRecord(java.util.UUID)
     *
     * "Hint: If you are connecting to a Bluetooth serial board then try using the
     * well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if you
     * are connecting to an Android peer then please generate your own unique
     * UUID."
     */
    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private boolean testInitiated = false;
    private boolean testComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!testInitiated)
            new TestAsyncTask().execute();
    }

//    private void startPicture(BITalinoDevice bitalino) {
//        Intent intent = new Intent(this, TakePictureActivity.class);
//        //String bString = (new Gson()).Json(bitalino);
//        //intent.putExtra("Object String",bString);
//        startActivity(intent);
//    }

    private void sendEmail(ArrayList<Tag> set){
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"ktzhou@stanford.edu"});
        i.putExtra(Intent.EXTRA_SUBJECT, "Data Snap");
        StringBuilder str = new StringBuilder();
        Collections.sort(set);

        for(Tag frame: set){
            str.append(frame.toString());
            str.append("\n");
        }
        i.putExtra(Intent.EXTRA_TEXT   , str.toString());
        try {
           startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private class TestAsyncTask extends AsyncTask<Void, String, Void> {
        private TextView tvLog = (TextView) findViewById(R.id.log);
        private BluetoothDevice dev = null;
        private BluetoothSocket sock = null;
        private InputStream is = null;
        private OutputStream os = null;

        @Override
        protected Void doInBackground(Void... paramses) {
            btAddData();
            return null;
        }




        protected Void btAddData(){
            try {
                // Let's get the remote Bluetooth device
               ArrayList<Tag> frameSet = new ArrayList<Tag>();
                final String remoteDevice = "98:D3:31:B1:83:46";

                final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                dev = btAdapter.getRemoteDevice(remoteDevice);
                Log.d(TAG, "Stopping Bluetooth discovery.");
                btAdapter.cancelDiscovery();

                sock = dev.createRfcommSocketToServiceRecord(MY_UUID);
                sock.connect();
                testInitiated = true;

                BITalinoDevice bitalino = new BITalinoDevice(10, new int[]{0});
                publishProgress("Connecting to BITalino [" + remoteDevice + "]..");
                bitalino.open(sock.getInputStream(), sock.getOutputStream());
                publishProgress("Connected.");

                // get BITalino version
                publishProgress("Version: " + bitalino.version());

                // start acquisition on predefined analog channels
                bitalino.start();
               // startPicture(bitalino);
                // read n samples

                final int numberOfSamplesToRead = 50;
                publishProgress("Reading " + numberOfSamplesToRead + " samples..");
                BITalinoFrame[] frames = bitalino.read(numberOfSamplesToRead);
                int count =0;
                for (BITalinoFrame frame : frames){
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String str = frame.toString();
                    count++;
                    Tag tag = new Tag(timeStamp, str, count);
                    frameSet.add(tag);
                }


                // trigger digital outputs
                // int[] digital = { 1, 1, 1, 1 };
                // device.trigger(digital);
               sendEmail(frameSet);
                // stop acquisition and close bluetooth connection
               bitalino.stop();
               // publishProgress("BITalino is stopped");

                sock.close();
                //publishProgress("And we're done! :-)");
            } catch (Exception e) {
                Log.e(TAG, "There was an error.", e);
            }
            return null;
        }




        @Override
        protected void onProgressUpdate(String... values) {
            tvLog.append("\n".concat(values[0]));
        }

    }

}