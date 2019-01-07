package com.customcamera.gallery.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.customcamera.gallery.R;
import com.customcamera.gallery.interfaces.MediaSelectionCallback;
import com.customcamera.gallery.activities.CameraGalleryActivity;
import com.customcamera.gallery.utils.MediaFileUtils;

import java.util.ArrayList;
import java.util.List;

public class HorizontalMediaGalleryAdapter extends RecyclerView.Adapter<HorizontalMediaGalleryAdapter.MyViewHolder> {

    private List<String> mFileList;
    private Context mContext;
    private RequestOptions options;
    private MediaSelectionCallback mediaCallback;
    private ArrayList<String> mySelectedMediaList;
    private boolean isMultiSelect;
    private boolean isRemoved;

    public HorizontalMediaGalleryAdapter(List<String> files, Context context, MediaSelectionCallback mediaCallback, boolean isMultiSelect) {
        this.mFileList = files;
        mContext = context;
        this.isMultiSelect = isMultiSelect;
        options = new RequestOptions();
        this.mediaCallback = mediaCallback;
        mySelectedMediaList = new ArrayList<>();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.horizontal_gallery_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {
        final int pos = holder.getAdapterPosition();

        if (MediaFileUtils.isGifFile(mFileList.get(pos))) {
            holder.mediaTypeIcon.setImageResource(0);
        } else if (MediaFileUtils.isVideoFile(mFileList.get(pos))) {
            holder.mediaTypeIcon.setImageResource(R.drawable.ic_outline_videocam_24px);
        } else {
            holder.mediaTypeIcon.setImageResource(0);
        }


        if (MediaFileUtils.isVideoFile(mFileList.get(pos))) {
            Glide.with(mContext).load(mFileList.get(pos)).apply(options.frame(0).error(R.drawable.image_placeholder_icon)).into(holder.mediaPreview);
        } else {
            Glide.with(mContext).load(mFileList.get(pos)).apply(options.error(R.drawable.image_placeholder_icon)).into(holder.mediaPreview);
        }

        if (mySelectedMediaList != null && mySelectedMediaList.contains(mFileList.get(pos))) {
            holder.tick.setImageResource(R.drawable.ic_outline_done_24px);
            holder.tick.setBackgroundColor(mContext.getResources().getColor(R.color.selected_bg));
        } else {
            holder.tick.setImageResource(0);
            holder.tick.setBackgroundColor(0);
        }

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                if (mContext instanceof CameraGalleryActivity && isMultiSelect) {
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
                    notifyDataSetChanged();
                    mediaCallback.setSelectionButtonVisibility(mySelectedMediaList.size());
                }
                return true;
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!isMultiSelect) {
                    ((CameraGalleryActivity) mContext).previewSelectedGalleryMedia(mFileList.get(pos));
                } else {

                    if (mContext instanceof CameraGalleryActivity) {
                        if (mySelectedMediaList.contains(mFileList.get(pos)) && mySelectedMediaList.size() > 0) {
                            mySelectedMediaList.remove(mFileList.get(pos));
                            isRemoved = true;
                        } else {
                            isRemoved = false;
                            if (mySelectedMediaList.size() == 10) {
                                Toast.makeText(mContext, "Can't share more than 10 media files", Toast.LENGTH_LONG).show();
                            } else {
                                mySelectedMediaList.add(mFileList.get(pos));
                            }
                        }

                        if (mySelectedMediaList.size() <= 1) {
                            if (isRemoved) {
                                mediaCallback.setSelectionButtonVisibility(mySelectedMediaList.size());
                            } else {
                                mediaCallback.onSelection(mFileList.get(pos));
                            }
                        } else if (mySelectedMediaList.size() > 1) {
                            mediaCallback.setSelectionButtonVisibility(mySelectedMediaList.size());
                        } else {
                            mediaCallback.onSelection(mFileList.get(pos));
                        }
                    } else {
                        mediaCallback.onSelection(mFileList.get(pos));
                    }
                }
                notifyDataSetChanged();
            }
        });

    }

    public void clearFileList() {
        mySelectedMediaList.clear();
        notifyDataSetChanged();
    }

    public ArrayList<String> getSelectedFilesList() {
        return mySelectedMediaList;
    }

    @Override
    public int getItemCount() {
        return mFileList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        private AppCompatImageView mediaPreview;
        private AppCompatImageView mediaTypeIcon;
        private AppCompatImageView tick;

        MyViewHolder(View view) {
            super(view);
            mediaPreview = view.findViewById(R.id.imagePreview);
            mediaTypeIcon = view.findViewById(R.id.mediaType);
            tick = view.findViewById(R.id.tick);
        }

    }

}
