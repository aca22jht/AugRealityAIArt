/*
 * Functions in this class have been taken and modified from the following source under
 * the Apache License Version 2.0
 *
 * IBM watson-developer-cloud android-sdk example MainActivity.java file
 * Kevin Kowa (28 Dec 2020), Logan Patino (11 Oct 2019), Roger Miret (03 May 2019), German Attanasio (09 Aug 2017),
 * Harrison Saylor (13 Feb 2017), Hendra Wijaya Djiono (20 Dec 2016), Keith Abdulla (11 Nov 2016),
 * Blake Ball (Sep 19 2016), Vince Herrin (26 Apr 2016), John Petitto (14 Apr 2016)
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

import android.content.Context;
import android.os.AsyncTask;

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.text_to_speech.v1.model.SynthesizeOptions;

import team6.project.R;

public class TextToSpeechHelper {
    private Context context;
    private TextToSpeech textService;
    private StreamPlayer player;

    public TextToSpeechHelper(Context context) {
        this.context = context;
        this.textService = initTextToSpeechService();
        this.player = new StreamPlayer();
    }

    private TextToSpeech initTextToSpeechService() {
        Authenticator authenticator = new IamAuthenticator(
                context.getString(R.string.text_speech_iam_apikey)
        );
        TextToSpeech service = new TextToSpeech(authenticator);
        service.setServiceUrl(context.getString(R.string.text_speech_url));
        return service;
    }

    public void playText(String text) {
        new SynthesisTask().execute(text);
    }

    public void pauseText() {
        player.pauseStream();
    }

    public void continueText() {
        player.continueStream();
    }

    private class SynthesisTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            SynthesizeOptions synthesizeOptions = new SynthesizeOptions.Builder()
                    .text(params[0])
                    .voice(SynthesizeOptions.Voice.EN_GB_CHARLOTTEV3VOICE)
                    .accept(HttpMediaType.AUDIO_WAV)
                    .build();
            player.playStream(textService.synthesize(synthesizeOptions).execute().getResult());
            return "Did synthesis";
        }
    }
}
