/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2025 Zoff <zoff@zoff.cc>
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

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import info.guardianproject.iocipher.File;

import static com.zoffcc.applications.trifa.ImageviewerActivity.current_image_postiton_in_list;

public class MainGalleryAdapter extends RecyclerView.Adapter<MainGalleryAdapter.ViewHolder> {

        private final Context context;
        public static ArrayList<String> maingallery_images_list = new ArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view= LayoutInflater.from(context).inflate(R.layout.main_gallery_item,null,true);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final String filename_fullpath = maingallery_images_list.get(holder.getBindingAdapterPosition());
            holder.itemView.setOnTouchListener(new View.OnTouchListener()
            {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    if (event.getAction() == MotionEvent.ACTION_UP)
                    {
                        try
                        {
                            Intent intent = new Intent(v.getContext(), ImageviewerActivity.class);
                            intent.putExtra("image_filename", filename_fullpath);
                            current_image_postiton_in_list = holder.getBindingAdapterPosition();
                            v.getContext().startActivity(intent);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                    }
                    return true;
                }
            });

            File f2 = new File(filename_fullpath);
            try
            {
                // final RequestOptions glide_options = new RequestOptions().fitCenter().optionalTransform(
                //        new RoundedCorners((int) dp2px(200)));
                GlideApp.
                        with(context).
                        load(f2).
                        diskCacheStrategy(DiskCacheStrategy.RESOURCE).
                        skipMemoryCache(false).
                        priority(Priority.LOW).
                        placeholder(R.drawable.round_loading_animation).
                        into(holder.image);

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            //if (image_file.exists()) {
            //    Glide.with(context).load(image_file).into(holder.image);
            //}
        }

        @Override
        public int getItemCount() {
            return maingallery_images_list.size();
        }

        public MainGalleryAdapter(Context context, ArrayList<String> images_list) {
            this.context = context;
            maingallery_images_list = images_list;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView image;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.gallery_item);
            }
        }

}
