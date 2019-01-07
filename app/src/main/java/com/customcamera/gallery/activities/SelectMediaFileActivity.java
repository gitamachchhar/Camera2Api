package com.customcamera.gallery.activities;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.customcamera.gallery.R;
import com.customcamera.gallery.adapters.HorizontalMediaGalleryAdapter;
import com.customcamera.gallery.interfaces.MediaSelectionCallback;
import com.customcamera.gallery.interfaces.OnTrimVideoListener;
import com.customcamera.gallery.interfaces.OnVideoListener;
import com.customcamera.gallery.model.MediaData;
import com.customcamera.gallery.model.MediaDataFields;
import com.customcamera.gallery.utils.MediaFileUtils;
import com.customcamera.gallery.widgets.VideoTrimmer;

import java.io.IOException;
import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;

import static com.customcamera.gallery.utils.Constants.ISIMAGEONLY;

public class SelectMediaFileActivity extends ProgressActivity implements View.OnClickListener, MediaSelectionCallback, OnTrimVideoListener, OnVideoListener {

    private static final int MEDIA_VIDEO = 1;
    private static final int MEDIA_PICTURE = 0;

    private AppCompatImageView fullImageView;
    private AppCompatEditText edtCaption;
    private RecyclerView selectedListMedia;
    private LinearLayout rlVideoView;
    private LinearLayout llContainer;

