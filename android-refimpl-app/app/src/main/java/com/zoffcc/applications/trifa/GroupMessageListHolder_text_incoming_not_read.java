/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2022 Zoff <zoff@zoff.cc>
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
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.daimajia.swipe.SwipeLayout;
import com.luseen.autolinklibrary.AutoLinkMode;
import com.luseen.autolinklibrary.AutoLinkOnClickListener;
import com.luseen.autolinklibrary.EmojiTextViewLinks;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.zoffcc.applications.sorm.FriendList;
import com.zoffcc.applications.sorm.GroupMessage;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import static com.zoffcc.applications.trifa.GroupMessageListActivity.add_quote_group_message_text;
import static com.zoffcc.applications.trifa.GroupMessageListFragment.group_search_messages_text;
import static com.zoffcc.applications.trifa.HelperFriend.add_friend_real;
import static com.zoffcc.applications.trifa.HelperGeneric.darkenColor;
import static com.zoffcc.applications.trifa.HelperGeneric.dp2px;
import static com.zoffcc.applications.trifa.HelperGeneric.hash_to_bucket;
import static com.zoffcc.applications.trifa.HelperGeneric.isColorDarkBrightness;
import static com.zoffcc.applications.trifa.HelperGeneric.lightenColor;
import static com.zoffcc.applications.trifa.HelperGeneric.long_date_time_format;
import static com.zoffcc.applications.trifa.HelperGeneric.string_is_in_list;
import static com.zoffcc.applications.trifa.HelperGroup.tox_group_peer_get_name__wrapper;
import static com.zoffcc.applications.trifa.Identicon.bytesToHex;
import static com.zoffcc.applications.trifa.MainActivity.PREF__compact_chatlist;
import static com.zoffcc.applications.trifa.MainActivity.PREF__global_font_size;
import static com.zoffcc.applications.trifa.MainActivity.PREF__toxirc_muted_peers;
import static com.zoffcc.applications.trifa.MainActivity.selected_group_messages;
import static com.zoffcc.applications.trifa.TRIFAGlobals.CONFERENCE_CHAT_BG_CORNER_RADIUS_IN_PX;
import static com.zoffcc.applications.trifa.TRIFAGlobals.MESSAGES_TIMEDELTA_NO_TIMESTAMP_MS;
import static com.zoffcc.applications.trifa.TRIFAGlobals.MESSAGE_EMOJI_ONLY_EMOJI_SIZE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.MESSAGE_EMOJI_SIZE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.MESSAGE_TEXT_SIZE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TOXIRC_NGC_PUBKEY;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TOXIRC_TOKTOK_GROUPID;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TOXIRC_TOKTOK_IRC_USER_PUBKEY;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TOXURL_PATTERN;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TRIFA_SYSTEM_MESSAGE_PEER_CHATCOLOR;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TRIFA_SYSTEM_MESSAGE_PEER_PUBKEY;

public class GroupMessageListHolder_text_incoming_not_read extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener
{
    private static final String TAG = "trifa.MessageListHolder";

    private GroupMessage message_;
    private Context context;

    EmojiTextViewLinks textView;
    ImageView imageView;
    de.hdodenhof.circleimageview.CircleImageView img_avatar;
    TextView date_time;
    ViewGroup textView_container;
    ViewGroup layout_peer_name_container;
    TextView peer_name_text;
    ViewGroup layout_message_container;
    boolean is_selected = false;
    boolean is_system_message = false;
    ImageView img_corner;
    int swipe_state = 0;
    int swipe_state_done = 0;
    SwipeLayout swipeLayout = null;

    public GroupMessageListHolder_text_incoming_not_read(View itemView, Context c)
    {
        super(itemView);

        // Log.i(TAG, "MessageListHolder");

        this.context = c;

        textView_container = (ViewGroup) itemView.findViewById(R.id.m_container);
        textView = (EmojiTextViewLinks) itemView.findViewById(R.id.m_text);
        imageView = (ImageView) itemView.findViewById(R.id.m_icon);
        img_avatar = (de.hdodenhof.circleimageview.CircleImageView) itemView.findViewById(R.id.img_avatar);
        date_time = (TextView) itemView.findViewById(R.id.date_time);
        layout_peer_name_container = (ViewGroup) itemView.findViewById(R.id.layout_peer_name_container);
        peer_name_text = (TextView) itemView.findViewById(R.id.peer_name_text);
        layout_message_container = (ViewGroup) itemView.findViewById(R.id.layout_message_container);
        img_corner = (ImageView) itemView.findViewById(R.id.img_corner);

        swipe_state = 0;
        swipe_state_done = 0;

        swipeLayout = (SwipeLayout) itemView.findViewById(R.id.msg_swipe_container);
        swipeLayout.setShowMode(SwipeLayout.ShowMode.PullOut);
    }

