package com.meta4projects.takenote.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.meta4projects.takenote.R
import com.meta4projects.takenote.others.Image
import com.meta4projects.takenote.others.Utils

class ImageActivity : FullscreenActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        val imageViewFullImage = findViewById<ImageView>(R.id.imageView_full)
        val imageViewRemoveFullImage = findViewById<ImageView>(R.id.imageView_remove_full_image)
        imageViewRemoveFullImage.setOnClickListener { showRemoveImageDialog() }
        val imagePath = intent.getStringExtra(NoteActivity.IMAGE_PATH_EXTRA)
        val bitmap = Image.getScaledBitmap(imagePath, this@ImageActivity)
        Glide.with(this).asBitmap().load(bitmap).into(imageViewFullImage)
    }

    private fun showRemoveImageDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_remove_image, findViewById(R.id.layout_remove_image_dialog), false)
        val dialogRemoveImage = Utils.getDialogView(this, view)
        view.findViewById<View>(R.id.text_remove_image).setOnClickListener {
            dialogRemoveImage.dismiss()
            setResult(RESULT_OK)
            finish()
        }
        view.findViewById<View>(R.id.text_cancel_remove).setOnClickListener { dialogRemoveImage.dismiss() }
        dialogRemoveImage.show()
    }
}