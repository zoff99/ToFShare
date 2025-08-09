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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import static com.zoffcc.applications.trifa.HelperGeneric.IPisValid;
import static com.zoffcc.applications.trifa.HelperGeneric.isIPPortValid;
import static com.zoffcc.applications.trifa.HelperGeneric.is_valid_tox_public_key;
import static com.zoffcc.applications.trifa.TRIFAGlobals.PREF_KEY_CUSTOM_BOOTSTRAP_TCP_IP;
import static com.zoffcc.applications.trifa.TRIFAGlobals.PREF_KEY_CUSTOM_BOOTSTRAP_TCP_KEYHEX;
import static com.zoffcc.applications.trifa.TRIFAGlobals.PREF_KEY_CUSTOM_BOOTSTRAP_TCP_PORT;
import static com.zoffcc.applications.trifa.TRIFAGlobals.PREF_KEY_CUSTOM_BOOTSTRAP_UDP_IP;
import static com.zoffcc.applications.trifa.TRIFAGlobals.PREF_KEY_CUSTOM_BOOTSTRAP_UDP_KEYHEX;
import static com.zoffcc.applications.trifa.TRIFAGlobals.PREF_KEY_CUSTOM_BOOTSTRAP_UDP_PORT;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TOX_PUSH_MSG_APP_PLAYSTORE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TOX_PUSH_MSG_APP_WEBDOWNLOAD;

public class SettingsActivity extends AppCompatPreferenceActivity
{
    private static final String TAG = "trifa.SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);

        LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener()
    {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value)
        {
            String stringValue = value.toString();

            if (preference instanceof ListPreference)
            {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

            }
            else if (preference instanceof RingtonePreference)
            {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue))
                {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                }
                else
                {
                    Ringtone ringtone = RingtoneManager.getRingtone(preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null)
                    {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    }
                    else
                    {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            }
            else
            {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context)
    {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >=
               Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private static void bindPreferenceSummaryToValue(Preference preference)
    {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                                                                 PreferenceManager.getDefaultSharedPreferences(
                                                                         preference.getContext()).getString(
                                                                         preference.getKey(), ""));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane()
    {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target)
    {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    protected boolean isValidFragment(String fragmentName)
    {
        return PreferenceFragment.class.getName().equals(fragmentName) ||
               GeneralPreferenceFragment.class.getName().equals(fragmentName) ||
               NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            final SwitchPreference pref_keepnpspam = (SwitchPreference) findPreference("U_keep_nospam");

            if (pref_keepnpspam.isChecked() == true)
            {
                try
                {
                    final Drawable d1 = new IconicsDrawable(pref_keepnpspam.getContext()).
                            icon(FontAwesome.Icon.faw_exclamation_circle).
                            color(getResources().getColor(R.color.md_red_700)).sizeDp(100);
                    pref_keepnpspam.setIcon(d1);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            else
            {
                try
                {
                    pref_keepnpspam.setIcon(null);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            pref_keepnpspam.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    // Here you can enable/disable whatever you need to
                    if (newValue == (Object) true)
                    {
                        try
                        {
                            final Drawable d1 = new IconicsDrawable(preference.getContext()).
                                    icon(FontAwesome.Icon.faw_exclamation_circle).
                                    color(getResources().getColor(R.color.md_red_600)).
                                    sizeDp(100);
                            preference.setIcon(d1);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        try
                        {
                            preference.setIcon(null);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    return true;
                }
            });

            final SwitchPreference pref_startonboot = (SwitchPreference) findPreference("start_on_boot");

            pref_startonboot.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    if (newValue == (Object) true)
                    {
                        try
                        {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(preference.getContext());
                                builder.setTitle("Android 10 and up");
                                // @formatter:off
                                builder.setMessage(
                                        "\n"+
                                        "Starting with Android 10 you must enable a setting that the App can start on Boot and display the Password Screen.\n"+
                                        "Go to Android App Settings for TRIfA and select:\n\n"+
                                        "     \"Display over other apps\"\n\n"+
                                        "then enable\n\n"+
                                        "     \"Allow display over other apps\""+
                                        "\n\n"+
                                        "Sorry for the inconvenience, Google Android does not allow it any other way.\n\n"+
                                        "See:\n\n"+
                                        "https://developer.android.com/guide/components/activities/background-starts\n\n"+
                                        "for more details.\n"
                                );
                                // @formatter:on

                                builder.setPositiveButton("OK", null);
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                    }
                    return true;
                }
            });

            Preference pref_download_push_msg_app = (Preference) findPreference("download_push_msg_app");
            pref_download_push_msg_app.setSummary(TOX_PUSH_MSG_APP_PLAYSTORE);

            pref_download_push_msg_app.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    String[] destinations = {"Google Play", "Github"};

                    AlertDialog.Builder builder = new AlertDialog.Builder(preference.getContext());
                    builder.setTitle("Download from:");
                    builder.setItems(destinations, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            if (which == 1)
                            {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(TOX_PUSH_MSG_APP_WEBDOWNLOAD));
                                startActivity(i);
                            }
                            else
                            {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(TOX_PUSH_MSG_APP_PLAYSTORE));
                                startActivity(i);
                            }
                        }
                    });
                    builder.show();

                    return true;
                }
            });


            Preference pref_custom_bootstrap_nodes = (Preference) findPreference("custom_bootstrap_nodes");

            final SharedPreferences settings = change_custombootstrapnode_summary_text(pref_custom_bootstrap_nodes);

            pref_custom_bootstrap_nodes.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    LayoutInflater li = LayoutInflater.from(preference.getContext());
                    View promptsView = li.inflate(R.layout.custom_bootstrap_prompt, null);

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(preference.getContext());

                    alertDialogBuilder.setView(promptsView);
                    final EditText edit_bootstrap_udp_ip = (EditText) promptsView.findViewById(
                            R.id.edit_bootstrap_udp_ip);
                    final EditText edit_bootstrap_udp_port = (EditText) promptsView.findViewById(
                            R.id.edit_bootstrap_udp_port);
                    final EditText edit_bootstrap_udp_keyhex = (EditText) promptsView.findViewById(
                            R.id.edit_bootstrap_udp_keyhex);
                    final EditText edit_bootstrap_tcp_ip = (EditText) promptsView.findViewById(
                            R.id.edit_bootstrap_tcp_ip);
                    final EditText edit_bootstrap_tcp_port = (EditText) promptsView.findViewById(
                            R.id.edit_bootstrap_tcp_port);
                    final EditText edit_bootstrap_tcp_keyhex = (EditText) promptsView.findViewById(
                            R.id.edit_bootstrap_tcp_keyhex);

                    alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                                                                              new DialogInterface.OnClickListener()
                                                                              {
                                                                                  public void onClick(DialogInterface dialog, int id)
                                                                                  {
                                                                                      try
                                                                                      {
                                                                                          settings.edit().putString(
                                                                                                  PREF_KEY_CUSTOM_BOOTSTRAP_UDP_IP,
                                                                                                  edit_bootstrap_udp_ip.getText().toString()).apply();
                                                                                      }
                                                                                      catch (Exception e)
                                                                                      {
                                                                                          settings.edit().putString(
                                                                                                  PREF_KEY_CUSTOM_BOOTSTRAP_UDP_IP,
                                                                                                  "").apply();
                                                                                      }

                                                                                      try
                                                                                      {
                                                                                          settings.edit().putString(
                                                                                                  PREF_KEY_CUSTOM_BOOTSTRAP_UDP_PORT,
                                                                                                  edit_bootstrap_udp_port.getText().toString()).apply();
                                                                                      }
                                                                                      catch (Exception e)
                                                                                      {
                                                                                          settings.edit().putString(
                                                                                                  PREF_KEY_CUSTOM_BOOTSTRAP_UDP_PORT,
                                                                                                  "").apply();
                                                                                      }

                                                                                      try
                                                                                      {
                                                                                          settings.edit().putString(
                                                                                                  PREF_KEY_CUSTOM_BOOTSTRAP_UDP_KEYHEX,
                                                                                                  edit_bootstrap_udp_keyhex.getText().toString().toUpperCase()).apply();
                                                                                      }
                                                                                      catch (Exception e)
                                                                                      {
                                                                                          settings.edit().putString(
                                                                                                  PREF_KEY_CUSTOM_BOOTSTRAP_UDP_KEYHEX,
                                                                                                  "").apply();
                                                                                      }

                                                                                      try
                                                                                      {
                                                                                          settings.edit().putString(
                                                                                                  PREF_KEY_CUSTOM_BOOTSTRAP_TCP_IP,
                                                                                                  edit_bootstrap_tcp_ip.getText().toString()).apply();
                                                                                      }
                                                                                      catch (Exception e)
                                                                                      {
                                                                                          settings.edit().putString(
                                                                                                  PREF_KEY_CUSTOM_BOOTSTRAP_TCP_IP,
                                                                                                  "").apply();
                                                                                      }

                                                                                      try
                                                                                      {
                                                                                          settings.edit().putString(
                                                                                                  PREF_KEY_CUSTOM_BOOTSTRAP_TCP_PORT,
                                                                                                  edit_bootstrap_tcp_port.getText().toString()).apply();
                                                                                      }
                                                                                      catch (Exception e)
                                                                                      {
                                                                                          settings.edit().putString(
                                                                                                  PREF_KEY_CUSTOM_BOOTSTRAP_TCP_PORT,
                                                                                                  "").apply();
                                                                                      }

                                                                                      try
                                                                                      {
                                                                                          settings.edit().putString(
                                                                                                  PREF_KEY_CUSTOM_BOOTSTRAP_TCP_KEYHEX,
                                                                                                  edit_bootstrap_tcp_keyhex.getText().toString().toUpperCase()).apply();
                                                                                      }
                                                                                      catch (Exception e)
                                                                                      {
                                                                                          settings.edit().putString(
                                                                                                  PREF_KEY_CUSTOM_BOOTSTRAP_TCP_KEYHEX,
                                                                                                  "").apply();
                                                                                      }

                                                                                      try
                                                                                      {
                                                                                          change_custombootstrapnode_summary_text(
                                                                                                  pref_custom_bootstrap_nodes);
                                                                                      }
                                                                                      catch (Exception e)
                                                                                      {
                                                                                      }
                                                                                  }
                                                                              }).setNegativeButton("Cancel",
                                                                                                   new DialogInterface.OnClickListener()
                                                                                                   {
                                                                                                       public void onClick(DialogInterface dialog, int id)
                                                                                                       {
                                                                                                           dialog.cancel();
                                                                                                       }
                                                                                                   });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    return true;
                }
            });

            final ListPreference pref_dark_mode_pref = (ListPreference) findPreference("dark_mode_pref");

            pref_dark_mode_pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    try
                    {
                        if (((String) newValue).equals("0"))
                        {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        }
                        else if (((String) newValue).equals("1"))
                        {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        }
                        else
                        {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        }
                        getActivity().recreate();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    return true;
                }
            });
        }
    }

    @NonNull
    private static SharedPreferences change_custombootstrapnode_summary_text(Preference pref_custom_bootstrap_nodes)
    {
        pref_custom_bootstrap_nodes.setSummary("using default bootstrap nodes.");

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
                pref_custom_bootstrap_nodes.getContext());
        final String bs_udp_ip = settings.getString(PREF_KEY_CUSTOM_BOOTSTRAP_UDP_IP, "");
        final String bs_udp_port = settings.getString(PREF_KEY_CUSTOM_BOOTSTRAP_UDP_PORT, "");
        final String bs_udp_keyhex = settings.getString(PREF_KEY_CUSTOM_BOOTSTRAP_UDP_KEYHEX, "");
        final String bs_tcp_ip = settings.getString(PREF_KEY_CUSTOM_BOOTSTRAP_TCP_IP, "");
        final String bs_tcp_port = settings.getString(PREF_KEY_CUSTOM_BOOTSTRAP_TCP_PORT, "");
        final String bs_tcp_keyhex = settings.getString(PREF_KEY_CUSTOM_BOOTSTRAP_TCP_KEYHEX, "");

        boolean udp_valid = false;
        boolean tcp_valid = false;

        if ((bs_udp_ip.length() > 0) && (bs_udp_port.length() > 0) && (IPisValid(bs_udp_ip)) &&
            (isIPPortValid(bs_udp_port)) && (is_valid_tox_public_key(bs_udp_keyhex)))
        {
            udp_valid = true;
        }

        if ((bs_tcp_ip.length() > 0) && (bs_tcp_port.length() > 0) && (IPisValid(bs_tcp_ip)) &&
            (isIPPortValid(bs_tcp_port)) && (is_valid_tox_public_key(bs_tcp_keyhex)))
        {
            tcp_valid = true;
        }

        if (udp_valid)
        {
            pref_custom_bootstrap_nodes.setSummary(
                    "using custom bootstrapnode:\nfor UDP: " + bs_udp_ip + ":" + bs_udp_port + "\nKEY: " +
                    bs_udp_keyhex);
        }

        if (tcp_valid)
        {
            if (!udp_valid)
            {
                pref_custom_bootstrap_nodes.setSummary(
                        "using custom bootstrapnode:\nfor TCP: " + bs_tcp_ip + ":" + bs_tcp_port + "\nKEY: " +
                        bs_tcp_keyhex);
            }
            else
            {
                pref_custom_bootstrap_nodes.setSummary(
                        "using custom bootstrapnode:\nfor TCP: " + bs_tcp_ip + ":" + bs_tcp_port + "\nKEY: " +
                        bs_tcp_keyhex);
            }
        }
        return settings;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);

            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }
    }
}
