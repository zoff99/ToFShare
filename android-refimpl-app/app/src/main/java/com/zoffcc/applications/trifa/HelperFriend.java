/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2020 Zoff <zoff@zoff.cc>
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

import android.util.Log;

import com.zoffcc.applications.sorm.FileDB;
import com.zoffcc.applications.sorm.FriendList;
import com.zoffcc.applications.trifa.R;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import okhttp3.CacheControl;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.zoffcc.applications.trifa.CombinedFriendsAndConferences.COMBINED_IS_FRIEND;
import static com.zoffcc.applications.trifa.HelperGeneric.display_toast;
import static com.zoffcc.applications.trifa.HelperMessage.get_message_in_db_sent_push_is_read;
import static com.zoffcc.applications.trifa.HelperMessage.update_message_in_db_sent_push_set;
import static com.zoffcc.applications.trifa.HelperRelay.get_pushurl_for_friend;
import static com.zoffcc.applications.trifa.HelperRelay.is_valid_pushurl_for_friend_with_whitelist;
import static com.zoffcc.applications.trifa.HelperRelay.own_push_token_load;
import static com.zoffcc.applications.trifa.HelperRelay.push_token_to_push_url;
import static com.zoffcc.applications.trifa.MainActivity.PREF__orbot_enabled;
import static com.zoffcc.applications.trifa.MainActivity.PREF__use_push_service;
import static com.zoffcc.applications.trifa.MainActivity.context_s;
import static com.zoffcc.applications.trifa.MainActivity.tox_friend_send_lossless_packet;
import static com.zoffcc.applications.trifa.TRIFAGlobals.CONTROL_PROXY_MESSAGE_TYPE.CONTROL_PROXY_MESSAGE_TYPE_PUSH_URL_FOR_FRIEND;
import static com.zoffcc.applications.trifa.TRIFAGlobals.GENERIC_TOR_USERAGENT;
import static com.zoffcc.applications.trifa.TRIFAGlobals.LAST_ONLINE_TIMSTAMP_ONLINE_NOW;
import static com.zoffcc.applications.trifa.TRIFAGlobals.ORBOT_PROXY_HOST;
import static com.zoffcc.applications.trifa.TRIFAGlobals.ORBOT_PROXY_PORT;
import static com.zoffcc.applications.trifa.TRIFAGlobals.PUSH_URL_TRIGGER_AGAIN_MAX_COUNT;
import static com.zoffcc.applications.trifa.TRIFAGlobals.PUSH_URL_TRIGGER_AGAIN_SECONDS;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TRIFA_FT_DIRECTION.TRIFA_FT_DIRECTION_INCOMING;
import static com.zoffcc.applications.trifa.TRIFAGlobals.UINT32_MAX_JAVA;
import static com.zoffcc.applications.trifa.TRIFAGlobals.global_my_name;
import static com.zoffcc.applications.trifa.TRIFAGlobals.global_my_toxid;
import static com.zoffcc.applications.trifa.ToxVars.TOX_PUBLIC_KEY_SIZE;
import static com.zoffcc.applications.trifa.TrifaToxService.orma;

public class HelperFriend
{
    private static final String TAG = "trifa.Hlp.Friend";

    static FriendList main_get_friend(long friendnum)
    {
        FriendList f = null;

        try
        {
            String pubkey_temp = tox_friend_get_public_key__wrapper(friendnum);
            // Log.i(TAG, "main_get_friend:pubkey=" + pubkey_temp + " fnum=" + friendnum);
            List<com.zoffcc.applications.sorm.FriendList> fl = orma.selectFromFriendList().
                    tox_public_key_stringEq(tox_friend_get_public_key__wrapper(friendnum)).
                    toList();

            // Log.i(TAG, "main_get_friend:fl=" + fl + " size=" + fl.size());

            if (fl.size() > 0)
            {
                f = (FriendList) fl.get(0);
                // Log.i(TAG, "main_get_friend:f=" + f);
            }
            else
            {
                f = null;
            }
        }
        catch (Exception e)
        {
            f = null;
        }

        return f;
    }

    static FriendList main_get_friend(String friend_pubkey)
    {
        FriendList f = null;

        try
        {
            List<com.zoffcc.applications.sorm.FriendList> fl = orma.selectFromFriendList().
                    tox_public_key_stringEq(friend_pubkey).
                    toList();

            if (fl.size() > 0)
            {
                f = (FriendList) fl.get(0);
            }
            else
            {
                f = null;
            }
        }
        catch (Exception e)
        {
            f = null;
        }

        return f;
    }

    static int is_friend_online(long friendnum)
    {
        try
        {
            return (orma.selectFromFriendList().
                    tox_public_key_stringEq(tox_friend_get_public_key__wrapper(friendnum)).
                    toList().get(0).TOX_CONNECTION);
        }
        catch (Exception e)
        {
            // e.printStackTrace();
            return 0;
        }
    }

