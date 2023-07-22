package com.linpack.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.linpack.myapplication.databinding.ActivityMainBinding;
import com.permissionx.guolindev.PermissionX;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_VIDEO_PICK = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;

    private static final int REQUEST_AUDIO_PICK = 4;
    private ActivityMainBinding mainBinding = null;
    private Uri selectedFileUri = null;
    private MediaController mediaController;
    private String selectedFileUriName = "";
    private MediaPlayer mediaPlayer;
    ActivityResultLauncher<Intent> mainActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // There are no request codes
                    Intent data = result.getData();
                    if (data != null) {
                        doSomeOperations(data);
                    } else {
                        System.out.println("data is null");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        mediaController = new MediaController(this);
        mainBinding.selectedVideo.setMediaController(mediaController);
        mainBinding.selectedVideo.requestFocus();
        mainBinding.actionMediaPicker.setOnClickListener(v -> {
            showImagePickerDialog();
        });
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Media Source");
        builder.setItems(new CharSequence[]{"Camera", "Gallery","Audio","PDF"}, (dialog, which) -> {
            if (which == 0) {
                openCamera();
            } else if (which == 1) {
                openGallery();
            }else if (which == 2) {
                selectAudio();
            }
            else if (which == 3) {
                selectPdf();
            }
        });
        builder.show();
    }

    private void selectAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, REQUEST_AUDIO_PICK);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    private void openGallery() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose File Type");
        builder.setItems(new CharSequence[]{"Image", "Video"}, (dialog, which) -> {
            if (which == 0) {
                // Camera option clicked
                selectImage();
            } else if (which == 1) {
                // Gallery option clicked
                selectVideo();
            }
        });
        builder.show();
    }

    private void selectImage() {
        mainBinding.selectedImage.setImageBitmap(null);
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void selectVideo() {
        mainBinding.selectedVideo.setVideoURI(null);
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_VIDEO_PICK);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Image captured from camera
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        mainBinding.selectedImage.setImageBitmap(imageBitmap);
                        mainBinding.selectedImage.setVisibility(View.VISIBLE);
                    }
                }
            } else if (requestCode == REQUEST_IMAGE_PICK) {
                Uri imageUri = data.getData();
                mainBinding.selectedImage.setImageURI(imageUri);
                mainBinding.selectedImage.setVisibility(View.VISIBLE);
            } else if (requestCode == REQUEST_VIDEO_PICK) {
                Uri videoUri = data.getData();
                mainBinding.selectedVideo.setVideoURI(videoUri);
                mainBinding.selectedVideo.setVisibility(View.VISIBLE);
                mainBinding.selectedVideo.start();
            }
            else if (requestCode == REQUEST_AUDIO_PICK) {
                Uri audioUri = data.getData();
                if (audioUri != null) {
                    playAudioFromUri(audioUri);
                }
            }
        }
    }

    private void playAudioFromUri(Uri audioUri) {
        try {
            // Set the data source for the media player
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
            mediaPlayer.setDataSource(getApplicationContext(), audioUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void openPdfFile(Uri uri) {
        Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
        pdfIntent.setDataAndType(uri, "application/pdf");
        pdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(pdfIntent);
        } catch (ActivityNotFoundException e) {
            // Handle the case where a PDF viewer application is not available on the device.
            // You can show a dialog or a message to the user to install a PDF viewer app.
            e.printStackTrace();
        }
    }

    private void selectPdf() {
        Intent pdfIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pdfIntent.setType("application/pdf");
        pdfIntent.addCategory(Intent.CATEGORY_OPENABLE);
        mainActivityResultLauncher.launch(pdfIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        AskPermissions();
    }

    private void AskPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extracted(
                    Manifest.permission.CAMERA,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            extracted(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_MEDIA_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            );
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            extracted(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_MEDIA_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            );
        } else {
            extracted(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void extracted(String... permissions) {
        PermissionX.init(MainActivity.this).permissions(
                        permissions
                )
                .onExplainRequestReason((scope, deniedList, beforeRequest) -> {
                    String message = "PermissionX needs following permissions to continue";
                    scope.showRequestReasonDialog(deniedList, message, "Allow", "Deny");
                })
                .onForwardToSettings((scope, deniedList) -> {
                    String message = "Please allow following permissions in settings";
                    scope.showForwardToSettingsDialog(deniedList, message, "Allow", "Deny");
                }).request((allGranted, grantedList, deniedList) -> {
                    if (allGranted) {
                        Toast.makeText(MainActivity.this, "All permissions are granted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "The following permissions are denied " + deniedList, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @SuppressLint("SetTextI18n")
    private void doSomeOperations(Intent data) {
        Uri uri = data.getData();
        selectedFileUri = uri;
        String fileName = getFileName(getApplicationContext(), uri);
        selectedFileUriName = fileName;
        mainBinding.selectedFile.setText(fileName + "-" + uri.getPath());

        Log.d(TAG, "doSomeOperations: " + fileName);
    }

    public String getFileName(Context context, Uri uri) {
        if (uri != null && context != null) {
            if (uri.toString().startsWith("content://")) {
                Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
                if (returnCursor != null) {
                    int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                    returnCursor.moveToFirst();
                    if (nameIndex >= 0 && sizeIndex >= 0) {
                        Boolean isValidFile = checkOtherFileType(returnCursor.getString(nameIndex));
                        if (isValidFile) {
                            return returnCursor.getString(nameIndex);
                        }
                    }
                    returnCursor.close();
                }
            } else if (uri.toString().startsWith("file://")) {
                return new File(uri.getPath()).getName();
            } else {
                new File(uri.getPath()).getName();
            }
        }
        return "";
    }

    private Boolean checkOtherFileType(String filePath) {
        if (!filePath.isEmpty()) {
            String filePathInLowerCase = filePath.toLowerCase();
            return filePathInLowerCase.endsWith(".pdf");
        }
        return false;
    }
}