package com.customcamera.gallery.activities;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.customcamera.gallery.R;
import com.customcamera.gallery.model.MediaData;
import com.customcamera.gallery.utils.MediaFileUtils;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.util.List;

import static com.customcamera.gallery.utils.Constants.ISGALLERY;
import static com.customcamera.gallery.utils.Constants.ISIMAGEONLY;
import static com.customcamera.gallery.utils.Constants.ISMULTISELECT;
import static com.customcamera.gallery.utils.Constants.MEDIA_VIDEO;
import static com.customcamera.gallery.utils.Constants.REQUEST_IMAGE;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.ClickMe).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askPermissionforCamera();
            }
        });
    }

    private PermissionListener permissionListener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            Intent intent = new Intent(MainActivity.this, CameraGalleryActivity.class);
            intent.putExtra(ISGALLERY, false);
            intent.putExtra(ISIMAGEONLY, false);
            intent.putExtra(ISMULTISELECT, true);
            startActivityForResult(intent, REQUEST_IMAGE);
        }

        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {
            Toast.makeText(MainActivity.this, getResources().getString(R.string.permission_denial_msg), Toast.LENGTH_LONG).show();
        }

    };

    private void askPermissionforCamera() {
        TedPermission.with(this)
                .setPermissionListener(permissionListener)
                .setDeniedMessage(getString(R.string.permission_denial_msg))
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO)
                .check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        for (MediaData mediaData : MediaFileUtils.getMediaList()) {
            if (mediaData != null) {
                if (mediaData.getFileType() == MEDIA_VIDEO) {
                    String filePath = mediaData.getOutPutFilePath();
                    if (!mediaData.isTrimmed())
                        filePath = mediaData.getFilePath();
                    File source = new File(filePath);

                } else {
                    String filePath = mediaData.getOutPutFilePath();
                    if (!mediaData.isTrimmed())
                        filePath = mediaData.getFilePath();
                    File source = new File(filePath);

                }
            }
        }

    }
}
