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

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.zoffcc.applications.sorm.FileDB;
import com.zoffcc.applications.sorm.Filetransfer;
import com.zoffcc.applications.sorm.Message;
import com.zoffcc.applications.trifa.MessageListActivity.outgoing_file_wrapped;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

import androidx.core.content.FileProvider;

import static android.webkit.MimeTypeMap.getFileExtensionFromUrl;
import static com.zoffcc.applications.trifa.HelperFriend.tox_friend_by_public_key__wrapper;
import static com.zoffcc.applications.trifa.HelperGeneric.display_toast;
import static com.zoffcc.applications.trifa.HelperGeneric.get_fileExt;
import static com.zoffcc.applications.trifa.HelperGeneric.set_message_accepted_from_id;
import static com.zoffcc.applications.trifa.HelperMessage.set_message_queueing_from_id;
import static com.zoffcc.applications.trifa.HelperMessage.set_message_start_sending_from_id;
import static com.zoffcc.applications.trifa.HelperMessage.set_message_state_from_id;
import static com.zoffcc.applications.trifa.HelperMessage.update_message_in_db_filetransfer_kind;
import static com.zoffcc.applications.trifa.HelperMessage.update_single_message_from_messge_id;
import static com.zoffcc.applications.trifa.Identicon.bytesToHex;
import static com.zoffcc.applications.trifa.MainActivity.PREF__auto_accept_all_upto;
import static com.zoffcc.applications.trifa.MainActivity.PREF__auto_accept_image;
import static com.zoffcc.applications.trifa.MainActivity.PREF__auto_accept_video;
import static com.zoffcc.applications.trifa.MainActivity.SD_CARD_FILES_EXPORT_DIR;
import static com.zoffcc.applications.trifa.MainActivity.SD_CARD_FILES_OUTGOING_WRAPPER_DIR;
import static com.zoffcc.applications.trifa.MainActivity.context_s;
import static com.zoffcc.applications.trifa.MainActivity.tox_file_control;
import static com.zoffcc.applications.trifa.MainActivity.tox_file_send;
import static com.zoffcc.applications.trifa.MainActivity.tox_friend_get_capabilities;
import static com.zoffcc.applications.trifa.TRIFAGlobals.AUTO_ACCEPT_FT_MAX_ANYKIND_SIZE_IN_MB;
import static com.zoffcc.applications.trifa.TRIFAGlobals.AUTO_ACCEPT_FT_MAX_IMAGE_SIZE_IN_MB;
import static com.zoffcc.applications.trifa.TRIFAGlobals.AUTO_ACCEPT_FT_MAX_VIDEO_SIZE_IN_MB;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TRIFA_FT_DIRECTION.TRIFA_FT_DIRECTION_INCOMING;
import static com.zoffcc.applications.trifa.TRIFAGlobals.UINT32_MAX_JAVA;
import static com.zoffcc.applications.trifa.TRIFAGlobals.VFS_FILE_DIR;
import static com.zoffcc.applications.trifa.TRIFAGlobals.VFS_PREFIX;
import static com.zoffcc.applications.trifa.TRIFAGlobals.VFS_TMP_FILE_DIR;
import static com.zoffcc.applications.trifa.TRIFAGlobals.cache_ft_fis_saf;
import static com.zoffcc.applications.trifa.TRIFAGlobals.cache_ft_fos;
import static com.zoffcc.applications.trifa.ToxVars.TOX_CAPABILITY_DECODE;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_CONTROL.TOX_FILE_CONTROL_CANCEL;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_CONTROL.TOX_FILE_CONTROL_PAUSE;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_CONTROL.TOX_FILE_CONTROL_RESUME;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_ID_LENGTH;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_KIND.TOX_FILE_KIND_DATA;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_KIND.TOX_FILE_KIND_FTV2;
import static com.zoffcc.applications.trifa.TrifaToxService.orma;

public class HelperFiletransfer
{
    private static final String TAG = "trifa.Hlp.Filetransfer";

