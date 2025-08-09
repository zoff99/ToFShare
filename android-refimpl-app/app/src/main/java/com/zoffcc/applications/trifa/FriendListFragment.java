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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zoffcc.applications.sorm.ConferenceDB;
import com.zoffcc.applications.sorm.FriendList;
import com.zoffcc.applications.sorm.GroupDB;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import static com.zoffcc.applications.trifa.CombinedFriendsAndConferences.COMBINED_IS_CONFERENCE;
import static com.zoffcc.applications.trifa.CombinedFriendsAndConferences.COMBINED_IS_FRIEND;
import static com.zoffcc.applications.trifa.CombinedFriendsAndConferences.COMBINED_IS_GROUP;
import static com.zoffcc.applications.trifa.MainActivity.main_handler_s;
import static com.zoffcc.applications.trifa.TRIFAGlobals.INTERVAL_ADD_ALL_FRIENDS_CLEAR_MS;
import static com.zoffcc.applications.trifa.TRIFAGlobals.ONE_HOUR_IN_MS;
import static com.zoffcc.applications.trifa.TrifaToxService.orma;

public class FriendListFragment extends Fragment
{
    private static final String TAG = "trifa.FriendListFrgnt";
    static final int MessageListActivity_ID = 2;
    static final int FriendInfoActivity_ID = 3;
    List<FriendList> data_values2 = new ArrayList<FriendList>();
    // FriendlistArrayAdapter a = null;
    static Boolean in_update_data = false;
    static final Boolean in_update_data_lock = false;
    //  View view1 = null;
    com.l4digital.fastscroll.FastScrollRecyclerView listingsView = null;
    static View fl_loading_progressbar = null;
    FriendlistAdapter adapter = null;
    public static Semaphore semaphore_friendlist_ui_01 = new Semaphore(1);
    private static long add_all_friends_clear_last_trigger_ts = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);

        Log.i(TAG, "onCreateView");
        View view1 = inflater.inflate(R.layout.friend_list_layout, container, false);
        Log.i(TAG, "onCreateView:view1=" + view1);

        // -------------------------------------------
        // -------------------------------------------
        // -------------------------------------------
        List<CombinedFriendsAndConferences> data_values = new ArrayList<CombinedFriendsAndConferences>();
        data_values.clear();

        fl_loading_progressbar = view1.findViewById(R.id.fl_loading_progressbar);
        fl_loading_progressbar.setVisibility(View.GONE);

        listingsView = (com.l4digital.fastscroll.FastScrollRecyclerView) view1.findViewById(R.id.rv_list);
        listingsView.getRecycledViewPool().clear();
        listingsView.setLayoutManager(new LinearLayoutManager(view1.getContext()));
        listingsView.setItemAnimator(new DefaultItemAnimator());
        listingsView.setHasFixedSize(true);

        adapter = new FriendlistAdapter(view1.getContext(), data_values);
        Log.i(TAG, "onCreateView:adapter=" + adapter);
        Log.i(TAG, "onCreateView:listingsView=" + listingsView);
        listingsView.setAdapter(adapter);
        listingsView.getRecycledViewPool().clear();
        adapter.clear_items(); // clears friends AND conferences!!
        adapter.notifyDataSetChanged();

        MainActivity.friend_list_fragment = this;
        // -------------------------------------------
        // -------------------------------------------
        // -------------------------------------------

        return view1;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        Log.i(TAG, "onActivityCreated");

        MainActivity.friend_list_fragment = this;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "onViewCreated");
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        Log.i(TAG, "onAttach(Context)");

        in_update_data = false;
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        Log.i(TAG, "onAttach(Activity)");
    }

    synchronized void modify_friend(final CombinedFriendsAndConferences c, int is_friend)
    {
        // Log.i(TAG, "modify_friend");

        if (is_friend == COMBINED_IS_FRIEND)
        {
            final FriendList f = c.friend_item;

            if (f == null)
            {
                Log.i(TAG, "modify_friend:EE02:" + f+ " FRIEND is NULL, this should not happen!!");
                return;
            }

            try
            {
                if (f.is_relay == true)
                {
                    // do not update anything if this is a relay
                    return;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.i(TAG, "modify_friend:EE01:" + e.getMessage());
            }

            // Log.i(TAG, "modify_friend:start");
            Runnable myRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        final FriendList f2 = (FriendList) orma.selectFromFriendList().
                                tox_public_key_stringEq(f.tox_public_key_string).
                                toList().get(0);

                        if (f2 != null)
                        {
                            FriendList n = (FriendList) com.zoffcc.applications.sorm.FriendList.deep_copy(f2);
                            CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                            cfac.is_friend = COMBINED_IS_FRIEND;
                            cfac.friend_item = n;
                            boolean found_friend = adapter.update_item(cfac, cfac.is_friend);
                            // Log.i(TAG, "modify_friend:found_friend=" + found_friend + " n=" + n);

                            if (!found_friend)
                            {
                                adapter.add_item(cfac);
                                // Log.i(TAG, "modify_friend:add_item");
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            };

            try
            {
                if (main_handler_s != null)
                {
                    main_handler_s.post(myRunnable);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.i(TAG, "modify_friend:EE1:" + e.getMessage());
            }
        }
        else if (is_friend == COMBINED_IS_GROUP)
        {
            final GroupDB cc = c.group_item;

            // Log.i(TAG, "modify_friend:start");
            Runnable myRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        // who_invited__tox_public_key_stringEq(cc.who_invited__tox_public_key_string).
                        // and().
                        final GroupDB conf2 = (GroupDB) orma.selectFromGroupDB().
                                group_identifierEq(cc.group_identifier.toLowerCase()).
                                toList().get(0);

                        if (conf2 != null)
                        {
                            GroupDB n = (GroupDB) GroupDB.deep_copy(conf2);
                            CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                            cfac.is_friend = COMBINED_IS_GROUP;
                            cfac.group_item = n;
                            boolean found_friend = adapter.update_item(cfac, cfac.is_friend);
                            // Log.i(TAG, "modify_friend:found_friend=" + found_friend + " n=" + n);

                            if (!found_friend)
                            {
                                adapter.add_item(cfac);
                                // Log.i(TAG, "modify_friend:add_item");
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            };

            try
            {
                if (main_handler_s != null)
                {
                    main_handler_s.post(myRunnable);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.i(TAG, "modify_friend:EE1:" + e.getMessage());
            }
        }
        else // is conference -----------------------------
        {
            final ConferenceDB cc = c.conference_item;

            // Log.i(TAG, "modify_friend:start");
            Runnable myRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        // who_invited__tox_public_key_stringEq(cc.who_invited__tox_public_key_string).
                        // and().
                        final ConferenceDB conf2 = (ConferenceDB) orma.selectFromConferenceDB().
                                conference_identifierEq(cc.conference_identifier).
                                toList().get(0);

                        if (conf2 != null)
                        {
                            ConferenceDB n = (ConferenceDB) ConferenceDB.deep_copy(conf2);
                            CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                            cfac.is_friend = COMBINED_IS_CONFERENCE;
                            cfac.conference_item = n;
                            boolean found_friend = adapter.update_item(cfac, cfac.is_friend);
                            // Log.i(TAG, "modify_friend:found_friend=" + found_friend + " n=" + n);

                            if (!found_friend)
                            {
                                adapter.add_item(cfac);
                                // Log.i(TAG, "modify_friend:add_item");
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        // e.printStackTrace();
                    }
                }
            };

            try
            {
                if (main_handler_s != null)
                {
                    main_handler_s.post(myRunnable);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.i(TAG, "modify_friend:EE1:" + e.getMessage());
            }
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Log.i(TAG, "onResume");

        // reset friend update trigger timestamp
        add_all_friends_clear_last_trigger_ts = 0;

        try
        {
            FriendListHolder.remove_progress_dialog();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            ConferenceListHolder.remove_progress_dialog();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            GroupListHolder.remove_progress_dialog();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Log.i(TAG, "onResume");

        try
        {
            boolean SORT_CORRECTLY = true;
            if (SORT_CORRECTLY != true)
            {
                try
                {
                    // reload friendlist
                    Log.i(TAG, "onResume:AA");
                    List<com.zoffcc.applications.sorm.FriendList> fl = orma.selectFromFriendList().
                            is_relayNotEq(true).
                            orderByTOX_CONNECTION_on_offDesc().
                            orderByNotification_silentAsc().
                            orderByLast_online_timestampDesc().
                            toList();

                    if (fl != null)
                    {
                        Log.i(TAG, "onResume:fl.size=" + fl.size());
                        if (fl.size() > 0)
                        {
                            int i = 0;
                            for (i = 0; i < fl.size(); i++)
                            {
                                FriendList n = (FriendList) com.zoffcc.applications.sorm.FriendList.deep_copy(fl.get(i));
                                final CombinedFriendsAndConferences cc = new CombinedFriendsAndConferences();
                                cc.is_friend = COMBINED_IS_FRIEND;
                                cc.friend_item = n;
                                modify_friend(cc, cc.is_friend);
                                // Log.i(TAG, "onResume:modify_friend:" + n);
                            }
                        }
                    }

                    // reload conferences
                    List<com.zoffcc.applications.sorm.ConferenceDB> confs = orma.selectFromConferenceDB().
                            orderByConference_activeDesc().
                            orderByNotification_silentAsc().
                            toList();

                    if (confs != null)
                    {
                        if (confs.size() > 0)
                        {
                            int i = 0;
                            for (i = 0; i < confs.size(); i++)
                            {
                                ConferenceDB n = (ConferenceDB) ConferenceDB.deep_copy(confs.get(i));
                                CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                                cfac.is_friend = COMBINED_IS_CONFERENCE;
                                cfac.conference_item = n;
                                modify_friend(cfac, cfac.is_friend);
                                // Log.i(TAG, "onResume:modify_friend:" + n);
                            }
                        }
                    }

                    // reload groups
                    List<com.zoffcc.applications.sorm.GroupDB> groups = orma.selectFromGroupDB().
                            orderByNotification_silentAsc().
                            toList();

                    if (groups != null)
                    {
                        if (groups.size() > 0)
                        {
                            int i = 0;
                            for (i = 0; i < groups.size(); i++)
                            {
                                GroupDB n = (GroupDB) GroupDB.deep_copy(groups.get(i));
                                CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                                cfac.is_friend = COMBINED_IS_GROUP;
                                cfac.group_item = n;
                                modify_friend(cfac, cfac.is_friend);
                                // Log.i(TAG, "onResume:modify_friend:" + n);
                            }
                        }
                    }


                    Log.i(TAG, "onResume:BB");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            else
            {
                add_all_friends_clear(0);
            }
        }
        catch (Exception ee)
        {
            ee.printStackTrace();

            try
            {
                // reload friendlist
                Log.i(TAG, "onResume:AA");
                List<com.zoffcc.applications.sorm.FriendList> fl = orma.selectFromFriendList().
                        is_relayNotEq(true).
                        orderByTOX_CONNECTION_on_offDesc().
                        orderByNotification_silentAsc().
                        orderByLast_online_timestampDesc().
                        toList();

                if (fl != null)
                {
                    Log.i(TAG, "onResume:fl.size=" + fl.size());
                    if (fl.size() > 0)
                    {
                        int i = 0;
                        for (i = 0; i < fl.size(); i++)
                        {
                            FriendList n = (FriendList) com.zoffcc.applications.sorm.FriendList.deep_copy(fl.get(i));
                            final CombinedFriendsAndConferences cc = new CombinedFriendsAndConferences();
                            cc.is_friend = COMBINED_IS_FRIEND;
                            cc.friend_item = n;
                            modify_friend(cc, cc.is_friend);
                            // Log.i(TAG, "onResume:modify_friend:" + n);
                        }
                    }
                }

                // reload conferences
                List<com.zoffcc.applications.sorm.ConferenceDB> confs = orma.selectFromConferenceDB().
                        orderByConference_activeDesc().
                        orderByNotification_silentAsc().
                        toList();

                if (confs != null)
                {
                    if (confs.size() > 0)
                    {
                        int i = 0;
                        for (i = 0; i < confs.size(); i++)
                        {
                            ConferenceDB n = (ConferenceDB) ConferenceDB.deep_copy(confs.get(i));
                            CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                            cfac.is_friend = COMBINED_IS_CONFERENCE;
                            cfac.conference_item = n;
                            modify_friend(cfac, cfac.is_friend);
                            // Log.i(TAG, "onResume:modify_friend:" + n);
                        }
                    }
                }

                // reload groups
                List<com.zoffcc.applications.sorm.GroupDB> groups = orma.selectFromGroupDB().
                        orderByNotification_silentAsc().
                        toList();

                if (groups != null)
                {
                    if (groups.size() > 0)
                    {
                        int i = 0;
                        for (i = 0; i < groups.size(); i++)
                        {
                            GroupDB n = (GroupDB) GroupDB.deep_copy(groups.get(i));
                            CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                            cfac.is_friend = COMBINED_IS_GROUP;
                            cfac.group_item = n;
                            modify_friend(cfac, cfac.is_friend);
                            // Log.i(TAG, "onResume:modify_friend:" + n);
                        }
                    }
                }

                Log.i(TAG, "onResume:BB");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        MainActivity.friend_list_fragment = this;
    }

    void add_all_friends_clear_real(final int delay)
    {
        // Log.i(TAG, "add_all_friends_clear");

        try
        {
            final Thread t = new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        try
                        {
                            // Log.i(TAG, "semaphore_friendlist_ui_01.acquire");
                            semaphore_friendlist_ui_01.acquire();
                            // Log.i(TAG, "semaphore_friendlist_ui_01.acquire:DONE");
                        }
                        catch (InterruptedException e)
                        {
                            // Log.i(TAG, "semaphore_friendlist_ui_01.release:1");
                            semaphore_friendlist_ui_01.release();
                            // Log.i(TAG, "semaphore_friendlist_ui_01.release:1:DONE");
                            return;
                        }

                        // on UI thread ---------------
                        final Runnable myRunnable = () -> {
                            try
                            {
                                Thread.sleep(delay);
                                adapter.clear_items(); // clears friends AND conferences!!

                                // ------------- add friends that were added recently first -------------
                                List<com.zoffcc.applications.sorm.FriendList> fl = orma.selectFromFriendList().
                                        is_relayNotEq(true).
                                        added_timestampGt(System.currentTimeMillis() - ONE_HOUR_IN_MS).
                                        orderByTOX_CONNECTION_on_offDesc().
                                        orderByNotification_silentAsc().
                                        orderByLast_online_timestampDesc().
                                        toList();

                                if (fl != null)
                                {
                                    // Log.i(TAG, "add_all_friends_clear:fl.size=" + fl.size());
                                    if (fl.size() > 0)
                                    {
                                        int i = 0;
                                        for (i = 0; i < fl.size(); i++)
                                        {
                                            FriendList n = (FriendList) FriendList.deep_copy(fl.get(i));
                                            CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                                            cfac.is_friend = COMBINED_IS_FRIEND;
                                            cfac.friend_item = n;
                                            adapter.add_item(cfac);
                                            // Log.i(TAG, "add_all_friends_clear:add:" + n);
                                        }
                                    }
                                }
                                // ------------- add friends that were added recently first -------------

                                // ------------- add rest of friends (with new messages) -------------
                                List<com.zoffcc.applications.sorm.FriendList> fl2m = orma.selectFromFriendList().
                                        is_relayNotEq(true).
                                        added_timestampLe(System.currentTimeMillis() - ONE_HOUR_IN_MS).
                                        orderByTOX_CONNECTION_on_offDesc().
                                        orderByNotification_silentAsc().
                                        orderByLast_online_timestampDesc().
                                        toList();

                                if (fl2m != null)
                                {
                                    // Log.i(TAG, "add_all_friends_clear:fl.size=" + fl.size());
                                    if (fl2m.size() > 0)
                                    {
                                        int i = 0;
                                        for (i = 0; i < fl2m.size(); i++)
                                        {
                                            FriendList n = (FriendList) FriendList.deep_copy(fl2m.get(i));

                                            try
                                            {
                                                int new_messages_count = orma.selectFromMessage().tox_friendpubkeyEq(
                                                        n.tox_public_key_string).is_newEq(true).count();
                                                if (new_messages_count > 0)
                                                {
                                                    CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                                                    cfac.is_friend = COMBINED_IS_FRIEND;
                                                    cfac.friend_item = n;
                                                    adapter.add_item(cfac);
                                                    // Log.i(TAG, "add_all_friends_clear:add:" + n);
                                                }
                                            }
                                            catch (Exception e)
                                            {
                                            }
                                        }
                                    }
                                }
                                // ------------- add rest of friends (with new messages) -------------

                                // ------------- add conferences (with new messages) -------------
                                List<com.zoffcc.applications.sorm.ConferenceDB> confsm = orma.selectFromConferenceDB().
                                        orderByConference_activeDesc().
                                        orderByNotification_silentAsc().
                                        toList();

                                if (confsm != null)
                                {
                                    if (confsm.size() > 0)
                                    {
                                        int i = 0;
                                        for (i = 0; i < confsm.size(); i++)
                                        {
                                            ConferenceDB n = (ConferenceDB) ConferenceDB.deep_copy(confsm.get(i));

                                            try
                                            {
                                                int new_messages_count = orma.selectFromConferenceMessage().
                                                        conference_identifierEq(n.conference_identifier).is_newEq(
                                                        true).count();

                                                if (new_messages_count > 0)
                                                {
                                                    CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                                                    cfac.is_friend = COMBINED_IS_CONFERENCE;
                                                    cfac.conference_item = n;
                                                    adapter.add_item(cfac);
                                                    // Log.i(TAG, "add_all_friends_clear:add:" + n);
                                                }
                                            }
                                            catch (Exception e)
                                            {
                                            }
                                        }
                                    }
                                }
                                // ------------- add conferences (with new messages) -------------

                                // ------------- add groups (with new messages) -------------
                                List<com.zoffcc.applications.sorm.GroupDB> groupsm = orma.selectFromGroupDB().
                                        orderByNotification_silentAsc().
                                        toList();

                                if (groupsm != null)
                                {
                                    if (groupsm.size() > 0)
                                    {
                                        int i = 0;
                                        for (i = 0; i < groupsm.size(); i++)
                                        {
                                            GroupDB n = (GroupDB) GroupDB.deep_copy(groupsm.get(i));

                                            try
                                            {
                                                int new_messages_count = orma.selectFromGroupMessage().
                                                        group_identifierEq(
                                                        n.group_identifier.toLowerCase()).is_newEq(
                                                        true).count();

                                                if (new_messages_count > 0)
                                                {
                                                    CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                                                    cfac.is_friend = COMBINED_IS_GROUP;
                                                    cfac.group_item = n;
                                                    adapter.add_item(cfac);
                                                    // Log.i(TAG, "add_all_friends_clear:add:" + n);
                                                }
                                            }
                                            catch (Exception e)
                                            {
                                            }
                                        }
                                    }
                                }
                                // ------------- add groups (with new messages) -------------

                                // ------------- add rest of friends  -------------
                                List<com.zoffcc.applications.sorm.FriendList> fl2 = orma.selectFromFriendList().
                                        is_relayNotEq(true).
                                        added_timestampLe(System.currentTimeMillis() - ONE_HOUR_IN_MS).
                                        orderByTOX_CONNECTION_on_offDesc().
                                        orderByNotification_silentAsc().
                                        orderByLast_online_timestampDesc().
                                        toList();

                                if (fl2 != null)
                                {
                                    // Log.i(TAG, "add_all_friends_clear:fl.size=" + fl.size());
                                    if (fl2.size() > 0)
                                    {
                                        int i = 0;
                                        for (i = 0; i < fl2.size(); i++)
                                        {
                                            FriendList n = (FriendList) FriendList.deep_copy(fl2.get(i));

                                            int new_messages_count = 0;
                                            try
                                            {
                                                new_messages_count = orma.selectFromMessage().tox_friendpubkeyEq(
                                                        n.tox_public_key_string).is_newEq(true).count();
                                            }
                                            catch (Exception e)
                                            {
                                            }

                                            if (new_messages_count == 0)
                                            {
                                                CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                                                cfac.is_friend = COMBINED_IS_FRIEND;
                                                cfac.friend_item = n;
                                                adapter.add_item(cfac);
                                                // Log.i(TAG, "add_all_friends_clear:add:" + n);
                                            }
                                        }
                                    }
                                }
                                // ------------- add rest of friends  -------------

                                // ------------- add conferences -------------
                                List<com.zoffcc.applications.sorm.ConferenceDB> confs = orma.selectFromConferenceDB().
                                        orderByConference_activeDesc().
                                        orderByNotification_silentAsc().
                                        toList();

                                if (confs != null)
                                {
                                    if (confs.size() > 0)
                                    {
                                        int i = 0;
                                        for (i = 0; i < confs.size(); i++)
                                        {
                                            ConferenceDB n = (ConferenceDB) ConferenceDB.deep_copy(confs.get(i));

                                            int new_messages_count = 0;
                                            try
                                            {
                                                new_messages_count = orma.selectFromConferenceMessage().
                                                        conference_identifierEq(n.conference_identifier).is_newEq(
                                                        true).count();
                                            }
                                            catch (Exception e)
                                            {
                                            }

                                            if (new_messages_count == 0)
                                            {
                                                CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                                                cfac.is_friend = COMBINED_IS_CONFERENCE;
                                                cfac.conference_item = n;
                                                adapter.add_item(cfac);
                                                // Log.i(TAG, "add_all_friends_clear:add:" + n);
                                            }
                                        }
                                    }
                                }
                                // ------------- add conferences -------------

                                // ------------- add groups -------------
                                List<com.zoffcc.applications.sorm.GroupDB> groups = orma.selectFromGroupDB().
                                        orderByNotification_silentAsc().
                                        toList();

                                if (groups != null)
                                {
                                    if (groups.size() > 0)
                                    {
                                        int i = 0;
                                        for (i = 0; i < groups.size(); i++)
                                        {
                                            GroupDB n = (GroupDB) GroupDB.deep_copy(groups.get(i));

                                            int new_messages_count = 0;
                                            try
                                            {
                                                new_messages_count = orma.selectFromGroupMessage().
                                                        group_identifierEq(
                                                        n.group_identifier.toLowerCase()).is_newEq(
                                                        true).count();
                                            }
                                            catch (Exception e)
                                            {
                                            }

                                            if (new_messages_count == 0)
                                            {
                                                CombinedFriendsAndConferences cfac = new CombinedFriendsAndConferences();
                                                cfac.is_friend = COMBINED_IS_GROUP;
                                                cfac.group_item = n;
                                                adapter.add_item(cfac);
                                                // Log.i(TAG, "add_all_friends_clear:add:" + n);
                                            }
                                        }
                                    }
                                }
                                // ------------- add groups -------------
                            }
                            catch (Exception e)
                            {
                            }
                            // Log.i(TAG, "semaphore_friendlist_ui_01.release:2");
                            semaphore_friendlist_ui_01.release();
                            // Log.i(TAG, "semaphore_friendlist_ui_01.release:2:DONE");
                        };

                        if (main_handler_s != null)
                        {
                            main_handler_s.post(myRunnable);
                        }
                        // on UI thread ---------------
                    }
                    catch (Exception e)
                    {
                    }
                }
            };
            t.start();
        }
        catch (Exception e)
        {
            Log.i(TAG, "add_all_friends_clear_real:EE:" + e.getMessage());
            e.printStackTrace();
        }

        // Log.i(TAG, "add_all_friends_clear:READY");

        // Log.i(TAG, "add_all_friends_clear:B:");
    }

    synchronized void add_all_friends_clear(final int delay)
    {
        // Log.i(TAG, "add_all_friends_clear:** CALL");
        long currentTime = System.currentTimeMillis();
        if (currentTime - add_all_friends_clear_last_trigger_ts >= INTERVAL_ADD_ALL_FRIENDS_CLEAR_MS)
        {
            // Log.i(TAG, "add_all_friends_clear:-> REAL");
            add_all_friends_clear_real(delay);
            add_all_friends_clear_last_trigger_ts = currentTime;
        }
        else
        {
            long delta_t_ms = currentTime - add_all_friends_clear_last_trigger_ts;
            // Log.i(TAG, "add_all_friends_clear:  TRIG delta ms=" + delta_t_ms);
            long trigger_in_ms_again = INTERVAL_ADD_ALL_FRIENDS_CLEAR_MS - delta_t_ms;
            if ((trigger_in_ms_again < 1) || (trigger_in_ms_again > (INTERVAL_ADD_ALL_FRIENDS_CLEAR_MS + 1)))
            {
                trigger_in_ms_again = INTERVAL_ADD_ALL_FRIENDS_CLEAR_MS;
            }
            final long trigger_in_ms_again_ = trigger_in_ms_again + 2;
            final Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(trigger_in_ms_again_);
                    // Log.i(TAG, "add_all_friends_clear:__ CALL from Trigger");
                    add_all_friends_clear(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        }
    }

    // name is confusing, just update all friends!! already set to offline in DB
    public void set_all_friends_to_offline()
    {
        // Log.i(TAG, "set_all_friends_to_offline");
        add_all_friends_clear(0);
    }
}
