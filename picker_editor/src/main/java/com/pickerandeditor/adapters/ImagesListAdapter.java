package com.pickerandeditor.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import com.pickerandeditor.R;
import com.pickerandeditor.modelclasses.ImageModel;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by APPZLOGIC on 2/15/2018.
 */

public class ImagesListAdapter extends RecyclerView.Adapter<ImagesListAdapter.ImagesHolder>{

    private ArrayList<ImageModel> imageslist;
    private Context context;
    private ImageClickListener imageClickListener;

    public ImagesListAdapter(ArrayList<ImageModel> imageslist, Context context, ImageClickListener imageClickListener) {
        this.imageslist = imageslist;
        this.context = context;
        this.imageClickListener = imageClickListener;
    }

    @Override
    public ImagesHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.images_list_view,parent,false);
        return new ImagesHolder(view);
    }

    @Override
    public void onBindViewHolder(ImagesHolder holder, int position) {
        Picasso.get().load(new File(imageslist.get(position).getPath())).fit().into(holder.imageListView);
    }

    @Override
    public int getItemCount() {
        return imageslist.size();
    }

    public class ImagesHolder extends RecyclerView.ViewHolder{

        private ImageView imageListView;
        public ImagesHolder(View itemView) {
            super(itemView);
            imageListView = (ImageView) itemView.findViewById(R.id.imageListView);
            imageListView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    imageClickListener.onImageClick(getLayoutPosition());
                }
            });
        }
    }

    public interface ImageClickListener{
        void onImageClick(int position);
    }
}
