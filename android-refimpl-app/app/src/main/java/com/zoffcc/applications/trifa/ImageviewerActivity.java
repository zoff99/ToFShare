/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2017-2025 Zoff <zoff@zoff.cc>
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package com.zoffcc.applications.trifa;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import static com.zoffcc.applications.trifa.MainActivity.VFS_ENCRYPT;
import static com.zoffcc.applications.trifa.MainGalleryAdapter.maingallery_images_list;

/** @noinspection CallToPrintStackTrace, ResultOfMethodCallIgnored */
public class ImageviewerActivity extends AppCompatActivity
{
    private static final String TAG = "trifa.ImageviewerActy";
    static int current_image_postiton_in_list = -1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imageviewer);

        // TODO: bad!
        String image_filename = "/xx/xyz.png";

        try
        {
            image_filename = getIntent().getStringExtra("image_filename");
        }
        catch (Exception e)
        {
            e.getMessage();
        }

        final PhotoView photoView = findViewById(R.id.big_image);
        photoView.setImageResource(R.drawable.round_loading_animation);

        photoView.setOnTouchListener(new OnSwipeTouchListener(photoView.getContext()) {
            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                // Toast.makeText(photoView.getContext(), "Swipe Left gesture detected", Toast.LENGTH_SHORT).show();
                current_image_postiton_in_list++;
                try
                {
                    String try_to_get_position = maingallery_images_list.get(current_image_postiton_in_list);
                    load_current_image(try_to_get_position, photoView);
                }
                catch(Exception e)
                {
                    current_image_postiton_in_list--;
                }
            }
            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                // Toast.makeText(photoView.getContext(), "Swipe Right gesture detected", Toast.LENGTH_SHORT).show();
                boolean did_decrease = false;
                if (current_image_postiton_in_list > 0)
                {
                    current_image_postiton_in_list--;
                    did_decrease = true;
                }
                try
                {
                    String try_to_get_position = maingallery_images_list.get(current_image_postiton_in_list);
                    load_current_image(try_to_get_position, photoView);
                }
                catch(Exception e)
                {
                    if (did_decrease)
                    {
                        current_image_postiton_in_list++;
                    }
                }
            }
        });

        load_current_image(image_filename, photoView);
    }

    private void load_current_image(final String image_filename, PhotoView photoView)
    {
        if (VFS_ENCRYPT)
        {
            info.guardianproject.iocipher.File f2 = new info.guardianproject.iocipher.File(image_filename);
            try
            {
                RequestOptions req_options = new RequestOptions();
                GlideApp.
                        with(this).
                        load(f2).
                        diskCacheStrategy(DiskCacheStrategy.RESOURCE).
                        skipMemoryCache(false).
                        apply(req_options).
                        placeholder(R.drawable.round_loading_animation).
                        into(photoView);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        current_image_postiton_in_list = -1;
    }
}
