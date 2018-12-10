package com.counter.hegi.util

import android.databinding.BindingAdapter
import android.net.Uri
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import opencv.hegi.countpeopleopencv.R

@BindingAdapter("app:loadImg")
fun setImage(img: CircleImageView, url: String){
    Picasso.get()
            .load(Uri.parse(url))
            .placeholder(R.drawable.user_unknown)
            .into(img)
}