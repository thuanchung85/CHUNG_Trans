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

package nie.translator.rtranslatordevedition.voice_translation;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.bluetooth.communicator.Message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import io.socket.emitter.Emitter;
import nie.translator.rtranslatordevedition.Global;
import nie.translator.rtranslatordevedition.R;
import nie.translator.rtranslatordevedition.api_management.ApiManagementActivity;
import nie.translator.rtranslatordevedition.tools.ErrorCodes;
import nie.translator.rtranslatordevedition.tools.gui.ButtonChooseModeAI;
import nie.translator.rtranslatordevedition.tools.gui.ButtonKeyboard;
import nie.translator.rtranslatordevedition.tools.gui.ButtonMic;
import nie.translator.rtranslatordevedition.tools.gui.ButtonSound;
import nie.translator.rtranslatordevedition.tools.gui.DeactivableButton;
import nie.translator.rtranslatordevedition.tools.gui.MicrophoneComunicable;
import nie.translator.rtranslatordevedition.tools.gui.messages.GuiMessage;
import nie.translator.rtranslatordevedition.tools.gui.messages.MessagesAdapter;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.myWhipper.AsyncTaskListener;

import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.myWhipper.OpenAIWhisperSTT;

//===QUAN TRONG==//
public abstract class VoiceTranslationFragment extends Fragment implements MicrophoneComunicable, AsyncTaskListener {

    //=================khi nhận được endcall từ server trả về================//
    private Emitter.Listener onReceive_UserEndCallCallBack = new Emitter.Listener(){
        @Override
        //hàm websocket server tra ra data về
        public void call(final Object... args){
            String argsReponse =  Arrays.toString(args);
            try {
                JSONArray jsonArray = new JSONArray(argsReponse);
                String myName = global.getName();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    JSONObject dataJSON = jsonObject.getJSONObject("data");
                    String FormName = dataJSON.getString("from");
                    String ToName = dataJSON.getString("to");
                    //vì socket board cast cho all user nên ta phải check coi có tên của mình không
                    //nếu trong from hay to có chứa tên của chính mình thì là đúng rồi, thoát chat
                    if(  myName.equals(ToName)){
                        //cần check luôn co tên của người kia đung năm trong jSON hay không, có trường hơp có tên mình, nhưng tên người kia thì lại khác với người mình đang nói chuyên
                        if(FormName.equals(global.getPeerWantTalkName())){
                            Log.d("CHUNG-", "CHUNG- OK I QUIT() -> FormName.equals(global.getPeerWantTalkName()");

                            voiceTranslationActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //user còn online trong room chat sẽ bị auto đá ra bởi lệnh dưới, do user kia tắt app
                                    voiceTranslationActivity.onBackPressed_NOTCALL_AGAIN();

                                    //clear tên người dã liên lac
                                    global.setPeerWantTalkName("");
                                }
                            });
                        }
                        //quay trơ về parring page
                        Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> ENd_CALL ->GET BACK");

                    }
                }




            } catch (JSONException e) {
                Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> onReceive_UserEndCallCallBack ->JSONException  " + e.getMessage() );
                throw new RuntimeException(e);
            }
        }
    };

    //======khi nhận được receive_message từ server gọi về===///
    private TextToSpeech textToSpeech;
    private Emitter.Listener onReceive_receive_messageCallBack = new Emitter.Listener(){
        @Override
        //hàm websocket server tra ra data về
        public void call(final Object... args){
            String argsReponse =  Arrays.toString(args);
            try {
                JSONArray jsonArray = new JSONArray(argsReponse);
                //anh tự dich nhé.
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String socketreturn_message = jsonObject.getString("message");
                    String socketreturn_from = jsonObject.getString("from");

                    String socketreturn_to = jsonObject.getString("to");
                    String socketreturn_translated = jsonObject.getString("translated");

                    //gan text phan hoi vao recyclerview tren UI( neu la tu user khac)  //tu user khac reply (co the là johnpham)
                    if(!socketreturn_from.equals(global.getName()))
                    {
                        voiceTranslationActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() ->runOnUiThread update recyclerview ");
                                Message mstypeFORGUI = new Message(voiceTranslationActivity, socketreturn_from, socketreturn_translated + "\n(" + socketreturn_message +")");
                                GuiMessage msFOR_recyclerview = new GuiMessage(mstypeFORGUI, false, true,false);
                                if (mAdapter != null) {
                                    mAdapter.addMessage(msFOR_recyclerview);
                                    //auto scroll
                                    //smooth scroll
                                    smoothScroller.setTargetPosition(mAdapter.getItemCount() - 1);
                                    mRecyclerView.getLayoutManager().startSmoothScroll(smoothScroller);

                                    //speak here if cant

                                     if(sound.isMute() == true){
                                         textToSpeech.stop();
                                     }
                                     else {
                                         stopMicrophone(true);

                                         textToSpeech.speak(socketreturn_translated, TextToSpeech.QUEUE_FLUSH, null, "ID");
                                     }

                                }
                            };
                        });
                    }
                }

            } catch (JSONException e) {
                Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> onReceive_receive_messageCallBack ->JSONException  " + e.getMessage() );
                throw new RuntimeException(e);
            }
        }
    };


    private String nameOfpeerWantConnect = "";







