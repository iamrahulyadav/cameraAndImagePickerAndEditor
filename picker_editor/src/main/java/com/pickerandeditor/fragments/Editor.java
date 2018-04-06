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
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.pickerandeditor.R;
import com.pickerandeditor.adapters.ImagesListAdapter;
import com.pickerandeditor.editor_classes.OnPhotoEditorListener;
import com.pickerandeditor.editor_classes.PhotoEditor;
import com.pickerandeditor.editor_classes.PhotoEditorView;
import com.pickerandeditor.editor_classes.ViewType;
import com.pickerandeditor.modelclasses.ImageModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Editor extends android.support.v4.app.Fragment implements View.OnClickListener,
        ImagesListAdapter.ImageClickListener, OnPhotoEditorListener {

    private static final String TAG = "Editor";

    private ImageView crossIV, pencilIV, undoIV;
    private ImageView send;
    private EditText message;
    private RecyclerView imagesRV;
    private RelativeLayout editorViewParent;

    private String type;
    private String fileType;
    private ImagesListAdapter adapter;
    private ArrayList<ImageModel> resultList;
    private int currentPoistion = 0;
    private int viewsAdded;
    private ProgressDialog dialog;
    private HashMap<Integer, List<Object>> valuesMap;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.editor_layout,null);

        Bundle bundle = getArguments();
        resultList = (ArrayList<ImageModel>) bundle.getSerializable("IMAGES");
        type = bundle.getString("TYPE");

        send = (ImageView) view.findViewById(R.id.send);
        crossIV = (ImageView) view.findViewById(R.id.crossIV);
        pencilIV = (ImageView) view.findViewById(R.id.pencilIV);
        undoIV = (ImageView) view.findViewById(R.id.undoIV);
        message = (EditText) view.findViewById(R.id.message);
        imagesRV = (RecyclerView) view.findViewById(R.id.imagesRV);
        editorViewParent = (RelativeLayout) view.findViewById(R.id.editorViewParent);

        valuesMap = new HashMap<>();

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
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(resultList.get(0).getPath(), options);
        if (type.equalsIgnoreCase("CAMERA")) {
            fileType = "PHOTO";
            float rotation = rotationForImage(getActivity(), Uri.fromFile(new File(resultList.get(0).getPath())));
            if (rotation != 0) {
                //New rotation matrix
                Matrix matrix2 = new Matrix();
                matrix2.preRotate(rotation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                        matrix2, true);
            }
        } else if (type.equalsIgnoreCase("GALLERY")) {
            fileType = "PHOTO";
        }
        addView(0, bitmap);

        message.setText(resultList.get(0).getCaption());

        for (int i = 0; i < resultList.size(); i++) {
            java.util.Random random = new java.util.Random();
            int r = Math.abs(random.nextInt());
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            //Bitmap newBitmap = BitmapFactory.decodeFile(resultList.get(i).getPath(), bitmapOptions);
            if (type.equalsIgnoreCase("CAMERA")) {
                float rotation = rotationForImage(getActivity(), Uri.fromFile(new File(resultList.get(i).getPath())));
                if (rotation != 0) {
                    // New rotation matrix
                    Matrix matrix2 = new Matrix();
                    matrix2.preRotate(rotation);
                  //  newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                  //          matrix2, true);
                }
            }
           // StorageOperations.saveImage(Singleton.INSTANCE, String.valueOf(r) + "noteImage.png", newBitmap);
//            Log.e("path of dir", "" + getExternalFilesDir(null).getAbsolutePath() + "/"
//                    + "ImageNote/noteImage.png");
            File noteResource = null;
            if (fileType.equalsIgnoreCase("PHOTO")) {
                if (bitmap != null) {
                    noteResource = new File(resultList.get(i).getPath());
                }
            }
            ImageModel values = resultList.get(i);
            values.setPath(noteResource.getAbsolutePath());
            resultList.set(i, values);
        }

        if (resultList.size() > 1) {
            imagesRV.setVisibility(View.VISIBLE);
            imagesRV.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
            adapter = new ImagesListAdapter(resultList, getActivity(), this);
            imagesRV.setAdapter(adapter);
        }
    }

    public static float rotationForImage(Context context, Uri uri) {
        try {
            if (uri.getScheme().equals("content")) {
                //From the media gallery
                String[] projection = {MediaStore.Images.ImageColumns.ORIENTATION};
                Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
                if (c.moveToFirst()) {
                    return c.getInt(0);
                }
            } else if (uri.getScheme().equals("file")) {
                //From a file saved by the camera
                ExifInterface exif = new ExifInterface(uri.getPath());
                int rotation = (int) exifOrientationToDegrees(exif.getAttributeInt(ExifInterface
                        .TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface
                        .ORIENTATION_NORMAL);
                exif.saveAttributes();
                return rotation;
            }
            return 0;

        } catch (IOException e) {
            Log.e(TAG, "Error checking exit", e);
            return 0;
        }
    }

    private static float exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.send){
            saveImagesAndSend();
        }else if (view.getId() == R.id.crossIV){
            //finish();
        }else if (view.getId() == R.id.pencilIV){
            List<Object> list = valuesMap.get(currentPoistion);
            PhotoEditor editor = (PhotoEditor) list.get(1);
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
        if (valuesMap.containsKey(currentPoistion)) {
            List<Object> list = valuesMap.get(currentPoistion);
            PhotoEditor editor = (PhotoEditor) list.get(1);
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
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(resultList.get(position).getPath(), options);
        addView(position, bitmap);
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
        viewsAdded = numberOfAddedViews;
    }

    @Override
    public void onRemoveViewListener(int numberOfAddedViews) {
        Log.d(TAG, "onRemoveViewListener " + numberOfAddedViews);
        viewsAdded = numberOfAddedViews;
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
        if (valuesMap.containsKey(position)) {
            List<Object> list = valuesMap.get(position);
            PhotoEditorView photoEditorView = (PhotoEditorView) list.get(0);
            PhotoEditor editor = (PhotoEditor) list.get(1);
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
            List<Object> list = new ArrayList<>();
            list.add(view);
            list.add(editor);
            valuesMap.put(position, list);
        }
    }


    private void saveImagesAndSend() {
        new Thread(new Runnable() {
            @Override
            public void run(){
                Iterator iterator = valuesMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, List<Object>> entry = (Map.Entry<Integer, List<Object>>) iterator.next();
                    List<Object> list = entry.getValue();
                    PhotoEditor editor = (PhotoEditor) list.get(1);
                    if (editor.getAddedViews().size() > 0) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!dialog.isShowing()) {
                                    dialog.show();
                                }
                            }
                        });
                        File file = new File(resultList.get(entry.getKey()).getPath());
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
                            dialog.show();
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

}
