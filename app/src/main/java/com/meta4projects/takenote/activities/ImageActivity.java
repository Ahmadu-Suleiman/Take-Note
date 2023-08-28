package com.meta4projects.takenote.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.others.Image;
import com.meta4projects.takenote.others.Utils;


public class ImageActivity extends FullscreenActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        ImageView imageViewFullImage = findViewById(R.id.imageView_full);
        ImageView imageViewRemoveFullImage = findViewById(R.id.imageView_remove_full_image);
        imageViewRemoveFullImage.setOnClickListener(v -> showRemoveImageDialog());

        String imagePath = getIntent().getStringExtra(NoteActivity.IMAGE_PATH_EXTRA);
        Bitmap bitmap = Image.getScaledBitmap(imagePath, ImageActivity.this);
        Glide.with(this).asBitmap().load(bitmap).into(imageViewFullImage);
    }

    private void showRemoveImageDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_remove_image, findViewById(R.id.layout_remove_image_dialog), false);
        final AlertDialog dialogRemoveImage = Utils.getDialogView(this, view);

        view.findViewById(R.id.text_remove_image).setOnClickListener(v -> {
            dialogRemoveImage.dismiss();
            setResult(RESULT_OK);
            finish();
        });
        view.findViewById(R.id.text_cancel_remove).setOnClickListener(v -> dialogRemoveImage.dismiss());
        dialogRemoveImage.show();
    }
}
