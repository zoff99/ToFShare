package com.zoffcc.applications.trifa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
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
import com.luseen.autolinklibrary.EmojiTextViewLinks;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.zoffcc.applications.sorm.GroupMessage;

import java.net.URLConnection;

import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import static com.zoffcc.applications.trifa.HelperGeneric.dp2px;
import static com.zoffcc.applications.trifa.HelperGeneric.long_date_time_format;
import static com.zoffcc.applications.trifa.MainActivity.PREF__compact_chatlist;
import static com.zoffcc.applications.trifa.MainActivity.PREF__global_font_size;
import static com.zoffcc.applications.trifa.MainActivity.selected_group_messages;
import static com.zoffcc.applications.trifa.TRIFAGlobals.MESSAGE_TEXT_SIZE;

public class GroupMessageListHolder_file_outgoing_state_cancel extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener
{
    private static final String TAG = "trifa.MessageListHolder";

    private GroupMessage message_;
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
    ViewGroup layout_message_container;
    ViewGroup rounded_bg_container;
    boolean is_selected = false;
    TextView message_text_date_string;
    ViewGroup message_text_date;
    me.jagar.chatvoiceplayerlibrary.VoicePlayerView ft_audio_player;

    public GroupMessageListHolder_file_outgoing_state_cancel(View itemView, Context c)
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
        layout_message_container = (ViewGroup) itemView.findViewById(R.id.layout_message_container);
        message_text_date_string = (TextView) itemView.findViewById(R.id.message_text_date_string);
        message_text_date = (ViewGroup) itemView.findViewById(R.id.message_text_date);
        ft_audio_player = itemView.findViewById(R.id.ft_audio_player);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void bindMessageList(GroupMessage m)
    {
        // Log.i(TAG, "bindMessageList");

        if (m == null)
        {
            // TODO: should never be null!!
            // only afer a crash
            m = new GroupMessage();
        }

        message_ = m;

        ft_audio_player.setVisibility(View.GONE);
        ft_preview_image.getLayoutParams().height = (int)dp2px(150);

        int drawable_id = R.drawable.rounded_blue_bg;
        try
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

        is_selected = false;
        if (selected_group_messages.isEmpty())
        {
            is_selected = false;
        }
        else
        {
            is_selected = selected_group_messages.contains(m.id);
        }

        if (is_selected)
        {
            layout_message_container.setBackgroundColor(Color.GRAY);
        }
        else
        {
            layout_message_container.setBackgroundColor(Color.TRANSPARENT);
        }

        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);

        layout_message_container.setOnClickListener(onclick_listener);
        layout_message_container.setOnLongClickListener(onlongclick_listener);

        date_time.setText(long_date_time_format(m.sent_timestamp));

        textView.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.VISIBLE);

        final GroupMessage message = m;

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SIZE[PREF__global_font_size]);

        // TODO: show preview and "click" to open/delete file
        textView.setAutoLinkText("" + message.text + "\n OK");

        boolean is_image = false;
        boolean is_video = false;
        boolean is_audio = false;
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

            if (mimeType.startsWith("audio/"))
            {
                is_audio = true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (!is_image)
        {
            try
            {
                String mimeType = URLConnection.guessContentTypeFromName(message.filename_fullpath.toLowerCase());
                if (mimeType.startsWith("video/"))
                {
                    is_video = true;
                }
            }
            catch (Exception e)
            {
                // e.printStackTrace();
            }
        }

        // set default image
        ft_preview_image.setImageResource(R.drawable.round_loading_animation);

        if (is_image)
        {
            ft_preview_image.setImageResource(R.drawable.round_loading_animation);

            if (PREF__compact_chatlist)
            {
                textView.setVisibility(View.GONE);
                imageView.setVisibility(View.GONE);
            }
            else
            {
                textView.setVisibility(View.VISIBLE);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SIZE[PREF__global_font_size]);
            }

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
                                intent.putExtra("image_filename", message.filename_fullpath);
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
                java.io.File f2 = new java.io.File(message.filename_fullpath);
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
        else if (is_audio) // ---- an audio file ----
        {
            if (PREF__compact_chatlist)
            {
                textView.setVisibility(View.GONE);
                imageView.setVisibility(View.GONE);
            }

            ft_progressbar.setVisibility(View.GONE);
            ft_buttons_container.setVisibility(View.GONE);
            button_ok.setVisibility(View.GONE);
            button_cancel.setVisibility(View.GONE);

            ft_preview_container.setVisibility(View.VISIBLE);
            ft_preview_image.setVisibility(View.GONE);
            ft_preview_image.getLayoutParams().height = 2;

            ft_audio_player.setVisibility(View.VISIBLE);
            ft_audio_player.refreshPlayer(message.filename_fullpath);
            ft_audio_player.refreshVisualizer();
        }
        else if (is_video)  // ---- a video ----
        {
            final Drawable d4 = new IconicsDrawable(context).
                    icon(GoogleMaterial.Icon.gmd_ondemand_video).
                    backgroundColor(Color.TRANSPARENT).
                    color(Color.parseColor("#AA000000")).sizeDp(50);

            ft_preview_image.setImageDrawable(d4);
        }
        else // ---- not an image or a video or an audio ----
        {
            final Drawable d3 = new IconicsDrawable(this.context).
                    icon(GoogleMaterial.Icon.gmd_attachment).
                    backgroundColor(Color.TRANSPARENT).
                    color(Color.parseColor("#AA000000")).sizeDp(50);

            ft_preview_image.setImageDrawable(d3);
        }

        ft_preview_container.setVisibility(View.VISIBLE);
        ft_preview_image.setVisibility(View.VISIBLE);
        ft_progressbar.setVisibility(View.GONE);
        ft_buttons_container.setVisibility(View.GONE);

        HelperGeneric.fill_own_avatar_icon(context, img_avatar);
        HelperGeneric.set_avatar_img_height_in_chat(img_avatar);
    }

    @Override
    public void onClick(View v)
    {
        // Log.i(TAG, "onClick");
    }

    void DetachedFromWindow(boolean release)
    {
        ft_audio_player.onPause();
        if (release)
        {
            ft_audio_player.onStop();
        }
    }

    @Override
    public boolean onLongClick(final View v)
    {
        // Log.i(TAG, "onLongClick");
        return true;
    }

    private View.OnClickListener onclick_listener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            is_selected = GroupMessageListActivity.onClick_message_helper(v, is_selected, message_);
        }
    };

    private View.OnLongClickListener onlongclick_listener = new View.OnLongClickListener()
    {
        @Override
        public boolean onLongClick(final View v)
        {
            GroupMessageListActivity.long_click_message_return res =
                    GroupMessageListActivity.onLongClick_message_helper(context, v, is_selected,
                                                                                           message_);
            is_selected = res.is_selected;
            return res.ret_value;
        }
    };
}
