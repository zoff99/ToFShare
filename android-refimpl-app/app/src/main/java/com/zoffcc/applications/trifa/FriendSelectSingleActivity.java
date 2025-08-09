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

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.zoffcc.applications.sorm.FriendList;
import com.zoffcc.applications.sorm.GroupDB;

import java.security.acl.Group;
import java.util.ArrayList;
import java.util.List;

import static com.zoffcc.applications.trifa.TrifaToxService.orma;

public class FriendSelectSingleActivity extends ListActivity
{
    private static final String TAG = "trifa.FrndSelSingleActy";
    List<FriendSelectSingle> friends_list;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate:001");

        Bundle extras = getIntent().getExtras();
        int also_offline_friends = 0;
        if (extras != null)
        {
            also_offline_friends = extras.getInt("offline", 0);
        }

        int also_ngc_groups = 0;
        if (extras != null)
        {
            also_ngc_groups = extras.getInt("ngc_groups", 0);
        }
        Log.i(TAG, "onCreate:also_ngc_groups=" + also_ngc_groups);

        List<com.zoffcc.applications.sorm.FriendList> fl = null;
        try
        {
            if (also_offline_friends == 1)
            {
                fl = orma.selectFromFriendList().
                        is_relayNotEq(true).
                        orderByTOX_CONNECTION_on_offDesc().
                        orderByNotification_silentAsc().
                        orderByLast_online_timestampDesc().
                        toList();
            }
            else
            {
                fl = orma.selectFromFriendList().
                        is_relayNotEq(true).
                        TOX_CONNECTION_realNotEq(0).
                        orderByTOX_CONNECTION_on_offDesc().
                        orderByNotification_silentAsc().
                        orderByLast_online_timestampDesc().
                        toList();
            }

            if (also_ngc_groups == 0)
            {
                if (fl == null)
                {
                    this.finish();
                }

                if (fl.size() < 1)
                {
                    this.finish();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            if (also_ngc_groups == 0)
            {
                this.finish();
            }
        }

        List<com.zoffcc.applications.sorm.GroupDB> gr = null;
        if (also_ngc_groups == 1)
        {
            try
            {
                gr = orma.selectFromGroupDB().
                        orderByNotification_silentAsc().
                        orderByNameAsc().
                        toList();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        try
        {
            Log.i(TAG, "onCreate:002:fl.size()=" + fl.size());
        }
        catch (Exception ignored)
        {
        }

        try
        {
            Log.i(TAG, "onCreate:002:gr.size()=" + gr.size());
        }
        catch (Exception ignored)
        {
        }

        try
        {
            friends_list = new ArrayList<>();

            for (FriendList f : fl)
            {
                if (f.alias_name == null)
                {
                    friends_list.add(new FriendSelectSingle(f.name, f.tox_public_key_string));
                }
                else if (f.alias_name.length() < 1)
                {
                    friends_list.add(new FriendSelectSingle(f.name, f.tox_public_key_string));
                }
                else
                {
                    friends_list.add(new FriendSelectSingle(f.alias_name, f.tox_public_key_string));
                }
            }

            try
            {
                for (GroupDB g : gr)
                {
                    // Log.i(TAG, "g___:" + g);
                    friends_list.add(new FriendSelectSingle("NGC Group: " + g.name, g.group_identifier, 2));
                }
            }
            catch(Exception e)
            {
                Log.i(TAG, "onCreate:EE001:add:gr:" +e.getMessage());
            }

            FriendSelectSingleAdapter adapter = new FriendSelectSingleAdapter(this,
                                                                              R.layout.friend_select_single_single_list_item_view_custom,
                                                                              friends_list);
            this.setListAdapter(adapter);
            ListView lv = getListView();

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    Intent data = new Intent();
                    try
                    {
                        if (friends_list.get((int) id).getType() == 0)
                        {
                            String return_friend_pubkey = "0:" + friends_list.get((int) id).pubkey;
                            data.setData(Uri.parse(return_friend_pubkey));
                        }
                        else if (friends_list.get((int) id).getType() == 2)
                        {
                            String return_ngc_id = "2:" + friends_list.get((int) id).pubkey;
                            data.setData(Uri.parse(return_ngc_id));
                        }
                        setResult(RESULT_OK, data);
                    }
                    catch (Exception e3)
                    {
                        e3.printStackTrace();
                    }

                    finish();
                }
            });

            Log.i(TAG, "onCreate:009");

        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.finish();
        }

        Log.i(TAG, "onCreate:010");
    }
}
