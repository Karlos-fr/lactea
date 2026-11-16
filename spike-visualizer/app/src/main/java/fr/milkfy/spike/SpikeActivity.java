package fr.milkfy.spike;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

/**
 * Spike: can we read the global output mix (session 0) with android.media.audiofx.Visualizer
 * while Spotify plays on this TV? Draws waveform + FFT and logs stats under tag MILKFY_SPIKE.
 */
public class SpikeActivity extends Activity {
    private static final String TAG = "MILKFY_SPIKE";
    private static final int REQ_AUDIO = 1;

    private Visualizer visualizer;
    private SpikeView view;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private volatile byte[] lastWave = new byte[0];
    private volatile byte[] lastFft = new byte[0];
    private volatile int waveCallbacks, fftCallbacks;
    private volatile String status = "init";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        view = new SpikeView(this);
        setContentView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            status = "requesting RECORD_AUDIO";
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        } else {
            startCapture();
        }
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
        stopCapture();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode == REQ_AUDIO && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            startCapture();
        } else {
            status = "RECORD_AUDIO denied";
            Log.w(TAG, status);
        }
    }

    private void startCapture() {
        if (visualizer != null) return;
        try {
            int[] range = Visualizer.getCaptureSizeRange();
            visualizer = new Visualizer(0);
            visualizer.setEnabled(false);
            visualizer.setCaptureSize(range[1]);
            visualizer.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);
            int rate = Visualizer.getMaxCaptureRate();
            int res = visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer v, byte[] waveform, int samplingRate) {
                    lastWave = waveform.clone();
                    waveCallbacks++;
                }

                @Override
                public void onFftDataCapture(Visualizer v, byte[] fft, int samplingRate) {
                    lastFft = fft.clone();
                    fftCallbacks++;
                }
            }, rate, true, true);
            int en = visualizer.setEnabled(true);
            status = "capturing size=" + range[1] + " rate=" + rate + "mHz listener=" + res + " enable=" + en
                    + " samplingRate=" + visualizer.getSamplingRate();
            Log.i(TAG, "START " + status);
        } catch (Throwable t) {
            status = "Visualizer(0) failed: " + t;
            Log.e(TAG, status, t);
            visualizer = null;
        }
    }

    private void stopCapture() {
        if (visualizer != null) {
            visualizer.setEnabled(false);
            visualizer.release();
            visualizer = null;
        }
    }

    private float waveRms(byte[] w) {
        if (w.length == 0) return 0;
        double sum = 0;
        for (byte b : w) {
            int s = (b & 0xFF) - 128; // 8-bit unsigned PCM
            sum += s * s;
        }
        return (float) Math.sqrt(sum / w.length);
    }

    private int wavePeak(byte[] w) {
        int peak = 0;
        for (byte b : w) peak = Math.max(peak, Math.abs((b & 0xFF) - 128));
        return peak;
    }

    private float fftEnergy(byte[] f) {
        double sum = 0;
        for (int i = 2; i + 1 < f.length; i += 2) {
            sum += Math.hypot(f[i], f[i + 1]);
        }
        return (float) sum;
    }

    private int lastLogWave, lastLogFft;
    private long lastLogTime;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            if (now - lastLogTime >= 1000) {
                byte[] w = lastWave, f = lastFft;
                Log.i(TAG, String.format("STATS waveCb/s=%d fftCb/s=%d rms=%.2f peak=%d fftEnergy=%.0f | %s",
                        waveCallbacks - lastLogWave, fftCallbacks - lastLogFft,
                        waveRms(w), wavePeak(w), fftEnergy(f), status));
                lastLogWave = waveCallbacks;
                lastLogFft = fftCallbacks;
                lastLogTime = now;
            }
            view.invalidate();
            handler.postDelayed(this, 33);
        }
    };

    private class SpikeView extends View {
        private final Paint wave = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bars = new Paint();
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

        SpikeView(Context c) {
            super(c);
            setBackgroundColor(Color.BLACK);
            wave.setColor(Color.rgb(0, 255, 180));
            wave.setStrokeWidth(3);
            bars.setColor(Color.rgb(255, 120, 0));
            text.setColor(Color.WHITE);
            text.setTextSize(32);
        }

        @Override
        protected void onDraw(Canvas c) {
            int wdt = getWidth(), h = getHeight();
            byte[] w = lastWave, f = lastFft;

            // waveform: top half
            if (w.length > 1) {
                float mid = h * 0.3f, scale = h * 0.25f / 128f, step = (float) wdt / (w.length - 1);
                for (int i = 1; i < w.length; i++) {
                    c.drawLine((i - 1) * step, mid - ((w[i - 1] & 0xFF) - 128) * scale,
                            i * step, mid - ((w[i] & 0xFF) - 128) * scale, wave);
                }
            }
            // FFT magnitudes: bottom half (first 128 bins)
            int bins = Math.min(128, f.length / 2);
            if (bins > 1) {
                float bw = (float) wdt / bins;
                for (int i = 1; i < bins; i++) {
                    float mag = (float) Math.hypot(f[2 * i], f[2 * i + 1]);
                    float bh = Math.min(h * 0.4f, mag * h * 0.4f / 90f);
                    c.drawRect(i * bw, h - bh, (i + 1) * bw - 2, h, bars);
                }
            }
            c.drawText(status, 30, 50, text);
            c.drawText(String.format("rms=%.2f peak=%d fftEnergy=%.0f  callbacks wave=%d fft=%d",
                    waveRms(w), wavePeak(w), fftEnergy(f), waveCallbacks, fftCallbacks), 30, 95, text);
        }
    }
}
