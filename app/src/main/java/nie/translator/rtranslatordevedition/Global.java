/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copyFile of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nie.translator.rtranslatordevedition;

import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import nie.translator.rtranslatordevedition.api_management.ConsumptionsDataManager;
import nie.translator.rtranslatordevedition.settings.UserImagePreference;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.tools.ErrorCodes;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.ConversationBluetoothCommunicator;
import com.bluetooth.communicator.BluetoothCommunicator;
import com.bluetooth.communicator.Peer;

import org.json.JSONException;
import org.json.JSONObject;

import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.recent_peer.RecentPeersDataManager;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.translation.Translator;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.voice.Recorder;


public class Global extends Application {
    ////////////////////////////===============================START SOCKET ZONE=================//////
    //khởi tạo websocket object
    public Socket mSocket;
    private boolean autoSendMessage = true;

    {
        try {
            //String urlS = "http://27.74.249.34:8017";
            //String urlS = "http://192.168.1.52:4000";
            String urlS = "http://42.112.59.88:4000";

            mSocket = IO.socket(urlS);
            Log.d("CHUNG-", "CHUNG- Global()  -> mSocket() Global -> DA TAO SUCCESSES!!"+ mSocket);

        } catch (URISyntaxException e) {
            Log.d("CHUNG-", "CHUNG- Global()  -> mSocket() Global -> FAIL ->  "+ e.getMessage());

        }
    }
    //emit login
    public void SendData_to_mSocketFORLOGIN(String usernamedata, String firstnamedata, String lastnamedata , String personal_languagedata, String fcm_token) {

        String jsonString = String.format("{\"username\": \"%s\", \"firstname\": \"%s\", \"lastname\": \"%s\", \"personal_language\": \"%s\" , \"fcm_token\": \"%s\"}",usernamedata, firstnamedata, lastnamedata, personal_languagedata, fcm_token);
        //covert string to json
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            mSocket.emit("login", jsonObject);
            Log.d("CHUNG-", "CHUNG- global() -> mSocket.emit(\"login\", jsonObject);");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //emit call
    public void SendData_to_mSocket_FORCONNECT2USER(String fromUser, String toUser) {

        String jsonString = String.format("{\"from\": \"%s\", \"to\": \"%s\"}",fromUser, toUser);
        //covert string to json
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            mSocket.emit("call", jsonObject);
            Log.d("CHUNG-", "CHUNG- global() -> mSocket.emit(\"call\", jsonObject);");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //emit end_call
    public void SendData_to_mSocket_FOR_END_CONNECT2USER(String fromUser, String toUser) {

        String jsonString = String.format("{\"from\": \"%s\", \"to\": \"%s\"}",fromUser, toUser);
        //covert string to json
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            mSocket.emit("end_call", jsonObject);
            Log.d("CHUNG-", "CHUNG- global() -> END_CALL END_CALL END_CALL ");

            //clear tên người dã liên lac
            this.setPeerWantTalkName("");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    //bắn vào socket thông tin event là send_message json là { message, username, to, createdtime }
    public void SendData_to_mSocket_FOR_SENDMESSAGE(String message, String fromUser, String toOtherUser , String boiAPI) {
        Log.d("CHUNG-", "CHUNG- global() -> mSocket.emit(\"send_message\" " +boiAPI);
        
        String jsonString = String.format("{\"message\": \"%s\", \"from\": \"%s\", \"to\": \"%s\"}",message, fromUser, toOtherUser);
        //covert string to json
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            mSocket.emit("send_message", jsonObject);
            Log.d("CHUNG-", "CHUNG- global() -> mSocket.emit(\"send_message\" " +jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void SendData_to_mSocket_FOR_ACCEPT_CONNECT2USER(String fromUser, String toUser, Boolean accept){
        String jsonString = String.format("{\"from\": \"%s\", \"to\": \"%s\", \"accept\": %b}",fromUser, toUser, accept);
        //covert string to json
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            mSocket.emit("accept_call", jsonObject);
            Log.d("CHUNG-", "CHUNG- global() -> mSocket.emit(\"accept_call\"" + jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER( int  n, String lydo){
        String jsonString = String.format("{\"status\": %d}",n);
        //covert string to json
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            System.out.println(mSocket.isActive());
            mSocket.emit("update_status", jsonObject);
            Log.d("CHUNG-", "CHUNG- global() -> mSocket.emit(\"update_status\"" + jsonString + " " + lydo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    ////////////////////////////===============================END SOCKET ZONE=================//////
    public static final List<String> SCOPE = Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    private static final int TOKEN_FETCH_MARGIN = 60 * 1000; // one minute
    private ArrayList<CustomLocale> languages = new ArrayList<>();
    public CustomLocale language;
    private CustomLocale firstLanguage;
    private CustomLocale secondLanguage;
    private RecentPeersDataManager recentPeersDataManager;
    private ConversationBluetoothCommunicator bluetoothCommunicator;
    private Translator translator;
    private String name = "";
    private String apiKeyFileName = "";
    private ConsumptionsDataManager databaseManager;
    private AccessToken apiToken;
    private int micSensitivity = -1;
    private int speechTimeout = -1;
    private int prevVoiceDuration = -1;
    private int amplitudeThreshold = Recorder.DEFAULT_AMPLITUDE_THRESHOLD;
    private Handler mainHandler;
    private Thread getApiTokenThread;
    private ArrayDeque<ApiTokenListener> apiTokenListeners = new ArrayDeque<>();
    private static Handler mHandler = new Handler();
    private final Object lock = new Object();
    private String peerWantTalkName = "";

    public String FMCToken = "";

    private Bitmap imageViewOfuserBitmap ;
    public void setImageViewOfuserBitmap(Bitmap d) {
        imageViewOfuserBitmap = d;
    }
    public Bitmap getImageViewOfuserBitmap(){

        return imageViewOfuserBitmap;
    }

    private String FileVoiceRecordStringPath = "";

    public String getFileVoiceRecordStringPath() {
        return FileVoiceRecordStringPath;
    }
    public void setFileVoiceRecordStringPath(String path) {
        FileVoiceRecordStringPath = path;

    }
    public void  setAutoSendMessage(boolean autoSendMessage) {
        this.autoSendMessage = autoSendMessage;
    }
    public boolean getAutoSendMessage() {
        return autoSendMessage;

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        recentPeersDataManager = new RecentPeersDataManager(this);
        bluetoothCommunicator = new ConversationBluetoothCommunicator(this, getName(), BluetoothCommunicator.STRATEGY_P2P_WITH_RECONNECTION);
        translator = new Translator(this);
        databaseManager = new ConsumptionsDataManager(this);
        getMicSensitivity();
    }


    public ConversationBluetoothCommunicator getBluetoothCommunicator() {
        return bluetoothCommunicator;
    }

    public void resetBluetoothCommunicator() {
        bluetoothCommunicator.destroy(new BluetoothCommunicator.DestroyCallback() {
            @Override
            public void onDestroyed() {
                bluetoothCommunicator = new ConversationBluetoothCommunicator(Global.this, getName(), BluetoothCommunicator.STRATEGY_P2P_WITH_RECONNECTION);
            }
        });
    }

    public void getLanguages(final boolean recycleResult, final GetLocalesListListener responseListener) {
        if (recycleResult && languages.size() > 0) {
            responseListener.onSuccess(languages);
        } else {
            translator.getSupportedLanguages(CustomLocale.getDefault(), new Translator.SupportedLanguagesListener() {
                @Override
                public void onLanguagesListAvailable(ArrayList<CustomLocale> languages) {
                    Global.this.languages = languages;
                    responseListener.onSuccess(languages);
                }

                @Override
                public void onFailure(int[] reasons, long value) {
                    responseListener.onFailure(reasons, value);
                }
            });
        }
    }

    public interface GetLocalesListListener {
        void onSuccess(ArrayList<CustomLocale> result);

        void onFailure(int[] reasons, long value);
    }

    public void getLanguage(final boolean recycleResult, final GetLocaleListener responseListener) {
        getLanguages(true, new GetLocalesListListener() {
            @Override
            public void onSuccess(ArrayList<CustomLocale> languages) {
                CustomLocale predefinedLanguage = CustomLocale.getDefault();
                String ss = getCurrentLanguageinPhone();
                CustomLocale language = null;
                if (recycleResult && Global.this.language != null) {
                    language = Global.this.language;
                } else {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Global.this);
                    String code = sharedPreferences.getString("language", predefinedLanguage.getCode());
                    if (code != null) {
                        language = CustomLocale.getInstance(code);
                    }
                }

                int index = CustomLocale.search(languages, language);
                if (index != -1) {
                    language = languages.get(index);
                } else {

                    int index2 = CustomLocale.search(languages, predefinedLanguage);
                    if (index2 != -1) {
                        language = predefinedLanguage;
                    } else {
                        language = new CustomLocale("en", "US");
                    }
                }
                ///===ADD ON LAI QUA NGON NGU MICRO MA MINH CHINH TRONG SETTING==//
                /*
                if(ss!="") {
                    if (ss.equals("Tiếng Hàn (Hàn Quốc)") || ss.equals("Korean (South Korea)") || ss.equals("한국어 (대한민국)") ) {
                        language = new CustomLocale("ko", "KR");
                    } else {
                        if (ss.equals("Tiếng Việt (Việt Nam)") || ss.equals("Vietnamese (Vietnam)") || ss.equals("베트남어 (베트남)")) {
                            language = new CustomLocale("vi", "VN");
                        } else {
                            language = new CustomLocale("en", "US");
                        }
                    }
                }*/
                Global.this.language = language;
                responseListener.onSuccess(language);
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });
    }

    public void getFirstLanguage(final boolean recycleResult, final GetLocaleListener responseListener) {
        getLanguages(true, new GetLocalesListListener() {
            @Override
            public void onSuccess(final ArrayList<CustomLocale> languages) {
                getLanguage(true, new GetLocaleListener() {
                    @Override
                    public void onSuccess(CustomLocale predefinedLanguage) {
                        CustomLocale language = null;
                        if (recycleResult && Global.this.firstLanguage != null) {
                            language = Global.this.firstLanguage;
                        } else {
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Global.this);
                            String code = sharedPreferences.getString("firstLanguage", predefinedLanguage.getCode());
                            if (code != null) {
                                language = CustomLocale.getInstance(code);
                            }
                        }

                        int index = CustomLocale.search(languages, language);
                        if (index != -1) {
                            language = languages.get(index);
                        } else {
                            int index2 = CustomLocale.search(languages, predefinedLanguage);
                            if (index2 != -1) {
                                language = predefinedLanguage;
                            } else {
                                language = new CustomLocale("en", "US");
                            }
                        }

                        Global.this.firstLanguage = language;
                        responseListener.onSuccess(language);
                    }

                    @Override
                    public void onFailure(int[] reasons, long value) {
                        responseListener.onFailure(reasons, value);
                    }
                });
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });

    }

    public void getSecondLanguage(final boolean recycleResult, final GetLocaleListener responseListener) {
        getLanguages(true, new GetLocalesListListener() {
            @Override
            public void onSuccess(ArrayList<CustomLocale> languages) {
                CustomLocale predefinedLanguage = CustomLocale.getDefault();
                CustomLocale language = null;
                if (recycleResult && Global.this.secondLanguage != null) {
                    language = Global.this.secondLanguage;
                } else {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Global.this);
                    String code = sharedPreferences.getString("secondLanguage", null);
                    if (code != null) {
                        language = CustomLocale.getInstance(code);
                    }
                }

                int index = CustomLocale.search(languages, language);
                if (index != -1) {
                    language = languages.get(index);
                } else {
                    language = new CustomLocale("en", "US");
                }

                Global.this.secondLanguage = language;
                responseListener.onSuccess(language);
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });
    }

    public interface GetLocaleListener {
        void onSuccess(CustomLocale result);

        void onFailure(int[] reasons, long value);
    }

    public void setLanguage(CustomLocale language) {
        this.language = language;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("language", language.getCode());
        editor.apply();
    }

    public String getCurrentLanguageinPhone(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getString("language","");
    }
    public void setCurrentLanguageinPhone(String language) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("language", language);
        editor.apply();
    }



    public void setPeerWantTalkName(String peerWantTalkName) {
        this.peerWantTalkName = peerWantTalkName;
        Log.d("CHUNG-", "CHUNG- GLOBAL() -> setPeerWantTalkName - > +" + peerWantTalkName);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("peerWantTalkName", peerWantTalkName);
        editor.apply();
    }
    public String getPeerWantTalkName(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getString("peerWantTalkName","");
    }

    public void setFirstLanguage(CustomLocale language) {
        this.firstLanguage = language;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("firstLanguage", language.getCode());
        editor.putString("firstLanguageDisplay", language.getDisplayName());
        editor.apply();
    }


    public void setLanguageDetectInput(String language) {
//        this.firstLanguage = language;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("languageDetectInput", language);
        editor.apply();
    }

    public void setLanguageDetectOutput(String language) {
//        this.firstLanguage = language;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("languageDetectOutput", language);
        editor.apply();
    }

    public String getInputLanguage(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getString("languageDetectInput","");
    }

    public String getOutputLanguage(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getString("languageDetectOutput","");
    }




    public String getFirstLanguage(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getString("firstLanguage","");
    }

    public String getSecondLanguage(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getString("secondLanguage","");
    }

    public String getDisplayFirstLanguage(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getString("firstLanguageDisplay","");
    }

    public String getDisplaySecondLanguage(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getString("secondLanguageDisplay","");
    }

    public void setSecondLanguage(CustomLocale language) {
        this.secondLanguage = language;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("secondLanguage", language.getCode());
        editor.putString("secondLanguageDisplay", language.getDisplayName());
        editor.apply();
    }


    public int getAmplitudeThreshold() {
        return amplitudeThreshold;
    }


    public int getMicSensitivity() {
        if (micSensitivity == -1) {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            micSensitivity = sharedPreferences.getInt("micSensibility", 50);
            setAmplitudeThreshold(micSensitivity);
        }
        return micSensitivity;
    }

    public void setMicSensitivity(int value) {
        micSensitivity = value;
        setAmplitudeThreshold(micSensitivity);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("micSensibility", value);
        editor.apply();
    }

    public int getSpeechTimeout() {
        if (speechTimeout == -1) {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            speechTimeout = sharedPreferences.getInt("speechTimeout", Recorder.DEFAULT_SPEECH_TIMEOUT_MILLIS);
        }
        return speechTimeout;
    }

    public void setSpeechTimeout(int value) {
        speechTimeout = value;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("speechTimeout", value);
        editor.apply();
    }

    public int getPrevVoiceDuration() {
        if (prevVoiceDuration == -1) {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            prevVoiceDuration = sharedPreferences.getInt("prevVoiceDuration", Recorder.DEFAULT_PREV_VOICE_DURATION);
        }
        return prevVoiceDuration;
    }

    public void setPrevVoiceDuration(int value) {
        prevVoiceDuration = value;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("prevVoiceDuration", value);
        editor.apply();
    }

    private void setAmplitudeThreshold(int micSensitivity) {
        float amplitudePercentage = 1f - (micSensitivity / 100f);
        if (amplitudePercentage < 0.5f) {
            amplitudeThreshold = Math.round(Recorder.MIN_AMPLITUDE_THRESHOLD + ((Recorder.DEFAULT_AMPLITUDE_THRESHOLD - Recorder.MIN_AMPLITUDE_THRESHOLD) * (amplitudePercentage * 2)));
        } else {
            amplitudeThreshold = Math.round(Recorder.DEFAULT_AMPLITUDE_THRESHOLD + ((Recorder.MAX_AMPLITUDE_THRESHOLD - Recorder.DEFAULT_AMPLITUDE_THRESHOLD) * ((amplitudePercentage - 0.5F) * 2)));
        }
    }

    public String getName() {
        if (name.length() == 0) {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            name = sharedPreferences.getString("name", "user");
        }
        return name;
    }

    public void setName(String savedName) {
        name = savedName;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", savedName);
        editor.apply();
        getBluetoothCommunicator().setName(savedName);  //si aggiorna il nome anche per il comunicator
    }

    public Peer getMyPeer() {
        return new Peer(null, getName(), false);
    }

    public abstract static class MyPeerListener {
        public abstract void onSuccess(Peer myPeer);

        public void onFailure(int[] reasons, long value) {
        }
    }

    public void getMyID(final MyIDListener responseListener) {
        responseListener.onSuccess(Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID));
    }

    public abstract static class MyIDListener {
        public abstract void onSuccess(String id);

        public void onFailure(int[] reasons, long value) {
        }
    }

    public RecentPeersDataManager getRecentPeersDataManager() {
        return recentPeersDataManager;
    }

    public abstract static class ResponseListener {
        public void onSuccess() {

        }

        public void onFailure(int[] reasons, long value) {
        }
    }

    public void addUsage(float creditToSub) {
        // add consumption to the database
        databaseManager.addUsage(creditToSub);
    }

    public boolean isFirstStart() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("firstStart", true);
    }

    public void setFirstStart(boolean firstStart) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("firstStart", firstStart);
        editor.apply();
    }

    public String getApiKeyFileName() {
        if (apiKeyFileName.length() == 0) {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            apiKeyFileName = sharedPreferences.getString("apiKeyFileName", "");
        }
        return "chat-demo-cfb06-8f95921614d9.json";
       // return apiKeyFileName;
    }

    public void setApiKeyFileName(String apiKeyFileName) {
        this.apiKeyFileName = apiKeyFileName;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("apiKeyFileName", apiKeyFileName);
        editor.apply();
    }


    //api token

    public synchronized void resetApiToken() {
        apiToken = null;
    }

    public void getApiToken(final boolean recycleResult, @Nullable final ApiTokenListener responseListener) {
        synchronized (lock) {
            if (responseListener != null) {
                apiTokenListeners.addLast(responseListener);
            }
            if (recycleResult && apiToken != null && apiToken.getExpirationTime().getTime() > System.currentTimeMillis()) {
                //notifica del successo a tutti i listeners
                notifyGetApiTokenSuccess();
            } else {
                if (getApiTokenThread == null) {
                    getApiTokenThread = new Thread(new GetApiTokenRunnable(new ApiTokenListener() {
                        @Override
                        public void onSuccess(final AccessToken apiToken) {
                            //notifica del successo a tutti i listeners
                            notifyGetApiTokenSuccess();
                        }

                        @Override
                        public void onFailure(final int[] reasons, final long value) {
                            //notifica del fallimento a tutti i listeners
                            notifyGetApiTokenFailure(reasons, value);
                        }
                    }), "getAppToken");
                    getApiTokenThread.start();
                }
            }
        }
    }

    private void notifyGetApiTokenSuccess() {
        synchronized (lock) {
            while (apiTokenListeners.peekFirst() != null) {
                apiTokenListeners.pollFirst().onSuccess(apiToken);
            }
            getApiTokenThread = null;
        }
    }

    private void notifyGetApiTokenFailure(final int[] reasons, final long value) {
        synchronized (lock) {
            while (apiTokenListeners.peekFirst() != null) {
                apiTokenListeners.pollFirst().onFailure(reasons, value);
            }
            getApiTokenThread = null;
        }
    }

    private class GetApiTokenRunnable implements Runnable {
        @Nullable
        private ApiTokenListener responseListener;

        private GetApiTokenRunnable(@Nullable ApiTokenListener responseListener) {
            this.responseListener = responseListener;
        }

        @Override
        public void run() {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    Log.d("token", "token fetched");
                    final InputStream stream;
                    try {
                        //hardcode luôn file json vào đây


                        stream =  getAssets().open("chat-demo-cfb06-8f95921614d9.json");

                        //hang zin nguyen ban là dùng getApiKeyFileName()
                        //stream = new FileInputStream(new File(getFilesDir(), getApiKeyFileName()));
                        try {
                            final GoogleCredentials credentials = GoogleCredentials.fromStream(stream).createScoped(SCOPE);
                            apiToken = credentials.refreshAccessToken();
                            if (responseListener != null) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (apiToken != null) {
                                            responseListener.onSuccess(apiToken);
                                        } else {
                                            responseListener.onFailure(new int[]{ErrorCodes.WRONG_API_KEY}, -1);
                                        }
                                    }
                                });
                            }

                            // Schedule access token refresh before it expires
                            if (mHandler != null) {
                                // elimination of all runnables in handlers to ensure that only one getAppToken is scheduled at a time
                                mHandler.removeCallbacksAndMessages(null);
                                long refreshTime = Math.max(apiToken.getExpirationTime().getTime() - System.currentTimeMillis() - TOKEN_FETCH_MARGIN, TOKEN_FETCH_MARGIN);
                                mHandler.postDelayed(new GetApiTokenRunnable(null), refreshTime);
                            }
                        } catch (final IOException e) {
                            Log.e("token", "Failed to obtain access token.", e);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (responseListener != null) {
                                        if (e.getCause() instanceof UnknownHostException) {
                                            responseListener.onFailure(new int[]{ErrorCodes.MISSED_CONNECTION}, -1);
                                        } else {
                                            responseListener.onFailure(new int[]{ErrorCodes.WRONG_API_KEY}, -1);
                                        }
                                    }
                                }
                            });
                        }
                    } catch (FileNotFoundException e) {
                        Log.e("token", "Failed to obtain access token.", e);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (responseListener != null) {
                                    responseListener.onFailure(new int[]{ErrorCodes.MISSING_API_KEY}, -1);
                                }
                            }
                        });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.start();
        }
    }

    public interface ApiTokenListener {
        void onSuccess(AccessToken apiToken);

        void onFailure(int[] reasons, long value);
    }
}

