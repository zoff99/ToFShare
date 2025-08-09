/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2021 Zoff <zoff@zoff.cc>
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

// inspired by https://github.com/commonsguy/cw-omnibus/tree/master/ContentProvider/Pipe

import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;

import static com.zoffcc.applications.trifa.MainActivity.PREF__allow_open_encrypted_file_via_intent;

public class IOCipherContentProvider extends AbstractFileProvider
{
    public static final String TAG = "IOCipherContentProvider";
    public static final Uri FILES_URI = Uri.parse("content://com.zoffcc.applications.trifa.ext1_fileprovider/");
    private static final int PIPE_BLOCKSIZE = 8192; // do not change this. its the blocksize of IOCipher.
    private MimeTypeMap mimeTypeMap;

    @Override
    public boolean onCreate()
    {
        mimeTypeMap = MimeTypeMap.getSingleton();
        return true;
    }

    @Override
    public long getDataLength(Uri uri)
    {
        if (!PREF__allow_open_encrypted_file_via_intent)
        {
            // Log.i(TAG, "getDataLength:ret:001:allow_open_encrypted_file_via_intent");
            return 0;
        }

        String path = uri.getPath();
        long filesize = new File(path).length();
        if ((filesize < 1) || (filesize > (Long.MAX_VALUE - 2)))
        {
            filesize = AssetFileDescriptor.UNKNOWN_LENGTH;
        }
        // Log.i(TAG, "getDataLength:ret=" + filesize);

        return filesize;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException
    {
        if (!PREF__allow_open_encrypted_file_via_intent)
        {
            // Log.i(TAG, "openFile:err:002:allow_open_encrypted_file_via_intent");
            throw new FileNotFoundException("No Access");
        }

        ParcelFileDescriptor[] pipe = null;
        InputStream in = null;

        try
        {
            pipe = ParcelFileDescriptor.createPipe();
            String path = uri.getPath();
            // Log.i(TAG, "openFile:streaming " + path + " mode=" + mode + "tid = " + Thread.currentThread().getId());

            long filesize = new File(path).length();
            if ((filesize < 1) || (filesize > (Long.MAX_VALUE - 2)))
            {
                filesize = AssetFileDescriptor.UNKNOWN_LENGTH;
            }
            // Log.i(TAG, "openFile:getDataLength:ret=" + filesize);

            in = new FileInputStream(new File(path));
            new PipeFeederThread(in, new AutoCloseOutputStream(pipe[1])).start();
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error opening pipe", e);
            throw new FileNotFoundException("Could not open pipe for: " + uri.toString());
        }

        return (pipe[0]);
    }

    static class PipeFeederThread extends Thread
    {
        InputStream in;
        OutputStream out;

        PipeFeederThread(InputStream in, OutputStream out)
        {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run()
        {
            byte[] buf = new byte[PIPE_BLOCKSIZE];
            int len;

            try
            {
                while ((len = in.read(buf)) >= 0)
                {
                    // Log.i(TAG, "write_data:" + len + " buf.length=" + buf.length + " buf=" + buf + " tid=" +
                    //            Thread.currentThread().getId());
                    out.write(buf, 0, len);
                }

                in.close();
                out.flush();
                out.close();
            }
            catch (IOException e)
            {
                // Log.e(TAG, "File transfer failed:", e);
                try
                {
                    in.close();
                }
                catch (Exception ignored)
                {
                }
                try
                {
                    out.flush();
                }
                catch (Exception ignored)
                {
                }
                try
                {
                    out.close();
                }
                catch (Exception ignored)
                {
                }
            }
        }
    }
}