    public void bindMessageList(GroupMessage m)
    {
        message_ = m;

        String message__text = m.text;

        if (m.private_message == 1)
        {
            message__text = "Private Message:\n" + m.text;
        }

        if (m.tox_group_peer_role == ToxVars.Tox_Group_Role.TOX_GROUP_ROLE_FOUNDER.value)
        {
            // TODO: make something nice looking here // message__text = "*Founder Message*\n" + message__text;
        }
        else if (m.tox_group_peer_role == ToxVars.Tox_Group_Role.TOX_GROUP_ROLE_MODERATOR.value)
        {
            // TODO: make something nice looking here // message__text = "+Moderator Message+\n" + message__text;
        }

        String message__tox_peername = m.tox_group_peername;
        String message__tox_peerpubkey = m.tox_group_peer_pubkey;

        /*
        try
        {
            message__text = message__text + "\npeerid=" +
                            tox_group_peer_by_public_key(tox_group_by_groupid__wrapper(m.group_identifier),
                                                         m.tox_group_peer_pubkey);
        }
        catch (Exception e)
        {
        }
        */

        boolean handle_special_name = false;

        name_test_pk res = correct_pubkey(m);
        if (res.changed)
        {
            try
            {
                message__tox_peername = res.tox_peername;
                peer_name_text.setText(message__tox_peername);
                message__text = res.text;
                if (m.private_message == 1)
                {
                    message__text = "Private Message:\n" + res.text;
                }
                message__tox_peerpubkey = res.tox_peerpubkey;
                handle_special_name = true;
            }
            catch (Exception e)
            {
            }
        }

        swipeLayout.addSwipeListener(new SwipeLayout.SwipeListener()
        {
            @Override
            public void onClose(SwipeLayout layout)
            {
                // when the SurfaceView totally cover the BottomView.
                // Log.i(TAG, "onClose: state=");
            }

            @Override
            public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset)
            {
                // you are swiping.
                // Log.i(TAG, "onUpdate: " + leftOffset + " " + topOffset);
                if (leftOffset > 60)
                {
                    swipeLayout.close(true);
                    // Log.i(TAG, "onUpdate: --> close");
                    if (swipe_state == 0)
                    {
                        swipe_state = 1;
                    }
                }
                else if (leftOffset == 0)
                {
                    if (swipe_state == 1)
                    {
                        swipe_state = 0;
                        Log.i(TAG, "onUpdate: --> QUOTE");
                        add_quote_group_message_text(message_.text);
                    }
                }
            }

            @Override
            public void onStartOpen(SwipeLayout layout)
            {
                // Log.i(TAG, "onStartOpen");
            }

            @Override
            public void onOpen(SwipeLayout layout)
            {
                // when the BottomView totally show.
                // Log.i(TAG, "onOpen");
                swipeLayout.close(true);
            }

            @Override
            public void onStartClose(SwipeLayout layout)
            {
                // Log.i(TAG, "onStartClose");
            }

            @Override
            public void onHandRelease(SwipeLayout layout, float xvel, float yvel)
            {
                // when user's hand released.
                // Log.i(TAG, "onHandRelease");
                swipeLayout.close(true);
            }
        });

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SIZE[PREF__global_font_size]);

        // Log.i(TAG, "have_avatar_for_pubkey:0000:==========================");

        is_system_message = message__tox_peerpubkey.equals(TRIFA_SYSTEM_MESSAGE_PEER_PUBKEY);
        // Log.i(TAG, "is_system_message=" + is_system_message + " message__tox_peerpubkey=" + message__tox_peerpubkey);

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

        layout_message_container.setOnClickListener(onclick_listener);
        layout_message_container.setOnLongClickListener(onlongclick_listener);

        textView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                layout_message_container.performClick();
            }
        });

        textView.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view)
            {
                layout_message_container.performLongClick();
                return true;
            }
        });

        // Log.i(TAG, "bindMessageList");

        // textView.setText("#" + m.id + ":" + message__text);
        textView.setCustomRegex(TOXURL_PATTERN);
        textView.addAutoLinkMode(AutoLinkMode.MODE_URL, AutoLinkMode.MODE_EMAIL, AutoLinkMode.MODE_HASHTAG,
                                 AutoLinkMode.MODE_MENTION, AutoLinkMode.MODE_CUSTOM);

        try
        {
            String peer_name = tox_group_peer_get_name__wrapper(m.group_identifier, message__tox_peerpubkey);

            if (peer_name == null)
            {
                peer_name = message__tox_peername;

                if ((peer_name == null) || (message__tox_peername.equals("")) || (peer_name.equals("-1")))
                {
                    peer_name = "Unknown";
                }
            }
            else
            {
                if (peer_name.equals("-1"))
                {
                    if ((message__tox_peername == null) || (message__tox_peername.equals("")))
                    {
                        peer_name = "Unknown";
                    }
                    else
                    {
                        peer_name = message__tox_peername;
                    }
                }
            }

            layout_peer_name_container.setVisibility(View.VISIBLE);
            try
            {
                if (message__tox_peerpubkey.compareTo("-1") == 0)
                {
                    peer_name_text.setText("-system-");
                }
                else
                {
                    peer_name_text.setText(peer_name + " / " +
                                           message__tox_peerpubkey.substring((message__tox_peerpubkey.length() - 6),
                                                                             message__tox_peerpubkey.length()));
                }
            }
            catch (Exception e2)
            {
                e2.printStackTrace();
                Log.i(TAG, "bindMessageList:EE2:" + e2.getMessage());

                peer_name_text.setText(peer_name);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "bindMessageList:EE:" + e.getMessage());
        }

        //        textView.setAutoLinkText("" + message__tox_peerpubkey.substring((message__tox_peerpubkey.length() - 6),
        //                //
        //                message__tox_peerpubkey.length())
        //                //
        //                + ":" + message__text);

        if (com.vanniktech.emoji.EmojiUtils.isOnlyEmojis(message__text))
        {
            // text consits only of emojis -> increase size
            textView.setEmojiSize((int) dp2px(MESSAGE_EMOJI_ONLY_EMOJI_SIZE[PREF__global_font_size]));
        }
        else
        {
            textView.setEmojiSize((int) dp2px(MESSAGE_EMOJI_SIZE[PREF__global_font_size]));
        }

        int peer_color_fg = context.getResources().getColor(R.color.colorPrimaryDark);
        int peer_color_bg = context.getResources().getColor(R.color.material_drawer_background);
        int alpha_value = 160;
        // int peer_color_bg_with_alpha = (peer_color_bg & 0x00FFFFFF) | (alpha_value << 24);

        final int linkcolor = lightenColor(Color.BLUE, 0.3f);
        textView.setMentionModeColor(linkcolor);
        textView.setHashtagModeColor(linkcolor);
        textView.setUrlModeColor(linkcolor);
        textView.setPhoneModeColor(linkcolor);
        textView.setEmailModeColor(linkcolor);
        textView.setCustomModeColor(linkcolor);
        textView.setLinkTextColor(linkcolor);
        //
        textView.setTextColor(Color.BLACK);

        try
        {
            if (message__tox_peerpubkey.compareTo("-1") == 0)
            {
                peer_color_bg = TRIFA_SYSTEM_MESSAGE_PEER_CHATCOLOR;
            }
            else
            {
                peer_color_bg = ChatColors.get_shade(
                        ChatColors.PeerAvatarColors[hash_to_bucket(message__tox_peerpubkey, ChatColors.get_size())],
                        message__tox_peerpubkey);
            }
            // peer_color_bg_with_alpha = (peer_color_bg & 0x00FFFFFF) | (alpha_value << 24);
            textView.setTextColor(Color.BLACK);

            if (isColorDarkBrightness(peer_color_bg))
            {
                textView.setTextColor(darkenColor(Color.WHITE, 0.1f));
                //
                final int linkcolor_for_other_bg = darkenColor(Color.YELLOW, 0.1f);
                textView.setMentionModeColor(linkcolor_for_other_bg);
                textView.setHashtagModeColor(linkcolor_for_other_bg);
                textView.setUrlModeColor(linkcolor_for_other_bg);
                textView.setPhoneModeColor(linkcolor_for_other_bg);
                textView.setEmailModeColor(linkcolor_for_other_bg);
                textView.setCustomModeColor(linkcolor_for_other_bg);
                textView.setLinkTextColor(linkcolor_for_other_bg);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if ((group_search_messages_text == null) || (group_search_messages_text.length() == 0))
        {
            textView.setAutoLinkText(message__text);
        }
        else
        {
            textView.setAutoLinkTextHighlight(message__text, group_search_messages_text);
        }

        date_time.setText(long_date_time_format(m.sent_timestamp));

        textView.setAutoLinkOnClickListener(new AutoLinkOnClickListener()
        {
            @Override
            public void onAutoLinkTextClick(AutoLinkMode autoLinkMode, String matchedText)
            {
                if (autoLinkMode == AutoLinkMode.MODE_URL)
                {
                    showDialog_url(context, "open URL?", matchedText.replaceFirst("^\\s", ""));
                }
                else if (autoLinkMode == AutoLinkMode.MODE_EMAIL)
                {
                    showDialog_email(context, "send Email?", matchedText.replaceFirst("^\\s", ""));
                }
                else if (autoLinkMode == AutoLinkMode.MODE_MENTION)
                {
                    showDialog_url(context, "open URL?", "https://twitter.com/" +
                                                         matchedText.replaceFirst("^\\s", "").replaceFirst("^@", ""));
                }
                else if (autoLinkMode == AutoLinkMode.MODE_HASHTAG)
                {
                    showDialog_url(context, "open URL?", "https://twitter.com/hashtag/" +
                                                         matchedText.replaceFirst("^\\s", "").replaceFirst("^#", ""));
                }
                else if (autoLinkMode == AutoLinkMode.MODE_CUSTOM) // tox: urls
                {
                    showDialog_tox(context, "add ToxID?", matchedText.replaceFirst("^\\s", ""));
                }
            }
        });

        boolean have_avatar_for_pubkey = false;
        FriendList fl_temp = null;

        // we need to do the rounded corner background manually here, to change the color ---------------
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadii(
                new float[]{CONFERENCE_CHAT_BG_CORNER_RADIUS_IN_PX, CONFERENCE_CHAT_BG_CORNER_RADIUS_IN_PX, CONFERENCE_CHAT_BG_CORNER_RADIUS_IN_PX, CONFERENCE_CHAT_BG_CORNER_RADIUS_IN_PX, CONFERENCE_CHAT_BG_CORNER_RADIUS_IN_PX, CONFERENCE_CHAT_BG_CORNER_RADIUS_IN_PX, CONFERENCE_CHAT_BG_CORNER_RADIUS_IN_PX, CONFERENCE_CHAT_BG_CORNER_RADIUS_IN_PX});
        shape.setColor(peer_color_bg);
        // shape.setStroke(3, borderColor);
        textView_container.setBackground(shape);
        // we need to do the rounded corner background manually here, to change the color ---------------

        final Drawable smiley_face = new IconicsDrawable(context).
                icon(GoogleMaterial.Icon.gmd_sentiment_satisfied).
                backgroundColor(peer_color_bg).
                color(peer_color_fg).sizeDp(70);

        img_corner.setVisibility(View.GONE);
        date_time.setVisibility(View.VISIBLE);

        if (is_system_message)
        {
            img_avatar.setVisibility(View.GONE);
            img_corner.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
            textView_container.setMinimumHeight(4);
            textView_container.setPadding((int) dp2px(4), textView_container.getPaddingTop(), (int) dp2px(4),
                                          textView_container.getPaddingBottom()); // left, top, right, bottom
            LinearLayout.LayoutParams parameter = (LinearLayout.LayoutParams) textView_container.getLayoutParams();
            parameter.setMargins((int) dp2px(20), parameter.topMargin, parameter.rightMargin,
                                 parameter.bottomMargin); // left, top, right, bottom
            textView_container.setLayoutParams(parameter);
            // peer_name_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);

            // -------------------------------
            // make text smaller for system messages
            int system_font_size_used = MESSAGE_TEXT_SIZE[PREF__global_font_size] - 5;
            if (system_font_size_used < 9)
            {
                system_font_size_used = 9;
            }
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, system_font_size_used);
            // -------------------------------
        }
        else
        {

            // TODO: do we need to reset here? -> yes
            img_avatar.setVisibility(View.VISIBLE);
            if (PREF__compact_chatlist)
            {
                img_corner.setVisibility(View.GONE);
            }
            else
            {
                img_corner.setVisibility(View.VISIBLE);
            }
            imageView.setVisibility(View.VISIBLE);
            textView_container.setMinimumHeight((int) dp2px(0));
            textView_container.setPadding(0, textView_container.getPaddingTop(), 0,
                                          textView_container.getPaddingBottom()); // left, top, right, bottom
            LinearLayout.LayoutParams parameter = (LinearLayout.LayoutParams) textView_container.getLayoutParams();
            parameter.setMargins(0, parameter.topMargin, parameter.rightMargin,
                                 parameter.bottomMargin); // left, top, right, bottom
            textView_container.setLayoutParams(parameter);
            // peer_name_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

            img_avatar.setImageDrawable(smiley_face);

            img_avatar.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(final View v)
                {
                    if (!is_selected)
                    {
                        Intent intent = new Intent(v.getContext(),
                                                   GroupPeerInfoActivity.class);
                        intent.putExtra("peer_pubkey", m.tox_group_peer_pubkey);
                        intent.putExtra("group_id", m.group_identifier);
                        v.getContext().startActivity(intent);
                    }
                }
            });

            if (m.was_synced)
            {
                try
                {
                    if (m.TRIFA_SYNC_TYPE == TRIFAGlobals.TRIFA_SYNC_TYPE.TRIFA_SYNC_TYPE_NGC_PEERS.value)
                    {
                        imageView.setImageResource(R.drawable.circle_pink);

                        if (m.sync_confirmations > 0)
                        {
                            String confirmations_text = "" + m.sync_confirmations;
                            if (m.sync_confirmations > 9)
                            {
                                confirmations_text = "+";
                            }

                            final TextDrawable drawable2 = TextDrawable.builder().beginConfig().textColor(Color.WHITE).
                                    bold().width(60).height(60).fontSize(58).endConfig().
                                    buildRound(confirmations_text, Color.GRAY);
                            imageView.setImageDrawable(drawable2);
                        }
                    }
                    else
                    {
                        imageView.setImageResource(R.drawable.circle_orange);

                        if (m.sync_confirmations > 0)
                        {
                            String confirmations_text = "" + m.sync_confirmations;
                            if (m.sync_confirmations > 9)
                            {
                                confirmations_text = "+";
                            }

                            final TextDrawable drawable2 = TextDrawable.builder().beginConfig().textColor(Color.BLACK).
                                    bold().width(60).height(60).fontSize(58).endConfig().
                                    buildRound(confirmations_text, Color.parseColor("#ffce00"));
                            imageView.setImageDrawable(drawable2);
                        }
                    }
                }
                catch(Exception e3)
                {
                    imageView.setImageResource(R.drawable.circle_orange);
                }
            }
            else
            {
                // received directly
                imageView.setImageResource(R.drawable.circle_green);
            }

        }

        // --------- peer name (show only if different from previous message) ---------
        // --------- peer name (show only if different from previous message) ---------
        // --------- peer name (show only if different from previous message) ---------
        peer_name_text.setVisibility(View.GONE);
        int my_position = this.getAdapterPosition();
        if (my_position != RecyclerView.NO_POSITION)
        {
            try
            {
                if (MainActivity.group_message_list_fragment.adapter != null)
                {
                    if (my_position < 1)
                    {
                        peer_name_text.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        name_test_pk res2 = correct_pubkey(
                                MainActivity.group_message_list_fragment.adapter.get_item(my_position));

                        name_test_pk res3 = correct_pubkey(
                                MainActivity.group_message_list_fragment.adapter.get_item(my_position - 1));

                        String peer_cur = null;
                        String peer_prev = null;

                        if (res2.changed)
                        {
                            peer_cur = res2.tox_peerpubkey;
                        }
                        else
                        {
                            peer_cur = MainActivity.group_message_list_fragment.adapter.get_item(
                                    my_position).tox_group_peer_pubkey;
                        }

                        if (res3.changed)
                        {
                            peer_prev = res3.tox_peerpubkey;
                        }
                        else
                        {
                            peer_prev = MainActivity.group_message_list_fragment.adapter.get_item(
                                    my_position - 1).tox_group_peer_pubkey;
                        }


                        if ((peer_cur == null) || (peer_prev == null))
                        {
                            peer_name_text.setVisibility(View.VISIBLE);
                        }
                        else if (!peer_cur.equals(peer_prev))
                        {
                            peer_name_text.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
            catch (Exception e)
            {
            }
        }
        // --------- peer name (show only if different from previous message) ---------
        // --------- peer name (show only if different from previous message) ---------
        // --------- peer name (show only if different from previous message) ---------

        // --------- timestamp (show only if different from previous message) ---------
        // --------- timestamp (show only if different from previous message) ---------
        // --------- timestamp (show only if different from previous message) ---------
        date_time.setVisibility(View.GONE);
        if (my_position != RecyclerView.NO_POSITION)
        {
            try
            {
                if (MainActivity.group_message_list_fragment.adapter != null)
                {
                    if (my_position < 1)
                    {
                        date_time.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        final GroupMessagelistAdapter.DateTime_in_out peer_cur = MainActivity.group_message_list_fragment.adapter.getDateTime(
                                my_position);
                        final GroupMessagelistAdapter.DateTime_in_out peer_prev = MainActivity.group_message_list_fragment.adapter.getDateTime(
                                my_position - 1);
                        if ((peer_cur == null) || (peer_prev == null))
                        {
                            date_time.setVisibility(View.VISIBLE);
                        }
                        // else if (peer_cur.direction != peer_prev.direction)
                        // {
                        //     date_time.setVisibility(View.VISIBLE);
                        // }
                        // else if (!peer_cur.pk.equals(peer_prev.pk))
                        // {
                        //     date_time.setVisibility(View.VISIBLE);
                        // }
                        else
                        {
                            // if message is within 20 seconds of previous message and same direction and same peer
                            // then do not show timestamp
                            if (peer_cur.timestamp > peer_prev.timestamp + (MESSAGES_TIMEDELTA_NO_TIMESTAMP_MS))
                            {
                                date_time.setVisibility(View.VISIBLE);
                            }
                        }

                    }
                }
            }
            catch (Exception e)
            {
            }
        }
        else
        {
        }
        // --------- timestamp (show only if different from previous message) ---------
        // --------- timestamp (show only if different from previous message) ---------
        // --------- timestamp (show only if different from previous message) ---------

        HelperGeneric.set_avatar_img_height_in_chat(img_avatar);
    }

    @Override
    public void onClick(View v)
    {
        // Log.i(TAG, "onClick");
    }

    @Override
    public boolean onLongClick(final View v)
    {
        // Log.i(TAG, "onLongClick");
        return true;
    }

    private void showDialog_url(final Context c, final String title, final String url1)
    {
        String url2 = url1;

        // check to see if protocol is specified in URL, otherwise add "http://"
        if (!url2.contains("://"))
        {
            url2 = "http://" + url1;
        }
        final String url = url2;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setMessage(url).setTitle(title).
                setCancelable(false).
                setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        try
                        {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            c.startActivity(intent);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
            }
        });

        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void showDialog_email(final Context c, final String title, final String email_addr)
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setMessage(email_addr).setTitle(title).
                setCancelable(false).
                setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        try
                        {
                            Intent emailIntent = new Intent(Intent.ACTION_SENDTO,
                                                            Uri.fromParts("mailto", email_addr, null));
                            emailIntent.setType("message/rfc822");
                            // emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
                            // emailIntent.putExtra(Intent.EXTRA_TEXT, "Body");
                            c.startActivity(Intent.createChooser(emailIntent, "Send email..."));
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
            }
        });

        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void showDialog_tox(final Context c, final String title, final String toxid)
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setMessage(toxid.toUpperCase()).setTitle(title).
                setCancelable(false).
                setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        try
                        {
                            String friend_tox_id = toxid.toUpperCase().replace(" ", "").replaceFirst("tox:",
                                                                                                     "").replaceFirst(
                                    "TOX:", "").replaceFirst("Tox:", "");
                            add_friend_real(friend_tox_id);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
            }
        });

        final AlertDialog alert = builder.create();
        alert.show();
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
            GroupMessageListActivity.long_click_message_return res = GroupMessageListActivity.onLongClick_message_helper(
                    context, v, is_selected, message_);
            is_selected = res.is_selected;
            return res.ret_value;
        }
    };

    class name_test_pk
    {
        boolean changed;
        String tox_peername;
        String text;
        String tox_peerpubkey;
    }

    name_test_pk correct_pubkey(GroupMessage m)
    {
        name_test_pk ret = new name_test_pk();
        ret.changed = false;

        try
        {
            if (m.group_identifier.equals(TOXIRC_TOKTOK_GROUPID))
            {
                try
                {
                    if (m.tox_group_peer_pubkey.equals(TOXIRC_NGC_PUBKEY))
                    {
                        // toxirc messages will be displayed in a special way
                        if (m.text.length() > (3 + 1))
                        {
                            if (m.text.startsWith("<"))
                            {
                                int start_pos = m.text.indexOf("<");
                                int end_pos = m.text.indexOf("> ");

                                if ((start_pos > -1) && (end_pos > -1) && (end_pos > start_pos))
                                {
                                    try
                                    {
                                        String peer_name_corrected = m.text.substring(start_pos + 1, end_pos);

                                        ret.tox_peername = peer_name_corrected;

                                        if (string_is_in_list(peer_name_corrected, PREF__toxirc_muted_peers))
                                        {
                                            ret.text = "** muted **";
                                        }
                                        else
                                        {
                                            ret.text = m.text.substring(end_pos + 2);
                                        }

                                        String new_fake_pubkey = bytesToHex(TrifaSetPatternActivity.sha256(
                                                TrifaSetPatternActivity.StringToBytes2(
                                                        m.tox_group_peer_pubkey + "--" + peer_name_corrected)));

                                        new_fake_pubkey = new_fake_pubkey.substring(1, new_fake_pubkey.length() - 2);
                                        ret.tox_peerpubkey = new_fake_pubkey;
                                        ret.changed = true;
                                    }
                                    catch (Exception e)
                                    {
                                    }
                                }
                            }
                        }
                    }
                }
                catch (Exception ignored)
                {
                }

                try
                {
                    if (ret.tox_peerpubkey.equals(TOXIRC_TOKTOK_IRC_USER_PUBKEY) && ret.changed)
                    {
                        // toktok irc messages will be displayed in a special way
                        if (ret.text.length() > (3 + 1))
                        {
                            if (ret.text.startsWith("<"))
                            {
                                int start_pos = ret.text.indexOf("<");
                                int end_pos = ret.text.indexOf("> ");

                                if ((start_pos > -1) && (end_pos > -1) && (end_pos > start_pos))
                                {
                                    try
                                    {
                                        String peer_name_corrected = ret.text.substring(start_pos + 1, end_pos);

                                        ret.tox_peername = peer_name_corrected;

                                        if (string_is_in_list(peer_name_corrected, PREF__toxirc_muted_peers))
                                        {
                                            ret.text = "** muted **";
                                        }
                                        else
                                        {
                                            ret.text = ret.text.substring(end_pos + 2);
                                        }

                                        String new_fake_pubkey = bytesToHex(TrifaSetPatternActivity.sha256(
                                                TrifaSetPatternActivity.StringToBytes2(
                                                        ret.tox_peerpubkey + "--" + peer_name_corrected)));

                                        new_fake_pubkey = new_fake_pubkey.substring(1, new_fake_pubkey.length() - 2);
                                        ret.tox_peerpubkey = new_fake_pubkey;
                                        ret.changed = true;
                                    }
                                    catch (Exception e)
                                    {
                                    }
                                }
                            }
                        }
                    }
                }
                catch (Exception ignored)
                {
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return ret;
    }
}