    public static boolean check_auto_accept_incoming_filetransfer(Message message)
    {
        try
        {
            String mimeType = URLConnection.guessContentTypeFromName(
                    get_filetransfer_filename_from_id(message.filetransfer_id).toLowerCase());
            Log.i(TAG, "check_auto_accept_incoming_filetransfer:mime-type=" + mimeType);

            if (mimeType != null)
            {
                if (PREF__auto_accept_image)
                {
                    Log.i(TAG, "check_auto_accept_incoming_filetransfer:image:check");
                    if (get_filetransfer_filesize_from_id(message.filetransfer_id) <=
                        (long) AUTO_ACCEPT_FT_MAX_IMAGE_SIZE_IN_MB * 1024 *
                        1024) // if file size is smaller than x MByte accept FT
                    {
                        Log.i(TAG, "check_auto_accept_incoming_filetransfer:image:size:check");
                        if (mimeType.startsWith("image/"))
                        {
                            if (get_filetransfer_state_from_id(message.filetransfer_id) == TOX_FILE_CONTROL_PAUSE.value)
                            {
                                // accept FT
                                set_filetransfer_accepted_from_id(message.filetransfer_id);
                                set_filetransfer_state_from_id(message.filetransfer_id, TOX_FILE_CONTROL_RESUME.value);
                                set_message_accepted_from_id(message.id);
                                set_message_state_from_id(message.id, TOX_FILE_CONTROL_RESUME.value);
                                tox_file_control(tox_friend_by_public_key__wrapper(message.tox_friendpubkey),
                                                 get_filetransfer_filenum_from_id(message.filetransfer_id),
                                                 TOX_FILE_CONTROL_RESUME.value);

                                // update message view
                                update_single_message_from_messge_id(message.id, true);
                                // Log.i(TAG, "check_auto_accept_incoming_filetransfer:image:accepted");
                                return true;
                            }
                        }
                    }
                }

                if (PREF__auto_accept_video)
                {
                    Log.i(TAG, "check_auto_accept_incoming_filetransfer:video:check");
                    if (get_filetransfer_filesize_from_id(message.filetransfer_id) <=
                        (long) AUTO_ACCEPT_FT_MAX_VIDEO_SIZE_IN_MB * 1024 *
                        1024) // if file size is smaller than xx MByte accept FT
                    {
                        Log.i(TAG, "check_auto_accept_incoming_filetransfer:video:size:check");
                        if (mimeType.startsWith("video/"))
                        {
                            if (get_filetransfer_state_from_id(message.filetransfer_id) == TOX_FILE_CONTROL_PAUSE.value)
                            {
                                // accept FT
                                set_filetransfer_accepted_from_id(message.filetransfer_id);
                                set_filetransfer_state_from_id(message.filetransfer_id, TOX_FILE_CONTROL_RESUME.value);
                                set_message_accepted_from_id(message.id);
                                set_message_state_from_id(message.id, TOX_FILE_CONTROL_RESUME.value);
                                tox_file_control(tox_friend_by_public_key__wrapper(message.tox_friendpubkey),
                                                 get_filetransfer_filenum_from_id(message.filetransfer_id),
                                                 TOX_FILE_CONTROL_RESUME.value);

                                // update message view
                                update_single_message_from_messge_id(message.id, true);
                                // Log.i(TAG, "check_auto_accept_incoming_filetransfer:video:accepted");
                                return true;
                            }
                        }
                    }
                }
            }

            if (PREF__auto_accept_all_upto)
            {
                Log.i(TAG, "check_auto_accept_incoming_filetransfer:other:check");
                if (get_filetransfer_filesize_from_id(message.filetransfer_id) <=
                    (long) AUTO_ACCEPT_FT_MAX_ANYKIND_SIZE_IN_MB * 1014 *
                    1024) // if file size is smaller than xx GByte accept FT
                {
                    Log.i(TAG, "check_auto_accept_incoming_filetransfer:other:size:check");
                    if (get_filetransfer_state_from_id(message.filetransfer_id) == TOX_FILE_CONTROL_PAUSE.value)
                    {
                        // accept FT
                        set_filetransfer_accepted_from_id(message.filetransfer_id);
                        set_filetransfer_state_from_id(message.filetransfer_id, TOX_FILE_CONTROL_RESUME.value);
                        set_message_accepted_from_id(message.id);
                        set_message_state_from_id(message.id, TOX_FILE_CONTROL_RESUME.value);
                        tox_file_control(tox_friend_by_public_key__wrapper(message.tox_friendpubkey),
                                         get_filetransfer_filenum_from_id(message.filetransfer_id),
                                         TOX_FILE_CONTROL_RESUME.value);

                        // update message view
                        update_single_message_from_messge_id(message.id, true);
                        // Log.i(TAG, "check_auto_accept_incoming_filetransfer:video:accepted");
                        return true;
                    }
                }
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }

    public static String get_incoming_filetransfer_local_filename(String incoming_filename, String friend_pubkey_str)
    {
        String result = TrifaSetPatternActivity.filter_out_specials_from_filepath(incoming_filename);
        String wanted_full_filename_path = VFS_PREFIX + VFS_FILE_DIR + "/" + friend_pubkey_str;

        // Log.i(TAG, "check_auto_accept_incoming_filetransfer:start=" + incoming_filename + " " + result + " " +
        //           wanted_full_filename_path);

        info.guardianproject.iocipher.File f1 = new info.guardianproject.iocipher.File(
                wanted_full_filename_path + "/" + result);

        if (f1.exists())
        {
            Random random = new Random();
            long new_random_log = (long) random.nextInt() + (1L << 31);

            // Log.i(TAG, "check_auto_accept_incoming_filetransfer:new_random_log=" + new_random_log);

            String random_filename_addon = TrifaSetPatternActivity.filter_out_specials(
                    TrifaSetPatternActivity.bytesToString(TrifaSetPatternActivity.sha256(
                            TrifaSetPatternActivity.StringToBytes2("" + new_random_log))));

            // Log.i(TAG, "check_auto_accept_incoming_filetransfer:random_filename_addon=" + random_filename_addon);

            String extension = "";

            try
            {
                extension = result.substring(result.lastIndexOf("."));

                if (extension.equalsIgnoreCase("."))
                {
                    extension = "";
                }
            }
            catch (Exception e)
            {
                extension = "";
            }

            result = result + "_" + random_filename_addon + extension;

            // Log.i(TAG, "check_auto_accept_incoming_filetransfer:result=" + result);
        }

        return result;
    }

    public static long get_filetransfer_id_from_friendnum_and_filenum(long friend_number, long file_number)
    {
        try
        {
            // Log.i(TAG, "get_filetransfer_id_from_friendnum_and_filenum:friend_number=" + friend_number + " file_number=" + file_number);
            long ft_id = orma.selectFromFiletransfer().
                    tox_public_key_stringEq(HelperFriend.tox_friend_get_public_key__wrapper(friend_number)).
                    file_numberEq(file_number).
                    orderByIdDesc().
                    toList().
                    get(0).id;
            // Log.i(TAG, "get_filetransfer_id_from_friendnum_and_filenum:ft_id=" + ft_id);
            // ----- DEBUG -----
            //            try
            //            {
            //                Filetransfer ft_tmp = orma.selectFromFiletransfer().idEq(ft_id).get(0);
            //                //if (ft_tmp.kind != TOX_FILE_KIND_AVATAR.value)
            //                //{
            //                Log.i(TAG, "get_filetransfer_id_from_friendnum_and_filenum:ft full=" + ft_tmp);
            //                //}
            //            }
            //            catch (Exception e)
            //            {
            //                e.printStackTrace();
            //                Log.i(TAG, "get_filetransfer_id_from_friendnum_and_filenum:EE2:" + e.getMessage());
            //            }
            // ----- DEBUG -----
            return ft_id;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "get_filetransfer_id_from_friendnum_and_filenum:EE:" + e.getMessage());
            return -1;
        }
    }

    public static void delete_filetransfer_tmpfile(long friend_number, long file_number)
    {
        try
        {
            delete_filetransfer_tmpfile(orma.selectFromFiletransfer().tox_public_key_stringEq(
                    HelperFriend.tox_friend_get_public_key__wrapper(friend_number)).file_numberEq(
                    file_number).get(0).id);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void delete_filetransfer_tmpfile(long filetransfer_id)
    {
        try
        {
            Filetransfer ft = (Filetransfer) orma.selectFromFiletransfer().idEq(filetransfer_id).get(0);

            if (MainActivity.VFS_ENCRYPT)
            {
                info.guardianproject.iocipher.File f1 = new info.guardianproject.iocipher.File(
                        VFS_PREFIX + VFS_TMP_FILE_DIR + "/" + ft.tox_public_key_string + "/" + ft.file_name);
                f1.delete();
            }
            else
            {
                java.io.File f1 = new java.io.File(
                        VFS_PREFIX + VFS_TMP_FILE_DIR + "/" + ft.tox_public_key_string + "/" + ft.file_name);
                f1.delete();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void set_filetransfer_state_from_id(long filetransfer_id, int state)
    {
        try
        {
            orma.updateFiletransfer().idEq(filetransfer_id).state(state).execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void set_filetransfer_start_sending_from_id(long filetransfer_id)
    {
        try
        {
            orma.updateFiletransfer().idEq(filetransfer_id).ft_outgoing_started(true).execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void set_filetransfer_accepted_from_id(long filetransfer_id)
    {
        try
        {
            orma.updateFiletransfer().idEq(filetransfer_id).ft_accepted(true).execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static long get_filetransfer_filenum_from_id(long filetransfer_id)
    {
        try
        {
            if (orma.selectFromFiletransfer().idEq(filetransfer_id).count() == 1)
            {
                return orma.selectFromFiletransfer().idEq(filetransfer_id).get(0).file_number;
            }
            else
            {
                return -1;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return -1;
        }
    }

    public static long get_filetransfer_filesize_from_id(long filetransfer_id)
    {
        try
        {
            if (orma.selectFromFiletransfer().idEq(filetransfer_id).count() == 1)
            {
                return orma.selectFromFiletransfer().idEq(filetransfer_id).get(0).filesize;
            }
            else
            {
                return -1;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return -1;
        }
    }

    public static long get_filetransfer_state_from_id(long filetransfer_id)
    {
        try
        {
            if (orma.selectFromFiletransfer().idEq(filetransfer_id).count() == 1)
            {
                return orma.selectFromFiletransfer().idEq(filetransfer_id).get(0).state;
            }
            else
            {
                return TOX_FILE_CONTROL_CANCEL.value;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return TOX_FILE_CONTROL_CANCEL.value;
        }
    }

    public static String get_filetransfer_filename_from_id(long filetransfer_id)
    {
        try
        {
            if (orma.selectFromFiletransfer().idEq(filetransfer_id).count() == 1)
            {
                return orma.selectFromFiletransfer().idEq(filetransfer_id).get(0).file_name;
            }
            else
            {
                return "";
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "";
        }
    }

    public static String get_filetransfer_path_name_from_id(long filetransfer_id)
    {
        try
        {
            if (orma.selectFromFiletransfer().idEq(filetransfer_id).count() == 1)
            {
                return orma.selectFromFiletransfer().idEq(filetransfer_id).get(0).path_name;
            }
            else
            {
                return "";
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "";
        }
    }

    public static void set_filetransfer_for_message_from_friendnum_and_filenum(long friend_number, long file_number, long ft_id)
    {
        try
        {
            set_filetransfer_for_message_from_filetransfer_id(orma.selectFromFiletransfer().
                    tox_public_key_stringEq(HelperFriend.tox_friend_get_public_key__wrapper(friend_number)).
                    file_numberEq(file_number).
                    orderByIdDesc().
                    get(0).id, ft_id);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void set_filetransfer_for_message_from_filetransfer_id(long filetransfer_id, long ft_id)
    {
        try
        {
            orma.updateMessage().filetransfer_idEq(filetransfer_id).filetransfer_id(ft_id).execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void delete_filetransfers_from_friendnum_and_filenum(long friend_number, long file_number)
    {
        try
        {
            long del_ft_id = orma.selectFromFiletransfer().
                    tox_public_key_stringEq(HelperFriend.tox_friend_get_public_key__wrapper(friend_number)).
                    file_numberEq(file_number).
                    orderByIdDesc().
                    get(0).id;
            // Log.i(TAG, "delete_ft:id=" + del_ft_id);
            delete_filetransfers_from_id(del_ft_id);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void delete_filetransfers_from_id(long filetransfer_id)
    {
        try
        {
            // Log.i(TAG, "delete_ft:id=" + filetransfer_id);
            orma.deleteFromFiletransfer().idEq(filetransfer_id).execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static void cancel_filetransfer_f(final Filetransfer f)
    {
        try
        {
            if (f == null)
            {
                return;
            }

            long ft_id = f.id;
            long msg_id = HelperMessage.get_message_id_from_filetransfer_id(ft_id);

            if (f.direction == TRIFA_FT_DIRECTION_INCOMING.value)
            {
                if ((f.kind == TOX_FILE_KIND_DATA.value) || (f.kind == TOX_FILE_KIND_FTV2.value))
                {
                    delete_filetransfer_tmpfile(ft_id);
                    HelperMessage.set_message_state_from_id(msg_id, TOX_FILE_CONTROL_CANCEL.value);
                    remove_vfs_ft_from_cache(f);
                    set_filetransfer_for_message_from_filetransfer_id(ft_id, -1);
                    delete_filetransfers_from_id(ft_id);
                    try
                    {
                        if (f.id != -1)
                        {
                            HelperMessage.update_single_message_from_messge_id(msg_id, true);
                        }
                    }
                    catch (Exception e)
                    {
                    }
                }
                else // avatar FT
                {
                    set_filetransfer_state_from_id(ft_id, TOX_FILE_CONTROL_CANCEL.value);
                    HelperMessage.set_message_state_from_id(msg_id, TOX_FILE_CONTROL_CANCEL.value);
                    remove_vfs_ft_from_cache(f);
                    delete_filetransfer_tmpfile(ft_id);
                    delete_filetransfers_from_id(ft_id);
                }
            }
            else // outgoing FT
            {
                if ((f.kind == TOX_FILE_KIND_DATA.value) || (f.kind == TOX_FILE_KIND_FTV2.value))
                {
                    HelperMessage.set_message_state_from_id(msg_id, TOX_FILE_CONTROL_CANCEL.value);
                    remove_ft_from_cache(f);
                    set_filetransfer_for_message_from_filetransfer_id(ft_id, -1);
                    delete_filetransfers_from_id(ft_id);
                    try
                    {
                        if (f.id != -1)
                        {
                            HelperMessage.update_single_message_from_messge_id(msg_id, true);
                        }
                    }
                    catch (Exception e)
                    {
                    }
                }
                else // avatar FT
                {
                    set_filetransfer_state_from_id(ft_id, TOX_FILE_CONTROL_CANCEL.value);
                    if (msg_id > -1)
                    {
                        HelperMessage.set_message_state_from_id(msg_id, TOX_FILE_CONTROL_CANCEL.value);
                    }
                    remove_ft_from_cache(f);
                    delete_filetransfer_tmpfile(ft_id);
                    delete_filetransfers_from_id(ft_id);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static void cancel_filetransfer(long friend_number, long file_number)
    {
        // Log.i(TAG, "FTFTFT:cancel_filetransfer");
        Filetransfer f = null;

        try
        {
            f = (Filetransfer) orma.selectFromFiletransfer().
                    file_numberEq(file_number).
                    tox_public_key_stringEq(HelperFriend.tox_friend_get_public_key__wrapper(friend_number)).
                    orderByIdDesc().
                    toList().get(0);

            if (f.direction == TRIFA_FT_DIRECTION_INCOMING.value)
            {
                if ((f.kind == TOX_FILE_KIND_DATA.value) || (f.kind == TOX_FILE_KIND_FTV2.value))
                {
                    long ft_id = get_filetransfer_id_from_friendnum_and_filenum(friend_number, file_number);
                    long msg_id = HelperMessage.get_message_id_from_filetransfer_id_and_friendnum(ft_id, friend_number);
                    // delete tmp file
                    delete_filetransfer_tmpfile(friend_number, file_number);
                    // set state for FT in message
                    HelperMessage.set_message_state_from_id(msg_id, TOX_FILE_CONTROL_CANCEL.value);
                    remove_vfs_ft_from_cache(f);
                    // remove link to any message
                    set_filetransfer_for_message_from_friendnum_and_filenum(friend_number, file_number, -1);
                    // delete FT in DB
                    // Log.i(TAG, "FTFTFT:002");
                    delete_filetransfers_from_friendnum_and_filenum(friend_number, file_number);

                    // update UI
                    // TODO: updates all messages, this is bad
                    // update_all_messages_global(false);
                    try
                    {
                        if (f.id != -1)
                        {
                            HelperMessage.update_single_message_from_messge_id(msg_id, true);
                        }
                    }
                    catch (Exception e)
                    {
                    }
                }
                else // avatar FT
                {
                    long ft_id = get_filetransfer_id_from_friendnum_and_filenum(friend_number, file_number);
                    long msg_id = HelperMessage.get_message_id_from_filetransfer_id_and_friendnum(ft_id, friend_number);
                    set_filetransfer_state_from_id(ft_id, TOX_FILE_CONTROL_CANCEL.value);
                    HelperMessage.set_message_state_from_id(msg_id, TOX_FILE_CONTROL_CANCEL.value);
                    remove_vfs_ft_from_cache(f);
                    // delete tmp file
                    delete_filetransfer_tmpfile(friend_number, file_number);
                    // delete FT in DB
                    // Log.i(TAG, "FTFTFT:003");
                    delete_filetransfers_from_friendnum_and_filenum(friend_number, file_number);
                }
            }
            else // outgoing FT
            {
                if ((f.kind == TOX_FILE_KIND_DATA.value) || (f.kind == TOX_FILE_KIND_FTV2.value))
                {
                    long ft_id = get_filetransfer_id_from_friendnum_and_filenum(friend_number, file_number);
                    long msg_id = HelperMessage.get_message_id_from_filetransfer_id_and_friendnum(ft_id, friend_number);
                    // set state for FT in message
                    HelperMessage.set_message_state_from_id(msg_id, TOX_FILE_CONTROL_CANCEL.value);
                    remove_ft_from_cache(f);
                    // remove link to any message
                    set_filetransfer_for_message_from_friendnum_and_filenum(friend_number, file_number, -1);
                    // delete FT in DB
                    // Log.i(TAG, "FTFTFT:OGFT:002");
                    delete_filetransfers_from_friendnum_and_filenum(friend_number, file_number);

                    // update UI
                    try
                    {
                        if (f.id != -1)
                        {
                            HelperMessage.update_single_message_from_messge_id(msg_id, true);
                        }
                    }
                    catch (Exception e)
                    {
                    }
                }
                else // avatar FT
                {
                    long ft_id = get_filetransfer_id_from_friendnum_and_filenum(friend_number, file_number);
                    long msg_id = HelperMessage.get_message_id_from_filetransfer_id_and_friendnum(ft_id, friend_number);
                    set_filetransfer_state_from_id(ft_id, TOX_FILE_CONTROL_CANCEL.value);

                    if (msg_id > -1)
                    {
                        HelperMessage.set_message_state_from_id(msg_id, TOX_FILE_CONTROL_CANCEL.value);
                    }

                    remove_ft_from_cache(f);

                    // delete tmp file
                    delete_filetransfer_tmpfile(friend_number, file_number);
                    // delete FT in DB
                    // Log.i(TAG, "FTFTFT:OGFT:003");
                    delete_filetransfers_from_friendnum_and_filenum(friend_number, file_number);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static void set_all_filetransfers_inactive()
    {
        try
        {
            List<com.zoffcc.applications.sorm.Filetransfer> fts_active = orma.selectFromFiletransfer().file_numberNotEq(-1).toList();
            for (com.zoffcc.applications.sorm.Filetransfer f : fts_active)
            {
                // Log.i(TAG, "set_all_filetransfers_inactive:cancel:id=" + f.tox_file_id_hex + " filename=" + f.file_name);
                cancel_filetransfer_f((Filetransfer) f);
            }

            orma.updateFiletransfer().
                    file_number(-1).
                    state(TOX_FILE_CONTROL_CANCEL.value).
                    execute();
            Log.i(TAG, "set_all_filetransfers_inactive");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "set_all_filetransfers_inactive:EE:" + e.getMessage());
        }
    }

    static void update_filetransfer_db_current_position(final Filetransfer f)
    {
        orma.updateFiletransfer().
                tox_public_key_stringEq(f.tox_public_key_string).
                file_numberEq(f.file_number).
                current_position(f.current_position).
                execute();
    }

    static void update_filetransfer_db_full(final Filetransfer f)
    {
        orma.updateFiletransfer().
                idEq(f.id).
                tox_public_key_string(f.tox_public_key_string).
                direction(f.direction).
                file_number(f.file_number).
                kind(f.kind).
                state(f.state).
                path_name(f.path_name).
                message_id(f.message_id).
                file_name(f.file_name).
                fos_open(f.fos_open).
                filesize(f.filesize).
                current_position(f.current_position).
                tox_file_id_hex(f.tox_file_id_hex).
                execute();
    }

    static void update_filetransfer_db_full_from_id(final Filetransfer f, long fid)
    {
        orma.updateFiletransfer().
                idEq(fid).
                tox_public_key_string(f.tox_public_key_string).
                direction(f.direction).
                file_number(f.file_number).
                kind(f.kind).
                state(f.state).
                path_name(f.path_name).
                message_id(f.message_id).
                file_name(f.file_name).
                fos_open(f.fos_open).
                filesize(f.filesize).
                current_position(f.current_position).
                execute();
    }

    static void update_filetransfer_db_messageid_from_id(final Filetransfer f, long fid)
    {
        orma.updateFiletransfer().
                idEq(fid).
                message_id(f.message_id).
                execute();
    }

    static long insert_into_filetransfer_db(final Filetransfer f)
    {
        //Thread t = new Thread()
        //{
        //    @Override
        //    public void run()
        //    {
        try
        {
            long row_id = orma.insertIntoFiletransfer(f);
            return row_id;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "insert_into_filetransfer_db:EE:" + e.getMessage());
            return -1;
        }

        //    }
        //};
        //t.start();
    }

    static void flush_and_close_vfs_ft_from_cache(Filetransfer f)
    {
        try
        {
            BufferedOutputStreamCustom fos = cache_ft_fos.get(f.path_name + "/" + f.file_name);
            if (fos != null)
            {
                fos.flush();
                fos.close();
            }
        }
        catch (Exception ignored)
        {
        }
    }

    static void remove_vfs_ft_from_cache(Filetransfer f)
    {
        try
        {
            BufferedOutputStreamCustom fos = cache_ft_fos.get(f.path_name + "/" + f.file_name);
            if (fos != null)
            {
                fos.close();
            }
        }
        catch (Exception ignored)
        {
        }
        // Log.i(TAG, "remove_vfs_ft_from_cache:f:" + f.path_name + "/" + f.file_name);
        cache_ft_fos.remove(f.path_name + "/" + f.file_name);
    }

    static void remove_vfs_ft_from_cache(Message m)
    {
        try
        {
            String path_name = get_filetransfer_filename_from_id(m.filetransfer_id);
            String file_name = get_filetransfer_path_name_from_id(m.filetransfer_id);
            BufferedOutputStreamCustom fos = cache_ft_fos.get(path_name + "/" + file_name);
            // Log.i(TAG, "remove_vfs_ft_from_cache:m:" + path_name + "/" + file_name);
            if (fos != null)
            {
                try
                {
                    fos.close();
                }
                catch (Exception e2)
                {
                }
            }
            cache_ft_fos.remove(path_name + "/" + file_name);
        }
        catch (Exception e2)
        {
        }
    }

    static void remove_ft_from_cache(Filetransfer f)
    {
        try
        {
            if (f.storage_frame_work)
            {
                // Log.i(TAG, "remove_ft_from_cache:" + f.path_name);
                try
                {
                    PositionInputStream fis = cache_ft_fis_saf.get(f.path_name);
                    if (fis != null)
                    {
                        fis.close();
                    }
                }
                catch (Exception e2)
                {
                }
                cache_ft_fis_saf.remove(f.path_name);
            }
        }
        catch (Exception e)
        {
        }
    }

    static void remove_ft_from_cache(Message m)
    {
        try
        {
            if (m.storage_frame_work)
            {
                // Log.i(TAG, "remove_ft_from_cache:" + m.filename_fullpath);
                try
                {
                    PositionInputStream fis = cache_ft_fis_saf.get(m.filename_fullpath);
                    if (fis != null)
                    {
                        fis.close();
                    }
                }
                catch (Exception e2)
                {
                }
                cache_ft_fis_saf.remove(m.filename_fullpath);
            }
        }
        catch (Exception e)
        {
        }
    }

    static void start_outgoing_ft(Message m)
    {
        try
        {
            set_message_queueing_from_id(m.id, false);

            // accept FT
            set_message_start_sending_from_id(m.id);
            set_filetransfer_start_sending_from_id(m.filetransfer_id);

            // update message view
            update_single_message_from_messge_id(m.id, true);

            Filetransfer ft = (Filetransfer) orma.selectFromFiletransfer().
                    idEq(m.filetransfer_id).
                    orderByIdDesc().get(0);

            Log.i(TAG, "MM2MM:8:ft.filesize=" + ft.filesize + " ftid=" + ft.id + " ft.mid=" + ft.message_id + " mid=" +
                       m.id);

            // ------ DEBUG ------
            // Log.i(TAG, "MM2MM:8a:ft full=" + ft);
            // ------ DEBUG ------

            ByteBuffer file_id_buffer = ByteBuffer.allocateDirect(TOX_FILE_ID_LENGTH);

            //if (TOX_CAPABILITY_DECODE(
            //        tox_friend_get_capabilities(tox_friend_by_public_key__wrapper(m.tox_friendpubkey))).ftv2)
            //{
            MainActivity.tox_messagev3_get_new_message_id(file_id_buffer);
            //}
            //else
            //{
            //    byte[] sha256_buf = TrifaSetPatternActivity.sha256(TrifaSetPatternActivity.StringToBytes2(
            //            "" + ft.path_name + ":" + ft.file_name + ":" + ft.filesize));
            //    file_id_buffer.put(sha256_buf);
            //}

            final String file_id_buffer_hex = HelperGeneric.bytesToHex(file_id_buffer.array(),
                                                                       file_id_buffer.arrayOffset(),
                                                                       file_id_buffer.limit()).toUpperCase();
            Log.i(TAG, "TOX_FILE_ID_LENGTH=" + TOX_FILE_ID_LENGTH + " file_id_buffer_hex=" + file_id_buffer_hex);
            ft.tox_file_id_hex = file_id_buffer_hex;

            // actually start sending the file to friend
            long file_number = -1;
            if (TOX_CAPABILITY_DECODE(
                    tox_friend_get_capabilities(tox_friend_by_public_key__wrapper(m.tox_friendpubkey))).ftv2)
            {
                file_number = tox_file_send(tox_friend_by_public_key__wrapper(m.tox_friendpubkey),
                                            ToxVars.TOX_FILE_KIND.TOX_FILE_KIND_FTV2.value, ft.filesize, file_id_buffer,
                                            ft.file_name, ft.file_name.length());
                ft.kind = ToxVars.TOX_FILE_KIND.TOX_FILE_KIND_FTV2.value;
                m.filetransfer_kind = TOX_FILE_KIND_FTV2.value;
            }
            else
            {
                file_number = tox_file_send(tox_friend_by_public_key__wrapper(m.tox_friendpubkey),
                                            ToxVars.TOX_FILE_KIND.TOX_FILE_KIND_DATA.value, ft.filesize, file_id_buffer,
                                            ft.file_name, ft.file_name.length());
                ft.kind = ToxVars.TOX_FILE_KIND.TOX_FILE_KIND_DATA.value;
                m.filetransfer_kind = TOX_FILE_KIND_DATA.value;
            }
            // TODO: handle errors from tox_file_send() here -------

            update_message_in_db_filetransfer_kind(m);

            if (file_number < 0)
            {
                Log.i(TAG, "tox_file_send:EE:" + file_number);

                // cancel FT
                set_filetransfer_state_from_id(m.filetransfer_id, TOX_FILE_CONTROL_CANCEL.value);
                set_message_state_from_id(m.id, TOX_FILE_CONTROL_CANCEL.value);
                remove_ft_from_cache(m);
                // update message view
                update_single_message_from_messge_id(m.id, true);
            }
            else
            {

                Log.i(TAG, "MM2MM:9:new filenum=" + file_number);

                // update the tox file number in DB -----------
                ft.file_number = file_number;
                update_filetransfer_db_full(ft);
                // update the tox file number in DB -----------
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static String remove_bad_chars_from_outgoing_sdcard_filename(final String in)
    {
        try
        {
            return in.
                    replace("/", "_"). // / -> _
                    replace(":", "_"). // : -> _
                    replace("\n", "_"). // \n -> _
                    replace("\r", "_"). // \r -> _
                    replace("\t", "_"). // \t -> _
                    replace("..", "_"); // .. -> _
        }
        catch (Exception ignored)
        {
        }

        return null;
    }

    static outgoing_file_wrapped copy_outgoing_file_to_sdcard_dir(final String filepath, final String filename, final long filesize)
    {
        outgoing_file_wrapped ret = new outgoing_file_wrapped();

        String new_fake_filename_prefix = bytesToHex(TrifaSetPatternActivity.
                sha256(TrifaSetPatternActivity.StringToBytes2(filepath))).
                substring(1, 5).toLowerCase();

        String filename_sd_card = remove_bad_chars_from_outgoing_sdcard_filename(
                new_fake_filename_prefix + "_" + filename);
        //Log.i(TAG, "copy_outgoing_file_to_sdcard_dir:" + filename_sd_card + " : " + new_fake_filename_prefix + " : " +
        //           filepath + " : " + filename);

        if (filename_sd_card == null)
        {
            Log.i(TAG, "copy_outgoing_file_to_sdcard_dir:003");
            return null;
        }

        try
        {

            File dir = new File(SD_CARD_FILES_OUTGOING_WRAPPER_DIR);
            dir.mkdirs();
            String filename2 = filename_sd_card;
            File file = new File(SD_CARD_FILES_OUTGOING_WRAPPER_DIR, filename2);

            long counter = 0;
            while (file.exists())
            {
                String extension = get_fileExt(filename_sd_card);
                if ((extension != null) && (extension.length() > 0))
                {
                    extension = "." + extension;
                }
                else
                {
                    extension = "";
                }

                //filename2 = remove_bad_chars_from_outgoing_sdcard_filename(
                //        filename_sd_card + "." + (long) ((Math.random() * 10000000d)) + extension);

                filename2 = (long) ((Math.random() * 1000000d)) + "_" + filename_sd_card;

                if (filename2 == null)
                {
                    Log.i(TAG, "copy_outgoing_file_to_sdcard_dir:004");
                    return null;
                }

                file = new File(dir, filename2);
                counter++;

                // Log.i(TAG, "copy_outgoing_file_to_sdcard_dir:" + filename2 + " " + SD_CARD_FILES_OUTGOING_WRAPPER_DIR);

                if (counter > 5000)
                {
                    Log.i(TAG, "copy_outgoing_file_to_sdcard_dir:005");
                    return null;
                }
            }

            ret.filepath_wrapped = SD_CARD_FILES_OUTGOING_WRAPPER_DIR;
            ret.filename_wrapped = filename2;

            // now write that contents of the virtual file to the actual file on SD card ----------------
            // Log.i(TAG, "copy_outgoing_file_to_sdcard_dir:filepath=" + filepath);
            InputStream in = null;
            try
            {
                 in = context_s.getContentResolver().openInputStream(Uri.parse(filepath));
            }
            catch(Exception e)
            {
                File fin_regular_file = new File(filepath + "/" +  filename);
                in = new java.io.FileInputStream(fin_regular_file);
            }
            BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream(SD_CARD_FILES_OUTGOING_WRAPPER_DIR + "/" + filename2));

            final int chunk_size = 4096;
            byte[] buffer = new byte[chunk_size];
            int read;
            while ((read = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
            // now write that contents of the virtual file to the actual file on SD card ----------------

            File file2 = new File(SD_CARD_FILES_OUTGOING_WRAPPER_DIR, filename2);
            ret.file_size_wrapped = file2.length();

            if (ret.file_size_wrapped < 1)
            {
                file2.delete();
                Log.i(TAG, "copy_outgoing_file_to_sdcard_dir:006");
                return null;
            }

            return ret;
        }
        catch (Exception e)
        {
            Log.i(TAG, "copy_outgoing_file_to_sdcard_dir:007:" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    static String check_if_incoming_file_was_exported(final Message m)
    {
        try
        {
            final String export_filename = SD_CARD_FILES_EXPORT_DIR + "/" + m.tox_friendpubkey + "/";
            final FileDB file_ = (FileDB) orma.selectFromFileDB().idEq(m.filedb_id).get(0);
            java.io.File f = new java.io.File(export_filename + file_.file_name);
            if (f.exists() && (f.isFile()) && (f.canRead() && (f.length() > 0)))
            {
                return export_filename + file_.file_name;
            }
            else
            {
                // Log.i(TAG, "check_if_incoming_file_was_exported:null");
                return null;
            }
        }
        catch (Exception e)
        {
            Log.i(TAG, "check_if_incoming_file_was_exported:EE:" + e.getMessage());
            return null;
        }
    }

    static void share_local_file(final String filename_fullpath, final Context context)
    {
        Uri file_uri = FileProvider.getUriForFile(context, "com.zoffcc.applications.tofshare.std_fileprovider",
                                                  new java.io.File(filename_fullpath));

        Intent intent = new Intent(Intent.ACTION_SEND, file_uri);
        intent.putExtra(Intent.EXTRA_STREAM, file_uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        String mimeType = myMime.getMimeTypeFromExtension(getFileExtensionFromUrl(filename_fullpath));
        if (mimeType == null)
        {
            mimeType = "application/octet-stream";
        }
        // Log.i(TAG, "share_local_file:mime type: " + mimeType);
        intent.setDataAndType(file_uri, mimeType);
        try
        {
            context.startActivity(Intent.createChooser(intent, "Share"));
        }
        catch (ActivityNotFoundException e)
        {
            Log.e(TAG, "share_local_file:No relevant Activity found", e);
        }
    }

    static void open_local_file(final String filename_fullpath, final Context context)
    {
        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                Uri file_uri = FileProvider.getUriForFile(context, "com.zoffcc.applications.tofshare.std_fileprovider",
                                                          new java.io.File(filename_fullpath));
                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                String mimeType = myMime.getMimeTypeFromExtension(getFileExtensionFromUrl(filename_fullpath));
                Intent newIntent = new Intent(Intent.ACTION_VIEW);
                newIntent.setDataAndType(file_uri, mimeType);
                newIntent.setClipData(ClipData.newRawUri("", file_uri));
                newIntent.putExtra(Intent.EXTRA_STREAM, file_uri);
                newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                // Log.i(TAG, "open_local_file:23:intent=" + newIntent);
                context.startActivity(newIntent);
            }
            else
            {
                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                Intent newIntent = new Intent(Intent.ACTION_VIEW);
                String mimeType = myMime.getMimeTypeFromExtension(getFileExtensionFromUrl(filename_fullpath));
                Uri file_uri = Uri.parse(filename_fullpath);
                newIntent.setDataAndType(file_uri, mimeType);
                // Log.i(TAG, "open_local_file:lt23:intent=" + newIntent);
                context.startActivity(newIntent);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            display_toast("opening file failed", false, 0);
        }
    }

    static void open_local_outgoing_file(final String filename_fullpath, final Context context)
    {
        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                Uri file_uri = FileProvider.getUriForFile(context, "com.zoffcc.applications.tofshare.ext2_provider",
                                                          new java.io.File(filename_fullpath));
                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                String mimeType = myMime.getMimeTypeFromExtension(getFileExtensionFromUrl(filename_fullpath));
                Intent newIntent = new Intent(Intent.ACTION_VIEW);
                newIntent.setDataAndType(file_uri, mimeType);
                newIntent.setClipData(ClipData.newRawUri("", file_uri));
                newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                // Log.i(TAG, "open_local_outgoing_file:23:intent=" + newIntent);
                context.startActivity(newIntent);
            }
            else
            {
                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                Intent newIntent = new Intent(Intent.ACTION_VIEW);
                String mimeType = myMime.getMimeTypeFromExtension(getFileExtensionFromUrl(filename_fullpath));
                Uri file_uri = Uri.parse(filename_fullpath);
                newIntent.setDataAndType(file_uri, mimeType);
                // Log.i(TAG, "open_local_outgoing_file:lt23:intent=" + newIntent);
                context.startActivity(newIntent);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            display_toast("opening outgoing file failed", false, 0);
        }
    }

    static void save_group_incoming_file(String file_path_name, String file_name, byte[] data, long buffer_offset, long write_bytes)
    {
        try
        {
            if (buffer_offset >= 2100000L)
            {
                return;
            }
            if (write_bytes >= 2100000L)
            {
                return;
            }

            info.guardianproject.iocipher.File outfile = new info.guardianproject.iocipher.File(file_path_name + "/" + file_name);
            info.guardianproject.iocipher.FileOutputStream outputStream = new info.guardianproject.iocipher.FileOutputStream(outfile);
            outputStream.write(data, (int)buffer_offset, (int)write_bytes);
            outputStream.flush();
            outputStream.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "save_group_incoming_file:EE");
        }
    }
}
