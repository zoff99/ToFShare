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
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zoffcc.applications.nativeaudio.NativeAudio;

import java.nio.ByteBuffer;

import androidx.appcompat.app.AppCompatActivity;

import static com.zoffcc.applications.nativeaudio.NativeAudio.n_audio_in_buffer_max_count;
import static com.zoffcc.applications.nativeaudio.NativeAudio.setMicGainFactor;
import static com.zoffcc.applications.nativeaudio.NativeAudio.setMicGainToggle;
import static com.zoffcc.applications.nativeaudio.NativeAudio.set_rec_preset;
import static com.zoffcc.applications.trifa.AudioReceiver.channels_;
import static com.zoffcc.applications.trifa.AudioReceiver.sampling_rate_;
import static com.zoffcc.applications.trifa.AudioRecording.audio_engine_starting;
import static com.zoffcc.applications.trifa.HelperGeneric.reset_audio_mode;
import static com.zoffcc.applications.trifa.HelperGeneric.restart_audio_system;
import static com.zoffcc.applications.trifa.HelperGeneric.set_audio_to_loudspeaker;
import static com.zoffcc.applications.trifa.MainActivity.PREF__mic_gain_factor_toggle;
import static com.zoffcc.applications.trifa.MainActivity.PREF_mic_gain_factor;
import static com.zoffcc.applications.trifa.MainActivity.audio_buffer_2;
import static com.zoffcc.applications.trifa.MainActivity.audio_buffer_2_read_length;
import static com.zoffcc.applications.trifa.MainActivity.audio_out_buffer_mult;
import static com.zoffcc.applications.trifa.MainActivity.set_JNI_audio_buffer2;

