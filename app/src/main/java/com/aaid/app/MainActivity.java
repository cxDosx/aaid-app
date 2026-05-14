package com.aaid.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private String aaid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        int p = (int) (32 * getResources().getDisplayMetrics().density);
        layout.setPadding(p, p, p, p);

        TextView aaidText = new TextView(this);
        aaidText.setTextSize(18);
        aaidText.setTextIsSelectable(true);
        aaidText.setText("Loading...");
        layout.addView(aaidText);

        Button copyBtn = new Button(this);
        copyBtn.setText("Copy");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (24 * getResources().getDisplayMetrics().density);
        copyBtn.setLayoutParams(lp);
        layout.addView(copyBtn);

        setContentView(layout);

        new Thread(() -> {
            try {
                aaid = getAdvertisingId(this);
            } catch (Exception e) {
                aaid = "Failed: " + e.getMessage();
            }
            runOnUiThread(() -> aaidText.setText(aaid));
        }).start();

        copyBtn.setOnClickListener(v -> {
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("a", aaid));
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        });
    }

    private static String getAdvertisingId(Context ctx) throws Exception {
        final IBinder[] b = new IBinder[1];
        final Object lock = new Object();
        ServiceConnection c = new ServiceConnection() {
            public void onServiceConnected(ComponentName n, IBinder s) {
                synchronized (lock) { b[0] = s; lock.notifyAll(); }
            }
            public void onServiceDisconnected(ComponentName n) {}
        };
        Intent i = new Intent("com.google.android.gms.ads.identifier.service.START");
        i.setPackage("com.google.android.gms");
        ctx.bindService(i, c, Context.BIND_AUTO_CREATE);
        synchronized (lock) { while (b[0] == null) lock.wait(5000); }
        if (b[0] == null) throw new Exception("Timeout");
        Parcel d = Parcel.obtain(), r = Parcel.obtain();
        try {
            d.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
            b[0].transact(1, d, r, 0);
            r.readException();
            return r.readString();
        } finally {
            r.recycle(); d.recycle(); ctx.unbindService(c);
        }
    }
}
