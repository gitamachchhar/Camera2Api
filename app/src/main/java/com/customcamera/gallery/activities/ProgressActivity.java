package com.customcamera.gallery.activities;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;

import com.customcamera.gallery.R;


public class ProgressActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;

    public void loadProgressBar(boolean cancellable) {
        if (progressDialog == null && !isFinishing()) {
            progressDialog = new ProgressDialog(this, R.style.MyTheme);
            progressDialog.setCancelable(cancellable);
            progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
            progressDialog.show();
        }
    }

    public void dismissProgressBar() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
    }

}
