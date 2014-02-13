package com.bitalino.bitalinodroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.net.Uri;
import android.widget.Toast;

import com.bitalino.BITalinoDevice;
import com.bitalino.BITalinoFrame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Date;
import java.text.SimpleDateFormat;

public class TakePictureActivity extends Activity {

    private static final String TAG = "TakePictureActivity";
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static ImageView mImageView;
    static String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        BITalinoDevice bitalino = (BITalinoDevice)intent.getSerializableExtra("bitalino");
        setContentView(R.layout.activity_take_picture);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    /** Called when the user clicks the take pictures button */
    public void takePicture(View view) {
        // create Intent to take a picture and return control to the calling application
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.d(TAG, "Error with file create.");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
            // Take 20 s snapshot of arduino data store as an array with



        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();

        Log.d(TAG, "File Created");
        Log.d(TAG, mCurrentPhotoPath);

        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        Log.d(TAG, "Scanner initiated");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ArrayList<Tag> frameSet = new ArrayList<Tag>();
        sendEmail(frameSet);
        // read n samples
        final int numberOfSamplesToRead = 50;
        publishProgress("Reading " + numberOfSamplesToRead + " samples..");
        BITalinoFrame[] frames = bitalino.read(numberOfSamplesToRead);
        int count =0;
        ArrayList<Integer> samples  = new ArrayList<Integer>();
        for (BITalinoFrame frame : frames){
            String frameString = frame.toString();
            String[] sep1 = frameString.split("analog=");
            String[] sep2 = sep1[1].split(", 0, 0, 0, 0, 0");
            String str = sep2[0].substring(1);
            int reading = Integer.parseInt(str);
            samples.add(reading);
            count++;
        }
        int sum = 0;
        int avCount = 0;
        double criteria = .30;
        for(int read : samples) {
            sum += read;
        }
        double firstAv = sum / samples.size();
        double upper = firstAv * (1 + criteria);
        double lower = firstAv * (1 - criteria);
        sum = 0;
        for(int read : samples) {
            if(read < upper && read > lower) {
                sum += read;
                avCount++;
            }
        }
        double average = sum / avCount;

        long endTime=System.currentTimeMillis();
        Date date = new Date(endTime);
        DateFormat dForm =DateFormat.getDateInstance();
        String timeStamp=dForm.format(date);
        Tag tag = new Tag(timeStamp, average, count);
        frameSet.add(tag);

        sendEmail(frameSet);




//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//            Log.d(TAG, "Pic taken");
//            Bundle extras = data.getExtras();
//            Bitmap imageBitmap = (Bitmap) extras.get("data");
//            mImageView.setImageBitmap(imageBitmap);
//        }
        galleryAddPic();
    }

}