public class AudioRoundtripActivity extends AppCompatActivity
{
    private static final String TAG = "trifa.ARoundtrActy";
    private TextView roundtrip_time_textview;
    private TextView roundtrip_info_textview;
    private Button roundtrip_start_button;
    private Button fastpath_toggle_button = null;
    private boolean fastpath_active = false;
    private boolean test_running = false;
    private Thread LatencyTestThread = null;
    public static boolean LatencyTestActive = false;
    public static long d1 = 0;
    public static long measured_audio_latency = -1;
    public static boolean measured_audio_latency_set = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audioroundtrip);
        roundtrip_time_textview = findViewById(R.id.roundtrip_time_textview);
        roundtrip_info_textview = findViewById(R.id.roundtrip_info_textview);
        roundtrip_start_button = findViewById(R.id.roundtrip_start_button);
        fastpath_toggle_button = findViewById(R.id.fastpath_toggle_button);

        try
        {
            PackageManager pm = getPackageManager();
            boolean claimsFeature = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);
            Log.i(TAG, "FEATURE_AUDIO_LOW_LATENCY=" + claimsFeature);
            if (claimsFeature)
            {
                roundtrip_info_textview.setText("has FEATURE_AUDIO_LOW_LATENCY");
            }
        }
        catch (Exception ignored)
        {
        }

        try
        {
            PackageManager pm = getPackageManager();
            boolean claimsFeature = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO);
            Log.i(TAG, "FEATURE_AUDIO_PRO=" + claimsFeature);
            if (claimsFeature)
            {
                roundtrip_info_textview.setText(roundtrip_info_textview.getText() + "\nhas FEATURE_AUDIO_PRO");
            }
        }
        catch (Exception ignored)
        {
        }

        try
        {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            String sampleRate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            String framesPerBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            Log.i(TAG, "AUDIO_LOW_LATENCY:sampleRate=" + sampleRate + " framesPerBuffer=" + framesPerBuffer);
            roundtrip_info_textview.setText(
                    roundtrip_info_textview.getText() + "\nAUDIO_LOW_LATENCY values:\nsampleRate=" + sampleRate +
                    " framesPerBuffer=" + framesPerBuffer);
        }
        catch (Exception ignored)
        {
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        test_running = false;
        LatencyTestActive = false;

        fastpath_active = false;
        try
        {
            set_rec_preset(!fastpath_active);
        }
        catch (Exception ignored)
        {
        }
        fastpath_toggle_button.setText("slow audio path");

        fastpath_toggle_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                try
                {
                    if (test_running)
                    {
                        if (fastpath_active)
                        {
                            fastpath_active = false;
                            switch_fast_audio_path(fastpath_active);
                            fastpath_toggle_button.setText("slow audio path");
                        }
                        else
                        {
                            fastpath_active = true;
                            switch_fast_audio_path(fastpath_active);
                            fastpath_toggle_button.setText("fast audio path");
                        }
                    }
                }
                catch (Exception ignored)
                {
                }
            }
        });

        roundtrip_start_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                try
                {
                    if (test_running)
                    {
                        test_running = false;
                        roundtrip_start_button.setText("Start");
                        roundtrip_time_textview.setText("stopping ...");
                        try
                        {
                            LatencyTestThread.join();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        roundtrip_time_textview.setText("test stopped");
                    }
                    else
                    {
                        test_running = true;
                        roundtrip_start_button.setText("Stop");
                        roundtrip_time_textview.setText("running ...");
                        start_test();
                    }
                }
                catch (Exception ignored)
                {
                }
            }
        });
    }

    public static byte[] createSineWaveBuffer(double freq, int ms)
    {
        final int SAMPLE_RATE = 48000;
        int samples = (int) ((ms * SAMPLE_RATE) / 1000);
        byte[] output = new byte[samples];
        //
        double period = (double) SAMPLE_RATE / freq;
        for (int i = 0; i < output.length; i++)
        {
            double angle = 2.0 * Math.PI * i / period;
            output[i] = (byte) (Math.sin(angle) * 127f);
        }
        return output;
    }

    private void start_test()
    {
        LatencyTestThread = new Thread()
        {
            @Override
            public void run()
            {
                Log.i(TAG, "LatencyTestThread:starting");

                try
                {
                    this.setName("t_latency");
                    android.os.Process.setThreadPriority(Thread.MAX_PRIORITY);
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }


                /*
                 *
                 * init native audio
                 *
                 */
                reset_audio_mode();
                try
                {
                    setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                }
                catch (Exception e2)
                {
                    e2.printStackTrace();
                }

                AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                set_audio_to_loudspeaker(manager);
                int sample_count = 1920;
                int sampling_rate = 48000;
                int channels = 1;
                AudioReceiver.buffer_size =
                        ((int) ((48000 * 2) * 2)) * audio_out_buffer_mult; // TODO: this is really bad
                AudioReceiver.sleep_millis = (int) (((float) sample_count / (float) sampling_rate) * 1000.0f *
                                                    0.9f); // TODO: this is bad also
                Log.i(TAG, "LatencyTestThread:read:init buffer_size=" + AudioReceiver.buffer_size);
                Log.i(TAG, "LatencyTestThread:read:init sleep_millis=" + AudioReceiver.sleep_millis);
                try
                {
                    if (audio_buffer_2 != null)
                    {
                        audio_buffer_2.clear();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                if (audio_buffer_2 == null)
                {
                    audio_buffer_2 = ByteBuffer.allocateDirect(AudioReceiver.buffer_size);
                    Log.i(TAG, "LatencyTestThread:audio_buffer_2[" + 0 + "] size=" + AudioReceiver.buffer_size);
                    set_JNI_audio_buffer2(audio_buffer_2);
                }
                audio_buffer_2_read_length[0] = 0;
                int frame_size_ = (int) ((sample_count * 1000) / sampling_rate);
                Log.i(TAG, "LatencyTestThread:audio_buffer_play size=" + AudioReceiver.buffer_size);
                sampling_rate_ = sampling_rate;
                channels_ = channels;

                try
                {
                    if (!AudioRecording.stopped)
                    {
                        AudioRecording.close();
                        CallingActivity.audio_thread.join();
                        CallingActivity.audio_thread = null;
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                try
                {
                    if (!AudioReceiver.stopped)
                    {
                        AudioReceiver.close();
                        CallingActivity.audio_receiver_thread.join();
                        CallingActivity.audio_receiver_thread = null;
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                try
                {
                    if (AudioReceiver.stopped)
                    {
                        CallingActivity.audio_receiver_thread = new AudioReceiver();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                try
                {
                    if (AudioRecording.stopped)
                    {
                        CallingActivity.audio_thread = new AudioRecording();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                try
                {
                    Thread.sleep(300);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                if (audio_engine_starting)
                {
                    // native audio engine is down. lets wait for it to get up ...
                    while (audio_engine_starting == true)
                    {
                        try
                        {
                            Thread.sleep(20);
                            Log.i(TAG, "LatencyTestThread:sleep --------");
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
                audio_buffer_2.position(0);
                set_audio_to_loudspeaker(manager);

                fastpath_active = false;
                switch_fast_audio_path(false);

                /*
                 *
                 * init native audio
                 *
                 */

                try
                {
                    d1 = 0;
                    byte[] toneBuffer = createSineWaveBuffer(800, 80); // yeah it's bytes. but it'll do for now
                    Log.i(TAG, "LatencyTestThread:toneBuffer:" + toneBuffer.length);
                    byte[] silenceBuffer = new byte[3840];
                    Log.i(TAG, "LatencyTestThread:silenceBuffer:" + silenceBuffer.length);

                    final long silence_iters = (1000 / 40) * 4; // play a sound about every 4 seconds
                    final long sound_iters = 2;
                    long cur_iter = 0;
                    LatencyTestActive = true;
                    setMicGainToggle(false);
                    setMicGainFactor(1.0f);
                    while (test_running)
                    {
                        //Log.i(TAG, "LatencyTestThread:fill_play_buffer:" +
                        //           NativeAudio.n_audio_buffer[NativeAudio.n_cur_buf].position() + " " +
                        //           NativeAudio.n_audio_buffer[NativeAudio.n_cur_buf].limit());
                        NativeAudio.n_audio_buffer[NativeAudio.n_cur_buf].rewind();
                        if (cur_iter >= silence_iters)
                        {
                            if (cur_iter == silence_iters)
                            {
                                Log.i(TAG, "LatencyTestThread:--sound:start--");
                                measured_audio_latency_set = false;
                                d1 = SystemClock.uptimeMillis();
                            }
                            else
                            {
                                // put result into text box

                                final Thread update_ui_textbox = new Thread()
                                {
                                    @Override
                                    public void run()
                                    {
                                        try
                                        {
                                            Thread.sleep(1000);
                                            runOnUiThread(() -> roundtrip_time_textview.setText(
                                                    String.valueOf("latency in ms = " + measured_audio_latency)));
                                        }
                                        catch (Exception ignored)
                                        {
                                        }
                                    }
                                };
                                update_ui_textbox.start();
                            }

                            Log.i(TAG, "LatencyTestThread:--sound--");
                            try
                            {
                                NativeAudio.n_audio_buffer[NativeAudio.n_cur_buf].rewind();
                                NativeAudio.n_audio_buffer[NativeAudio.n_cur_buf].put(toneBuffer,0,NativeAudio.n_audio_buffer[NativeAudio.n_cur_buf].limit());
                            }
                            catch(Exception e2)
                            {
                                e2.printStackTrace();
                            }
                        }
                        else
                        {
                            try
                            {
                                NativeAudio.n_audio_buffer[NativeAudio.n_cur_buf].rewind();
                                Log.i(TAG, "bufsize1:"+NativeAudio.n_audio_buffer[NativeAudio.n_cur_buf].limit()+" bufsize2:"+silenceBuffer.length);
                                NativeAudio.n_audio_buffer[NativeAudio.n_cur_buf].put(silenceBuffer,0,NativeAudio.n_audio_buffer[NativeAudio.n_cur_buf].limit());
                            }
                            catch(Exception e1)
                            {
                                e1.printStackTrace();
                            }
                        }

                        audio_buffer_2.position(0);
                        int res = NativeAudio.PlayPCM16(NativeAudio.n_cur_buf);
                        if (NativeAudio.n_cur_buf + 1 >= n_audio_in_buffer_max_count)
                        {
                            NativeAudio.n_cur_buf = 0;
                        }
                        else
                        {
                            NativeAudio.n_cur_buf++;
                        }
                        cur_iter++;
                        if (cur_iter >= silence_iters + sound_iters)
                        {
                            cur_iter = 0;
                        }
                        Thread.sleep(40); // sleep 40ms
                    }
                }
                catch (Exception e1)
                {
                    e1.printStackTrace();
                }

                LatencyTestActive = false;
                setMicGainToggle(PREF__mic_gain_factor_toggle);
                setMicGainFactor(PREF_mic_gain_factor);

                try
                {
                    if (!AudioRecording.stopped)
                    {
                        AudioRecording.close();
                        CallingActivity.audio_thread.join();
                        CallingActivity.audio_thread = null;
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                try
                {
                    if (!AudioReceiver.stopped)
                    {
                        AudioReceiver.close();
                        CallingActivity.audio_receiver_thread.join();
                        CallingActivity.audio_receiver_thread = null;
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                Log.i(TAG, "LatencyTestThread:finished");
                test_running = false;
            }
        };
        LatencyTestThread.start();
    }

    private void switch_fast_audio_path(final boolean enable_fast_path)
    {
        if (enable_fast_path)
        {
            // ------------ test with fast audio path ------------
            try
            {
                set_rec_preset(false);
            }
            catch (Exception ignored)
            {
            }
            Log.i(TAG, "restart_audio_system__normal_call:201:preset_false");
            restart_audio_system();
            // ------------ test with fast audio path ------------
        }
        else
        {
            // ------------ test with "slower" audio path ------------
            try
            {
                set_rec_preset(true);
            }
            catch (Exception ignored)
            {
            }
            Log.i(TAG, "restart_audio_system__normal_call:202:preset_TRUE");
            restart_audio_system();
            // ------------ test with "slower" audio path ------------
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        test_running = false;
        LatencyTestActive = false;
        try
        {
            LatencyTestThread.join();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        reset_audio_mode();
        try
        {
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        }
        catch (Exception e2)
        {
            e2.printStackTrace();
        }

        setMicGainToggle(PREF__mic_gain_factor_toggle);
        setMicGainFactor(PREF_mic_gain_factor);
        roundtrip_time_textview.setText("test finished");
    }
}
