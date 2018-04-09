package com.pickerandeditor;

import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.pickerandeditor.fragments.CameraAndPicker;
import com.pickerandeditor.modelclasses.ImageModel;
import com.pickerandeditor.videoCompressor.Log;

import java.util.ArrayList;

public class BaseActivity extends AppCompatActivity {


    private static final String TAG = "BASE_EDITOR_ACT";

    private RelativeLayout containerView;
    private FragmentManager fragmentManager;
    public static ArrayList<ImageModel> imageModels, selectedImagesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.
                LayoutParams.FLAG_KEEP_SCREEN_ON);

        containerView = findViewById(R.id.containerView);

        imageModels = new ArrayList<>();
        selectedImagesList = new ArrayList<>();

        addFragment(new CameraAndPicker(),"Picker");
    }

    public void addFragment(@NonNull Fragment fragment,
                               @NonNull String fragmentTag) {
        fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.containerView, fragment, fragmentTag);
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() == 1){
            finish();
            return;
        }
        super.onBackPressed();
    }
}
