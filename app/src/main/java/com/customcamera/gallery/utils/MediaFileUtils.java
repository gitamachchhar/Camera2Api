package com.customcamera.gallery.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import com.customcamera.gallery.model.MediaData;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import io.realm.Realm;
import io.realm.RealmResults;

import static android.content.ContentValues.TAG;

public class MediaFileUtils {

    private static ArrayList<String> filePathList = new ArrayList<>();

    public static ArrayList<String> getFile(final File dir) throws PatternSyntaxException {

        File listFile[] = dir.listFiles();

        if (listFile != null && listFile.length > 0) {

            for (File lFile : listFile) {

                if ((lFile.getName().equalsIgnoreCase("Android") && lFile.isDirectory()) || lFile.getName().startsWith(".")) {
                    continue;
                }

                if (lFile.isDirectory()) {
                    getFile(lFile);
                } else {

                    if (lFile.getName().contains(".jpg")
                            || lFile.getName().contains(".jpeg")
                            || lFile.getName().contains(".gif")
                            || lFile.getName().contains(".png")
                            || lFile.getName().contains(".mp4")
                            || lFile.getName().contains(".mpeg4")
                            || lFile.getName().contains(".avi")
                            || lFile.getName().contains(".wmv")
                            || lFile.getName().contains(".3gp")
                            ) {

                        if (lFile.getName().toLowerCase().contains(".3gp") && !isVideoFile(lFile.getName()))
                            continue;
                        else if (lFile.getName().toLowerCase().contains(".wav") && !isVideoFile(lFile.getName()))
                            continue;

                        filePathList.add(lFile.getAbsolutePath());
                    }
                }
            }
        }

        if (filePathList == null || filePathList.size() == 0)
            return null;

        return filePathList;
    }

    public static List<MediaData> getMediaList() {
        RealmResults<MediaData> realmObject = Realm.getDefaultInstance().where(MediaData.class).findAll();
        if (realmObject != null)
            return Realm.getDefaultInstance().copyFromRealm(realmObject);
        else
            return new ArrayList<>();
    }

    public static void clearFileList() {
        filePathList.clear();
    }

    public static ArrayList<String> getImageFile(final File dir) throws PatternSyntaxException {

        File listFile[] = dir.listFiles();

        if (listFile != null && listFile.length > 0) {

            for (File lFile : listFile) {

                if ((lFile.getName().equalsIgnoreCase("Android") && lFile.isDirectory()) || lFile.getName().startsWith(".")) {
                    continue;
                }

                if (lFile.isDirectory()) {
                    getImageFile(lFile);

                } else {

                    if (lFile.getName().contains(".jpg")
                            || lFile.getName().contains(".jpeg")
                            || lFile.getName().contains(".png")
                            ) {
                        filePathList.add(lFile.getAbsolutePath());
                    }
                }
            }
        }

        if (filePathList == null || filePathList.size() == 0)
            return null;

        return filePathList;
    }

    public static class FileTimeComparator implements Comparator<String> {

        public int compare(String o1, String o2) {

            File e1 = new File(o1);
            File e2 = new File(o2);

            long k = e1.lastModified() - e2.lastModified();

            if (k > 0) {
                return 1;
            } else if (k == 0) {
                return 0;
            } else {
                return -1;
            }

        }
    }

    public static boolean isVideoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("video");
    }

    public static boolean isGifFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.contains("gif");
    }

    public static int getWidth() {
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        return width / 3;

    }

    public static int convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static String formatTimer(int i) {

        String strTimer = "";

        int hour = i / 3600;
        int minute = (i % 3600) / 60;
        int seconds = i % 60;

        if (hour >= 1) {
            strTimer = String.format("%02d:%02d:%02d", hour, minute, seconds);
        } else {
            strTimer = String.format("%02d:%02d", minute, seconds);
        }

        return strTimer;

    }

    public static String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        Formatter mFormatter = new Formatter();
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void genVideoUsingMuxer(String srcPath, String dstPath,
                                          int startMs, int endMs, boolean useAudio, boolean
                                                  useVideo)
            throws IOException {
        // Set up MediaExtractor to read from the source.
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(srcPath);
        int trackCount = extractor.getTrackCount();
        // Set up MediaMuxer for the destination.
        MediaMuxer muxer;
        muxer = new MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        // Set up the tracks and retrieve the max buffer size for selected
        // tracks.
        HashMap<Integer, Integer> indexMap = new HashMap<>(trackCount);
        int bufferSize = -1;
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            boolean selectCurrentTrack = false;

            if (mime.startsWith("audio/") && useAudio) {
                selectCurrentTrack = true;
            } else if (mime.startsWith("video/") && useVideo) {
                selectCurrentTrack = true;
            }

            if (selectCurrentTrack) {
                extractor.selectTrack(i);
                int dstIndex = muxer.addTrack(format);
                indexMap.put(i, dstIndex);
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    int newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    bufferSize = newSize > bufferSize ? newSize : bufferSize;
                }
            }
        }

        if (bufferSize < 0) {
            bufferSize = 64;
        }
        // Set up the orientation and starting time for extractor.
        MediaMetadataRetriever retrieverSrc = new MediaMetadataRetriever();
        retrieverSrc.setDataSource(srcPath);
        String degreesString = retrieverSrc.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (degreesString != null) {
            int degrees = Integer.parseInt(degreesString);
            if (degrees >= 0) {
                muxer.setOrientationHint(degrees);
            }
        }

        if (startMs > 0) {
            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        }
        // Copy the samples from MediaExtractor to MediaMuxer. We will loop
        // for copying each sample and stop when we get to the end of the source
        // file or exceed the end time of the trimming.

        int offset = 0;
        int trackIndex = -1;
        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        try {
            muxer.start();
            while (true) {
                bufferInfo.offset = offset;
                bufferInfo.size = extractor.readSampleData(dstBuf, offset);
                if (bufferInfo.size < 0) {
                    Log.d(TAG, "Saw input EOS.");
                    bufferInfo.size = 0;
                    break;
                } else {
                    bufferInfo.presentationTimeUs = extractor.getSampleTime();
                    if (endMs > 0 && bufferInfo.presentationTimeUs > (endMs * 1000)) {
                        Log.d(TAG, "The current sample is over the trim end time.");
                        break;
                    } else {
                        bufferInfo.flags = extractor.getSampleFlags();
                        trackIndex = extractor.getSampleTrackIndex();
                        muxer.writeSampleData(indexMap.get(trackIndex), dstBuf,
                                bufferInfo);
                        extractor.advance();
                    }
                }
            }
            muxer.stop();

            //deleting the old file
            /*File file = new File(srcPath);
            file.delete();*/
        } catch (IllegalStateException e) {
            // Swallow the exception due to malformed source.
            Log.w(TAG, "The source video file is malformed");
        } finally {
            muxer.release();
        }
        return;
    }

}