//===========================================================================================================================//

    //gui
    private boolean isEditTextOpen = false;
    private boolean isInputActive = true;
    protected VoiceTranslationActivity voiceTranslationActivity;
    protected Global global;
    private ButtonKeyboard keyboard;
    protected ButtonMic microphone;
    protected TextView description;
    private ButtonSound sound;

    private ButtonChooseModeAI buttonChooseModeAI;
    private EditText editText;
    private MessagesAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private RecyclerView.SmoothScroller smoothScroller;
    private View.OnClickListener micClickListener;
    //connection
    protected VoiceTranslationService.VoiceTranslationServiceCommunicator voiceTranslationServiceCommunicator;
    protected VoiceTranslationService.VoiceTranslationServiceCallback voiceTranslationServiceCallback;

    //AudioRecorder audioRecorder;
   String whipperReturnText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> onCreate() "));
        super.onCreate(savedInstanceState);
        voiceTranslationServiceCommunicator = null;
        voiceTranslationServiceCallback = null;
        microphone = null;


    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> onViewCreated() "));
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        description = view.findViewById(R.id.description);
        keyboard = view.findViewById(R.id.buttonKeyboard);
        microphone = view.findViewById(R.id.buttonMic);
        sound = view.findViewById(R.id.buttonSound);
        buttonChooseModeAI = view.findViewById(R.id.buttonAUTOSEND);

        editText = view.findViewById(R.id.editText);
        microphone.setFragment(this);
        microphone.setEditText(editText);
        deactivateInputs(DeactivableButton.DEACTIVATED);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> beforeTextChanged() -> %s",charSequence));
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> onTextChanged() -> %s",charSequence));
            }

            @Override
            public void afterTextChanged(Editable text) {
                Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> afterTextChanged() -> %s",text));

                if ((text == null || text.length() == 0) && microphone.getState() == ButtonMic.STATE_SEND) {
                    microphone.setState(ButtonMic.STATE_RETURN);

                } else if (microphone.getState() == ButtonMic.STATE_RETURN) {
                    microphone.setState(ButtonMic.STATE_SEND);


                }
            }
        });





    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> onActivityCreated() "));
        super.onActivityCreated(savedInstanceState);
        voiceTranslationActivity = (VoiceTranslationActivity) requireActivity();
        global = (Global) voiceTranslationActivity.getApplication();
        LinearLayoutManager layoutManager = new LinearLayoutManager(voiceTranslationActivity);
        layoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(layoutManager);
        smoothScroller = new LinearSmoothScroller(voiceTranslationActivity) {
            @Override
            protected int calculateTimeForScrolling(int dx) {
                return 100;
            }
        };
        //smoothScroller = new LinearSmoothScroller(activity);
        final View.OnClickListener deactivatedClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               // Toast.makeText(voiceTranslationActivity, getResources().getString(R.string.error_wait_initialization), Toast.LENGTH_SHORT).show();
            }
        };


        sound.setOnClickListenerForDeactivated(deactivatedClickListener);
        sound.setOnClickListenerForActivated(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (sound.isMute()) {
                    startSound();
                } else {
                    stopSound();
                }
            }
        });

        buttonChooseModeAI.setAimode(global.getAIMode());
        buttonChooseModeAI.setOnClickListenerForActivated(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int currentAImode = buttonChooseModeAI.getAimode();
                currentAImode += 1;
                global.setAIMode(currentAImode);
                 buttonChooseModeAI.setAimode(currentAImode);
                if(currentAImode > 2) {
                    currentAImode = 0;
                    buttonChooseModeAI.setAimode(currentAImode);
                    global.setAIMode(currentAImode);
                }
            }
        });


        keyboard.setOnClickListenerForActivated(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonChooseModeAI.setVisibility(View.GONE);
                isEditTextOpen = true;
                keyboard.generateEditText(voiceTranslationActivity, VoiceTranslationFragment.this, microphone, editText, true);
                voiceTranslationServiceCommunicator.setEditTextOpen(true);
            }
        });

        microphone.setOnClickListenerForActivated(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                switch (microphone.getState()) {
                    case ButtonMic.STATE_NORMAL:
                        if (microphone.isMute()) {
                            Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> onClick() -> startMicrophone");
                            startMicrophone(true);
                            buttonChooseModeAI.setVisibility(View.VISIBLE);
                        } else {
                            stopMicrophone(true);
                            buttonChooseModeAI.setVisibility(View.VISIBLE);
                        }
                        break;
                    case ButtonMic.STATE_RETURN:
                        buttonChooseModeAI.setVisibility(View.VISIBLE);
                        isEditTextOpen = false;
                        voiceTranslationServiceCommunicator.setEditTextOpen(false);
                        microphone.deleteEditText(voiceTranslationActivity, VoiceTranslationFragment.this, keyboard, editText);
                        break;
                    case ButtonMic.STATE_SEND:

                        // sending the message to be translated to the service
                        voiceTranslationServiceCommunicator.receiveText(editText.getText().toString());
                        editText.setText("");
                        break;
                }
            }
        });

        microphone.setOnClickListenerForDeactivatedForCreditExhausted(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (microphone.getState() == ButtonMic.STATE_RETURN) {
                    isEditTextOpen = false;
                    voiceTranslationServiceCommunicator.setEditTextOpen(false);
                    microphone.deleteEditText(voiceTranslationActivity, VoiceTranslationFragment.this, keyboard, editText);
                }
            }
        });
        microphone.setOnClickListenerForDeactivatedForMissingMicPermission(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (microphone.getState() == ButtonMic.STATE_RETURN) {
                    isEditTextOpen = false;
                    voiceTranslationServiceCommunicator.setEditTextOpen(false);
                    microphone.deleteEditText(voiceTranslationActivity, VoiceTranslationFragment.this, keyboard, editText);
                } else {
                    Toast.makeText(voiceTranslationActivity, R.string.error_missing_mic_permissions, Toast.LENGTH_SHORT).show();
                }
            }
        });
        microphone.setOnClickListenerForDeactivated(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (microphone.getState() == ButtonMic.STATE_RETURN) {
                    isEditTextOpen = false;
                    voiceTranslationServiceCommunicator.setEditTextOpen(false);
                    microphone.deleteEditText(voiceTranslationActivity, VoiceTranslationFragment.this, keyboard, editText);
                } else {
                    deactivatedClickListener.onClick(v);
                }
            }
        });
        microphone.setOnClickListenerForDeactivatedForMissingOrWrongKeyfile(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (microphone.getState() == ButtonMic.STATE_RETURN) {
                    isEditTextOpen = false;
                    voiceTranslationServiceCommunicator.setEditTextOpen(false);
                    microphone.deleteEditText(voiceTranslationActivity, VoiceTranslationFragment.this, keyboard, editText);
                } else {
                    Toast.makeText(voiceTranslationActivity, getResources().getString(R.string.error_invalid_key), Toast.LENGTH_SHORT).show();
                }
            }
        });




        ///====KHỞi Tạo SOCKET CONNECTION========//
        ///STEP 3:
        // nhận về Event receive_call để nhận

        Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> onActivityCreated - > gọi mSocket.connect()");

        //đăng ký khi event socket tra ve là receive_end_call -> kiêu user thoát chat trở về parring page
        global.mSocket.on("receive_end_call", onReceive_UserEndCallCallBack);

        //đăng ký khi event socket tra ve là receive_message -> hiện message chat lên listview
        global.mSocket.on("receive_message", onReceive_receive_messageCallBack);


         nameOfpeerWantConnect = global.getPeerWantTalkName();
        //Toast.makeText(voiceTranslationActivity, "You will connect to " + nameOfpeerWantConnect, Toast.LENGTH_SHORT).show();
       // String tempUserChungPhone =  global.getName();
       // String tempUserChungPhoneFirstname =  "f_" + global.getName();
        //String tempUserChungPhoneLastname =  "l_" + global.getName();

        //String tempUserChungPhoneLanguage = activity.getResources().getConfiguration().locale.getLanguage();
        /*
        String lang = global.getCurrentLanguageinPhone();

        if(lang != "") {
            if (lang.equals("Tiếng Hàn (Hàn Quốc)") || lang.equals("Korean (South Korea)") || lang.equals("한국어 (대한민국)")  ) {
                tempUserChungPhoneLanguage = "ko";
            } else {
                if (lang.equals("Tiếng Việt (Việt Nam)") || lang.equals("Vietnamese (Vietnam)")|| lang.equals("베트남어 (베트남)")  ) {
                    tempUserChungPhoneLanguage = "vi";
                } else {
                    tempUserChungPhoneLanguage = "en";
                }
            }
        }
        else{
            tempUserChungPhoneLanguage = activity.getResources().getConfiguration().locale.getLanguage();
        }*/
        //Toast.makeText(activity, "lang" + "->" + tempUserChungPhoneLanguage, Toast.LENGTH_SHORT).show();
        //SendData_to_mSocket_FORLOGIN(tempUserChungPhone, tempUserChungPhoneFirstname, tempUserChungPhoneLastname, tempUserChungPhoneLanguage);




        //init TTS by CHUNG==//
        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this.getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    // Set the language (Optional, you can skip this if you want to use the default language)
                    Locale current = getResources().getConfiguration().locale;

                    textToSpeech.setLanguage(current);
                } else {
                    Toast.makeText(voiceTranslationActivity, "Initialization TextToSpeech failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // Set an UtteranceProgressListener to monitor the speech process
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // Called when the TTS engine starts speaking
                Log.d("CHUNG-", String.format("CHUNG- TTS() -> onStart() "));
                stopMicrophone(true);
            }

            @Override
            public void onDone(String utteranceId) {
                Log.d("CHUNG-", String.format("CHUNG- TTS() -> onDone() "));
                startMicrophone(true);
            }

            @Override
            public void onError(String utteranceId) {
                Log.d("CHUNG-", String.format("CHUNG- TTS() -> onError() "));
            }
        });


        //cố xoá file luc trước khi chay app lai
        /*
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/YourAppName");
        if(directory.exists()){
            File[] files = directory.listFiles();
            for (File file : files) {
                file.delete();
            }
        }*/

        //set trang thai của user là 2 thể hiện đang busy nói chuyên
        global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(2,"VoiceTranslationFragment -> onActivityCreated");

    }//end onActivityCreated


    @Override
    public void onDestroy() {
        Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> onDestroy - > gọi mSocket.connect()");
        super.onDestroy();
        //khi qua trang khac thi bỏ connect socket củ
        //global.mSocket.off("receive_call");
        Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> onDestroy - > gọi mSocket.disconnect()");
        global.mSocket.off("receive_end_call");


        // Shutdown TextToSpeech when activity is destroyed
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            //textToSpeech = null;
        }
        global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(0,"VoiceTranslationFragment() -> onDestroy");
    }


    protected void connectToService() {
    }

    @Override
    public void onStop() {
        Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> onStop() "));
        super.onStop();
        deactivateInputs(DeactivableButton.DEACTIVATED);
        if (voiceTranslationActivity.getCurrentFragment() != VoiceTranslationActivity.DEFAULT_FRAGMENT) {
            Toast.makeText(voiceTranslationActivity, getResources().getString(R.string.toast_working_background), Toast.LENGTH_SHORT).show();
        }

       // if(audioRecorder!=null) {
            //audioRecorder.stopRecording();
       // }
        global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(0, "VoiceTranslationFragment() -> onStop");
    }


    @Override
    //start micro phone khi vào fragment này bắt đầu noí chuyên, thu âm giọng nói và chuyển qua text.
    public void startMicrophone(boolean changeAspect) {
        //tam off su dung stt cua rtranlate
        Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> startMicrophone() "));
        if (changeAspect) {
            microphone.setMute(false);
        }
        voiceTranslationServiceCommunicator.startMic();
    }



    @Override
    public void stopMicrophone(boolean changeAspect) {
          //tam off su dung stt cua rtranlate
        Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> stopMicrophone() "));
        if (changeAspect) {
            microphone.setMute(true);
        }
        voiceTranslationServiceCommunicator.stopMic(changeAspect);
    }


    //gọi TTS nói ra âm thanh
    protected void startSound() {
        sound.setMute(false);
        Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> startSound()");
        voiceTranslationServiceCommunicator.startSound();
    }

    protected void stopSound() {
        sound.setMute(true);
        Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> stopSound()");
        voiceTranslationServiceCommunicator.stopSound();
    }

    protected void deactivateInputs(int cause) {
        Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> deactivateInputs()");
        microphone.deactivate(cause);
        if (cause == DeactivableButton.DEACTIVATED) {
            sound.deactivate(DeactivableButton.DEACTIVATED);
        } else {
            sound.activate(false);  // to activate the button sound which otherwise remains deactivated and when clicked it shows the message "wait for initialisation"
        }
    }

    protected void activateInputs(boolean start) {
        Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> activateInputs()");
        microphone.activate(start);
        sound.activate(start);
    }

    public boolean isEditTextOpen() {
        return isEditTextOpen;
    }

    public void deleteEditText() {
        isEditTextOpen = false;
        voiceTranslationServiceCommunicator.setEditTextOpen(false);
        microphone.deleteEditText(voiceTranslationActivity, VoiceTranslationFragment.this, keyboard, editText);
    }

    public boolean isInputActive() {
        return isInputActive;
    }

    public void setInputActive(boolean inputActive) {
        isInputActive = inputActive;
    }

    /**
     * Handles user acceptance (or denial) of our permission request.
     */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != VoiceTranslationService.REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(voiceTranslationActivity, R.string.error_missing_mic_permissions, Toast.LENGTH_LONG).show();
                deactivateInputs(DeactivableButton.DEACTIVATED_FOR_MISSING_MIC_PERMISSION);
                return;
            }
        }

        // possible activation of the mic
        if (!microphone.isMute() && microphone.getActivationStatus() == DeactivableButton.ACTIVATED) {
            Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> microphone() -> startMicrophone");
            startMicrophone(false);
        }
    }

    public class VoiceTranslationServiceCallback extends VoiceTranslationService.VoiceTranslationServiceCallback {
        @Override
        public void onVoiceStarted() {
            Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> VoiceTranslationServiceCallback() -> onVoiceStarted" );
            super.onVoiceStarted();
            microphone.onVoiceStarted();
        }

        @Override
        public void onVoiceEnded() {
            Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> VoiceTranslationServiceCallback() -> onVoiceEnded" );
            super.onVoiceEnded();
            microphone.onVoiceEnded();
        }

        @Override
        public void onMessage(GuiMessage message) {
            Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> VoiceTranslationServiceCallback() -> onMessage" );
            super.onMessage(message);
            if (message != null) {
                if (message.isFinal()) {
                    if (message.isMine()) {
                        Message mM = message.getMessage();
                        mM.setText("GOOGLE: " + message.getMessage().getText() + "\n\n (tap to send)");
                        GuiMessage myNewGoogleMessage = new GuiMessage(mM, true, true, false);
                        int previewIndex = mAdapter.getPreviewIndex();
                        if (previewIndex != -1) {
                            Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> onMessage(1) -> %s",message.getMessage().getText()));
                            //======ban data text cho socket========//
                            if(nameOfpeerWantConnect.equals("")){
                                nameOfpeerWantConnect = global.getPeerWantTalkName();
                            }
                            //AUTO bắn text của google api cho socket
                            //global.SendData_to_mSocket_FOR_SENDMESSAGE(message.getMessage().getText(), global.getName(), nameOfpeerWantConnect, "GOOGLE CLOUD");

                            //mAdapter.setMessage(previewIndex, message);
                            mAdapter.setMessage(previewIndex, myNewGoogleMessage);
                            //smooth scroll
                            smoothScroller.setTargetPosition(mAdapter.getItemCount() - 1);
                            mRecyclerView.getLayoutManager().startSmoothScroll(smoothScroller);

                            //==run whipper test==//
                            if(voiceTranslationActivity.getCurrentFragment() == VoiceTranslationActivity.CONVERSATION_FRAGMENT) {
                                callWhipper();
                            }

                        } else {
                            Message mm = message.getMessage();
                            String smm = mm.getText();
                            Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> onMessage(2) -> %s",smm));

                            mAdapter.addMessage(myNewGoogleMessage);
                            //smooth scroll
                            smoothScroller.setTargetPosition(mAdapter.getItemCount() - 1);
                            mRecyclerView.getLayoutManager().startSmoothScroll(smoothScroller);

                            //======ban data text cho socket========//
                           // global.SendData_to_mSocket_FOR_SENDMESSAGE(message.getMessage().getText(), global.getName(), nameOfpeerWantConnect, "GOOGLE CLOUD");
                            //==run whipper test==//
                            if(voiceTranslationActivity.getCurrentFragment() == VoiceTranslationActivity.CONVERSATION_FRAGMENT) {
                            callWhipper();
                            }
                        }
                    }
                    else
                    {
                        Message mm = message.getMessage();
                        String smm = mm.getText();
                        Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> onMessage(3) -> %s",smm));
                        mAdapter.addMessage(message);
                        //smooth scroll
                        int previewIndex = mAdapter.getPreviewIndex();
                        if (previewIndex == -1) {
                            smoothScroller.setTargetPosition(mAdapter.getItemCount() - 1);
                        } else {
                            smoothScroller.setTargetPosition(mAdapter.getItemCount() - 2);  // because the message is added in the penultimate position, not in the last one, because there is a preview
                        }
                        mRecyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
                    }
                }


                else
                {
                    GuiMessage preview = mAdapter.getPreview();
                    if (preview != null) {
                        // update the component_message_preview
                        Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> onMessage(4) -> %s",message.getMessage().getText()));
                        //======ban data text cho socket========//
                        //SendData_to_mSocket_FOR_SENDMESSAGE(message.getMessage().getText(), "Usertest1", "johnpham");

                        mAdapter.setPreviewText(message.getMessage().getText());

                    } else {
                        //add the component_message_preview
                        //==khi nói thì nó set text vào đây=//
                        Message mm = message.getMessage();
                        String smm = mm.getText();
                        Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> onMessage(5) -> %s",smm));
                        mAdapter.addMessage(message);
                        //smooth scroll
                        smoothScroller.setTargetPosition(mAdapter.getItemCount() - 1);
                        mRecyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
                    }
                }
            }
        }

        @Override
        public void onError(int[] reasons, long value) {
            Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> VoiceTranslationServiceCallback() -> onError" );
            for (int aReason : reasons) {
                switch (aReason) {
                    case ErrorCodes.SAFETY_NET_EXCEPTION:
                    case ErrorCodes.MISSED_CONNECTION:
                        voiceTranslationActivity.showInternetLackDialog(R.string.error_internet_lack_services, null);
                        break;
                    case ErrorCodes.MISSING_GOOGLE_TTS:
                        sound.setMute(true);
                        voiceTranslationActivity.showMissingGoogleTTSDialog();
                        break;
                    case ErrorCodes.GOOGLE_TTS_ERROR:
                        sound.setMute(true);
                        voiceTranslationActivity.showGoogleTTSErrorDialog();
                        break;
                    case VoiceTranslationService.MISSING_MIC_PERMISSION: {
                        requestPermissions(VoiceTranslationService.REQUIRED_PERMISSIONS, VoiceTranslationService.REQUEST_CODE_REQUIRED_PERMISSIONS);
                        break;
                    }
                    default: {
                        voiceTranslationActivity.onError(aReason, value);
                        break;
                    }
                }
            }
        }
    }


    public void restoreAttributesFromService() {
        Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> restoreAttributesFromService() "));
        voiceTranslationServiceCommunicator.getAttributes(new VoiceTranslationService.AttributesListener() {
            @Override
            public void onSuccess(ArrayList<GuiMessage> messages, boolean isMicMute, boolean isAudioMute, final boolean isEditTextOpen, boolean isBluetoothHeadsetConnected) {
                // initialization with service values
                mAdapter = new MessagesAdapter(messages, new MessagesAdapter.Callback() {
                    @Override
                    public void onFirstItemAdded() {
                        description.setVisibility(View.GONE);
                        mRecyclerView.setVisibility(View.VISIBLE);
                    }
                });
                // Applying OnClickListener to our Adapter
                mAdapter.setOnClickListener(new MessagesAdapter.OnClickListener() {
                    @Override
                    public void monClick(int position, String message) {
                        //mode 0 la mode tu do chon all ai message click de send message
                        if(global.getAIMode() == 0) {
                            Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> mAdapter ITEM Click() "));
                            //nếu tap lên massage của whipper
                            if (message.contains("WHISPER:")) {
                                //khi tap lên message cua whipper thi send message qua bên kia
                                String StringFilter = message.replaceAll("WHISPER:", "").trim().replace("\n (tap on to send!)", "");
                                global.SendData_to_mSocket_FOR_SENDMESSAGE(StringFilter.trim(), global.getName(), nameOfpeerWantConnect, "GOOGLE CLOUD");
                            }
                            //nếu tap lên message cua google
                            if (message.contains("GOOGLE:")) {
                                //khi tap lên message cua whipper thi send message qua bên kia
                                String StringFilter = message.replaceAll("GOOGLE:", "").trim().replace("\n\n (tap to send)", "");
                                global.SendData_to_mSocket_FOR_SENDMESSAGE(StringFilter.trim(), global.getName(), nameOfpeerWantConnect, "GOOGLE CLOUD");
                            }
                        }
                    }


                });
                mRecyclerView.setAdapter(mAdapter);
                // restore microphone and sound status
                microphone.setMute(isMicMute);
                sound.setMute(isAudioMute);
                // restore editText
                VoiceTranslationFragment.this.isEditTextOpen = isEditTextOpen;
                if (isEditTextOpen) {
                    keyboard.generateEditText(voiceTranslationActivity, VoiceTranslationFragment.this, microphone, editText, false);
                }
                if (isBluetoothHeadsetConnected) {
                    voiceTranslationServiceCallback.onBluetoothHeadsetConnected();
                } else {
                    voiceTranslationServiceCallback.onBluetoothHeadsetDisconnected();
                }

                if (!microphone.isMute() && !isEditTextOpen) {
                    activateInputs(true);
                } else {
                    activateInputs(false);
                }
            }
        });
    }



    public void callWhipper(){
        // When recording is done (e.g., when user presses a button to stop recording):
        Log.d("CHUNG-", String.format("CHUNG- VoiceTranslationFragment() -> callWhipper() "));

        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/YourAppName");
        if (directory.exists()) {
            String OUTPUT_FILE = "CHUNGrecorded_audio.wav";
            File outputFile = new File(directory, OUTPUT_FILE);
            System.out.println(outputFile);

            if(outputFile.exists()) {
                Log.d("CHUNG-", String.format("CHUNG- truyền cho OPENAI() -> OpenAIWhisperSTT() "));
                //truyền cho OPENAI
                OpenAIWhisperSTT openAIWhipper = new OpenAIWhisperSTT(this);

                openAIWhipper.execute();
            }
        }

    }
    //=======WHIPPER RETURN DATA=====///
    @Override
    public void onTaskComplete(String result) {
        // Handle the result here
        Log.d("CHUNG-", "CHUNG- WHIPPER onTaskComplete result() -> " + result);
        stopMicrophone(true);

        //show on screen
        String value =  "WHISPER: " + result +("\n (tap on to send!)");
        Message mstypeFORGUI = new Message(voiceTranslationActivity, value);
        GuiMessage msFOR_recyclerview = new GuiMessage(mstypeFORGUI, true, true, true);
        if (mAdapter != null) {
            mAdapter.addMessage(msFOR_recyclerview);
            //auto scroll
            //smooth scroll
            smoothScroller.setTargetPosition(mAdapter.getItemCount() - 1);
            mRecyclerView.getLayoutManager().startSmoothScroll(smoothScroller);



            //bắn text qua socket cho user ben kia chổ này mình dung whipper nên vậy
            //======ban data text cho socket========//
            //global.SendData_to_mSocket_FOR_SENDMESSAGE(value, global.getName(), nameOfpeerWantConnect, "WHIPPER");



        }

        //retreat lai micro cho nguoi dung noi tiep
        startMicrophone(true);


    }

    protected void onFailureConnectingWithService(int[] reasons, long value) {
        Log.d("CHUNG-", "CHUNG- VoiceTranslationFragment() -> VoiceTranslationServiceCallback() -> onFailureConnectingWithService" );
        for (int aReason : reasons) {
            switch (aReason) {
                case ErrorCodes.MISSED_ARGUMENT:
                case ErrorCodes.SAFETY_NET_EXCEPTION:
                case ErrorCodes.MISSED_CONNECTION:
                    //creation of the dialog.
                    AlertDialog.Builder builder = new AlertDialog.Builder(voiceTranslationActivity);
                    builder.setMessage(R.string.error_internet_lack_accessing);
                    builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            voiceTranslationActivity.exitFromVoiceTranslation();
                        }
                    });
                    builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            connectToService();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                    break;
                case ErrorCodes.MISSING_GOOGLE_TTS:
                    voiceTranslationActivity.showMissingGoogleTTSDialog();
                    break;
                case ErrorCodes.GOOGLE_TTS_ERROR:
                    voiceTranslationActivity.showGoogleTTSErrorDialog(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            connectToService();
                        }
                    });
                    break;
                case ErrorCodes.MISSING_API_KEY: {
                    deactivateInputs(DeactivableButton.DEACTIVATED_FOR_MISSING_OR_WRONG_KEYFILE);
                    voiceTranslationActivity.showApiKeyFileErrorDialog(R.string.error_missing_api_key_file, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(voiceTranslationActivity, ApiManagementActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            voiceTranslationActivity.exitFromVoiceTranslation();
                        }
                    });
                    break;
                }
                case ErrorCodes.WRONG_API_KEY: {
                    deactivateInputs(DeactivableButton.DEACTIVATED_FOR_MISSING_OR_WRONG_KEYFILE);
                    voiceTranslationActivity.showApiKeyFileErrorDialog(R.string.error_wrong_api_key_file, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(voiceTranslationActivity, ApiManagementActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            voiceTranslationActivity.exitFromVoiceTranslation();
                        }
                    });
                    break;
                }
                default:
                    voiceTranslationActivity.onError(aReason, value);
                    break;
            }
        }
    }
}
