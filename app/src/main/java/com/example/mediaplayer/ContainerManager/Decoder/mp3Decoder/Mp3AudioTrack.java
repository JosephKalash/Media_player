package com.example.mediaplayer.ContainerManager.Decoder.mp3Decoder;

import android.media.AudioTrack;
import android.util.Log;

import com.example.mediaplayer.MediaControl.PlaybackAudio;

import java.io.IOException;
import java.io.InputStream;

public class Mp3AudioTrack  {
    Mp3Decoder.SoundData soundData;
    public Mp3AudioTrack(InputStream in) {
        try {
            soundData = Mp3Decoder.init(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(soundData == null)
            Log.d("fuck","null");
    }

    public Thread decodeFullyInto(AudioTrack audioTrack) {
        Thread thread  = new Thread(() -> {

            while (PlaybackAudio.mShouldContinue)
            {
                try {
                    if (!Mp3Decoder.decodeFrame(soundData)) break;
                    Log.i("fuck" , soundData.samplesBuffer[100] + "");

                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(PlaybackAudio.mShouldContinue && audioTrack != null )
                    audioTrack.write(soundData.samplesBuffer , 0 , soundData.samplesBuffer.length);
                else break;

            }
        });
        thread.start();
        return thread;
    }

    public boolean isStereo() {
        return soundData.stereo == 1;
    }

}
