package image.amit.com.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by applect on 2/12/16.
 */

public class ImageProcessingActivity extends Activity {
    private static final String TAG = "ImageProcessingActivity";
    private EditText tv_width;
    private EditText tv_height;
    private ImageView iv_image;
    private SeekBar seekbar;
    private RadioGroup rg_sizes;
    private File finalFile;
    private Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
    private Bitmap.Config bitmapConfig = Bitmap.Config.ARGB_8888;
    private int quality = 80;
    private String destinationDirectoryPath;
    private File compressedImage;
    private float width;
    private float height;
    private TextView tv_size;
    private Bitmap bmp;
    private int MaxTextureSize;
    private RadioButton pixelRadioButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imageprocess);
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-2895717348641142~6640738710");
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        try {
            finalFile = FileUtil.from(this, getIntent().getData());

        } catch (Exception e) {
            String[] projection = new String[]{
                    MediaStore.Images.ImageColumns._ID,
                    MediaStore.Images.ImageColumns.DATA,
                    MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.ImageColumns.DATE_TAKEN,
                    MediaStore.Images.ImageColumns.MIME_TYPE
            };
            final Cursor cursor = getContentResolver()
                    .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                            null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
            if (cursor.moveToFirst()) {
                String imageLocation = cursor.getString(1);
                File imageFile = new File(imageLocation);
                if (imageFile.exists()) {   // TODO: is there a better way to do this?
                    finalFile = imageFile;
                }
            }
        }
        compressedImage = finalFile;
        Log.e("Tag", "Compressed Image path" + compressedImage);
        tv_width = (EditText) findViewById(R.id.tv_width);
        tv_height = (EditText) findViewById(R.id.tv_height);
         pixelRadioButton=(RadioButton)findViewById(R.id.rb_pixel);
        tv_size = (TextView) findViewById(R.id.tv_size);
        destinationDirectoryPath = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).getAbsolutePath();
        iv_image = (ImageView) findViewById(R.id.iv_image);
        seekbar = (SeekBar) findViewById(R.id.seekBar2);
        rg_sizes = (RadioGroup) findViewById(R.id.rg_sizes);
        bmp = BitmapFactory.decodeFile(finalFile.getPath());
        tv_width.setText(bmp.getWidth() + "");
        tv_height.setText(bmp.getHeight() + "");
        Log.e("Tag", "Texture Height" + bmp.getHeight());
        Log.i("glinfo", "Max texture size = " + getMaxTextureSize());
        MaxTextureSize = getMaxTextureSize();
        if (bmp.getHeight() >= MaxTextureSize) {
            float aspectRatio = (float) bmp.getHeight() / bmp.getWidth();
            float actualheight = MaxTextureSize / 2;
            float actualWidth = actualheight / aspectRatio;
            tv_width.setText(actualWidth + "");
            tv_height.setText(actualheight + "");
        }
        selection();
