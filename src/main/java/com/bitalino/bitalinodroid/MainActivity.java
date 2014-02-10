package com.bitalino.bitalinodroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.net.Uri;
import android.widget.Toast;

import com.bitalino.BITalinoDevice;
import com.bitalino.BITalinoFrame;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.util.UUID;
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
  public static final int MEDIA_TYPE_IMAGE = 1;
  public static final int MEDIA_TYPE_VIDEO = 2;
  private Uri fileUri;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    if (!testInitiated)
      new TestAsyncTask().execute();

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
      try {
        // Let's get the remote Bluetooth device
        final String remoteDevice = "20:13:08:08:15:83";

        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        dev = btAdapter.getRemoteDevice(remoteDevice);

    /*
     * Establish Bluetooth connection
     *
     * Because discovery is a heavyweight procedure for the Bluetooth adapter,
     * this method should always be called before attempting to connect to a
     * remote device with connect(). Discovery is not managed by the Activity,
     * but is run as a system service, so an application should always call
     * cancel discovery even if it did not directly request a discovery, just to
     * be sure. If Bluetooth state is not STATE_ON, this API will return false.
     *
     * see
     * http://developer.android.com/reference/android/bluetooth/BluetoothAdapter
     * .html#cancelDiscovery()
     */
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

        // read n samples
        final int numberOfSamplesToRead = 10;
        publishProgress("Reading " + numberOfSamplesToRead + " samples..");
        BITalinoFrame[] frames = bitalino.read(numberOfSamplesToRead);
        for (BITalinoFrame frame : frames)
          publishProgress(frame.toString());

        // trigger digital outputs
        // int[] digital = { 1, 1, 1, 1 };
        // device.trigger(digital);

        // stop acquisition and close bluetooth connection
        bitalino.stop();
        publishProgress("BITalino is stopped");

        sock.close();
        publishProgress("And we're done! :-)");
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

  /** Called when the user clicks the Send button */
  public void takePicture(View view) {
    // create Intent to take a picture and return control to the calling application
    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

    fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

    // start the image capture Intent
    startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
  }

  /** Create a file Uri for saving an image or video */
  private static Uri getOutputMediaFileUri(int type){
      return Uri.fromFile(getOutputMediaFile(type));
  }
  /** Create a File for saving an image or video */
  private static File getOutputMediaFile(int type){
      // To be safe, you should check that the SDCard is mounted
      // using Environment.getExternalStorageState() before doing this.

      File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
              Environment.DIRECTORY_PICTURES), "EmotionSense_Storage");
      // This location works best if you want the created images to be shared
      // between applications and persist after your app has been uninstalled.

      // Create the storage directory if it does not exist
      if (! mediaStorageDir.exists()){
          if (! mediaStorageDir.mkdirs()){
              Log.d("MyCameraApp", "failed to create directory");
              return null;
          }
      }

      // Create a media file name
      String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
      File mediaFile;
      if (type == MEDIA_TYPE_IMAGE){
          mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                  "IMG_"+ timeStamp + ".jpg");
      } else if(type == MEDIA_TYPE_VIDEO) {
          mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                  "VID_"+ timeStamp + ".mp4");
      } else {
          return null;
      }

      return mediaFile;
  }

  private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
  private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
          if (resultCode == RESULT_OK) {
              // Image captured and saved to fileUri specified in the Intent
              Toast.makeText(this, "Image saved to:\n" +
                      data.getData(), Toast.LENGTH_LONG).show();
          } else if (resultCode == RESULT_CANCELED) {
              // User cancelled the image capture
          } else {
              // Image capture failed, advise user
          }
      }

      if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
          if (resultCode == RESULT_OK) {
              // Video captured and saved to fileUri specified in the Intent
              Toast.makeText(this, "Video saved to:\n" +
                      data.getData(), Toast.LENGTH_LONG).show();
          } else if (resultCode == RESULT_CANCELED) {
              // User cancelled the video capture
          } else {
              // Video capture failed, advise user
          }
      }
  }



}