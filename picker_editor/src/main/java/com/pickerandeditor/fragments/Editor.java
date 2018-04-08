package com.pickerandeditor.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.pickerandeditor.R;
import com.pickerandeditor.adapters.ImagesListAdapter;
import com.pickerandeditor.editor_classes.OnPhotoEditorListener;
import com.pickerandeditor.editor_classes.PhotoEditor;
import com.pickerandeditor.editor_classes.PhotoEditorView;
import com.pickerandeditor.editor_classes.ViewType;
import com.pickerandeditor.modelclasses.ImageModel;
import com.pickerandeditor.videoCompressor.K4LVideoTrimmer;
import com.pickerandeditor.videoCompressor.interfaces.OnK4LVideoListener;
import com.pickerandeditor.videoCompressor.interfaces.OnTrimVideoListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Editor extends android.support.v4.app.Fragment implements View.OnClickListener,
        ImagesListAdapter.ImageClickListener, OnPhotoEditorListener, OnTrimVideoListener, OnK4LVideoListener {

    private static final String TAG = "Editor";

    private ImageView crossIV, pencilIV, undoIV;
    private ImageView send;
    private EditText message;
    private RecyclerView imagesRV;
    private RelativeLayout editorViewParent,imageEditorView;
    private FrameLayout videoTrimmer;

    private String fileType;
    private ImagesListAdapter adapter;
    private ArrayList<ImageModel> resultList;
    private int currentPoistion = 0;
    private ProgressDialog dialog;
    private static final int MEDIA_TYPE_VIDEO = 1;
    private static final int MEDIA_TYPE_IMAGE = 2;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.editor_layout,null);

        Bundle bundle = getArguments();
        resultList = (ArrayList<ImageModel>) bundle.getSerializable("IMAGES");

        send = (ImageView) view.findViewById(R.id.send);
        crossIV = (ImageView) view.findViewById(R.id.crossIV);
        pencilIV = (ImageView) view.findViewById(R.id.pencilIV);
        undoIV = (ImageView) view.findViewById(R.id.undoIV);
        message = (EditText) view.findViewById(R.id.message);
        imagesRV = (RecyclerView) view.findViewById(R.id.imagesRV);
        editorViewParent = (RelativeLayout) view.findViewById(R.id.editorViewParent);
        imageEditorView = (RelativeLayout) view.findViewById(R.id.imageEditorView);
        videoTrimmer = (FrameLayout) view.findViewById(R.id.videoTrimmer);


        dialog = new ProgressDialog(getActivity());
        dialog.setCancelable(false);

        send.setOnClickListener(this);
        crossIV.setOnClickListener(this);
        pencilIV.setOnClickListener(this);
        undoIV.setOnClickListener(this);
        loadData();
        return view;
    }


    private void loadData() {
        if (resultList.get(0).getVideo()){
            videoTrimmer.setVisibility(View.VISIBLE);
            imageEditorView.setVisibility(View.GONE);
            addVideoView(0,resultList.get(0).getPath());
        }else {
            videoTrimmer.setVisibility(View.GONE);
            imageEditorView.setVisibility(View.VISIBLE);
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            Bitmap newBitmap = BitmapFactory.decodeFile(resultList.get(0).getPath(), bitmapOptions);
            addView(0, newBitmap);
        }
        message.setText(resultList.get(0).getCaption());
        if (resultList.size() > 1) {
            imagesRV.setVisibility(View.VISIBLE);
            imagesRV.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
            adapter = new ImagesListAdapter(resultList, getActivity(), this);
            imagesRV.setAdapter(adapter);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.send){
            saveImagesAndSend();
        }else if (view.getId() == R.id.crossIV){
            //finish();
        }else if (view.getId() == R.id.pencilIV){
            PhotoEditor editor = resultList.get(currentPoistion).getPhotoEditor();
            if (editor.getBrushDrawableMode()) {
                editor.setBrushDrawingMode(false);
                pencilIV.setImageResource(R.drawable.edit_pencil);
            } else {
                editor.setBrushDrawingMode(true);
                pencilIV.setImageResource(R.drawable.edit_pencil_selected);
            }
        }else if (view.getId() == R.id.undoIV){
            undoAction();
        }
    }

    private void undoAction() {
        if (resultList.get(currentPoistion).getPhotoEditor() != null) {
            PhotoEditor editor = resultList.get(currentPoistion).getPhotoEditor();
            editor.undo();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onImageClick(final int position) {
        if (currentPoistion == position) {
            return;
        }
        changePhoto(position);
    }

    private void changePhoto(int position) {
        ImageModel values = resultList.get(currentPoistion);
        values.setCaption(message.getText().toString());
        resultList.set(currentPoistion, values);
        message.setText("");
        if (resultList.get(position).getVideo()){
            videoTrimmer.setVisibility(View.VISIBLE);
            imageEditorView.setVisibility(View.GONE);
            addVideoView(position,resultList.get(position).getPath());
        }else {
            videoTrimmer.setVisibility(View.GONE);
            imageEditorView.setVisibility(View.VISIBLE);
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(resultList.get(position).getPath(), options);
            addView(position, bitmap);
        }
        message.setText(resultList.get(position).getCaption());
        message.setSelection(resultList.get(position).getCaption().length());
        currentPoistion = position;
    }


    @Override
    public void onEditTextChangeListener(View rootView, String text, int colorCode) {
        Log.d(TAG, "onStartViewChangeListener");
    }

    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {
        Log.d(TAG, "onAddViewListener " + numberOfAddedViews);
    }

    @Override
    public void onRemoveViewListener(int numberOfAddedViews) {
        Log.d(TAG, "onRemoveViewListener " + numberOfAddedViews);
    }

    @Override
    public void onStartViewChangeListener(ViewType viewType) {
        Log.d(TAG, "onStartViewChangeListener");
    }

    @Override
    public void onStopViewChangeListener(ViewType viewType) {
        Log.d(TAG, "onStopViewChangeListener");
    }

    private void addView(int position, Bitmap bitmap) {
        if (resultList.get(position).getPhotoEditorView() != null) {
            PhotoEditorView photoEditorView = resultList.get(position).getPhotoEditorView();
            PhotoEditor editor = resultList.get(position).getPhotoEditor();
            editor.setBrushDrawingMode(false);
            pencilIV.setImageResource(R.drawable.edit_pencil);
            editorViewParent.removeAllViews();
            editorViewParent.addView(photoEditorView);
        } else {
            PhotoEditorView view = new PhotoEditorView(getActivity());
            view.getSource().setImageBitmap(bitmap);
            PhotoEditor editor = new PhotoEditor.Builder(getActivity(), view).build();
            editor.setBrushColor(getResources().getColor(android.R.color.holo_red_light));
            editor.setBrushDrawingMode(false);
            pencilIV.setImageResource(R.drawable.edit_pencil);
            editorViewParent.removeAllViews();
            editorViewParent.addView(view);
            resultList.get(position).setPhotoEditorView(view);
            resultList.get(position).setPhotoEditor(editor);
        }
    }

    private void addVideoView(int position, String path){
        if (resultList.get(position).getK4LVideoTrimmer() == null) {
            K4LVideoTrimmer k4LVideoTrimmer = new K4LVideoTrimmer(getActivity(), null);
            k4LVideoTrimmer.setVideoURI(Uri.parse(path));
            k4LVideoTrimmer.setMaxDuration(30);
            k4LVideoTrimmer.setMaxDuration(30);
            k4LVideoTrimmer.setOnTrimVideoListener(this);
            k4LVideoTrimmer.setOnK4LVideoListener(this);
            k4LVideoTrimmer.setDestinationPath(getOutputMediaFile(MEDIA_TYPE_VIDEO).getAbsolutePath());
            k4LVideoTrimmer.setVideoInformationVisibility(true);
            videoTrimmer.removeAllViews();
            resultList.get(position).setK4LVideoTrimmer(k4LVideoTrimmer);
            videoTrimmer.addView(k4LVideoTrimmer);
        }else {
            K4LVideoTrimmer k4LVideoTrimmer = resultList.get(position).getK4LVideoTrimmer();
            videoTrimmer.removeAllViews();
            videoTrimmer.addView(k4LVideoTrimmer);
        }
    }

    private File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "MyVideoApps");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = null;
        if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath(), "VID_" + timeStamp + ".mp4");
        } else if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath(), "IMG_" + timeStamp + ".jpeg");
        }
        return mediaFile;
    }

    private void saveImagesAndSend() {
        new Thread(new Runnable() {
            @Override
            public void run(){
                for (int i = 0; i <resultList.size() ; i++) {
                    PhotoEditor editor = resultList.get(i).getPhotoEditor();
                    if (editor.getAddedViews().size() > 0) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!dialog.isShowing()) {
                                    dialog.show();
                                }
                            }
                        });
                        File file = new File(resultList.get(i).getPath());
                        try {
                            FileOutputStream out = new FileOutputStream(file, false);
                            if (editor.parentView != null) {
                                editor.parentView.setDrawingCacheEnabled(true);
                                Bitmap drawingCache = editor.parentView.getDrawingCache();
                                drawingCache.compress(Bitmap.CompressFormat.PNG, 100, out);
                            }
                            out.flush();
                            out.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        ImageModel values = resultList.get(currentPoistion);
                        values.setCaption(message.getText().toString());
                        resultList.set(currentPoistion, values);
                        Intent intent = new Intent();
                        intent.putExtra("IMAGES", resultList);
                    }
                });
            }
        }).start();
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
    public void onError(String message) {

    }

    @Override
    public void onVideoPrepared() {

    }
}