    static int is_friend_online_real_and_has_msgv3(long friendnum)
    {
        try
        {
            final FriendList f = (FriendList) orma.selectFromFriendList().
                    tox_public_key_stringEq(tox_friend_get_public_key__wrapper(friendnum)).
                    toList().get(0);
            if ((f.TOX_CONNECTION_real != 0) && (f.msgv3_capability == 1))
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }
        catch (Exception e)
        {
            // e.printStackTrace();
            return 0;
        }
    }

    static int is_friend_online_real_and_hasnot_msgv3(long friendnum)
    {
        try
        {
            final FriendList f = (FriendList) orma.selectFromFriendList().
                    tox_public_key_stringEq(tox_friend_get_public_key__wrapper(friendnum)).
                    toList().get(0);
            if ((f.TOX_CONNECTION_real != 0) && (f.msgv3_capability != 1))
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }
        catch (Exception e)
        {
            // e.printStackTrace();
            return 0;
        }
    }

    static int is_friend_online_real(long friendnum)
    {
        try
        {
            return (orma.selectFromFriendList().
                    tox_public_key_stringEq(tox_friend_get_public_key__wrapper(friendnum)).
                    toList().get(0).TOX_CONNECTION_real);
        }
        catch (Exception e)
        {
            // e.printStackTrace();
            return 0;
        }
    }

    synchronized static void set_all_friends_offline()
    {
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    orma.updateFriendList().
                            TOX_CONNECTION(0).
                            execute();
                }
                catch (Exception e)
                {
                }

                try
                {
                    orma.updateFriendList().
                            TOX_CONNECTION_real(0).
                            execute();
                }
                catch (Exception e)
                {
                }

                try
                {
                    orma.updateFriendList().
                            TOX_CONNECTION_on_off(0).
                            execute();
                }
                catch (Exception e)
                {
                }

                try
                {
                    orma.updateFriendList().
                            TOX_CONNECTION_on_off_real(0).
                            execute();
                }
                catch (Exception e)
                {
                }

                try
                {
                    orma.updateFriendList().
                            last_online_timestampEq(LAST_ONLINE_TIMSTAMP_ONLINE_NOW).
                            last_online_timestamp(System.currentTimeMillis()).
                            execute();
                }
                catch (Exception e)
                {
                }

                try
                {
                    orma.updateFriendList().
                            last_online_timestamp_realEq(LAST_ONLINE_TIMSTAMP_ONLINE_NOW).
                            last_online_timestamp_real(System.currentTimeMillis()).
                            execute();
                }
                catch (Exception e)
                {
                }

                // ------ DEBUG ------
                // ------ set all friends to "never" seen online ------
                // ------ DEBUG ------
                // try
                // {
                //     orma.updateFriendList().
                //             last_online_timestamp(LAST_ONLINE_TIMSTAMP_ONLINE_OFFLINE).
                //             execute();
                // }
                // catch (Exception e)
                // {
                // }
                // ------ DEBUG ------
                // ------ set all friends to "never" seen online ------
                // ------ DEBUG ------

