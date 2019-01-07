
package com.customcamera.gallery.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.customcamera.gallery.R;
import com.customcamera.gallery.interfaces.OnProgressVideoListener;
import com.customcamera.gallery.interfaces.OnRangeSeekBarListener;
import com.customcamera.gallery.interfaces.OnTrimVideoListener;
import com.customcamera.gallery.interfaces.OnVideoListener;
import com.customcamera.gallery.model.MediaData;
import com.customcamera.gallery.utils.BackgroundExecutor;
import com.customcamera.gallery.utils.UiThreadExecutor;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

import static com.customcamera.gallery.utils.MediaFileUtils.stringForTime;


public class VideoTrimmer extends FrameLayout {

    private static final String TAG = VideoTrimmer.class.getSimpleName();
    private static final int SHOW_PROGRESS = 2;

    private SeekBar mHolderTopView;
    private RangeSeekBarView mRangeSeekBarView;
    private RelativeLayout mLinearVideo;
    private View mTimeInfoContainer;
    private VideoView mVideoView;
    private ImageView mPlayView;
    private TextView mTextSize;
    private TextView mTextTimeFrame;
    private TextView mTextTime;
    private TimeLineView mTimeLineView;

    private ProgressBarView mVideoProgressIndicator;
    private List<OnProgressVideoListener> mListeners;
    private OnTrimVideoListener mOnTrimVideoListener;
    private OnVideoListener mOnVideoListener;

    private int mDuration = 0;
    private int mTimeVideo = 0;
    private int mStartPosition = 0;
    private int mEndPosition = 0;

    private long mOriginSizeFile;
    private boolean mResetSeekBar = true;
    public boolean isUpdateRequired = false;
    private final MessageHandler mMessageHandler = new MessageHandler(this);

    private MediaData mData;
    private MediaData mediaData;

