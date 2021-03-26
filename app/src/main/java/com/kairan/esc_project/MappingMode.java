package com.kairan.esc_project;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kairan.esc_project.KairanTriangulationAlgo.Point;
import com.kairan.esc_project.UIStuff.CustomView;
import com.kairan.esc_project.mappingModeDisplay.StorageChoser;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MappingMode extends AppCompatActivity {
    private Uri mImageUri;
    private String URLlink = null;

    EditText URLEntry;
    SubsamplingScaleImageView PreviewImage;
    //CustomView PreviewImage;
    Button DeviceUpload, UrlUpload, ConfirmURL,ConfirmImage,ChangeImage, FirebaseUpload;
    FirebaseUser user;
    DatabaseReference database;
    StorageReference storage;
    TextView TextViewInvalidPhoto;

    static String IMAGE_URL = "IMAGE_URL";
    static String IMAGE_DEVICE = "IMAGE_DEVICE";
    String invalidPhotoText;

    int StorageUploadCount =0;

    Bitmap bitmap1 = null;
    InputStream inputStream = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapping_mode);

        user = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());
        storage = FirebaseStorage.getInstance().getReference(user.getUid());


        DeviceUpload = findViewById(R.id.DeviceUpload);
        UrlUpload = findViewById(R.id.UrlUpload);
        FirebaseUpload = findViewById(R.id.FirebaseUpload);
        PreviewImage = findViewById(R.id.PreviewImage);
        ConfirmURL = findViewById(R.id.ConfirmURL);
        URLEntry = findViewById(R.id.UrlEntry);
        ConfirmImage = findViewById(R.id.button_confirm);
        ChangeImage = findViewById(R.id.button_changeImage);
        TextViewInvalidPhoto = findViewById(R.id.tvInvalidPhoto);
        invalidPhotoText = "Please Select a Photo";

        URLEntry.setVisibility(View.GONE);
        ConfirmURL.setVisibility(View.GONE);
        ConfirmImage.setVisibility(View.GONE);
        ChangeImage.setVisibility(View.GONE);

        /*PreviewImage.setOnTouchListener(new View.OnTouchListener() {
            GestureDetector gestureDetector = new GestureDetector(getApplicationContext(),new GestureDetector.SimpleOnGestureListener(){
                @Override
                public void onLongPress(MotionEvent e) {
                    float x = e.getX();
                    float y = e.getY();
                    // Send the data to the database
                    database.child("Scan").child("Scan Location").child("X").setValue(x);
                    database.child("Scan").child("Scan Location").child("Y").setValue(y);
                    Log.i("MAPPOSITION",PreviewImage.viewToSourceCoord(x,y).toString());
                    super.onLongPress(e);
                }
            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false;
            }
        });*/

       /**
        Either Upload a Map from device/URL or choose a Map from the existing firebase storage
         */
        DeviceUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChoser();
                DeviceUpload.setVisibility(View.GONE);
                UrlUpload.setVisibility(View.GONE);
                FirebaseUpload.setVisibility(View.GONE);
                ConfirmImage.setVisibility(View.VISIBLE);
                ChangeImage.setVisibility(View.VISIBLE);
            }
        });

        UrlUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeviceUpload.setVisibility(View.GONE);
                FirebaseUpload.setVisibility(View.GONE);
                UrlUpload.setVisibility(View.GONE);
                URLEntry.setVisibility(View.VISIBLE);
                ConfirmURL.setVisibility(View.VISIBLE);
            }
        });

        FirebaseUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MappingMode.this, StorageChoser.class);
                startActivity(intent);
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
                            PreviewImage.setImage(ImageSource.bitmap(loadedImage));  // subscaling
                            //PreviewImage.setImageBitmap(loadedImage);
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

        /**
         Change current image after uploading
         */
        ChangeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeviceUpload.setVisibility(View.VISIBLE);
                UrlUpload.setVisibility(View.VISIBLE);
                FirebaseUpload.setVisibility(View.VISIBLE);
                URLEntry.setVisibility(View.GONE);
                ConfirmURL.setVisibility(View.GONE);
                ConfirmImage.setVisibility(View.GONE);
                ChangeImage.setVisibility(View.GONE);
            }
        });

        /**
         Confirm the selected image: upload the image to firebase and go to mapping activity
         */
        ConfirmImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if the image that wants to be sent is from local device
                if (mImageUri != null){
                    StorageReference storage1 = storage.child(mImageUri.getPath());
                     storage1.putFile(mImageUri);

                    storage1.putFile(mImageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }

                            // Continue with the task to get the download URL
                            return storage1.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                URLlink = task.getResult().toString();
                                Intent intent = new Intent(MappingMode.this,MappingActivity.class);
                                intent.putExtra("Imageselected", URLlink);
                                startActivity(intent);
                            } else {
                            }
                        }
                    });
                } else if (URLlink != null){
                    database.child("DownloadURLs").push().addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            int numberofChild = (int)snapshot.getChildrenCount();
                            database.child("DownloadURLs").child(Integer.toString(numberofChild+1)).setValue(URLlink);
                            Intent intent = new Intent(MappingMode.this,MappingActivity.class);
                            intent.putExtra("Imageselected", URLlink);
                            startActivity(intent);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }

                // if the image that needs to be sent to the firebase is from URL
                else {
                    Toast.makeText(MappingMode.this,"Authentication Failed",Toast.LENGTH_LONG).show();    // display an error message
                    TextViewInvalidPhoto.setText(invalidPhotoText);
                }


//                intent.putExtra("Imageselected", URLlink);
//                if (mImageUri != null){
////                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
////                bitmap1.compress(Bitmap.CompressFormat.PNG, 100, stream);
////                byte[] bytes = stream.toByteArray();
////                intent.putExtra("LocalDevice", bytes);
//                }
//                else {




//                intent.putExtra(IMAGE_URL,URLlink);
//                PreviewImage.buildDrawingCache();
//                Bitmap bitmap_device = PreviewImage.getDrawingCache();
//                ByteArrayOutputStream bs = new ByteArrayOutputStream();
//                bitmap_device.compress(Bitmap.CompressFormat.PNG,50,bs);
//                intent.putExtra(IMAGE_DEVICE,bs.toByteArray());


            }
        });
    }

    private void openFileChoser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 1);
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
                inputStream = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            //PreviewImage.setImageBitmap(bitmap);
            PreviewImage.setImage(ImageSource.bitmap(bitmap));  //subscaling
            //PreviewImage.setImageBitmap(bitmap);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null){
            mImageUri = data.getData();
            Log.i("Testing", mImageUri.getPath());
            PreviewImage.setImage(ImageSource.uri(mImageUri));   //subscaling
            //PreviewImage.setImageURI(mImageUri);
            try {
                bitmap1 = MediaStore.Images.Media.getBitmap(getContentResolver(), mImageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Picasso.with(this).load(mImageUri).into(PreviewImage);
        }
    }
}

