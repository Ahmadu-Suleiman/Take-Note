package com.meta4projects.takenote.activities;

import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.meta4projects.takenote.R;
import com.meta4projects.takenote.others.Image;


public class ImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        makeFullscreen();

        setContentView(R.layout.activity_image);

        ImageView imageViewFullImage = findViewById(R.id.imageView_full);
        ImageView imageViewRemoveFullImage = findViewById(R.id.imageView_remove_full_image);

        String imagePath = getIntent().getStringExtra(NoteActivity.IMAGE_PATH_EXTRA);

        Bitmap bitmap = Image.getScaledBitmap(imagePath, ImageActivity.this);
        imageViewFullImage.setImageBitmap(bitmap);

        imageViewRemoveFullImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRemoveImageDialog();
            }
        });
    }

    private void showRemoveImageDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_remove_image, (ViewGroup) findViewById(R.id.layout_remove_image_dialog), false);

        final AlertDialog dialogRemoveImage = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialogRemoveImage.getWindow() != null) {
            dialogRemoveImage.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        view.findViewById(R.id.text_remove_image).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialogRemoveImage.dismiss();
                setResult(RESULT_OK);
                finish();
            }
        });

        view.findViewById(R.id.text_cancel_remove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogRemoveImage.dismiss();
            }
        });

        dialogRemoveImage.show();
    }

    private void makeFullscreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
