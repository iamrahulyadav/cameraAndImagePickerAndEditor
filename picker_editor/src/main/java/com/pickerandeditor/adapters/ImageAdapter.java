package com.pickerandeditor.adapters;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.pickerandeditor.R;
import com.pickerandeditor.modelclasses.ImageModel;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by APPZLOGIC on 4/4/2018.
 */

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageHolder> {

    private Context context;
    private ArrayList<ImageModel> imageModels;
    private OnItemClick onItemClick;

    public ImageAdapter(Context context, ArrayList<ImageModel> imageModels, OnItemClick onItemClick) {
        this.context = context;
        this.imageModels = imageModels;
        this.onItemClick = onItemClick;
    }

    @Override
    public ImageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item_view,parent,false);
        return new ImageHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageHolder holder, int position) {

        if (imageModels.get(position).getVideo()){
            Glide.with(context).asBitmap().load(Uri.fromFile(new File(imageModels.get(position).getPath())))
                    .into(holder.imagePreview);

        }else {
            Picasso.get().load(new File(imageModels.get(position).getPath()))
                    .resize(300, 300).centerCrop() .into(holder.imagePreview);
        }
        if (imageModels.get(position).getSelected()){
            holder.checkedLayout.setVisibility(View.VISIBLE);
        }else {
            holder.checkedLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return imageModels.size();
    }

    public class ImageHolder extends RecyclerView.ViewHolder{

        private ImageView imagePreview;
        private RelativeLayout checkedLayout;

        public ImageHolder(View itemView) {
            super(itemView);
            imagePreview = (ImageView) itemView.findViewById(R.id.imagePreview);
            checkedLayout = itemView.findViewById(R.id.checkedLayout);
            imagePreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemClick.onItemClickEvent(getLayoutPosition());
                }
            });
        }
    }
    public interface OnItemClick{
        void onItemClickEvent(int position);
    }
}
