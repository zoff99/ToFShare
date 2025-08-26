/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2017 Zoff <zoff@zoff.cc>
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
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.luseen.autolinklibrary.AutoLinkMode;
import com.luseen.autolinklibrary.EmojiTextViewLinks;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.zoffcc.applications.sorm.Message;

import java.net.URLConnection;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import static com.zoffcc.applications.trifa.HelperFiletransfer.get_filetransfer_filenum_from_id;
import static com.zoffcc.applications.trifa.HelperFiletransfer.remove_ft_from_cache;
import static com.zoffcc.applications.trifa.HelperFiletransfer.set_filetransfer_state_from_id;
import static com.zoffcc.applications.trifa.HelperFriend.tox_friend_by_public_key__wrapper;
import static com.zoffcc.applications.trifa.HelperGeneric.dp2px;
import static com.zoffcc.applications.trifa.HelperGeneric.long_date_time_format;
import static com.zoffcc.applications.trifa.HelperMessage.set_message_queueing_from_id;
import static com.zoffcc.applications.trifa.HelperMessage.set_message_state_from_id;
import static com.zoffcc.applications.trifa.HelperMessage.update_single_message_from_messge_id;
import static com.zoffcc.applications.trifa.MainActivity.tox_file_control;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_CONTROL.TOX_FILE_CONTROL_CANCEL;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_KIND.TOX_FILE_KIND_FTV2;

/*
 *
 * HINT: this is the START POINT for outgoing FT
 * when nothing has started yet, but a file was selected to send
 *
 */
public class MessageListHolder_file_outgoing_state_pause_not_yet_started extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener
{
    private static final String TAG = "trifa.MessageListHldr01";

    private Context context;

    ImageButton button_ok;
    ImageButton button_cancel;
    com.daimajia.numberprogressbar.NumberProgressBar ft_progressbar;
    ViewGroup ft_preview_container;
    ViewGroup ft_buttons_container;
    ImageButton ft_preview_image;
    EmojiTextViewLinks textView;
    ImageView imageView;
    de.hdodenhof.circleimageview.CircleImageView img_avatar;
    TextView date_time;
    TextView message_text_date_string;
    ViewGroup message_text_date;
    ViewGroup rounded_bg_container;

    public MessageListHolder_file_outgoing_state_pause_not_yet_started(View itemView, Context c)
    {
        super(itemView);

        // Log.i(TAG, "MessageListHolder");

        this.context = c;

        button_ok = (ImageButton) itemView.findViewById(R.id.ft_button_ok);
        button_cancel = (ImageButton) itemView.findViewById(R.id.ft_button_cancel);
        ft_progressbar = (com.daimajia.numberprogressbar.NumberProgressBar) itemView.findViewById(R.id.ft_progressbar);
        ft_preview_container = (ViewGroup) itemView.findViewById(R.id.ft_preview_container);
        ft_buttons_container = (ViewGroup) itemView.findViewById(R.id.ft_buttons_container);
        ft_preview_image = (ImageButton) itemView.findViewById(R.id.ft_preview_image);
        rounded_bg_container = (ViewGroup) itemView.findViewById(R.id.ft_outgoing_rounded_bg);
        textView = (EmojiTextViewLinks) itemView.findViewById(R.id.m_text);
        imageView = (ImageView) itemView.findViewById(R.id.m_icon);
        img_avatar = (de.hdodenhof.circleimageview.CircleImageView) itemView.findViewById(R.id.img_avatar);
        date_time = (TextView) itemView.findViewById(R.id.date_time);
        message_text_date_string = (TextView) itemView.findViewById(R.id.message_text_date_string);
        message_text_date = (ViewGroup) itemView.findViewById(R.id.message_text_date);
    }

