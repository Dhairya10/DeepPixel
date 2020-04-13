package com.datadit.deeppixel;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    LinearLayout linearLayoutEffects;
    ImageButton imageButtonUpload;
    ImageButton imageButtonClick;
    ImageButton imageButtonSketch;
    ImageButton imageButtonColormap;
    ImageView imageView;
    Bitmap bitmap, bitmapToSend;
    boolean bitmapReceived = false;
    static final int GALLERY_REQUEST_CODE = 1;
    static final int REQUEST_IMAGE_CAPTURE = 2;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;

    // OpenCV Initialisation
    static {
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        linearLayoutEffects = findViewById(R.id.linear_layout_effect);

        imageButtonUpload = findViewById(R.id.image_button_upload);
        imageButtonClick = findViewById(R.id.image_button_click);
        imageButtonSketch = findViewById(R.id.image_button_sketch);
        imageButtonColormap = findViewById(R.id.image_button_colormap);

        imageButtonUpload.setOnClickListener(this);
        imageButtonClick.setOnClickListener(this);
        imageButtonSketch.setOnClickListener(this);
        imageButtonColormap.setOnClickListener(this);

        imageView = findViewById(R.id.image_view);
    }

    // Function to check and request permission.
    public void checkPermissionCamera(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Toast.makeText(this, "Please enable camera permission", Toast.LENGTH_LONG).show();
                Log.d("PERMISSION", "Camera Permission Denied");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
                Log.d("PERMISSION", "Asking for permission : Camera ");
            }

        } else {
            dispatchTakePictureIntent();
        }
    }

    // Function to check and request permission.
    public void checkPermissionStorage(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Toast.makeText(this, "Please enable storage permission", Toast.LENGTH_LONG).show();
                Log.d("PERMISSION", "Storage Permission Denied");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
                Log.d("PERMISSION", "Asking for permission : Storage ");
            }

        } else {
            pickFromGallery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("PERMISSION", "Camera Permission Granted");
            } else {
                Toast.makeText(MainActivity.this,
                        "Camera Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("PERMISSION", "Storage Permission Granted");
            } else {
                Toast.makeText(MainActivity.this,
                        "Storage Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    // Function to upload image from gallery
    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/jpg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    // Function to capture an image
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();
            try {
                // Extracting the bitmap
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                bitmapToSend = bitmap;
                imageView.setImageBitmap(bitmap);
                bitmapReceived = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Making the options menu visible after checking if the bitmap is available or not
            if (bitmapReceived) {
                linearLayoutEffects.setVisibility(View.VISIBLE);
            }
        } else {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    bitmap = (Bitmap) extras.get("data");
                    bitmapToSend = bitmap;
                    imageView.setImageBitmap(bitmap);
                    bitmapReceived = true;
                }
                // Making the options menu visible after checking if the bitmap is available or not
                if (bitmapReceived) {
                    linearLayoutEffects.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void createSketch(Bitmap bitmap) {
        if (bitmap == null)
            return;

        // Instantiating a Mat object
        Mat mat = new Mat();
        // Converting a bitmap to mat
        Utils.bitmapToMat(bitmap, mat);

        try {
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
            Imgproc.adaptiveThreshold(mat, mat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);

            // Converting a mat to bitmap
            Utils.matToBitmap(mat, bitmap);
            imageView.setImageBitmap(bitmap);

        } catch (Exception e) {
            Log.d("SKETCH", String.valueOf(e));
            Toast.makeText(this, "Unable to create sketch", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void applyColorMap(Bitmap bitmap) {
        if (bitmap == null)
            return;

        // Instantiating a Mat object
        Mat mat = new Mat();
        // Converting a bitmap to mat
        Utils.bitmapToMat(bitmap, mat);

        try {
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
            Imgproc.applyColorMap(mat, mat, Imgproc.COLORMAP_BONE);

            // Converting a mat to bitmap
            Utils.matToBitmap(mat, bitmap);
            imageView.setImageBitmap(bitmap);

        } catch (Exception e) {
            Log.d("COLORMAP", String.valueOf(e));
            Toast.makeText(this, "Unable to apply colormap", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // Handling click events
    @Override
    public void onClick(View v) {
        if (v == imageButtonClick) {
            checkPermissionCamera(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
        } else if (v == imageButtonUpload) {
            checkPermissionStorage(Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);
        } else if (v == imageButtonSketch) {
            createSketch(bitmapToSend);
        } else if (v == imageButtonColormap) {
            applyColorMap(bitmapToSend);
        }
    }
}
