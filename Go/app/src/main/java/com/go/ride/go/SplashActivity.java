package com.go.ride.go;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.VideoView;

import com.go.ride.go.helpers.logger.Log;

/**
 * Created by Daryl on 6/29/15.
 */
public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.splash_screen);
            VideoView videoView = (VideoView) findViewById(R.id.atlantaSkylineVideo);
            Uri video = Uri.parse("android.resource://" + getPackageName() + "/"
                    + R.raw.atlanta_skyline);
            videoView.setVideoURI(video);
            videoView.setZOrderOnTop(true);
            videoView.setScrollBarStyle(VideoView.SCROLLBARS_OUTSIDE_OVERLAY);

            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    GoToNextScreen();
                }
            });

            videoView.start();
            //TODO Start Main Activity with splash screen in background
        } catch(Exception e) {
            GoToNextScreen();
        }
    }

    private void GoToNextScreen() {
        if (isFinishing()) return;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
