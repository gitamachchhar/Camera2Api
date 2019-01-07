
package com.customcamera.gallery.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.customcamera.gallery.R;
import com.customcamera.gallery.adapters.GalleryAdapter;
import com.customcamera.gallery.adapters.HorizontalMediaGalleryAdapter;
import com.customcamera.gallery.interfaces.MediaSelectionCallback;
import com.customcamera.gallery.model.MediaData;
import com.customcamera.gallery.utils.MediaFileUtils;
import com.customcamera.gallery.widgets.AutoFitTextureView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmList;

import static com.customcamera.gallery.utils.Constants.ISGALLERY;
import static com.customcamera.gallery.utils.Constants.ISIMAGEONLY;
import static com.customcamera.gallery.utils.Constants.ISMULTISELECT;
import static com.customcamera.gallery.utils.Constants.MEDIA_PICTURE;
import static com.customcamera.gallery.utils.Constants.MEDIA_VIDEO;
import static com.customcamera.gallery.utils.MediaFileUtils.formatTimer;


public class CameraGalleryActivity extends ProgressActivity
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback, View.OnLongClickListener, MediaSelectionCallback {

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int FRONT_CAMERA = 1;
    private static final int BACK_CAMERA = 0;
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;

    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private int cameraFace = BACK_CAMERA;
    private int mediaType = MEDIA_PICTURE;
    private int mState = STATE_PREVIEW;
    private int mWidth;
    private int mHeight;
    private int mSensorOrientation;
    private int pickHeight = 150;
    private int recordCounter;
    private boolean mIsRecordingVideo;
    private boolean mFlashSupported;

    private String mVideoAbsolutePath;
    private String mCameraId;

    private AutoFitTextureView mTextureView;
    private AppCompatTextView tvTimer;
    private AppCompatTextView fileCounter;
    private AppCompatTextView mTvMediacounter;
    private BottomSheetBehavior mBottomSheetBehavior;
    private LinearLayoutCompat bottomSheet;
    private AppCompatImageView expand_icon;
    private AppCompatImageView openLargeGallery;
    private RelativeLayout sendSelected;
    private AppCompatImageView mIvCaptureMedia;
    private AppCompatImageView mIvCameraface;
    private LinearLayout bottomCaptionLayout;
    private LinearLayout topCaptionLayout2;
    private LinearLayout topCaptionLayout1;
    private RecyclerView mRvListMedia;

    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ImageReader mImageReader;
    private File mImageFileAbsolutePath;
    private Size mVideoSize;
    private MediaRecorder mMediaRecorder;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private GestureDetectorCompat mDetector;
    private GalleryAdapter adapter;
    private HorizontalMediaGalleryAdapter mListAdapter;
    private Runnable updater;
    private boolean isGallery;
    private boolean isImageOnly;
    private boolean isMultiSelect;

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {

            mHeight = height;
            mWidth = width;

            if (mediaType == MEDIA_PICTURE) {
                openCamera(width, height, cameraFace);
            } else {
                openCameraForVideo(width, height, cameraFace);
            }

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {

            mCameraDevice = cameraDevice;

            if (mediaType == MEDIA_PICTURE) {
                mCameraOpenCloseLock.release();
                createCameraPreviewSession();

            } else {
//                startPreview();
                mCameraOpenCloseLock.release();

                if (null != mTextureView) {
                    configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
                }

                if (mIsRecordingVideo)
                    startRecordingVideo();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            finish();
        }

    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {

            switch (mState) {
                case STATE_PREVIEW: {
                    break;
                }
                case STATE_WAITING_LOCK: {

                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);

                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);

        }
    };

    private void showToast(final String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_gallery_activity);

        init();

        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        mBottomSheetBehavior.setPeekHeight(pickHeight);
                        expand_icon.animate().rotation(0).setDuration(500).start();
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        expand_icon.animate().rotation(180).setDuration(500).start();
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        if (isGallery) {
            expand_icon.setVisibility(View.INVISIBLE);
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) bottomSheet.getLayoutParams();
            params.setBehavior(null);

        }

        getImageFilePath();

        ArrayList<String> mediaFileList;
        com.customcamera.gallery.utils.MediaFileUtils.clearFileList();

        if (isImageOnly) {
            mediaFileList = MediaFileUtils.getImageFile(Environment.getExternalStorageDirectory());
        } else {
            mediaFileList = MediaFileUtils.getFile(Environment.getExternalStorageDirectory());
        }

        if (mediaFileList != null) {

            TreeSet<String> t1 = new TreeSet<>(new MediaFileUtils.FileTimeComparator());

            for (String path : mediaFileList) {
                t1.add(path);
            }

            Collections.reverse(mediaFileList);

            setupHorizontalGallary(mediaFileList);
            setupGridView(mediaFileList);
        }

        mIvCaptureMedia.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (mIsRecordingVideo) {
                        mIsRecordingVideo = false;
                        tvTimer.setVisibility(View.GONE);
                        stopRecordingVideo();
                    } else {
                        if (adapter != null)
                            adapter.clearFileList();

                        if (mListAdapter != null)
                            mListAdapter.clearFileList();

                        setSelectedSendButtonVisibility(0);
                        mIvCaptureMedia.setImageResource(R.drawable.capture_button_active);
                        takePicture();
                    }
                }

                return false;
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {

        isGallery = getIntent().getBooleanExtra(ISGALLERY, false);
        isImageOnly = getIntent().getBooleanExtra(ISIMAGEONLY, false);
        isMultiSelect = getIntent().getBooleanExtra(ISMULTISELECT, false);

        mIvCaptureMedia = findViewById(R.id.captureMedia);
        mIvCameraface = findViewById(R.id.cameraFace);
        openLargeGallery = findViewById(R.id.openLargeGallery);
        expand_icon = findViewById(R.id.expand_icon);
        mRvListMedia = findViewById(R.id.listMedia);
        tvTimer = findViewById(R.id.tvTimer);
        mTextureView = findViewById(R.id.texture);
        bottomCaptionLayout = findViewById(R.id.bottomCaptionLayout);
        CoordinatorLayout mClMain = findViewById(R.id.cl_main);

        AppCompatImageView icon_back = findViewById(R.id.icon_back);
        AppCompatTextView submitMedia = findViewById(R.id.submitMedia);
        topCaptionLayout2 = findViewById(R.id.topCaptionLayout2);
        topCaptionLayout1 = findViewById(R.id.topCaptionLayout1);
        fileCounter = findViewById(R.id.fileCounter);
        sendSelected = findViewById(R.id.sendSelected);
        mTvMediacounter = findViewById(R.id.media_counter);

        pickHeight = MediaFileUtils.convertDpToPixel(getResources().getDimension(R.dimen.bottom_sheet_height), this);
        pickHeight = 0;

        bottomSheet = findViewById(R.id.bottomSheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setPeekHeight(pickHeight);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        findViewById(R.id.ic_back).setOnClickListener(this);
        sendSelected.setOnClickListener(this);
        mIvCaptureMedia.setOnLongClickListener(this);
        mIvCameraface.setOnClickListener(this);
        expand_icon.setOnClickListener(this);

        icon_back.setOnClickListener(this);
        submitMedia.setOnClickListener(this);

        mDetector = new GestureDetectorCompat(this, new GalleryGestureListener());

        mClMain.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mDetector.onTouchEvent(motionEvent);
                return true;
            }
        });

    }

    class GalleryGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {

            if (event1.getY() > event2.getY()) {
                mBottomSheetBehavior.setState(mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED);
            }
            return true;
        }
    }


    public void setSelectedSendButtonVisibility(int isVisible) {
        if (isVisible > 0) {
            sendSelected.setVisibility(View.VISIBLE);
        } else {
            sendSelected.setVisibility(View.GONE);
        }
    }

    private void getImageFilePath() {
        String mFileName = "IMG_" + Calendar.getInstance().getTimeInMillis() + ".jpg";
        mImageFileAbsolutePath = new File(getExternalFilesDir(null), mFileName);
    }

    private void setCaptionVisibility(final boolean isVisible) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {

                    if (isVisible) {
                        bottomCaptionLayout.setVisibility(View.VISIBLE);
                        openLargeGallery.setVisibility(View.GONE);
                        mRvListMedia.setVisibility(View.GONE);
                        mIvCaptureMedia.setVisibility(View.GONE);
                        mIvCameraface.setVisibility(View.GONE);

                    } else {
                        bottomCaptionLayout.setVisibility(View.GONE);
                        openLargeGallery.setVisibility(View.VISIBLE);
                        mRvListMedia.setVisibility(View.VISIBLE);
                        mIvCaptureMedia.setVisibility(View.VISIBLE);
                        mIvCameraface.setVisibility(View.VISIBLE);

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void updateActionBar(int selectedFiles) {

        if (selectedFiles > 0) {
            topCaptionLayout1.setVisibility(View.GONE);
            topCaptionLayout2.setVisibility(View.VISIBLE);
            fileCounter.setText(selectedFiles + " File(s) Selected");
        } else {
            fileCounter.setText("");
            topCaptionLayout1.setVisibility(View.VISIBLE);
            topCaptionLayout2.setVisibility(View.GONE);
        }

    }

    private void setupHorizontalGallary(ArrayList<String> mediaFileList) {
        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRvListMedia.setLayoutManager(manager);
        mListAdapter = new HorizontalMediaGalleryAdapter(mediaFileList, this, this, isMultiSelect);
        mRvListMedia.setAdapter(mListAdapter);
    }

    private void setupGridView(ArrayList<String> mediaFileList) {
        adapter = new GalleryAdapter(mediaFileList, this, isMultiSelect);
        GridView fullscreenGallery = findViewById(R.id.fullscreenGallery);
        fullscreenGallery.setAdapter(adapter);
        fullscreenGallery.setNestedScrollingEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        mediaType = MEDIA_PICTURE;
        mState = STATE_PREVIEW;

        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight(), cameraFace);
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        mIvCaptureMedia.setImageResource(R.drawable.capture_button);
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.request_permission), Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage(), mImageFileAbsolutePath));
        }

    };

    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height, int cameraFace) {
        Activity activity = this;
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            assert manager != null;

            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null) {
                    if (facing == CameraCharacteristics.LENS_FACING_FRONT && cameraFace == BACK_CAMERA) {
                        continue;
                    } else if (facing == CameraCharacteristics.LENS_FACING_BACK && cameraFace == FRONT_CAMERA) {
                        continue;
                    }
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                Size largest = getFullScreenPreview(map.getOutputSizes(ImageFormat.JPEG), width, height);

                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/1);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);

                mPreviewSize = getFullScreenPreview(map.getOutputSizes(SurfaceTexture.class),
                        width, height);

                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;
                mCameraId = cameraId;
                return;
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            Toast.makeText(this, getString(R.string.camera_error), Toast.LENGTH_LONG).show();
        }
    }

    private Size getFullScreenPreview(Size[] outputSizes, int width, int height) {

        List<Size> outputSizeList = Arrays.asList(outputSizes);
        Size fullScreenSize = outputSizeList.get(0);

        for (int i = 0; i < outputSizeList.size(); i++) {

            int originalWidth = outputSizeList.get(i).getWidth();
            int originalHeight = outputSizeList.get(i).getHeight();

            float originalRatio = (float) originalWidth / (float) originalHeight;
            float requiredRatio;

            if (width > height) {
                requiredRatio = ((float) width / height);
                if ((outputSizeList.get(i).getWidth() > width && outputSizeList.get(i).getHeight() > height)) {
                    continue;
                }
            } else {
                requiredRatio = 1 / ((float) width / height);
                if ((outputSizeList.get(i).getWidth() > height && outputSizeList.get(i).getHeight() > width)) {
                    continue;
                }
            }
            if (originalRatio == requiredRatio) {
                fullScreenSize = outputSizeList.get(i);
                break;
            }
        }

        if (fullScreenSize.getWidth() > 3000) {
            return new Size(1794, 1080);
        } else {
            return fullScreenSize;
        }
    }

    private void openCamera(int width, int height, int cameraFace) {

        setUpCameraOutputs(width, height, cameraFace);
        configureTransform(width, height);
        Activity activity = this;
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (manager != null) {
                manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void openCameraForVideo(int width, int height, int cameraFace) {

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String cameraId = "";

        try {
            if (!mCameraOpenCloseLock.tryAcquire(5000, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            if (manager != null) {
                for (String camId : manager.getCameraIdList()) {
                    CameraCharacteristics characteristics
                            = manager.getCameraCharacteristics(camId);

                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null) {
                        if (facing == CameraCharacteristics.LENS_FACING_FRONT && cameraFace == BACK_CAMERA) {
                            continue;
                        } else if (facing == CameraCharacteristics.LENS_FACING_BACK && cameraFace == FRONT_CAMERA) {
                            continue;
                        }
                    }
                    cameraId = camId;
                }
            }

            CameraCharacteristics characteristics = null;

            if (manager != null) {
                characteristics = manager.getCameraCharacteristics(cameraId);
            }

            StreamConfigurationMap map = null;

            if (characteristics != null) {
                map = characteristics
                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            }

            //noinspection ConstantConditions
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }

            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));

            mPreviewSize = getFullScreenPreview(map.getOutputSizes(SurfaceTexture.class),
                    width, height);

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, mStateCallback, null);

        } catch (CameraAccessException e) {
            Toast.makeText(this, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NullPointerException e) {
            Toast.makeText(this, getString(R.string.camera_error), Toast.LENGTH_SHORT).show();

        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void setUpMediaRecorder() throws IOException {

        mMediaRecorder = new MediaRecorder();

        if (mVideoAbsolutePath == null || mVideoAbsolutePath.isEmpty()) {
            mVideoAbsolutePath = getVideoFilePath(this);
        }

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(1600 * 1000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
    }

    private String getVideoFilePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + System.currentTimeMillis() + ".mp4";
    }


    private void startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            setUpMediaRecorder();

            try {
                mMediaRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            SurfaceTexture mSTexture = mTextureView.getSurfaceTexture();
            assert mSTexture != null;
            mSTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            Surface previewSurface = new Surface(mSTexture);
            surfaces.add(previewSurface);
            mPreviewRequestBuilder.addTarget(previewSurface);

            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewRequestBuilder.addTarget(recorderSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCaptureSession = cameraCaptureSession;
                    updatePreview();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIsRecordingVideo = true;
                            mMediaRecorder.start();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(CameraGalleryActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                }
            }, mBackgroundHandler);

        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecordingVideo() {

        try {

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();

            mIsRecordingVideo = false;
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            sendData(mVideoAbsolutePath, MEDIA_VIDEO);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != 0) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void sendData(final String path, final int mediaType) {

        Realm.getDefaultInstance().executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(@NonNull Realm realm) {
                realm.where(MediaData.class).findAll().deleteAllFromRealm();
                MediaData mData = new MediaData();
                mData.setFilePath(path);
                mData.setOutPutFilePath(Environment.getExternalStorageDirectory() + File.separator + new File(path).getName());
                mData.setFileType(mediaType);
                mData.setTrimmed(false);
                mData.setStartTime(0);
                realm.insertOrUpdate(mData);
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                Intent i = new Intent(CameraGalleryActivity.this, SelectMediaFileActivity.class);
                i.putExtra("mode", "SingleSelect");
                i.putExtra(ISIMAGEONLY, isImageOnly);
                startActivityForResult(i, 102);
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(@NonNull Throwable error) {
                Log.e("System out", "On Error " + error.getMessage());
            }
        });
    }

    private void sendDataList(final RealmList<MediaData> path) {

        Realm.getDefaultInstance().executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(@NonNull Realm realm) {
                realm.where(MediaData.class).findAll().deleteAllFromRealm();
                realm.copyToRealm(path);
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                Intent i = new Intent(CameraGalleryActivity.this, SelectMediaFileActivity.class);
                i.putExtra("mode", "MultiSelect");
                i.putExtra(ISIMAGEONLY, isImageOnly);
                startActivityForResult(i, 102);
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(@NonNull Throwable error) {
                Log.e("System out", "error..." + error.getMessage());
            }
        });

    }

    public void previewSelectedGalleryMedia(String path) {

        if (MediaFileUtils.isVideoFile(path)) {
            mediaType = MEDIA_VIDEO;
        } else {
            mediaType = MEDIA_PICTURE;
        }
        sendData(path, mediaType);
    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewRequestBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    private void closePreviewSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        return choices[choices.length - 1];
    }

    private void closeCamera() {

        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {

        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {

                            if (null == mCameraDevice) {
                                return;
                            }

                            mCaptureSession = cameraCaptureSession;
                            try {
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_AUTO);
                                setAutoFlash(mPreviewRequestBuilder);
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");

                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void configureTransform(int viewWidth, int viewHeight) {

        if (null == mTextureView || null == mPreviewSize) {
            return;
        }

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void takePicture() {
        lockFocus();
    }

    private void lockFocus() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            mState = STATE_WAITING_LOCK;

            if (cameraFace == BACK_CAMERA) {
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                        mBackgroundHandler);
            } else if (cameraFace == FRONT_CAMERA) {
                captureStillPicture();
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecaptureSequence() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillPicture() {

        try {

            if (null == mCameraDevice) {
                return;
            }

            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO);
            setAutoFlash(captureBuilder);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {

                    setCaptionVisibility(true);
                    unlockFocus();
                    sendData(mImageFileAbsolutePath.getAbsolutePath(), MEDIA_PICTURE);
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    showToast("Saved: capture failed" + failure.getReason());
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int getOrientation(int rotation) {
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    private void unlockFocus() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onLongClick(View view) {

        switch (view.getId()) {
            case R.id.captureMedia:

                if (!isImageOnly) {

                    if (adapter != null)
                        adapter.clearFileList();

                    if (mListAdapter != null)
                        mListAdapter.clearFileList();

                    setSelectedSendButtonVisibility(0);
                    mIvCaptureMedia.setImageResource(R.drawable.capture_button_active);

                    mIsRecordingVideo = true;
                    mediaType = MEDIA_VIDEO;
                    closeCamera();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            openCameraForVideo(mWidth, mHeight, cameraFace);
                            updateVideoRecordingTimer();
                        }
                    }, 200);


                    recordCounter = 0;

                }
                break;
        }
        return true;
    }

    void updateVideoRecordingTimer() {

        tvTimer.setVisibility(View.VISIBLE);

        final Handler timerHandler = new Handler();
        updater = new Runnable() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvTimer.setText(formatTimer(recordCounter));
                    }
                });
                if (mIsRecordingVideo)
                    timerHandler.postDelayed(updater, 1000);

                recordCounter++;
            }
        };
        timerHandler.post(updater);

    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.cameraFace:

                if (cameraFace == 1) {
                    cameraFace = 0;
                } else if (cameraFace == 0) {
                    cameraFace = 1;
                }
                closeCamera();
                mIsRecordingVideo = false;
                mediaType = MEDIA_PICTURE;
                openCamera(mWidth, mHeight, cameraFace);
                break;

            case R.id.expand_icon:
                mBottomSheetBehavior.setState(mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED);
                break;

            case R.id.ic_back:
                onBackPressed();
                break;

            case R.id.icon_back:
                updateActionBar(0);
                if (adapter != null)
                    adapter.clearFileList();
                break;

            case R.id.submitMedia:
                sendDataToNextScreen(adapter.getSelectedFileList());
                break;
            case R.id.sendSelected:
                sendDataToNextScreen(mListAdapter.getSelectedFilesList());
                break;
        }
    }

    @Override
    public void onBackPressed() {

        if (isGallery) {
            if (adapter != null && adapter.getSelectedFileList().size() > 0) {
                adapter.clearFileList();
                updateActionBar(0);
            } else {
                finish();
            }
        } else if (mListAdapter != null && mListAdapter.getSelectedFilesList().size() > 0) {
            mListAdapter.clearFileList();
            setSelectionButtonVisibility(0);
        } else if (adapter != null && adapter.getSelectedFileList().size() > 0) {
            adapter.clearFileList();
            updateActionBar(0);
        } else if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }

    }

    private void sendDataToNextScreen(ArrayList<String> mFileList) {

        RealmList<MediaData> mediaList = new RealmList<>();

        for (String str : mFileList) {

            MediaData media = new MediaData();
            media.setFilePath(str);

            if (MediaFileUtils.isVideoFile(str)) {
                media.setFileType(MEDIA_VIDEO);
                media.setOutPutFilePath(Environment.getExternalStorageDirectory() + File.separator + new File(str).getName());
            } else {
                media.setFileType(MEDIA_PICTURE);
                media.setOutPutFilePath(Environment.getExternalStorageDirectory() + File.separator + new File(str).getName());
            }
            mediaList.add(media);
        }
        sendDataList(mediaList);
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    @Override
    public void onSelection(String path) {
        previewSelectedGalleryMedia(path);
    }

    @Override
    public void setSelectionButtonVisibility(int size) {
        setSelectedSendButtonVisibility(size);
        mTvMediacounter.setText(String.valueOf(size));
    }

    private static class ImageSaver implements Runnable {

        private final Image mImage;
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
