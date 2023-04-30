/*
 * This class has been taken and modified from the following source under
 * the Apache License Version 2.0
 *
 * IBM watson-developer-cloud android-sdk StreamPlayer.java file for Text-to-speech service
 * Logan Patino (01 Nov 2018), Jakob Schrettenbrunner (05 Dec 2017), German Attanasio (09 Aug 2017),
 * Harrison Saylor (01 Feb 2017)
 * https://github.com/watson-developer-cloud/android-sdk/blob/master/library/src/main/java/com/ibm/watson/developer_cloud/android/library/audio/StreamPlayer.java [accessed 25 Apr 2023]
 *
 * Copyright 2017 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package team6.project.backend;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Exposes the ability to play raw audio data from an InputStream.
 */
public final class StreamPlayer {
    private final String TAG = "StreamPlayer";
    // Default sample rate for .wav from Watson TTS
    // See https://console.bluemix.net/docs/services/text-to-speech/http.html#format
    private final int DEFAULT_SAMPLE_RATE = 22050;

    // Variables to save information about the audio track and data
    private AudioTrack audioTrack;
    private int pausedPosInBytes = 0;
    private byte [] audioData;

    /**
     * Convert the Text-to-speech stream to a byte array (Updated from source to improve efficiency)
     *
     * @param is input stream
     * @return   byte array
     * @throws IOException
     */
    private static byte[] convertStreamToByteArray(InputStream is) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BufferedInputStream bis = new BufferedInputStream(is)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            return baos.toByteArray();
        }
    }

    /**
     * Play the given InputStream. The stream must be a PCM audio format with a
     * sample rate of 22050. (Modified from source)
     *
     * @param stream the stream derived from a PCM audio source
     */
    public void playStream(InputStream stream) {
        try {
            // Interrupt added so any previously running track is stopped
            interrupt();
            // This will be a new stream, so reset paused position
            pausedPosInBytes = 0;

            // Convert input stream to audio data byte array
            byte[] data = convertStreamToByteArray(stream);
            int headSize = 44, metaDataSize = 48;
            int destPos = headSize + metaDataSize;
            int rawLength = data.length - destPos;
            audioData = new byte[rawLength];
            System.arraycopy(data, destPos, audioData, 0, rawLength);
            stream.close();

            // Initialise the audio track and write audio data
            initPlayer(DEFAULT_SAMPLE_RATE);
            audioTrack.write(audioData, 0, audioData.length);

            // Release resources
            releaseTrack();

        } catch (IOException e2) {
            Log.e(TAG, e2.getMessage());
        }
    }

    /**
     * Interrupt the audioTrack. (Modified from source)
     */
    public void interrupt() {
        if (isInitialised()) {
            audioTrack.pause();
            audioTrack.flush();
            audioTrack.release();
        }
    }

    /**
     * Pause the playing audioTrack and save the paused position. (Addition to source)
     */
    public void pauseStream() {
        if (isInitialised() || isPlaying()) {
            audioTrack.pause();
            // 2 bytes per frame for 16-bit PCM with mono channel
            pausedPosInBytes = audioTrack.getPlaybackHeadPosition() * 2;
        }
    }

    /**
     * Play the saved audio data from the paused position to the end. (Addition to source)
     */
    public void resumeStream() {
        // Interrupt to flush audio track so we can write new data
        interrupt();

        // Initialise audio track and write data from paused position to end of track
        initPlayer(DEFAULT_SAMPLE_RATE);
        int audioLengthToPlay = audioData.length - pausedPosInBytes;
        audioTrack.write(audioData, pausedPosInBytes, audioLengthToPlay);

        // release resources
        releaseTrack();
    }

    /**
     * Initialize AudioTrack by getting buffersize. (Updated function as source was deprecated)
     *
     * @param sampleRate the sample rate for the audio to be played
     */
    private void initPlayer(int sampleRate) {
        synchronized (this) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            int bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize == AudioTrack.ERROR_BAD_VALUE) {
                throw new RuntimeException("Could not determine buffer size for audio");
            }

            audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(bufferSize)
                    .build();

            audioTrack.play();
        }
    }

    /**
     * Get audio track initialised boolean.
     *
     * @return true if audioTrack initialised, otherwise false
     */
    public boolean isInitialised() {
        return (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED);
    }

    /**
     * Get audio track playing boolean.
     *
     * @return true if audioTrack playing, otherwise false
     */
    public boolean isPlaying() {
        return (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING);
    }

    /**
     * Get audio track paused boolean.
     *
     * @return true if audioTrack paused, otherwise false
     */
    public boolean isPaused() {
        return (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED);
    }

    /**
     * Flush and release audio track if it isn't paused or uninitialised.
     */
    private void releaseTrack() {
        if (!isPaused() && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
            audioTrack.pause();
            audioTrack.flush();
            audioTrack.release();
        }
    }
}