package com.purebeat.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.purebeat.PureBeatApplication;
import com.purebeat.service.MusicController;

public class MusicReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (action == null) return;

        PureBeatApplication app = PureBeatApplication.getInstance();
        MusicController controller = app.getMusicController();

        if (action.equals(Intent.ACTION_AUDIO_BECOMING_NOISY)) {
            // Pause when headphones are disconnected
            controller.pause();
        }
    }
}