                try
                {
                    MainActivity.friend_list_fragment.set_all_friends_to_offline();
                }
                catch (Exception e)
                {
                }
            }
        };
        t.start();
    }

    synchronized static void update_friend_in_db(FriendList f)
    {
        orma.updateFriendList().
                tox_public_key_string(f.tox_public_key_string).
                name(f.name).
                status_message(f.status_message).
                TOX_CONNECTION(f.TOX_CONNECTION).
                TOX_CONNECTION_on_off(f.TOX_CONNECTION_on_off).
                TOX_USER_STATUS(f.TOX_USER_STATUS).
                execute();
    }

    synchronized static void update_friend_in_db_status_message(FriendList f)
    {
        orma.updateFriendList().
                tox_public_key_stringEq(f.tox_public_key_string).
                status_message(f.status_message).
                execute();
    }

    synchronized static void update_friend_in_db_status(FriendList f)
    {
        // Log.i(TAG, "update_friend_in_db_status:f=" + f);
        orma.updateFriendList().
                tox_public_key_stringEq(f.tox_public_key_string).
                TOX_USER_STATUS(f.TOX_USER_STATUS).
                execute();
        // Log.i(TAG, "update_friend_in_db_status:numrows=" + numrows);
    }

    synchronized static void update_friend_in_db_capabilities(FriendList f)
    {
        orma.updateFriendList().
                tox_public_key_stringEq(f.tox_public_key_string).
                capabilities(f.capabilities).
                execute();
    }

    synchronized static void update_friend_in_db_connection_status(FriendList f)
    {
        try
        {
            orma.updateFriendList().
                    tox_public_key_stringEq(f.tox_public_key_string).
                    TOX_CONNECTION(f.TOX_CONNECTION).
                    TOX_CONNECTION_on_off(f.TOX_CONNECTION_on_off).
                    execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static void update_friend_in_db_ip_addr_str(FriendList f)
    {
        try
        {
            orma.updateFriendList().
                    tox_public_key_stringEq(f.tox_public_key_string).
                    ip_addr_str(f.ip_addr_str).
                    execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    synchronized static void update_friend_in_db_connection_status_real(FriendList f)
    {
        try
        {
            orma.updateFriendList().
                    tox_public_key_stringEq(f.tox_public_key_string).
                    TOX_CONNECTION_real(f.TOX_CONNECTION_real).
                    TOX_CONNECTION_on_off_real(f.TOX_CONNECTION_on_off_real).
                    execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    synchronized static void update_friend_in_db_last_online_timestamp(FriendList f)
    {
        // Log.i(TAG, "update_friend_in_db_last_online_timestamp");
        try
        {
            orma.updateFriendList().
                    tox_public_key_stringEq(f.tox_public_key_string).
                    last_online_timestamp(f.last_online_timestamp).
                    execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    synchronized static void update_friend_in_db_last_online_timestamp_real(FriendList f)
    {
        try
        {
            orma.updateFriendList().
                    tox_public_key_stringEq(f.tox_public_key_string).
                    last_online_timestamp_real(f.last_online_timestamp_real).
                    execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    synchronized static void update_friend_in_db_name(FriendList f)
    {
        orma.updateFriendList().
                tox_public_key_stringEq(f.tox_public_key_string).
                name(f.name).
                execute();
    }

    static long get_friend_msgv3_capability(@NonNull String friend_public_key_string)
    {
        long ret = 0;
        try
        {
            FriendList f = (FriendList) orma.selectFromFriendList().
                    tox_public_key_stringEq(friend_public_key_string).
                    get(0);
            if (f != null)
            {
                ret = f.msgv3_capability;
            }

            return ret;
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    static long get_friend_msgv3_capability(long friend_number)
    {
        long ret = 0;
        try
        {
            FriendList f = (FriendList) orma.selectFromFriendList().
                    tox_public_key_stringEq(HelperFriend.tox_friend_get_public_key__wrapper(friend_number)).
                    get(0);
            if (f != null)
            {
                // Log.i(TAG, "get_friend_msgv3_capability:f=" +
                //           get_friend_name_from_pubkey(HelperFriend.tox_friend_get_public_key__wrapper(friend_number)) +
                //           " f=" + f.msgv3_capability);
                ret = f.msgv3_capability;
            }

            return ret;
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    static void update_friend_msgv3_capability(long friend_number, int new_value)
    {
        try
        {
            if ((new_value == 0) || (new_value == 1))
            {
                FriendList f = (FriendList) orma.selectFromFriendList().
                        tox_public_key_stringEq(HelperFriend.tox_friend_get_public_key__wrapper(friend_number)).
                        get(0);
                if (f != null)
                {
                    if (f.msgv3_capability != new_value)
                    {
                        Log.i(TAG,
                              "update_friend_msgv3_capability f=" + get_friend_name_from_num(friend_number) + " new=" +
                              new_value + " old=" + f.msgv3_capability);
                        orma.updateFriendList().
                                tox_public_key_stringEq(HelperFriend.tox_friend_get_public_key__wrapper(friend_number)).
                                msgv3_capability(new_value).
                                execute();
                    }
                }
            }
        }
        catch (Exception e)
        {
        }
    }

    public static long tox_friend_by_public_key__wrapper(@NonNull String friend_public_key_string)
    {
        if (MainActivity.cache_pubkey_fnum.containsKey(friend_public_key_string))
        {
            // Log.i(TAG, "cache hit:1");
            return MainActivity.cache_pubkey_fnum.get(friend_public_key_string);
        }
        else
        {
            if (MainActivity.cache_pubkey_fnum.size() >= 180)
            {
                // TODO: bad!
                MainActivity.cache_pubkey_fnum.clear();
            }

            long result = MainActivity.tox_friend_by_public_key(friend_public_key_string);
            MainActivity.cache_pubkey_fnum.put(friend_public_key_string, result);
            return result;
        }
    }

    public static String tox_friend_get_public_key__wrapper(long friend_number)
    {
        if (MainActivity.cache_fnum_pubkey.containsKey(friend_number))
        {
            // Log.i(TAG, "cache hit:2");
            return MainActivity.cache_fnum_pubkey.get(friend_number);
        }
        else
        {
            if (MainActivity.cache_fnum_pubkey.size() >= 180)
            {
                // TODO: bad!
                MainActivity.cache_fnum_pubkey.clear();
            }

            String result = MainActivity.tox_friend_get_public_key(friend_number);
            MainActivity.cache_fnum_pubkey.put(friend_number, result);
            return result;
        }
    }

    static void del_friend_avatar(String friend_pubkey, String avatar_path_name, String avatar_file_name)
    {
        try
        {
            boolean avatar_filesize_non_zero = false;
            info.guardianproject.iocipher.File f1 = null;

            try
            {
                f1 = new info.guardianproject.iocipher.File(avatar_path_name + "/" + avatar_file_name);

                if (f1.length() > 0)
                {
                    avatar_filesize_non_zero = true;
                }

                f1.delete();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            orma.updateFriendList().tox_public_key_stringEq(friend_pubkey).
                    avatar_pathname(null).
                    avatar_filename(null).
                    avatar_ftid_hex(null).
                    avatar_update(false).
                    avatar_update_timestamp(System.currentTimeMillis()).
                    execute();

            HelperGeneric.update_display_friend_avatar(friend_pubkey, avatar_path_name, avatar_file_name);
        }
        catch (Exception e)
        {
            Log.i(TAG, "set_friend_avatar:EE:" + e.getMessage());
            e.printStackTrace();
        }
    }

    static void set_friend_avatar(String friend_pubkey, String avatar_path_name, String avatar_file_name, String avatar_ftid_hex)
    {
        try
        {
            boolean avatar_filesize_non_zero = false;
            info.guardianproject.iocipher.File f1 = null;

            String avatar_ftid_hex_wrap = avatar_ftid_hex;
            if (avatar_ftid_hex_wrap!= null)
            {
                avatar_ftid_hex_wrap = avatar_ftid_hex_wrap.toUpperCase();
            }

            try
            {
                f1 = new info.guardianproject.iocipher.File(avatar_path_name + "/" + avatar_file_name);

                if (f1.length() > 0)
                {
                    avatar_filesize_non_zero = true;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.i(TAG, "set_friend_avatar:EE01:" + e.getMessage());
            }

            // Log.i(TAG, "set_friend_avatar:update:pubkey=" + friend_pubkey.substring(0,4) + " path=" + avatar_path_name + " file=" +
            // avatar_file_name);

            if (avatar_filesize_non_zero)
            {
                orma.updateFriendList().tox_public_key_stringEq(friend_pubkey).
                        avatar_pathname(avatar_path_name).
                        avatar_filename(avatar_file_name).
                        avatar_ftid_hex(avatar_ftid_hex_wrap).
                        avatar_update(false).
                        avatar_update_timestamp(System.currentTimeMillis()).
                        execute();
            }
            else
            {
                orma.updateFriendList().tox_public_key_stringEq(friend_pubkey).
                        avatar_pathname(null).
                        avatar_filename(null).
                        avatar_ftid_hex(null).
                        avatar_update(false).
                        avatar_update_timestamp(System.currentTimeMillis()).
                        execute();
            }

            HelperGeneric.update_display_friend_avatar(friend_pubkey, avatar_path_name, avatar_file_name);
        }
        catch (Exception e)
        {
            Log.i(TAG, "set_friend_avatar:EE02:" + e.getMessage());
            e.printStackTrace();
        }
    }

    static void set_friend_avatar_update(String friend_pubkey, boolean avatar_update_value)
    {
        try
        {
            orma.updateFriendList().tox_public_key_stringEq(friend_pubkey).
                    avatar_update(avatar_update_value).
                    avatar_update_timestamp(System.currentTimeMillis()).
                    execute();
        }
        catch (Exception e)
        {
            Log.i(TAG, "set_friend_avatar_update:EE:" + e.getMessage());
            e.printStackTrace();
        }
    }

    static String get_friend_avatar_saved_hash_hex(String friend_pubkey)
    {
        String ret = null;
        try
        {
            ret = orma.selectFromFriendList().tox_public_key_stringEq(friend_pubkey).
                    get(0).avatar_ftid_hex.toUpperCase();
        }
        catch (Exception e)
        {
        }
        return ret;
    }

    static void add_friend_to_system(final String friend_public_key, final boolean as_friends_relay, final String owner_public_key)
    {
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    // toxcore needs this!!
                    Thread.sleep(10);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                // ---- auto add all friends ----
                // ---- auto add all friends ----
                // ---- auto add all friends ----
                long friendnum = MainActivity.tox_friend_add_norequest(friend_public_key); // add friend
                Log.d(TAG, "add_friend_to_system:fnum add=" + friendnum);

                if (friendnum == UINT32_MAX_JAVA) // 0xffffffff == UINT32_MAX
                {
                    // Log.d(TAG, "add_friend_to_system:fnum add res=0xffffffff as_friends_relay=" + as_friends_relay);
                    // adding friend failed
                    // if its a relay still try to update it in our DB
                    if (as_friends_relay)
                    {
                        // add relay for friend to DB
                        // Log.d(TAG, "add_friend_to_system:add_or_update_friend_relay");
                        HelperRelay.add_or_update_friend_relay(friend_public_key, owner_public_key);
                        add_all_friends_clear_wrapper(10);
                    }

                    return;
                }

                try
                {
                    Thread.sleep(20);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                HelperGeneric.update_savedata_file_wrapper(); // save toxcore datafile (new friend added)
                final FriendList f = new FriendList();
                f.tox_public_key_string = friend_public_key;
                f.TOX_USER_STATUS = 0;
                f.TOX_CONNECTION = 0;
                f.TOX_CONNECTION_on_off = HelperGeneric.get_toxconnection_wrapper(f.TOX_CONNECTION);
                // set name as the last 5 char of the publickey (until we get a proper name)
                f.name = friend_public_key.substring(friend_public_key.length() - 5, friend_public_key.length());
                f.avatar_pathname = null;
                f.avatar_filename = null;
                f.capabilities = 0;

                try
                {
                    // Log.i(TAG, "friend_request:insert:001:f=" + f);
                    f.added_timestamp = System.currentTimeMillis();
                    long res = orma.insertIntoFriendList(f);
                    Log.i(TAG, "friend_request:insert:002:res=" + res);
                }
                catch (android.database.sqlite.SQLiteConstraintException e)
                {
                    // e.printStackTrace();
                    Log.i(TAG, "friend_request:insert:EE1:" + e.getMessage());
                    return;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.i(TAG, "friend_request:insert:EE2:" + e.getMessage());
                    return;
                }

                if (as_friends_relay)
                {
                    // add relay for friend to DB
                    // Log.d(TAG, "add_friend_to_system:add_or_update_friend_relay");
                    HelperRelay.add_or_update_friend_relay(friend_public_key, owner_public_key);
                    // update friendlist on screen
                    add_all_friends_clear_wrapper(10);
                }
                else
                {
                    update_single_friend_in_friendlist_view(f);
                }

                // ---- auto add all friends ----
                // ---- auto add all friends ----
                // ---- auto add all friends ----

                try
                {
                    Thread.sleep(100);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                if (MainActivity.PREF__U_keep_nospam == false)
                {
                    // ---- set new random nospam value after each added friend ----
                    // ---- set new random nospam value after each added friend ----
                    // ---- set new random nospam value after each added friend ----
                    HelperGeneric.set_new_random_nospam_value();
                    // ---- set new random nospam value after each added friend ----
                    // ---- set new random nospam value after each added friend ----
                    // ---- set new random nospam value after each added friend ----
                }
            }
        };
        t.start();
    }

    static void add_pushurl_for_friend(final String friend_push_url, final String friend_pubkey)
    {
        try
        {
            orma.updateFriendList().tox_public_key_stringEq(friend_pubkey).push_url(friend_push_url).execute();
        }
        catch (Exception e)
        {
            Log.i(TAG, "add_pushurl_for_friend:EE:" + e.getMessage());
        }
    }

    static void remove_pushurl_for_friend(final String friend_pubkey)
    {
        try
        {
            orma.updateFriendList().tox_public_key_stringEq(friend_pubkey).push_url(null).execute();
        }
        catch (Exception e)
        {
            Log.i(TAG, "remove_pushurl_for_friend:EE:" + e.getMessage());
        }
    }

    synchronized static void insert_into_friendlist_db(final FriendList f)
    {
        try
        {
            if (orma.selectFromFriendList().tox_public_key_stringEq(f.tox_public_key_string).count() == 0)
            {
                f.added_timestamp = System.currentTimeMillis();
                f.push_url = null;
                orma.insertIntoFriendList(f);
                // Log.i(TAG, "friend added to DB: " + f.tox_public_key_string);
            }
            else
            {
                // friend already in DB
                // Log.i(TAG, "friend already in DB: " + f.tox_public_key_string);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "friend added to DB:EE:" + e.getMessage());
        }
    }

    static void delete_friend_all_files(final String friend_pubkey)
    {
        try
        {
            Iterator<FileDB> i1 = orma.selectFromFileDB().tox_public_key_stringEq(friend_pubkey).
                    directionEq(TRIFA_FT_DIRECTION_INCOMING.value).
                    is_in_VFSEq(true).
                    toList().iterator();
            MainActivity.selected_messages.clear();
            MainActivity.selected_messages_text_only.clear();
            MainActivity.selected_messages_incoming_file.clear();

            while (i1.hasNext())
            {
                try
                {
                    long file_id = i1.next().id;
                    long msg_id = orma.selectFromMessage().filedb_idEq(file_id).directionEq(0).
                            tox_friendpubkeyEq(friend_pubkey).get(0).id;
                    MainActivity.selected_messages.add(msg_id);
                    MainActivity.selected_messages_incoming_file.add(msg_id);
                }
                catch (Exception e2)
                {
                    e2.printStackTrace();
                }
            }

            HelperMessage.delete_selected_messages(MainActivity.main_activity_s, false, false, "deleting Messages ...");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            orma.deleteFromFileDB().tox_public_key_stringEq(friend_pubkey).execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static void delete_friend_all_filetransfers(final String friend_pubkey)
    {
        try
        {
            Log.i(TAG, "delete_ft:ALL for friend=" + friend_pubkey);
            orma.deleteFromFiletransfer().tox_public_key_stringEq(friend_pubkey).execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static void delete_friend_all_messages(final String friend_pubkey)
    {
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    orma.deleteFromMessage().tox_friendpubkeyEq(friend_pubkey).execute();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    static void delete_friend(final String friend_pubkey)
    {
        //Thread t = new Thread()
        //{
        //    @Override
        //    public void run()
        //    {
                try
                {
                    orma.deleteFromFriendList().
                        tox_public_key_stringEq(friend_pubkey).
                        execute();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
        //    }
        //};
        //t.start();
    }

    static void add_friend_real(String friend_tox_id)
    {
        // Log.i(TAG, "add_friend_real:add friend ID:" + friend_tox_id);
        // add friend ---------------
        if (friend_tox_id == null)
        {
            Log.i(TAG, "add_friend_real:add friend ID = NULL");
            return;
        }

        Log.i(TAG, "add_friend_real:add friend ID len:" + friend_tox_id.length());
        long friendnum = MainActivity.tox_friend_add(friend_tox_id, "please add me"); // add friend
        Log.i(TAG, "add_friend_real:add friend  #:" + friendnum);
        HelperGeneric.update_savedata_file_wrapper(); // save toxcore datafile (new friend added)

        if (friendnum > -1)
        {
            // nospam=8 chars, checksum=4 chars
            String friend_public_key = friend_tox_id.substring(0, friend_tox_id.length() - 12);
            // Log.i(TAG, "add_friend_real:add friend PK:" + friend_public_key);
            FriendList f = new FriendList();
            f.tox_public_key_string = friend_public_key;

            try
            {
                // set name as the last 5 char of TOXID (until we get a name sent from friend)
                f.name = friend_public_key.substring(friend_public_key.length() - 5, friend_public_key.length());
            }
            catch (Exception e)
            {
                e.printStackTrace();
                f.name = "Unknown";
            }

            f.TOX_USER_STATUS = 0;
            f.TOX_CONNECTION = 0;
            f.TOX_CONNECTION_on_off = HelperGeneric.get_toxconnection_wrapper(f.TOX_CONNECTION);
            f.avatar_filename = null;
            f.avatar_pathname = null;

            display_toast(context_s.getString(R.string.add_friend_success), false, 300);

            try
            {
                insert_into_friendlist_db(f);
            }
            catch (Exception e)
            {
                // e.printStackTrace();
            }

            update_single_friend_in_friendlist_view(f);
        }
        else
        {
            display_toast(context_s.getString(R.string.add_friend_failed), false, 300);
        }

        if (friendnum == -1)
        {
            Log.i(TAG, "add_friend_real:friend already added, or request already sent");

            /*
            // still add the friend to the DB
            String friend_public_key = friend_tox_id.substring(0, friend_tox_id.length() - 12);
            add_friend_to_system(friend_public_key, false, null);
            */
        }
        else if (friendnum < -1)
        {
            Log.i(TAG, "add_friend_real:some error occured");
        }

        // add friend ---------------
    }

    static String get_friend_name_from_pubkey(String friend_pubkey)
    {
        String ret = "Unknown";
        String friend_alias_name = "";
        String friend_name = "";

        try
        {
            friend_alias_name = orma.selectFromFriendList().
                    tox_public_key_stringEq(friend_pubkey).
                    toList().get(0).alias_name;
        }
        catch (Exception e)
        {
            friend_alias_name = "";
            // e.printStackTrace();
        }

        if ((friend_alias_name == null) || (friend_alias_name.equals("")))
        {
            try
            {
                friend_name = orma.selectFromFriendList().
                        tox_public_key_stringEq(friend_pubkey).
                        toList().get(0).name;
            }
            catch (Exception e)
            {
                friend_name = "";
                e.printStackTrace();
            }

            if ((friend_name != null) && (!friend_name.equals("")))
            {
                ret = friend_name;
            }
        }
        else
        {
            ret = friend_alias_name;
        }

        return ret;
    }

    static long get_friend_capabilities_from_pubkey(String friend_pubkey)
    {
        long friend_capabilities = 0;

        try
        {
            friend_capabilities = orma.selectFromFriendList().
                    tox_public_key_stringEq(friend_pubkey).
                    toList().get(0).capabilities;
        }
        catch (Exception e)
        {
            friend_capabilities = 0;
            e.printStackTrace();
        }

        return friend_capabilities;
    }

    static String get_friend_name_from_num(long friendnum)
    {
        String result = "Unknown";

        try
        {
            if (orma != null)
            {
                try
                {
                    String result_alias = orma.selectFromFriendList().
                            tox_public_key_stringEq(tox_friend_get_public_key__wrapper(friendnum)).
                            toList().get(0).alias_name;

                    if (result_alias != null)
                    {
                        if (result_alias.length() > 0)
                        {
                            result = result_alias;
                            return result;
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                result = orma.selectFromFriendList().
                        tox_public_key_stringEq(tox_friend_get_public_key__wrapper(friendnum)).
                        toList().get(0).name;
            }
        }
        catch (Exception e)
        {
            result = "Unknown";
            e.printStackTrace();
        }

        return result;
    }

    static String resolve_name_for_pubkey(String pub_key, String default_name)
    {
        String ret = default_name;

        try
        {
            try
            {
                if (pub_key.equals(global_my_toxid.substring(0, (TOX_PUBLIC_KEY_SIZE * 2))))
                {
                    // its our own key
                    ret = global_my_name;
                    return ret;
                }
            }
            catch (Exception e1)
            {
                e1.printStackTrace();
            }

            FriendList fl = (FriendList) orma.selectFromFriendList().
                    tox_public_key_stringEq(pub_key).
                    toList().get(0);

            if (fl.name != null)
            {
                if (fl.name.length() > 0)
                {
                    ret = fl.name;
                }
            }

            if (fl.alias_name != null)
            {
                if (fl.alias_name.length() > 0)
                {
                    ret = fl.alias_name;
                }
            }
        }
        catch (Exception e)
        {
            // e.printStackTrace();
            ret = default_name;
        }

        return ret;
    }

    static void send_friend_msg_receipt_v2_wrapper(final long friend_number, final int msg_type, final ByteBuffer msg_id_buffer, long t_sec_receipt)
    {
        // (msg_type == 1) msgV2 direct message
        // (msg_type == 2) msgV2 relay message
        // (msg_type == 3) msgV2 "conference" and "group" confirm msg received message
        // (msg_type == 4) msgV2 confirm unknown received message
        if (msg_type == 1)
        {
            // send message receipt v2
            MainActivity.tox_util_friend_send_msg_receipt_v2(friend_number, t_sec_receipt, msg_id_buffer);

            try
            {
                String relay_for_friend = HelperRelay.get_relay_for_friend(
                        tox_friend_get_public_key__wrapper(friend_number));

                if (relay_for_friend != null)
                {
                    // if friend has a relay, send the "msg receipt" also to the relay. just to be sure.
                    MainActivity.tox_util_friend_send_msg_receipt_v2(
                            tox_friend_by_public_key__wrapper(relay_for_friend), t_sec_receipt, msg_id_buffer);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else if (msg_type == 2)
        {
            // send message receipt v2
            final Thread t = new Thread()
            {
                @Override
                public void run()
                {

                    // send msg receipt on main thread
                    final Runnable myRunnable = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                String msg_id_as_hex_string = HelperGeneric.bytesToHex(msg_id_buffer.array(),
                                                                                       msg_id_buffer.arrayOffset(),
                                                                                       msg_id_buffer.limit());
                                // Log.i(TAG, "send_friend_msg_receipt_v2_wrapper:send delayed -> now msgid=" +
                                //           msg_id_as_hex_string);

                                try
                                {
                                    int res = MainActivity.tox_util_friend_send_msg_receipt_v2(friend_number,
                                                                                               t_sec_receipt,
                                                                                               msg_id_buffer);

                                    // Log.i(TAG, "send_friend_msg_receipt_v2_wrapper:ACK:1:res=" + res + " f=" +
                                    //           get_friend_name_from_num(friend_number));

                                    try
                                    {
                                        String relay_for_friend = HelperRelay.get_relay_for_friend(
                                                tox_friend_get_public_key__wrapper(friend_number));

                                        if (relay_for_friend != null)
                                        {
                                            // if friend has a relay, send the "msg receipt" also to the relay. just to be sure.
                                            int res_relay = MainActivity.tox_util_friend_send_msg_receipt_v2(
                                                    tox_friend_by_public_key__wrapper(relay_for_friend), t_sec_receipt,
                                                    msg_id_buffer);

                                            // Log.i(TAG,
                                            //      "send_friend_msg_receipt_v2_wrapper:ACK:2:res_relay=" + res_relay +
                                            //      " f=" + get_friend_name_from_num(
                                            //              tox_friend_by_public_key__wrapper(relay_for_friend)));

                                        }
                                    }
                                    catch (Exception e)
                                    {
                                        e.printStackTrace();
                                    }
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    };

                    if (MainActivity.main_handler_s != null)
                    {
                        MainActivity.main_handler_s.post(myRunnable);
                    }
                }
            };
            t.start();
        }
        else if (msg_type == 3)
        {
            // send message receipt v2
            /*
            String msg_id_as_hex_string_wrapped = HelperGeneric.bytesToHex(msg_id_buffer.array(),
                                                                           msg_id_buffer.arrayOffset(),
                                                                           msg_id_buffer.limit());

            Log.i(TAG, "send_friend_msg_receipt_v2_wrapper:(msg_type == 3):" + get_friend_name_from_num(friend_number) +
                       " buffer=" + msg_id_as_hex_string_wrapped);
             */
            MainActivity.tox_util_friend_send_msg_receipt_v2(friend_number, t_sec_receipt, msg_id_buffer);
        }
        else if (msg_type == 4)
        {
            // send message receipt v2 for unknown message
            MainActivity.tox_util_friend_send_msg_receipt_v2(friend_number, t_sec_receipt, msg_id_buffer);
        }
    }

    static void add_all_friends_clear_wrapper(int delay)
    {
        try
        {
            if (MainActivity.friend_list_fragment != null)
            {
                MainActivity.friend_list_fragment.add_all_friends_clear(delay);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static void update_single_friend_in_friendlist_view(final FriendList f)
    {
        try
        {
            if (MainActivity.friend_list_fragment != null)
            {
                CombinedFriendsAndConferences cc = new CombinedFriendsAndConferences();
                cc.is_friend = COMBINED_IS_FRIEND;
                cc.friend_item = f;
                MainActivity.friend_list_fragment.modify_friend(cc, cc.is_friend);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
     * return true if we should stop triggering push notifications
     *        false otherwise
     */
    static boolean friend_do_actual_weburl_call(final String friend_pubkey, final String pushurl_for_friend, final long message_timestamp_circa, final boolean update_message_flag)
    {
        OkHttpClient client = null;

        if (!update_message_flag)
        {
            if (get_message_in_db_sent_push_is_read(friend_pubkey, message_timestamp_circa))
            {
                // message is "read" (received) so stop triggering push notifications
                return true;
            }
        }

        if (PREF__orbot_enabled)
        {
            InetSocketAddress proxyAddr = new InetSocketAddress(ORBOT_PROXY_HOST, (int) ORBOT_PROXY_PORT);
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddr);

            client = new OkHttpClient.Builder().
                    proxy(proxy).
                    readTimeout(5, TimeUnit.SECONDS).
                    callTimeout(6, TimeUnit.SECONDS).
                    connectTimeout(8, TimeUnit.SECONDS).
                    writeTimeout(5, TimeUnit.SECONDS).
                    build();
        }
        else
        {
            client = new OkHttpClient.Builder().
                    readTimeout(5, TimeUnit.SECONDS).
                    callTimeout(6, TimeUnit.SECONDS).
                    connectTimeout(8, TimeUnit.SECONDS).
                    writeTimeout(5, TimeUnit.SECONDS).
                    build();
        }

        RequestBody formBody = new FormBody.Builder().
                add("ping", "1").
                build();

        Request request = new Request.
                Builder().
                cacheControl(new CacheControl.Builder().noCache().build()).
                url(pushurl_for_friend).
                header("User-Agent", GENERIC_TOR_USERAGENT).
                post(formBody).
                build();

        try (Response response = client.newCall(request).execute())
        {
            Log.i(TAG, "friend_call_push_url"); // :url=" + pushurl_for_friend + " RES=" + response.code());
            if ((response.code() < 300) && (response.code() > 199))
            {
                if (update_message_flag)
                {
                    update_message_in_db_sent_push_set(friend_pubkey, message_timestamp_circa);
                }
            }
        }
        catch (Exception e1)
        {
            Log.i(TAG, "friend_call_push_url:EE1:" + e1.getMessage());
            e1.printStackTrace();
        }

        return false;
    }

    static void friend_call_push_url(final String friend_pubkey, final long message_timestamp_circa)
    {
        try
        {
            if (!PREF__use_push_service)
            {
                return;
            }

            final String pushurl_for_friend = get_pushurl_for_friend(friend_pubkey);

            if (pushurl_for_friend != null)
            {
                if (pushurl_for_friend.length() > "https://".length())
                {
                    if (is_valid_pushurl_for_friend_with_whitelist(pushurl_for_friend))
                    {

                        Thread t = new Thread()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    friend_do_actual_weburl_call(friend_pubkey, pushurl_for_friend,
                                                                 message_timestamp_circa, true);
                                }
                                catch (Exception e)
                                {
                                    Log.i(TAG, "friend_call_push_url:EE2:" + e.getMessage());
                                }
                            }
                        };
                        t.start();
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    static void send_pushurl_to_friend(final String friend_pubkey)
    {
        own_push_token_load();

        if (TRIFAGlobals.global_notification_token != null)
        {
            final String notification_push_url = push_token_to_push_url(TRIFAGlobals.global_notification_token);
            if (notification_push_url != null)
            {
                String temp_string = "A" + notification_push_url; //  "A" is a placeholder to put the pkgID later
                // Log.i(TAG, "send_pushurl_to_friend:" +
                //           get_friend_name_from_num(friend_number) + ":send push url:" + temp_string);
                byte[] data_bin = temp_string.getBytes(); // TODO: use specific characterset
                int data_bin_len = data_bin.length;
                data_bin[0] = (byte) CONTROL_PROXY_MESSAGE_TYPE_PUSH_URL_FOR_FRIEND.value; // replace "A" with pkgID
                final int res = tox_friend_send_lossless_packet(tox_friend_by_public_key__wrapper(friend_pubkey),
                                                                data_bin, data_bin_len);
                // Log.i(TAG, "send_pushurl_to_friend:" +
                //           get_friend_name_from_pubkey(friend_pubkey) + ":send push url:RES=" + res);
            }
        }
    }

    static void send_pushurl_to_all_friends()
    {
        own_push_token_load();

        if (TRIFAGlobals.global_notification_token == null)
        {
            return;
        }

        try
        {
            List<com.zoffcc.applications.sorm.FriendList> fl = orma.selectFromFriendList().
                    is_relayNotEq(true).
                    toList();

            if (fl != null)
            {
                if (fl.size() > 0)
                {
                    int i = 0;
                    for (i = 0; i < fl.size(); i++)
                    {
                        FriendList n = (FriendList) fl.get(i);
                        send_pushurl_to_friend(n.tox_public_key_string);
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }
}
