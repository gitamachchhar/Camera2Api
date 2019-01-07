package com.customcamera.gallery.adapters;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.customcamera.gallery.R;
import com.customcamera.gallery.activities.CameraGalleryActivity;
import com.customcamera.gallery.utils.MediaFileUtils;

import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends BaseAdapter {

    private List<String> mFileList;
    private Context context;
    private MyViewHolder holder;
    private ArrayList<String> mySelectedMediaList;
    private RequestOptions options;
    private boolean isMultiSelect;
    private boolean isRemoved;

    public GalleryAdapter(List<String> files, Context context, boolean isMultiSelect) {
        this.mFileList = files;
        this.context = context;
        this.isMultiSelect = isMultiSelect;
        options = new RequestOptions();
        mySelectedMediaList = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return mFileList.size();
    }

    @Override
    public Object getItem(int i) {
        return mFileList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int pos, View view, ViewGroup viewGroup) {

        view = LayoutInflater.from(context).inflate(R.layout.horizontal_gallery_item, null);
        holder = new MyViewHolder(view);

        if (MediaFileUtils.isVideoFile(mFileList.get(pos))) {
            holder.mediaTypeIcon.setImageResource(R.drawable.ic_outline_videocam_24px);
        } else {
            holder.mediaTypeIcon.setImageResource(0);
        }

        holder.mediaPreview.setLayoutParams(new FrameLayout.LayoutParams(MediaFileUtils.getWidth(), MediaFileUtils.getWidth()));
        holder.tick.setLayoutParams(new FrameLayout.LayoutParams(MediaFileUtils.getWidth(), MediaFileUtils.getWidth()));

        if (mySelectedMediaList != null && mySelectedMediaList.contains(mFileList.get(pos))) {
            holder.tick.setImageResource(R.drawable.ic_outline_done_24px);
            holder.tick.setBackgroundColor(context.getResources().getColor(R.color.selected_bg));
        } else {
            holder.tick.setImageResource(0);
            holder.tick.setBackgroundColor(0);
        }

        if (MediaFileUtils.isVideoFile(mFileList.get(pos))) {
            Glide.with(context).load(mFileList.get(pos)).apply(options.frame(0).error(R.drawable.image_placeholder_icon)).into(holder.mediaPreview);
        } else {
            Glide.with(context).load(mFileList.get(pos)).apply(options.error(R.drawable.image_placeholder_icon)).into(holder.mediaPreview);
        }

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                if (isMultiSelect) {
                    try {
                        if (mySelectedMediaList.contains(mFileList.get(pos)) && mySelectedMediaList.size() > 0) {
                            mySelectedMediaList.remove(mFileList.get(pos));
                            isRemoved = true;
                        } else {
                            mySelectedMediaList.add(mFileList.get(pos));
                            isRemoved = false;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ((CameraGalleryActivity) context).updateActionBar(mySelectedMediaList.size());
                    notifyDataSetChanged();
                }

                return true;
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!isMultiSelect) {
                    ((CameraGalleryActivity) context).previewSelectedGalleryMedia(mFileList.get(pos));
                } else {
                    if (mySelectedMediaList.contains(mFileList.get(pos))) {
                        mySelectedMediaList.remove(mFileList.get(pos));
                        isRemoved = true;
                    } else {
                        isRemoved = false;
                        if (mySelectedMediaList.size() == 10) {
                            Toast.makeText(context, "Can't share more than 10 media files", Toast.LENGTH_LONG).show();
                        } else {
                            mySelectedMediaList.add(mFileList.get(pos));
                        }
                    }

                    if (mySelectedMediaList.size() <= 1) {
                        if (isRemoved) {
                            ((CameraGalleryActivity) context).updateActionBar(mySelectedMediaList.size());
                        } else {
                            ((CameraGalleryActivity) context).previewSelectedGalleryMedia(mFileList.get(pos));
                        }
                    } else if (mySelectedMediaList.size() > 1) {
                        ((CameraGalleryActivity) context).updateActionBar(mySelectedMediaList.size());
                    } else {
                        ((CameraGalleryActivity) context).previewSelectedGalleryMedia(mFileList.get(pos));
                    }
                }


                notifyDataSetChanged();
            }
        });

        return view;
    }

    public void clearFileList() {
        mySelectedMediaList.clear();
        notifyDataSetChanged();
    }

    public ArrayList<String> getSelectedFileList() {
        return mySelectedMediaList;
    }

    class MyViewHolder {

        private AppCompatImageView mediaPreview;
        private AppCompatImageView mediaTypeIcon;
        private AppCompatImageView tick;

        MyViewHolder(View view) {
            mediaPreview = view.findViewById(R.id.imagePreview);
            mediaTypeIcon = view.findViewById(R.id.mediaType);
            tick = view.findViewById(R.id.tick);
        }

    }

}