//        iv_image.setImageBitmap(bmp);
//        tv_size.setText("Size : " + getReadableFileSize(finalFile.length()) + "");
//        Toast.makeText(this, "Actual image size is " + getReadableFileSize(finalFile.length()), Toast.LENGTH_LONG).show();
        findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(compressedImage));
                shareIntent.setType("image/jpeg");
                startActivity(Intent.createChooser(shareIntent, ""));
            }
        });
        findViewById(R.id.doneButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selection();
            }
        });
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                selection();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public static int getMaxTextureSize() {
        // Safe minimum default size
        final int IMAGE_MAX_BITMAP_DIMENSION = 2048;

        // Get EGL Display
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        // Initialise
        int[] version = new int[2];
        egl.eglInitialize(display, version);

        // Query total number of configurations
        int[] totalConfigurations = new int[1];
        egl.eglGetConfigs(display, null, 0, totalConfigurations);

        // Query actual list configurations
        EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
        egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

        int[] textureSize = new int[1];
        int maximumTextureSize = 0;

        // Iterate through all the configurations to located the maximum texture size
        for (int i = 0; i < totalConfigurations[0]; i++) {
            // Only need to check for width since opengl textures are always squared
            egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);

            // Keep track of the maximum texture size
            if (maximumTextureSize < textureSize[0])
                maximumTextureSize = textureSize[0];
        }

        // Release
        egl.eglTerminate(display);

        // Return largest texture size found, or default
        return Math.max(maximumTextureSize, IMAGE_MAX_BITMAP_DIMENSION);
    }

    private void selection() {
        try {
            hideSoftKeyboard();
            int selectedId = rg_sizes.getCheckedRadioButtonId();

            if (selectedId == R.id.rb_inces) {
                width = convertInchesToPixel(Float.parseFloat(tv_width.getText().toString()));
                height = convertInchesToPixel(Float.parseFloat(tv_height.getText().toString()));
            } else if (selectedId == R.id.rb_mm) {
                width = convertMmtoPixel(Float.parseFloat(tv_width.getText().toString()));
                height = convertMmtoPixel(Float.parseFloat(tv_height.getText().toString()));
            } else if (selectedId == R.id.rb_cm) {
                width = convertCMtoPixel(Float.parseFloat(tv_width.getText().toString()));
                height = convertCMtoPixel(Float.parseFloat(tv_height.getText().toString()));
            } else if (selectedId == R.id.rb_pixel) {
                width = Float.parseFloat(tv_width.getText().toString());
                height = Float.parseFloat(tv_height.getText().toString());
            }
            if (MaxTextureSize != 0) {
                if (height - width >= 0 && height>=MaxTextureSize) {
                    Toast.makeText(ImageProcessingActivity.this, "Please correct the dimensions of image ", Toast.LENGTH_LONG).show();
                } else if(width - height >= 0 && width>=MaxTextureSize){
                    Toast.makeText(ImageProcessingActivity.this, "Please correct the dimensions of image ", Toast.LENGTH_LONG).show();
                }
                else{
                    compressedImage = compressToFile(finalFile, width, height, seekbar.getProgress());
                    Log.e("Tag", "Compressed Image path after" + compressedImage);
                    Bitmap bitmap = BitmapFactory.decodeFile(compressedImage.getAbsolutePath());
                    Log.e("Tag", "original bitmap width" + bitmap.getWidth());
                    iv_image.setImageBitmap(bitmap);
                    tv_size.setText("Size : " + getReadableFileSize(compressedImage.length()) + "");
                    Toast.makeText(ImageProcessingActivity.this, "Image is stored at " + compressedImage.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }

            }else {
                compressedImage = compressToFile(finalFile, width, height, seekbar.getProgress());
                Log.e("Tag", "Compressed Image path after" + compressedImage);
                Bitmap bitmap = BitmapFactory.decodeFile(compressedImage.getAbsolutePath());
                Log.e("Tag", "original bitmap width" + bitmap.getWidth());
                iv_image.setImageBitmap(bitmap);
                tv_size.setText("Size : " + getReadableFileSize(compressedImage.length()) + "");
                Toast.makeText(ImageProcessingActivity.this, "Image is stored at " + compressedImage.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }

//            Toast.makeText(this, "Compressed image size is " + getReadableFileSize(compressedImage.length()), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            System.out.println("Tagtesting" + e.getMessage());
            Log.e("tagging", "Tagtesting" + e.getMessage());
            if (e.getMessage().contains("exceeds") || e.getMessage().contains("memory")) {
                Toast.makeText(this, "Entered Image Size is too big to process.", Toast.LENGTH_LONG).show();
            } else
                Toast.makeText(this, "Width or Height can't be zero.", Toast.LENGTH_LONG).show();
        }
    }

    private float convertInchesToPixel(float value) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, value,
                getResources().getDisplayMetrics());
        return px;
    }

    private float convertMmtoPixel(float value) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, value,
                getResources().getDisplayMetrics());
        return px;
    }

    private float convertCMtoPixel(float value) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, value * 10,
                getResources().getDisplayMetrics());
        return px;
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }

    public String getReadableFileSize(long size) {
        if (size <= 0) {
            return "0";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public File compressToFile(File file, float maxWidth, float maxHeight, int quality) {
        Log.e("Tag", "width" + maxWidth);
        Log.e("Tag", "height" + maxHeight);
        Log.e("Tag", "quality" + quality * 10);
        return ImageUtil.compressImage(this, Uri.fromFile(file), maxWidth, maxHeight, compressFormat, bitmapConfig, quality * 10, destinationDirectoryPath);
    }

    public void hideSoftKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

//    private void currentImageProvessing(File file, ImageView iv_image) {
//        try {
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inJustDecodeBounds = true;
//            BitmapFactory.decodeFile(file.getPath(), options);
//            int targetW = iv_image.getWidth() * 2;
//            int targetH = targetW * options.outHeight / options.outWidth;
//            Bitmap captureBmp = ImageUtil.decodeSampledBitmapFromFile(file.getPath(), targetW, targetH);
//            iv_image.setImageBitmap(captureBmp);
//        } catch (Exception e) {
//        }
}
