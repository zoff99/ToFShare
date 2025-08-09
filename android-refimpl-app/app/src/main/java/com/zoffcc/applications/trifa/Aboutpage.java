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

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.zoffcc.applications.tofshare.BuildConfig;
import com.zoffcc.applications.tofshare.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

import static com.zoffcc.applications.trifa.HelperGeneric.dp2px;
import static com.zoffcc.applications.trifa.HelperGeneric.get_trifa_build_str;
import static com.zoffcc.applications.trifa.MainActivity.main_activity_s;

public class Aboutpage extends AppCompatActivity
{
    private static final String TAG = "trifa.Aboutpage";
    ProgressDialog progressDialog2 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        try
        {
            AboutPage aboutPage = new AboutPage(this).
                    isRTL(false).
                    setImage(R.drawable.web_hi_res_512).
                    addWebsite("https://github.com/zoff99/ToxAndroidRefImpl/blob/zoff99/dev003/README.md");

            String abis = "??";
            try
            {
                abis = Build.SUPPORTED_ABIS[0];
            }
            catch(Exception e)
            {
                try
                {
                    abis = Build.CPU_ABI;
                }
                catch(Exception e2)
                {
                }
            }

            aboutPage.setDescription(getString(R.string.Aboutpage_5a) + "\n" + getString(R.string.Aboutpage_5b) + " " +
                                     MainActivity.versionName + "\n\n" +
                                     "Build: " + get_trifa_build_str() + "\n" +
                                     "BuildType: " + BuildConfig.BUILD_TYPE + "\n" +
                                     "TRIfA commit hash: " + BuildConfig.GitHash +
                                     "\n" + "JNI commit hash: " + MainActivity.getNativeLibGITHASH() + "\n" +
                                     "c-toxcore commit hash: " + MainActivity.getNativeLibTOXGITHASH() + "\n" +
                                     "Android API Level: " + Build.VERSION.SDK_INT + "\n" +
                                     "CPU ABI: " + abis);

            Element tox_link = new Element();
            tox_link.setTitle(getString(R.string.Aboutpage_6));
            Intent tox_faq_page = new Intent(Intent.ACTION_VIEW, Uri.parse("https://tox.zoff.cc"));
            tox_link.setIntent(tox_faq_page);
            aboutPage.addItem(tox_link);

            /*
            mehdi.sakout.aboutpage.Element e001 = new mehdi.sakout.aboutpage.Element();
            e001.setTitle(getString(R.string.Aboutpage_2));
            e001.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    try
                    {
                        progressDialog2 = ProgressDialog.show(Aboutpage.this, "", getString(
                                R.string.Aboutpage_4));

                        progressDialog2.setCanceledOnTouchOutside(false);
                        progressDialog2.setOnCancelListener(new DialogInterface.OnCancelListener()
                        {
                            @Override
                            public void onCancel(DialogInterface dialog)
                            {
                            }
                        });

                        // get logcat messages ----------------
                        Logging x = new Logging();
                        Logging.delegate = Aboutpage.this;
                        x.new PopulateLogcatAsyncTask(Aboutpage.this.getApplicationContext()).execute();
                        // get logcat messages ----------------

                    }
                    catch (Exception e)
                    {
                    }
                }
            });
            aboutPage.addItem(e001);
            */

            Element trifa_commit = new Element();
            trifa_commit.setTitle("TRIfA commit hash link");
            Intent trifa_commit_page = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://github.com/zoff99/ToxAndroidRefImpl/commit/" + BuildConfig.GitHash));
            trifa_commit.setIntent(trifa_commit_page);
            aboutPage.addItem(trifa_commit);

            Element jni_commit = new Element();
            jni_commit.setTitle("JNI commit hash link");
            Intent jni_commit_page = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://github.com/zoff99/ToxAndroidRefImpl/commit/" + MainActivity.getNativeLibGITHASH()));
            jni_commit.setIntent(jni_commit_page);
            aboutPage.addItem(jni_commit);

            Element ct_commit = new Element();
            ct_commit.setTitle("c-toxcore commit hash link");
            Intent ct_commit_page = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://github.com/zoff99/c-toxcore/commit/" + MainActivity.getNativeLibTOXGITHASH()));
            ct_commit.setIntent(ct_commit_page);
            aboutPage.addItem(ct_commit);

            //  --------------------------------
            Element el2 = null;
            Intent link2 = null;
            //  --------------------------------
            //  --------------------------------
            //  --------------------------------
            //  --------- used libs ------------
            el2 = new Element();
            el2.setTitle(getString(R.string.Aboutpage_8));
            el2.setIconDrawable(R.drawable.about_icon_github);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.github.gfx.android.orma");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gfx/Android-Orma"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("info.guardianproject.iocipher:IOCipher");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/guardianproject/IOCipher"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.l4digital.fastscroll:fastscroll");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/L4Digital/FastScroll"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.github.bumptech.glide");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bumptech/glide"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("info.guardianproject.netcipher");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/guardianproject/NetCipher"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.mikepenz:fontawesome-typeface");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mikepenz/Android-Iconics"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.mikepenz:google-material-typeface");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mikepenz/Android-Iconics"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.google.zxing:core");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/zxing/zxing"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.github.hotchemi:permissionsdispatcher");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/permissions-dispatcher/PermissionsDispatcher"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.github.angads25:filepicker");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Angads25/android-filepicker"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.vanniktech:emoji-google");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/vanniktech/Emoji"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.google.code.gson");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/google/gson"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.github.medyo:android-about-page");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/medyo/android-about-page"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("de.hdodenhof:circleimageview");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hdodenhof/CircleImageView"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.github.armcha:AutoLinkTextView");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/armcha/AutoLinkTextView"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.github.chrisbanes:PhotoView");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/chrisbanes/PhotoView"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.squareup.okhttp3");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/square/okhttp"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------------------------------
            el2 = new Element();
            el2.setTitle("com.daimajia.numberprogressbar");
            link2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/daimajia/NumberProgressBar"));
            el2.setIntent(link2);
            aboutPage.addItem(el2);
            //  --------------------------------
            //  --------- used libs ------------

            setContentView(aboutPage.create());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "onCreate:EE1:" + e.getMessage());
        }

        try
        {
            // find the large top icon in aboutpage layout
            ImageView icon_big = (ImageView) findViewById(R.id.image);
            Log.i(TAG, "onCreate:icon_big=" + icon_big);

            final Bitmap bm1 = BitmapFactory.decodeResource(getResources(), R.drawable.web_hi_res_512);
            Log.i(TAG, "onCreate:bm1.getWidth()=" + bm1.getWidth() + " bm1.getHeight()=" +
                       bm1.getHeight());
            final Bitmap bm1_scaled = Bitmap.createScaledBitmap(bm1, (int) dp2px(200), (int) dp2px(200), true);
            Log.i(TAG, "onCreate:dp2px(200)=" + dp2px(200));

            icon_big.setImageBitmap(bm1_scaled);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "onCreate:EE2:" + e.getMessage());
        }
    }
}