    public VideoTrimmer(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoTrimmer(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public VideoTrimmer(@NonNull Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.videotrimming, this, true);
        init();
        setUpListeners();
        setUpMargins();
    }

    private void init() {

        mHolderTopView = findViewById(R.id.handlerTop);
        mVideoView = findViewById(R.id.videoView);
        mLinearVideo = findViewById(R.id.layout_surface_view);
        mTextTime = findViewById(R.id.textTime);
        mTextTimeFrame = findViewById(R.id.textTimeSelection);
        mTimeInfoContainer = findViewById(R.id.timeText);
        mRangeSeekBarView = findViewById(R.id.timeLineBar);
        mPlayView = findViewById(R.id.icon_video_play);
        mTimeLineView = findViewById(R.id.timeLineView);
        mTextSize = findViewById(R.id.textSize);
        mVideoProgressIndicator = findViewById(R.id.timeVideoView);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpListeners() {

        mListeners = new ArrayList<>();
        mListeners.add(new OnProgressVideoListener() {
            @Override
            public void updateProgress(int time, int max, float scale) {
                updateVideoProgress(time);
            }
        });

        mListeners.add(mVideoProgressIndicator);

        final GestureDetector gestureDetector = new
                GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        onClickVideoPlayPause();
                        return true;
                    }
                }
        );

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                if (mOnTrimVideoListener != null)
                    mOnTrimVideoListener.onError("Something went wrong reason : " + what);
                return false;
            }
        });

        mVideoView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, @NonNull MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });

        mRangeSeekBarView.addOnRangeSeekBarListener(new OnRangeSeekBarListener() {
            @Override
            public void onCreate(RangeSeekBarView rangeSeekBarView, int index, float value) {
                // Do nothing
            }

            @Override
            public void onSeek(RangeSeekBarView rangeSeekBarView, int index, float value) {
                onSeekThumbs(index, value);
            }

            @Override
            public void onSeekStart(RangeSeekBarView rangeSeekBarView, int index, float value) {
                // Do nothing
            }

            @Override
            public void onSeekStop(RangeSeekBarView rangeSeekBarView, int index, float value) {
                onStopSeekThumbs();
                insertMediaInTable();
            }
        });

        mRangeSeekBarView.addOnRangeSeekBarListener(mVideoProgressIndicator);

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                onVideoPrepared(mp);
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                onVideoCompleted();
            }
        });

    }

    public void insertMediaInTable() {

        Realm.getDefaultInstance().executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(@NonNull Realm realm) {
                if (mediaData != null) {
                    realm.insertOrUpdate(mediaData);
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                isUpdateRequired = false;
            }
        });
    }

    private void setUpMargins() {

        int marge = mRangeSeekBarView.getThumbs().get(0).getWidthBitmap();
        int widthSeek = mHolderTopView.getThumb().getMinimumWidth() / 2;

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mHolderTopView.getLayoutParams();
        lp.setMargins(marge - widthSeek, 0, marge - widthSeek, 0);
        mHolderTopView.setLayoutParams(lp);

        lp = (RelativeLayout.LayoutParams) mTimeLineView.getLayoutParams();
        lp.setMargins(marge, 0, marge, 0);
        mTimeLineView.setLayoutParams(lp);

        lp = (RelativeLayout.LayoutParams) mVideoProgressIndicator.getLayoutParams();
        lp.setMargins(marge, 0, marge, 0);
        mVideoProgressIndicator.setLayoutParams(lp);
    }

    private void onClickVideoPlayPause() {

        if (mVideoView.isPlaying()) {
            mPlayView.setVisibility(View.VISIBLE);
            mMessageHandler.removeMessages(SHOW_PROGRESS);
            mVideoView.pause();
        } else {
            mPlayView.setVisibility(View.GONE);

            if (mResetSeekBar) {
                mResetSeekBar = false;
                mVideoView.seekTo(mStartPosition);
            }

            mMessageHandler.sendEmptyMessage(SHOW_PROGRESS);
            mVideoView.start();
        }

    }

    private void onVideoPrepared(@NonNull MediaPlayer mp) {

        // Adjust the size of the video
        // so it fits on the screen

        int videoWidth = mp.getVideoWidth();
        int videoHeight = mp.getVideoHeight();
        float videoProportion = (float) videoWidth / (float) videoHeight;
        int screenWidth = mLinearVideo.getWidth();
        int screenHeight = mLinearVideo.getHeight();
        float screenProportion = (float) screenWidth / (float) screenHeight;
        ViewGroup.LayoutParams lp = mVideoView.getLayoutParams();

        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }

        mVideoView.setLayoutParams(lp);
        mPlayView.setVisibility(View.VISIBLE);
        mDuration = mVideoView.getDuration();
        setSeekBarPosition();
        setTimeFrames();
        setTimeVideo(0);

        if (mOnVideoListener != null) {
            mOnVideoListener.onVideoPrepared();
        }
    }

    private void setSeekBarPosition() {

        try {
            if (mData != null) {

                mediaData = Realm.getDefaultInstance().copyFromRealm(mData);

                if (mData.getStartTime() > 0) {
                    mediaData.setStartTime(mData.getStartTime());
                    mStartPosition = mData.getStartTime();
                } else {
                    mediaData.setStartTime(mStartPosition);
                    mStartPosition = 0;
                }

                if (mData.getEndTime() > 0) {
                    mediaData.setEndTime(mData.getEndTime());
                    mEndPosition = mData.getEndTime();
                } else {
                    mediaData.setEndTime(mEndPosition);
                    mEndPosition = mDuration;
                }

                if (mData.getmDuration() > 0) {
                    mediaData.setmDuration(mData.getmDuration());
                    mTimeVideo = mData.getmDuration();
                } else {
                    mediaData.setmDuration(mDuration);
                    mTimeVideo = mDuration;
                }

                mediaData.setTrimmed(mData.isTrimmed());

            }

            mVideoView.seekTo(mStartPosition);
            mRangeSeekBarView.initMaxWidth();

            if (mediaData.isTrimmed())
                mRangeSeekBarView.setThumbPosition(mediaData);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void onSeekThumbs(int index, float value) {

        switch (index) {

            case Thumb.LEFT: {
                mStartPosition = (int) ((mDuration * value) / 100L);
                mVideoView.seekTo(mStartPosition);
                break;
            }
            case Thumb.RIGHT: {
                mEndPosition = (int) ((mDuration * value) / 100L);
                break;
            }

        }

        setTimeFrames();
        mTimeVideo = mEndPosition - mStartPosition;

        if (mediaData == null) {
            mediaData = Realm.getDefaultInstance().copyFromRealm(mData);
        }

        mediaData.setStartTime(mStartPosition);
        mediaData.setEndTime(mEndPosition);
        mediaData.setmDuration(mTimeVideo);

        mediaData.setmThumb1Pos(mRangeSeekBarView.getThumbPos(0));

        if (!mRangeSeekBarView.fromUser) {
            mediaData.setmThumb2Pos(mediaData.getmThumb2Pos());
            mRangeSeekBarView.fromUser = true;
        } else {
            mediaData.setmThumb2Pos(mRangeSeekBarView.getThumbPos(1));
        }

        if (mTimeVideo != mDuration) {
            mediaData.setTrimmed(true);
        } else {
            mediaData.setTrimmed(false);
        }
        isUpdateRequired = true;
    }

    private void setTimeFrames() {
        String seconds = getContext().getString(R.string.short_seconds);
        mTextTimeFrame.setText(String.format("%s %s - %s %s", stringForTime(mStartPosition), seconds, stringForTime(mEndPosition), seconds));
    }

    private void setTimeVideo(int position) {
        String seconds = getContext().getString(R.string.short_seconds);
        mTextTime.setText(String.format("%s %s", stringForTime(position), seconds));
    }

    private void onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS);
        mVideoView.pause();
        mPlayView.setVisibility(View.VISIBLE);
    }

    private void onVideoCompleted() {
        mVideoView.seekTo(mStartPosition);
    }

    private void notifyProgressUpdate(boolean isAll) {

        if (mDuration == 0) return;

        int position = mVideoView.getCurrentPosition();
        if (isAll) {
            for (OnProgressVideoListener item : mListeners) {
                item.updateProgress(position, mDuration, ((position * 100) / mDuration));
            }
        } else {
            mListeners.get(1).updateProgress(position, mDuration, ((position * 100) / mDuration));
        }
    }

    private void updateVideoProgress(int time) {
        if (mVideoView == null) {
            return;
        }

        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS);
            mVideoView.pause();
            mPlayView.setVisibility(View.VISIBLE);
            mResetSeekBar = true;
            return;
        }
        setTimeVideo(time);
    }


    public void setVideoInformationVisibility(boolean visible) {
        mTimeInfoContainer.setVisibility(visible ? VISIBLE : GONE);
    }

    @SuppressWarnings("unused")
    public void setOnTrimVideoListener(OnTrimVideoListener onTrimVideoListener) {
        mOnTrimVideoListener = onTrimVideoListener;
    }

    @SuppressWarnings("unused")
    public void setOnVideoListenre(OnVideoListener onVideoListener) {
        mOnVideoListener = onVideoListener;
    }

    @SuppressWarnings("unused")
    public void setDestinationPath(final String finalPath) {
        String mFinalPath = finalPath;
        Log.d(TAG, "Setting custom path " + mFinalPath);
    }

    public void destroy() {
        BackgroundExecutor.cancelAll("", true);
        UiThreadExecutor.cancelAll("");
    }

    @SuppressWarnings("unused")
    public void setMaxDuration(int maxDuration) {
        int mMaxDuration = maxDuration * 1000;
    }

    @SuppressWarnings("unused")
    public void setVideoURI(final Uri videoURI) {

        if (videoURI == null)
                return;


        if (mOriginSizeFile == 0) {
            File file = new File(videoURI.getPath());

            mOriginSizeFile = file.length();
            long fileSizeInKB = mOriginSizeFile / 1024;

            if (fileSizeInKB > 1000) {
                long fileSizeInMB = fileSizeInKB / 1024;
                mTextSize.setText(String.format("%s %s", fileSizeInMB, getContext().getString(R.string.megabyte)));
            } else {
                mTextSize.setText(String.format("%s %s", fileSizeInKB, getContext().getString(R.string.kilobyte)));
            }
        }

        mVideoView.setVideoURI(videoURI);
        mVideoView.requestFocus();
        mTimeLineView.setVideo(videoURI);

    }

    public void setMediaData(MediaData mData) {
        this.mData = mData;
    }

    private static class MessageHandler extends Handler {

        @NonNull
        private final WeakReference<VideoTrimmer> mView;

        MessageHandler(VideoTrimmer view) {
            mView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoTrimmer view = mView.get();
            if (view == null || view.mVideoView == null) {
                return;
            }

            view.notifyProgressUpdate(true);
            if (view.mVideoView.isPlaying()) {
                sendEmptyMessageDelayed(0, 10);
            }
        }
    }
}
