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

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.zoffcc.applications.sorm.GroupPeerDB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;

import static com.zoffcc.applications.trifa.CameraWrapper.YUV420rotate90;
import static com.zoffcc.applications.trifa.HelperGeneric.display_toast;
import static com.zoffcc.applications.trifa.HelperGeneric.update_savedata_file_wrapper;
import static com.zoffcc.applications.trifa.HelperGroup.clear_group_group_we_left;
import static com.zoffcc.applications.trifa.HelperGroup.get_group_peernum_from_peer_pubkey;
import static com.zoffcc.applications.trifa.HelperGroup.is_group_we_left;
import static com.zoffcc.applications.trifa.HelperGroup.set_group_group_we_left;
import static com.zoffcc.applications.trifa.HelperGroup.tox_group_by_groupid__wrapper;
import static com.zoffcc.applications.trifa.HelperGroup.update_group_in_groupmessagelist;
import static com.zoffcc.applications.trifa.HelperGroup.update_group_peer_in_db;
import static com.zoffcc.applications.trifa.MainActivity.SD_CARD_ENC_FILES_EXPORT_DIR;
import static com.zoffcc.applications.trifa.MainActivity.context_s;
import static com.zoffcc.applications.trifa.MainActivity.main_handler_s;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_founder_set_peer_limit;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_founder_set_voice_state;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_get_name;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_get_peer_limit;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_get_voice_state;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_is_connected;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_mod_set_role;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_offline_peer_count;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_peer_count;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_peer_get_name;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_reconnect;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_savedpeer_get_public_key;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_self_get_peer_id;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_self_get_public_key;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_self_get_role;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_self_set_name;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NGC_NEW_PEERS_TIMEDELTA_IN_MS;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TRIFA_SYSTEM_MESSAGE_PEER_PUBKEY;
import static com.zoffcc.applications.trifa.ToxVars.GC_MAX_SAVED_PEERS;
import static com.zoffcc.applications.trifa.TrifaToxService.orma;

