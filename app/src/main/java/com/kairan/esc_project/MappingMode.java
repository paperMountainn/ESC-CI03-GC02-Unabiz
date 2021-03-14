package com.kairan.esc_project;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.kairan.esc_project.KairanTriangulationAlgo.Point;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MappingMode extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST =1;
    private Uri mImageUri;
    private String URLlink;

    EditText URLEntry;
    SubsamplingScaleImageView PreviewImage;
    Button DeviceUpload, UrlUpload, ConfirmURL,ConfirmImage,ChangeImage;
    FirebaseUser user;
    DatabaseReference database;
    StorageReference storage;

    static String IMAGE_URL = "IMAGE_URL";
    static String IMAGE_DEVICE = "IMAGE_DEVICE";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapping_mode);

        user = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance().getReference("WIFI").child(user.getUid());
        storage = FirebaseStorage.getInstance().getReference("Uploads");

        DeviceUpload = findViewById(R.id.DeviceUpload);
        UrlUpload = findViewById(R.id.UrlUpload);
        PreviewImage = (SubsamplingScaleImageView)findViewById(R.id.PreviewImage);
        ConfirmURL = findViewById(R.id.ConfirmURL);
        URLEntry = findViewById(R.id.UrlEntry);
        ConfirmImage = findViewById(R.id.button_confirm);
        ChangeImage = findViewById(R.id.button_changeImage);

        URLEntry.setVisibility(View.GONE);
        ConfirmURL.setVisibility(View.GONE);
        ConfirmImage.setVisibility(View.GONE);
        ChangeImage.setVisibility(View.GONE);

        PreviewImage.setOnTouchListener(new View.OnTouchListener() {
            GestureDetector gestureDetector = new GestureDetector(getApplicationContext(),new GestureDetector.SimpleOnGestureListener(){
                @Override
                public void onLongPress(MotionEvent e) {
                    float x = e.getX();
                    float y = e.getY();
                    Log.i("MAPPOSITION",PreviewImage.viewToSourceCoord(x,y).toString());
                    super.onLongPress(e);
                }
            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false;
            }
        });

        DeviceUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChoser();
                DeviceUpload.setVisibility(View.GONE);
                UrlUpload.setVisibility(View.GONE);
                URLEntry.setVisibility(View.GONE);
                ConfirmURL.setVisibility(View.GONE);
                ConfirmImage.setVisibility(View.VISIBLE);
                ChangeImage.setVisibility(View.VISIBLE);
            }
        });

        UrlUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UrlUpload.setVisibility(View.GONE);
                URLEntry.setVisibility(View.VISIBLE);
                ConfirmURL.setVisibility(View.VISIBLE);
            }
        });

        ConfirmURL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                URLlink = URLEntry.getText().toString();
                if (URLlink.isEmpty()){
                    Toast.makeText(MappingMode.this, "Please Enter An URL", Toast.LENGTH_SHORT).show();
                }
                else{
                    //LoadImage loadImage = new LoadImage(PreviewImage);
                    //loadImage.execute(URLlink);
                    ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(MappingMode.this).build();
                    ImageLoader imageLoader = ImageLoader.getInstance();
                    imageLoader.init(config);
                    imageLoader.loadImage(URLlink,new SimpleImageLoadingListener(){
                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            //super.onLoadingComplete(imageUri, view, loadedImage);
                            PreviewImage.setImage(ImageSource.bitmap(loadedImage));
                        }
                    });
                    //PreviewImage.setImage(ImageSource.uri(URLlink));
                    DeviceUpload.setVisibility(View.GONE);
                    UrlUpload.setVisibility(View.GONE);
                    URLEntry.setVisibility(View.GONE);
                    ConfirmURL.setVisibility(View.GONE);
                    ConfirmImage.setVisibility(View.VISIBLE);
                    ChangeImage.setVisibility(View.VISIBLE);
                }
            }
        });

        ChangeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeviceUpload.setVisibility(View.VISIBLE);
                UrlUpload.setVisibility(View.VISIBLE);
                URLEntry.setVisibility(View.GONE);
                ConfirmURL.setVisibility(View.GONE);
                ConfirmImage.setVisibility(View.GONE);
                ChangeImage.setVisibility(View.GONE);
            }
        });
    }

    private void openFileChoser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private class LoadImage extends AsyncTask<String, Void, Bitmap> {
        SubsamplingScaleImageView imageView;
        URL url;
        public LoadImage(SubsamplingScaleImageView PreviewImage){
            this.imageView = PreviewImage;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            String URLlink = strings[0];
            Bitmap bitmap = null;
            try {
                if(!URLlink.isEmpty()){
                    url = new URL(URLlink);
                }
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                //InputStream inputStream = new java.net.URL(URLlink).openStream();
                InputStream inputStream = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            //PreviewImage.setImageBitmap(bitmap);
            PreviewImage.setImage(ImageSource.bitmap(bitmap));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            mImageUri = data.getData();
            PreviewImage.setImage(ImageSource.uri(mImageUri));
            //Picasso.with(this).load(mImageUri).into(PreviewImage);






        }
    }
}
