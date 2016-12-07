package image.amit.com.myapplication;

import android.*;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity  {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_TAG = 1;
   final String CAMERA_PERMISSION_MESSAGE = "Allow app to access camera ?\n" +
            "Go to permission setting";
    final int READ_WRITE_EXTERNAl_TAG = 2;
    String STORAGE_PERMISSION_MESSAGE = "Allow app to access photos, media and storage permissions ?\n" +
            "Go to permission setting";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-2895717348641142~6640738710");
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        findViewById(R.id.btn_capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPermissionGranted(android.Manifest.permission.CAMERA) && isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE))
                    dispatchTakePictureIntent();
                else {
                    requestPermission(new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE}, CAMERA_PERMISSION_TAG, CAMERA_PERMISSION_MESSAGE);

                }
            }
        });
        findViewById(R.id.btn_browse_pic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE) && isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, PICK_IMAGE_REQUEST);
                } else {
                    requestPermission( new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, READ_WRITE_EXTERNAl_TAG, STORAGE_PERMISSION_MESSAGE);
                }
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//            Log.e("Tag","data"+data.getData());
            Intent intent = new Intent(this, ImageProcessingActivity.class);
            startActivity(intent);
        }
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
//            Log.e("TAG","Uri"+data.getData());
            if (data == null) {
                showError("Failed to open picture!");
                return;
            }


            Intent intent = new Intent(this, ImageProcessingActivity.class);
            intent.setData(data.getData());
            startActivity(intent);

        }
    }




    private void showDialogToAccessPermissions(String msg, DialogInterface.OnClickListener dialogListener) {
        new AlertDialog.Builder(MainActivity.this).setMessage(msg).setPositiveButton("Allow", dialogListener).setNegativeButton("Deny", null).create().show();
    }

    public void requestPermission(final String[] requestPermission, final int requestCodeAskPermission, final String storagePermissionMessage) {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, requestPermission[0])) {
            ActivityCompat.requestPermissions(this, requestPermission, requestCodeAskPermission);
        } else {
            showDialogToAccessPermissions(storagePermissionMessage, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String packageName = getApplicationContext().getPackageName();
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                }
            });
        }
    }

    public boolean isPermissionGranted(String requestPermission) {
        int result = ContextCompat.checkSelfPermission(this, requestPermission);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private boolean verifyPermissions(int[] grantResults) {
        // At least one result must be checked.
        if (grantResults.length < 1) {
            return false;
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void showError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_TAG:
                if (verifyPermissions(grantResults)) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(MainActivity.this, "Please Allow app to use Camera, From settings", Toast.LENGTH_LONG).show();
                }
                break;
            case READ_WRITE_EXTERNAl_TAG:
                if (verifyPermissions(grantResults)) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, PICK_IMAGE_REQUEST);
                } else {
                    Toast.makeText(MainActivity.this, "Please Allow app to use Storage to get Pictures, From settings", Toast.LENGTH_LONG).show();
                }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
}