    private int mediaType = MEDIA_PICTURE;
    private int trimCounter = 0;
    private boolean isPlaying = false;
    private boolean isTrimRequired;
    private boolean isTextChanged;
    private String mMediaPath;
    private MediaData mediaData;
    private VideoTrimmer mVideoTrimmer;
    private ArrayList<String> trimFileList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.topsheet);
        initViews();
        setData();
    }

    private void initViews() {

        boolean isImageOnly = getIntent().getBooleanExtra(ISIMAGEONLY, false);

        FloatingActionButton sendMessage = findViewById(R.id.sendMessage);
        AppCompatImageView iv_imageBack = findViewById(R.id.iv_imageBack);
        AppCompatImageView icon_back_camera = findViewById(R.id.icon_back_camera);

        llContainer = findViewById(R.id.llContainer);
        edtCaption = findViewById(R.id.edtCaption);
        fullImageView = findViewById(R.id.fullImageView);
        rlVideoView = findViewById(R.id.rlVideoView);
        LinearLayout bottomCaptionLayout = findViewById(R.id.bottomCaptionLayout);

        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        selectedListMedia = findViewById(R.id.selectedListMedia);
        selectedListMedia.setLayoutManager(manager);

        icon_back_camera.setOnClickListener(this);
        sendMessage.setOnClickListener(this);
        iv_imageBack.setOnClickListener(this);

        if (isImageOnly) {
            bottomCaptionLayout.setVisibility(View.INVISIBLE);
        }

        edtCaption.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                isTextChanged = !edtCaption.getText().toString().isEmpty();
            }
        });
    }

    private void setData() {

        String mode = getIntent().getStringExtra("mode");
        mediaData = Realm.getDefaultInstance().where(MediaData.class).findFirst();

        if (mediaData != null) {
            mMediaPath = mediaData.getFilePath();
            mediaType = mediaData.getFileType();
        }

        if (mode.equalsIgnoreCase("SingleSelect")) {

            if (mediaType == MEDIA_VIDEO) {
                setVideoSettings();
            } else {
                setSelectedMediaInView(MEDIA_PICTURE);
            }

        } else if (mode.equalsIgnoreCase("MultiSelect")) {

            RealmResults<MediaData> mediaDataArrayList = Realm.getDefaultInstance().where(MediaData.class).findAll();
            setSelectedMediaInView(mediaType);

            if (mediaType == MEDIA_VIDEO)
                setVideoSettings();

            ArrayList<String> pathList = new ArrayList<>();

            for (MediaData mData : mediaDataArrayList) {
                pathList.add(mData.getFilePath());
            }

            HorizontalMediaGalleryAdapter mListAdapter = new HorizontalMediaGalleryAdapter(pathList, this, this, true);
            selectedListMedia.setAdapter(mListAdapter);
        }
    }

    private void setVideoSettings() {

        fullImageView.setVisibility(View.GONE);
        llContainer.setVisibility(View.VISIBLE);
        MediaPlayer mp = MediaPlayer.create(this, Uri.parse(mMediaPath));
        int duration = mp.getDuration();
        mp.release();

        llContainer.removeAllViews();
        mVideoTrimmer = new VideoTrimmer(this);
        mVideoTrimmer.setMaxDuration(duration * 1000);
        mVideoTrimmer.setOnTrimVideoListener(this);
        mVideoTrimmer.setOnVideoListenre(this);
        mVideoTrimmer.setMediaData(mediaData);
        mVideoTrimmer.setVideoURI(Uri.parse(mMediaPath));
        mVideoTrimmer.setVideoInformationVisibility(true);
        llContainer.addView(mVideoTrimmer);

    }

    private void resetVideoView() {
        llContainer.setVisibility(View.GONE);
        rlVideoView.setVisibility(View.GONE);
        fullImageView.setVisibility(View.VISIBLE);
        isPlaying = false;
    }

    @Override
    public void onVideoPrepared() {

    }

    @Override
    public void onTrimStarted() {

    }

    @Override
    public void getResult(Uri uri) {

    }

    @Override
    public void cancelAction() {

    }

    @Override
    public void onError(final String message) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SelectMediaFileActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.iv_imageBack:
                finish();
                break;

            case R.id.sendMessage:

                if ((mVideoTrimmer != null && mVideoTrimmer.isUpdateRequired) || isTextChanged) {

                    if (isTextChanged) {

                        final MediaData data = Realm.getDefaultInstance().copyFromRealm(mediaData);
                        data.setFileCaption(edtCaption.getText().toString());

                        Realm.getDefaultInstance().executeTransactionAsync(new Realm.Transaction() {
                            @Override
                            public void execute(@NonNull Realm realm) {
                                realm.insertOrUpdate(data);
                            }
                        }, new Realm.Transaction.OnSuccess() {
                            @Override
                            public void onSuccess() {
                                isTextChanged = false;
                            }
                        });
                    }

                    if (mVideoTrimmer != null && mVideoTrimmer.isUpdateRequired) {
                        mVideoTrimmer.insertMediaInTable();
                    }

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            submitData();
                        }
                    }, 100);

                } else {
                    submitData();
                }
                break;

            case R.id.icon_play:
                rlVideoView.setVisibility(View.VISIBLE);
                fullImageView.setVisibility(View.GONE);
                isPlaying = !isPlaying;
                break;

            case R.id.icon_back_camera:
                finish();
                break;
        }
    }

    private void submitData() {

        RealmResults<MediaData> mediaDataList = Realm.getDefaultInstance().where(MediaData.class).findAll();

        for (MediaData d : mediaDataList) {
            if (d.getFileType() == MEDIA_VIDEO && d.isTrimmed()) {
                trimFileList.add(d.getFilePath());
                isTrimRequired = true;
            }
        }

        if (!isTrimRequired) {
            setResult(102);
            finish();
        } else {
            new VideoTrimmingTask().execute();
        }
    }

    private void setSelectedMediaInView(final int type) {

        fullImageView.setVisibility(View.VISIBLE);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (type == MEDIA_PICTURE) {
                    Glide.with(SelectMediaFileActivity.this)
                            .load(mMediaPath)
                            .apply(new RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE))
                            .into(fullImageView);
                }
            }
        });
    }

    public void previewSelectedGalleryMedia(final String path) {

        try {

            final MediaData data = Realm.getDefaultInstance().copyFromRealm(mediaData);
            data.setFileCaption(edtCaption.getText().toString());
            isTextChanged = false;

            Realm.getDefaultInstance().executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(@NonNull Realm realm) {
                    realm.insertOrUpdate(data);
                }
            }, new Realm.Transaction.OnSuccess() {
                @Override
                public void onSuccess() {
                    edtCaption.setText("");
                    viewImage(path);
                }
            }, new Realm.Transaction.OnError() {
                @Override
                public void onError(@NonNull Throwable error) {
                }
            });

        } catch (Exception e) {
            Log.e("System out", "my issue..." + e.getMessage());
        }
    }

    private void viewImage(String path) {

        mMediaPath = path;
        mediaData = Realm.getDefaultInstance().where(MediaData.class).equalTo(MediaDataFields.FILE_PATH, path).findFirst();

        if (MediaFileUtils.isVideoFile(path)) {
            mediaType = MEDIA_VIDEO;
            setVideoSettings();
        } else {
            mediaType = MEDIA_PICTURE;
            resetVideoView();
            setSelectedMediaInView(mediaType);
        }
        edtCaption.setText(mediaData.getFileCaption());
    }

    @Override
    public void onSelection(String path) {
        previewSelectedGalleryMedia(path);
    }

    @Override
    public void setSelectionButtonVisibility(int size) {

    }

    private class VideoTrimmingTask extends AsyncTask<String, Float, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (trimCounter == 0)
                loadProgressBar(true);
        }

        @Override
        protected Boolean doInBackground(String... paths) {
            try {
                MediaData d = Realm.getDefaultInstance().where(MediaData.class).equalTo(MediaDataFields.FILE_PATH, trimFileList.get(trimCounter)).findFirst();
                if (d != null)
                    MediaFileUtils.genVideoUsingMuxer(d.getFilePath(), d.getOutPutFilePath(), d.getStartTime(), d.getEndTime(), true, true);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Float... percent) {
            super.onProgressUpdate(percent);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            trimCounter++;
            if (trimCounter < trimFileList.size()) {
                new VideoTrimmingTask().execute();
            } else {
                dismissProgressBar();
                setResult(102);
                finish();
            }
        }
    }

}