public class GroupInfoActivity extends AppCompatActivity
{
    static final String TAG = "trifa.GrpInfoActy";
    TextView this_group_id = null;
    EditText this_title = null;
    EditText group_myname_text = null;
    EditText peer_limit_text = null;
    TextView this_privacy_status_text = null;
    TextView group_connection_status_text = null;
    TextView group_myrole_text = null;
    TextView group_mypubkey_text = null;
    static TextView group_num_msgs_text = null;
    static TextView group_num_system_msgs_text = null;
    Button group_reconnect_button = null;
    Button group_dumpofflinepeers_button = null;
    Button group_del_sysmsgs_button = null;
    AppCompatSpinner group_voicestate_select = null;
    private AppCompatButton group_voicestate_set_button = null;
    private String[] tox_ngc_group_voicestate_items;
    String group_id = "-1";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groupinfo);

        Intent intent = getIntent();
        group_id = intent.getStringExtra("group_id");

        this_group_id = (TextView) findViewById(R.id.group_id_text);
        group_mypubkey_text = (TextView) findViewById(R.id.group_mypubkey_text);
        this_title = (EditText) findViewById(R.id.group_name_text);
        group_myname_text = (EditText) findViewById(R.id.group_myname_text);
        peer_limit_text = (EditText) findViewById(R.id.peer_limit_text);
        this_privacy_status_text = (TextView) findViewById(R.id.group_privacy_status_text);
        group_connection_status_text = (TextView) findViewById(R.id.group_connection_status_text);
        group_myrole_text = (TextView) findViewById(R.id.group_myrole_text);
        group_num_msgs_text = (TextView) findViewById(R.id.group_num_msgs_text);
        group_num_system_msgs_text = (TextView) findViewById(R.id.group_num_system_msgs_text);
        group_reconnect_button = (Button) findViewById(R.id.group_reconnect_button);
        group_dumpofflinepeers_button = (Button) findViewById(R.id.group_dumpofflinepeers_button);
        group_del_sysmsgs_button = (Button) findViewById(R.id.group_del_sysmsgs_button);
        group_voicestate_select = findViewById(R.id.group_voicestate_select);
        group_voicestate_set_button = findViewById(R.id.group_voicestate_set_button);

        this.tox_ngc_group_voicestate_items = new String[]{"---", "FOUNDER", "MODERATOR", "ALL"};
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                                                                tox_ngc_group_voicestate_items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        group_voicestate_select.setAdapter(adapter);

        int current_voicestate = 0;
        try
        {
            current_voicestate = tox_group_get_voice_state(tox_group_by_groupid__wrapper(group_id));
            Log.i(TAG, "current_voicestate:" + current_voicestate);
            if (current_voicestate == ToxVars.Tox_Group_Voice_State.TOX_GROUP_VOICE_STATE_FOUNDER.value)
            {
                group_voicestate_select.setSelection(1);
            }
            else if (current_voicestate == ToxVars.Tox_Group_Voice_State.TOX_GROUP_VOICE_STATE_MODERATOR.value)
            {
                group_voicestate_select.setSelection(2);
            }
            else if (current_voicestate == ToxVars.Tox_Group_Voice_State.TOX_GROUP_VOICE_STATE_ALL.value)
            {
                group_voicestate_select.setSelection(3);
            }
            else
            {
                // nothing valid selected
                group_voicestate_select.setSelection(0);
            }
        }
        catch(Exception e)
        {
        }

        group_voicestate_select.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                Log.i(TAG, "selected_new_voicestate:" + parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });

        group_voicestate_set_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                try
                {
                    String role_str = (String) group_voicestate_select.getSelectedItem();
                    int new_role = ToxVars.Tox_Group_Voice_State.TOX_GROUP_VOICE_STATE_ALL.value;
                    if (role_str.equals("FOUNDER"))
                    {
                        new_role = ToxVars.Tox_Group_Voice_State.TOX_GROUP_VOICE_STATE_FOUNDER.value;
                    }
                    else if (role_str.equals("MODERATOR"))
                    {
                        new_role = ToxVars.Tox_Group_Voice_State.TOX_GROUP_VOICE_STATE_MODERATOR.value;
                    }
                    else if (role_str.equals("ALL"))
                    {
                        new_role = ToxVars.Tox_Group_Voice_State.TOX_GROUP_VOICE_STATE_ALL.value;
                    }
                    else
                    {
                        // nothing valid selected
                        return;
                    }

                    int result = tox_group_founder_set_voice_state(tox_group_by_groupid__wrapper(group_id), new_role);
                    Log.i(TAG, "setting new voicestate to: " + new_role + " result=" + result);
                    update_savedata_file_wrapper();
                }
                catch (Exception ignored)
                {
                }
            }
        });

        try
        {
            group_reconnect_button.setVisibility(View.GONE);
        }
        catch(Exception ignored)
        {
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if ((group_id == null) || (group_id.equals("-1")))
        {
            this_group_id.setText("*error*");
        }
        else
        {
            this_group_id.setText(group_id.toLowerCase());
        }
        this_title.setText("*error*");

        long group_num = -1;

        try
        {
            group_num = tox_group_by_groupid__wrapper(group_id);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            peer_limit_text.setText("" + tox_group_get_peer_limit(group_num));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            group_myname_text.setText(tox_group_peer_get_name(group_num, tox_group_self_get_peer_id(group_num)));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            group_mypubkey_text.setText(tox_group_self_get_public_key(group_num));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            this_title.setText(orma.selectFromGroupDB().
                    group_identifierEq(group_id.toLowerCase()).
                    toList().get(0).name);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        String privacy_state_text = "Unknown Group Privacy State";

        try
        {
            final int privacy_state = orma.selectFromGroupDB().
                    group_identifierEq(group_id.toLowerCase()).
                    toList().get(0).privacy_state;

            if (privacy_state == ToxVars.TOX_GROUP_PRIVACY_STATE.TOX_GROUP_PRIVACY_STATE_PUBLIC.value)
            {
                privacy_state_text = "Public Group";
            }
            else if (privacy_state == ToxVars.TOX_GROUP_PRIVACY_STATE.TOX_GROUP_PRIVACY_STATE_PRIVATE.value)
            {
                privacy_state_text = "Private (Invitation only) Group";
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        this_privacy_status_text.setText(privacy_state_text);

        group_update_connected_status_on_groupinfo(group_num);

        final long group_num_ = group_num;
        group_reconnect_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    tox_group_reconnect(group_num_);
                    update_savedata_file_wrapper();
                    clear_group_group_we_left(group_id);
                    group_update_connected_status_on_groupinfo(group_num_);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });


        group_dumpofflinepeers_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    long offline_num_peers = tox_group_offline_peer_count(group_num_);

                    if (offline_num_peers > 0)
                    {
                        List<GroupMessageListActivity.group_list_peer> group_peers_offline = new ArrayList<>();
                        long i = 0;
                        for (i = 0; i < GC_MAX_SAVED_PEERS; i++)
                        {
                            try
                            {
                                String peer_pubkey_temp = tox_group_savedpeer_get_public_key(group_num_, i);
                                if (peer_pubkey_temp.compareToIgnoreCase("-1") == 0)
                                {
                                    continue;
                                }
                                String peer_name = "zzzzzoffline " + i;
                                GroupPeerDB peer_from_db = null;
                                try
                                {
                                    peer_from_db = (GroupPeerDB) orma.selectFromGroupPeerDB().group_identifierEq(
                                            group_id).tox_group_peer_pubkeyEq(peer_pubkey_temp).toList().get(0);
                                }
                                catch (Exception e)
                                {
                                }

                                String peerrole = "";

                                if (peer_from_db != null)
                                {
                                    peer_name = peer_from_db.peer_name;
                                    if ((peer_from_db.first_join_timestamp + NGC_NEW_PEERS_TIMEDELTA_IN_MS) >
                                        System.currentTimeMillis())
                                    {
                                        peer_name = "_NEW_ " + peer_name;
                                    }
                                    peerrole = ToxVars.Tox_Group_Role.value_char(peer_from_db.Tox_Group_Role) + " ";
                                }

                                // Log.i(TAG, "groupnum=" + conference_num + " peernum=" + offline_peers[(int) i] + " peer_name=" +
                                //           peer_name);
                                String peer_name_temp =  peerrole + peer_name + " :" + i + ": " + peer_pubkey_temp.substring(0, 6);

                                GroupMessageListActivity.group_list_peer glp3 = new GroupMessageListActivity.group_list_peer();
                                glp3.peer_pubkey = peer_pubkey_temp;
                                glp3.peer_num = i;
                                glp3.peer_name = peer_name_temp;
                                glp3.peer_connection_status = ToxVars.TOX_CONNECTION.TOX_CONNECTION_NONE.value;
                                group_peers_offline.add(glp3);
                            }
                            catch (Exception ignored)
                            {
                            }
                        }

                        try
                        {
                            Collections.sort(group_peers_offline, new Comparator<GroupMessageListActivity.group_list_peer>()
                            {
                                @Override
                                public int compare(GroupMessageListActivity.group_list_peer p1, GroupMessageListActivity.group_list_peer p2)
                                {
                                    String name1 = p1.peer_pubkey;
                                    String name2 = p2.peer_pubkey;
                                    return name1.compareToIgnoreCase(name2);
                                }
                            });
                        }
                        catch (Exception ignored)
                        {
                        }

                        StringBuilder logstr = new StringBuilder();
                        for (GroupMessageListActivity.group_list_peer peerloffline : group_peers_offline)
                        {
                            logstr.append(peerloffline.peer_pubkey).append(":").append(peerloffline.peer_num).append("\n");
                        }
                        Log.i(TAG, "\n\nNGC_GROUP_OFFLINE_PEERLIST:\n" + logstr + "\n\n");

                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        group_del_sysmsgs_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle("Delete System Messages");
                    builder.setMessage(
                            "Do you want to delete ALL system generated messages (like join/leave/exit messages) in this group permanently?");

                    builder.setPositiveButton("Yes, I want to delete them!", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            try
                            {
                                final Thread t2 = new Thread()
                                {
                                    @Override
                                    public void run()
                                    {
                                        try
                                        {
                                            Log.i(TAG, "del_group_system_messages:START:");
                                            display_toast("starting to delete, please wait ...", true, 0);
                                            orma.deleteFromGroupMessage().
                                                    group_identifierEq(group_id).
                                                    tox_group_peer_pubkeyEq(TRIFA_SYSTEM_MESSAGE_PEER_PUBKEY).
                                                    execute();
                                            Log.i(TAG, "del_group_system_messages:DONE:");
                                            display_toast("System Messages deleted", true, 0);
                                            reload_message_counts(group_id);
                                        }
                                        catch (Exception e2)
                                        {
                                            e2.printStackTrace();
                                            Log.i(TAG, "del_group_system_messages:EE:" + e2.getMessage());
                                        }
                                    }
                                };
                                t2.start();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    });
                    builder.setNegativeButton("Cancel", null);

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        try
        {
            final int myrole = tox_group_self_get_role(group_num);
            group_myrole_text.setText(ToxVars.Tox_Group_Role.value_str(myrole));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void group_update_connected_status_on_groupinfo(final long group_num)
    {
        try
        {
            final int is_connected = tox_group_is_connected(group_num);
            if (is_group_we_left(group_id))
            {
                group_connection_status_text.setText("You left the group, but can rejoin it");
                group_reconnect_button.setVisibility(View.VISIBLE);
            }
            else
            {
                group_connection_status_text.setText(TRIFAGlobals.TOX_GROUP_CONNECTION_STATUS.value_str(is_connected));
                if (is_connected == TRIFAGlobals.TOX_GROUP_CONNECTION_STATUS.TOX_GROUP_CONNECTION_STATUS_CONNECTED.value)
                {
                    group_reconnect_button.setVisibility(View.GONE);
                }
                else
                {
                    group_reconnect_button.setVisibility(View.VISIBLE);
                }
            }
        }
        catch(Exception ignored)
        {
        }
    }

    static void reload_message_counts(final String group_id)
    {
        try
        {
            Thread t = new Thread()
            {
                @Override
                public void run()
                {
                    String num_str1 = "*ERROR*";
                    String num_str2 = "*ERROR*";
                    try
                    {
                        num_str1 = "" + orma.selectFromGroupMessage().group_identifierEq(group_id).tox_group_peer_pubkeyNotEq(TRIFA_SYSTEM_MESSAGE_PEER_PUBKEY).count();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    try
                    {
                        num_str2 = "" + orma.selectFromGroupMessage().group_identifierEq(group_id).tox_group_peer_pubkeyEq(
                                TRIFA_SYSTEM_MESSAGE_PEER_PUBKEY).count();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    final String num_str_1 = num_str1;
                    final String num_str_2 = num_str2;

                    Runnable myRunnable = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                group_num_msgs_text.setText("Non System Messages: " + num_str_1);
                                group_num_system_msgs_text.setText("System Messages: " + num_str_2);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    };

                    if (main_handler_s != null)
                    {
                        main_handler_s.post(myRunnable);
                    }
                }
            };
            t.start();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        reload_message_counts(group_id);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        // TODO dirty hack, just write "conf title"

        try
        {
            String my_new_name = group_myname_text.getText().toString();
            if (my_new_name != null)
            {
                if (my_new_name.length() > 0)
                {
                    int res = tox_group_self_set_name(tox_group_by_groupid__wrapper(group_id),
                                                      my_new_name);
                    update_savedata_file_wrapper();
                }
            }
        }
        catch (Exception ignored)
        {
        }

        try
        {
            String new_peer_limit = peer_limit_text.getText().toString();
            if (new_peer_limit != null)
            {
                if (new_peer_limit.length() > 0)
                {

                    int res = tox_group_founder_set_peer_limit(tox_group_by_groupid__wrapper(group_id),
                                                      Integer.parseInt(new_peer_limit));
                    update_savedata_file_wrapper();
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }
}
