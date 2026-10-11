package com.gyf.immersionbar.sample.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

/**
 * Unified Toast manager class in Demo.
 * Cancels the previous Toast immediately before showing a new Toast to avoid queued display.
 */
public final class ToastUtils {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static Toast sToast;

    private ToastUtils() {
    }

    public static void show(@NonNull Context context, @NonNull CharSequence text) {
        Context applicationContext = context.getApplicationContext();
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showInternal(applicationContext, text);
        } else {
            MAIN_HANDLER.post(() -> showInternal(applicationContext, text));
        }
    }

    private static void showInternal(Context context, CharSequence text) {
        if (sToast != null) {
            sToast.cancel();
        }
        sToast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        sToast.show();
    }
}