    public void bindMessageList(Message m)
    {
        // Log.i(TAG, "bindMessageList");

        if (m == null)
        {
            // TODO: should never be null!!
            // only afer a crash
            m = new Message();
        }

        date_time.setText(long_date_time_format(m.sent_timestamp));

        final Message message = m;

        textView.addAutoLinkMode(AutoLinkMode.MODE_URL, AutoLinkMode.MODE_EMAIL, AutoLinkMode.MODE_HASHTAG,
                                 AutoLinkMode.MODE_MENTION);

        ft_progressbar.setVisibility(View.GONE);
        ft_buttons_container.setVisibility(View.VISIBLE);
        ft_preview_container.setVisibility(View.VISIBLE);
        ft_preview_image.setVisibility(View.VISIBLE);

        final Message message2 = message;

        int drawable_id = R.drawable.rounded_blue_bg_with_border;
        try
        {
            if (m.filetransfer_kind == TOX_FILE_KIND_FTV2.value)
            {
                drawable_id = R.drawable.rounded_blue_bg;
            }

            final int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN)
            {
                rounded_bg_container.setBackgroundDrawable(ContextCompat.getDrawable(context, drawable_id));
            }
            else
            {
                rounded_bg_container.setBackground(ContextCompat.getDrawable(context, drawable_id));
            }
        }
        catch (Exception e)
        {
            final int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN)
            {
                rounded_bg_container.setBackgroundDrawable(ContextCompat.getDrawable(context, drawable_id));
            }
            else
            {
                rounded_bg_container.setBackground(ContextCompat.getDrawable(context, drawable_id));
            }
        }

        // --------- message date header (show only if different from previous message) ---------
        // --------- message date header (show only if different from previous message) ---------
        // --------- message date header (show only if different from previous message) ---------
        message_text_date.setVisibility(View.GONE);
        int my_position = this.getAdapterPosition();
        if (my_position != RecyclerView.NO_POSITION)
        {
            if (MainActivity.message_list_fragment != null)
            {
                if (MainActivity.message_list_fragment.adapter != null)
                {
                    if (my_position < 1)
                    {
                        message_text_date_string.setText(
                                MainActivity.message_list_fragment.adapter.getDateHeaderText(my_position));
                        message_text_date.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        if (!MainActivity.message_list_fragment.adapter.getDateHeaderText(my_position).equals(
                                MainActivity.message_list_fragment.adapter.getDateHeaderText(my_position - 1)))
                        {
                            message_text_date_string.setText(
                                    MainActivity.message_list_fragment.adapter.getDateHeaderText(my_position));
                            message_text_date.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        }
        // --------- message date header (show only if different from previous message) ---------
        // --------- message date header (show only if different from previous message) ---------
        // --------- message date header (show only if different from previous message) ---------


        final Drawable d1 = new IconicsDrawable(context).
                icon(GoogleMaterial.Icon.gmd_check_circle).
                backgroundColor(Color.TRANSPARENT).
                color(Color.parseColor("#EF088A29")).sizeDp(50);
        button_ok.setImageDrawable(d1);
        final Drawable d2 = new IconicsDrawable(context).
                icon(GoogleMaterial.Icon.gmd_highlight_off).
                backgroundColor(Color.TRANSPARENT).
                color(Color.parseColor("#A0FF0000")).sizeDp(50);
        button_cancel.setImageDrawable(d2);
        ft_buttons_container.setVisibility(View.VISIBLE);

        button_ok.setVisibility(View.VISIBLE);
        button_cancel.setVisibility(View.VISIBLE);

        HelperGeneric.fill_own_avatar_icon(context, img_avatar);

        if (message.ft_outgoing_queued)
        {
            textView.setAutoLinkText("" + message.text + "\n\nqueued ...");
        }
        else
        {
            textView.setAutoLinkText("" + message.text + "\n\nSend this file?");
        }

        boolean is_image = false;
        try
        {
            String mimeType = null;
            if (message.storage_frame_work)
            {
                Uri uri = Uri.parse(message.filename_fullpath);
                DocumentFile documentFile = DocumentFile.fromSingleUri(context, uri);
                String fileName = documentFile.getName();
                mimeType = URLConnection.guessContentTypeFromName(fileName.toLowerCase());
            }
            else
            {
                mimeType = URLConnection.guessContentTypeFromName(message.filename_fullpath.toLowerCase());
            }
            if (mimeType.startsWith("image/"))
            {
                is_image = true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (message.ft_outgoing_queued)
        {
            button_ok.setVisibility(View.GONE);
        }
        else
        {
            button_ok.setVisibility(View.VISIBLE);
        }

        button_ok.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    try
                    {
                        Log.i(TAG, "MM2MM:7:mid=" + message.id + " ftid:" + message.filetransfer_id);

                        // queue FT
                        set_message_queueing_from_id(message.id, true);
                        button_ok.setVisibility(View.GONE);

                        // update message view
                        update_single_message_from_messge_id(message.id, true);

                        Log.i(TAG, "button_ok:OnTouch:009");
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        Log.i(TAG, "MM2MM:EE1:" + e.getMessage());
                    }
                }
                else
                {
                }
                return true;
            }
        });


        button_cancel.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle(
                            v.getContext().getString(R.string.MessageListHolder_file_outgoing_cancel_ft_title));
                    builder.setMessage(
                            v.getContext().getString(R.string.MessageListHolder_file_outgoing_cancel_ft_message));

                    builder.setNegativeButton(v.getContext().getString(R.string.MainActivity_no_button), null);
                    builder.setPositiveButton(v.getContext().getString(R.string.MainActivity_yes_button),
                                              new DialogInterface.OnClickListener()
                                              {
                                                  @Override
                                                  public void onClick(DialogInterface dialog, int which)
                                                  {
                                                      cancel_outgoing_filetransfer(message);
                                                  }
                                              });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                else
                {
                }
                return true;
            }
        });


        if (is_image)
        {
            ft_preview_image.setImageResource(R.drawable.round_loading_animation);

            ft_preview_image.setOnTouchListener(new View.OnTouchListener()
            {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    if (event.getAction() == MotionEvent.ACTION_UP)
                    {
                        try
                        {
                            if (message.storage_frame_work)
                            {
                                Uri uri = Uri.parse(message.filename_fullpath);
                                DocumentFile documentFile = DocumentFile.fromSingleUri(context, uri);
                                String fileName = documentFile.getName();

                                Intent intent = new Intent(v.getContext(), ImageviewerActivity_SD.class);
                                intent.putExtra("image_filename", uri.toString());
                                intent.putExtra("storage_frame_work", "1");
                                v.getContext().startActivity(intent);
                            }
                            else
                            {
                                Intent intent = new Intent(v.getContext(), ImageviewerActivity_SD.class);
                                intent.putExtra("image_filename", message2.filename_fullpath);
                                v.getContext().startActivity(intent);
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            Log.i(TAG, "open_attachment_intent:EE:" + e.getMessage());
                        }
                    }
                    else
                    {
                    }
                    return true;
                }
            });


            if (message.storage_frame_work)
            {
                try
                {
                    final RequestOptions glide_options = new RequestOptions().fitCenter().optionalTransform(
                            new RoundedCorners((int) dp2px(20)));

                    GlideApp.
                            with(context).
                            load(Uri.parse(message.filename_fullpath)).
                            diskCacheStrategy(DiskCacheStrategy.RESOURCE).
                            skipMemoryCache(false).
                            priority(Priority.LOW).
                            placeholder(R.drawable.round_loading_animation).
                            into(ft_preview_image);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            else
            {
                java.io.File f2 = new java.io.File(message2.filename_fullpath);
                try
                {
                    final RequestOptions glide_options = new RequestOptions().fitCenter().optionalTransform(
                            new RoundedCorners((int) dp2px(20)));

                    GlideApp.
                            with(context).
                            load(f2).
                            diskCacheStrategy(DiskCacheStrategy.RESOURCE).
                            skipMemoryCache(false).
                            priority(Priority.LOW).
                            placeholder(R.drawable.round_loading_animation).
                            into(ft_preview_image);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        else
        {
            final Drawable d3 = new IconicsDrawable(this.context).
                    icon(GoogleMaterial.Icon.gmd_attachment).
                    backgroundColor(Color.TRANSPARENT).
                    color(Color.parseColor("#AA000000")).sizeDp(50);

            // ft_preview_image.setImageDrawable(d3);
            GlideApp.
                    with(context).
                    load(d3).
                    diskCacheStrategy(DiskCacheStrategy.NONE).
                    skipMemoryCache(false).
                    priority(Priority.LOW).
                    placeholder(R.drawable.round_loading_animation).
                    into(ft_preview_image);
        }

        HelperGeneric.set_avatar_img_height_in_chat(img_avatar);
    }

    private void cancel_outgoing_filetransfer(final Message message)
    {
        try
        {
            set_filetransfer_state_from_id(message.filetransfer_id, TOX_FILE_CONTROL_CANCEL.value);
            if (message.ft_outgoing_queued)
            {
                set_message_queueing_from_id(message.id, false);
            }
            else
            {
                // cancel FT
                Log.i(TAG, "button_cancel:OnTouch:001");
                int res = tox_file_control(tox_friend_by_public_key__wrapper(message.tox_friendpubkey),
                                           get_filetransfer_filenum_from_id(message.filetransfer_id),
                                           TOX_FILE_CONTROL_CANCEL.value);
                Log.i(TAG, "button_cancel:OnTouch:res=" + res);
            }

            set_message_state_from_id(message.id, TOX_FILE_CONTROL_CANCEL.value);

            // TODO: cleanup duplicated outgoing files from provider here ************
            remove_ft_from_cache(message);

            button_ok.setVisibility(View.GONE);
            button_cancel.setVisibility(View.GONE);
            ft_progressbar.setVisibility(View.GONE);

            // update message view
            update_single_message_from_messge_id(message.id, true);
            Log.i(TAG, "button_cancel:OnTouch:099");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "button_cancel:OnTouch:EE:" + e.getMessage());
        }
    }

    @Override
    public void onClick(View v)
    {
        Log.i(TAG, "onClick");
        try
        {
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "onClick:EE:" + e.getMessage());
        }
    }

    @Override
    public boolean onLongClick(final View v)
    {
        Log.i(TAG, "onLongClick");

        // final Message m2 = this.message;

        //        PopupMenu menu = new PopupMenu(v.getContext(), v);
        //        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        //        {
        //            @Override
        //            public boolean onMenuItemClick(MenuItem item)
        //            {
        //                int id = item.getItemId();
        //                return true;
        //            }
        //        });
        //        menu.inflate(R.menu.menu_friendlist_item);
        //        menu.show();

        return true;
    }
}
