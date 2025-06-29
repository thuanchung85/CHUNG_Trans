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

import android.Manifest;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.socket.emitter.Emitter;
import nie.translator.rtranslatordevedition.GeneralActivity;
import nie.translator.rtranslatordevedition.Global;
import nie.translator.rtranslatordevedition.R;
import nie.translator.rtranslatordevedition.api_management.ApiManagementActivity;
import nie.translator.rtranslatordevedition.api_management.KeyFileContainer;
import nie.translator.rtranslatordevedition.settings.SettingsActivity;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.tools.CustomServiceConnection;
import nie.translator.rtranslatordevedition.tools.Tools;
import nie.translator.rtranslatordevedition.tools.gui.RequestDialog;
import nie.translator.rtranslatordevedition.tools.gui.animations.CustomAnimator;
import nie.translator.rtranslatordevedition.tools.gui.peers.GuiPeer;
import nie.translator.rtranslatordevedition.tools.services_communication.ServiceCommunicatorListener;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.PairingFragment;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation.ConversationFragment;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation.ConversationService;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation.main.ConversationMainFragment;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.ConversationBluetoothCommunicator;
import com.bluetooth.communicator.BluetoothCommunicator;
import com.bluetooth.communicator.Peer;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nie.translator.rtranslatordevedition.voice_translation._walkie_talkie_mode._walkie_talkie.WalkieTalkieFragment;
import nie.translator.rtranslatordevedition.voice_translation._walkie_talkie_mode._walkie_talkie.WalkieTalkieService;

import io.socket.client.IO;
import io.socket.client.Socket;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.mySocketForeGroundService.ChungForegroundService;

///CỔNG VÀO THỨ 2
//đây là activity chổ show các user co thể connect với máy mình
public class VoiceTranslationActivity extends GeneralActivity {



    ///========================================
    //flags
    public static final int NORMAL_START = 0;
    public static final int FIRST_START = 1;
    //costants
    public static final int PAIRING_FRAGMENT = 0;
    public static final int CONVERSATION_FRAGMENT = 1;
    public static final int WALKIE_TALKIE_FRAGMENT = 2;
    public static final int DEFAULT_FRAGMENT = PAIRING_FRAGMENT;
    public static final int NO_PERMISSIONS = -10;
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 2;
    public static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };
    //objects
    private Global global;
    private Fragment fragment;
    private CoordinatorLayout fragmentContainer;
    private int currentFragment = -1;
    private ArrayList<Callback> clientsCallbacks = new ArrayList<>();
    private ArrayList<CustomServiceConnection> conversationServiceConnections = new ArrayList<>();
    private ArrayList<CustomServiceConnection> walkieTalkieServiceConnections = new ArrayList<>();
    private Handler mainHandler;  // handler that can be used to post to the main thread
    //variables
    private int connectionId = 1;


    public void startService() {
        Intent serviceIntent = new Intent(this, ChungForegroundService.class);
        serviceIntent.putExtra("inputExtra", "Foreground Service rTranslator socket in Android");
        ContextCompat.startForegroundService(this, serviceIntent);
        global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(0, "Foreground Service -> startService()" );
    }
    public void stopService() {
        Intent serviceIntent = new Intent(this, ChungForegroundService.class);
        stopService(serviceIntent);
        global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(1, "Foreground Service -> stopService()");
    }




    @Override
    protected void onPause() {
        global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(0, "VoiceTranslationActivity -> onPause");
        super.onPause();
        Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> onPause");


        stopConversationService();
        stopWalkieTalkieService();
        //Start socket serivce foreground for offline call
        startService();

        if(getCurrentFragment() == (VoiceTranslationActivity.CONVERSATION_FRAGMENT))  {
            setFragment(VoiceTranslationActivity.DEFAULT_FRAGMENT);
            global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(0, "VoiceTranslationActivity -> onPause");
        }


    }

    @Override
    protected void onDestroy() {
        //global.mSocket.disconnect();
        //thay vì disconnect mình gọi socket update status
        global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(0, "VoiceTranslationActivity -> onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(1, "VoiceTranslationActivity -> onResume");
        super.onResume();
        //stop foreground service when app active
        stopService();
    }

    //biến dùng để check khi nào activity này stop bởi chính nó, không phải bởi activity khác
    public boolean onlyVoiceTranslationActivityAllow = false;
    @Override
    protected void onStop() {
        super.onStop();

        if(onlyVoiceTranslationActivityAllow == true) {
            //===bắn vô socket end_call ===//
            //bị BUG chổ này current Fragement = -1 khi user mở api va setting activity
            Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> onStop -> SendData_to_mSocket_FOR_END_CONNECT2USER");
            global.SendData_to_mSocket_FOR_END_CONNECT2USER(global.getName(), global.getPeerWantTalkName());
        }
        //VẪN CÒN 1 BUGG CHỔ NÀY KHI CHÍNH USER NÀY THOAT APP NGANG THÌ NÓ VẪN VÀO onlyVoiceTranslationActivityAllow == TRUE
        //TUY LUC ĐÓ 2 USER KHAC ĐANG NÓI CHUYÊN, KHÔNG PHẢI USER NÀY.

    }

    // Method to request notification permissions
    private void requestNotificationPermissions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Notification Permission Required");
        builder.setMessage("Please grant notification permissions to receive notifications.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Open app settings to allow the user to grant notification permissions
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 101);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle the case where the user cancels the permission request
            }
        });
        builder.show();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //TRY to ask notification permission
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            requestNotificationPermissions();
        }

        // Keep the screen on for this activity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> onCreate");
        super.onCreate(savedInstanceState);
        //chính class này là class load activity_main.xml
        setContentView(R.layout.activity_main);

        //The getApplication() method is a method of the Context class in Android which returns a context object for the entire application
        //thử lấy biến toàn app
        Application mapp = getApplication();
        global = (Global) mapp; //cast no về thành Global

        //đây là main thread khởi taọ biến chạy trên main thread
        mainHandler = new Handler(Looper.getMainLooper());

        // Clean fragments (only if the app is recreated (When user disable permission))
        //khởi tạo fragment manager.
        FragmentManager mfragmentManager = getSupportFragmentManager();

        int entryCount = mfragmentManager.getBackStackEntryCount();
        if (entryCount > 0) {
            mfragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        // Remove previous fragments (case of the app was restarted after changed permission on android 6 and higher)
        List<Fragment> fragmentList = mfragmentManager.getFragments();

        for (Fragment item : fragmentList) {
            if (item != null) {
                mfragmentManager.beginTransaction().remove(item).commit();
            }
        }

        //edit custom status bar
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        //fragment_container CÁI NÀY khai báo trong layout activity_main.xml
        fragmentContainer = findViewById(R.id.fragment_container);


        //RE LOGIN NOW! with FMC_token
        String tempUserChungPhone =  global.getName();
        String tempUserChungPhoneFirstname =  "f_" + global.getName();
        String tempUserChungPhoneLastname =  "l_" + global.getName();
        String tempUserChungPhoneLanguage = getResources().getConfiguration().locale.getLanguage();
        String FMC_token = global.FMCToken;
        global.SendData_to_mSocketFORLOGIN(tempUserChungPhone, tempUserChungPhoneFirstname, tempUserChungPhoneLastname, tempUserChungPhoneLanguage, FMC_token);


        /*if (savedInstanceState != null) {
            //Restore the fragment's instance
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, "myFragmentName");
        }*/

        /*
        //===FIRE BASE TOKEN====//
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w("CHUNG", "Fetching FCM registration token failed", task.getException());
                            return;
                        }
                        // Get new FCM registration token
                        String token = task.getResult();
                        // Log and toast
                        String msg = "FMC TOKEN: -> \n" + token;
                        global.FMCToken = token;
                        Log.w("CHUNG", " FCM  token:" +  global.FMCToken);


                        //RE LOGIN NOW! with FMC_token
                        String tempUserChungPhone =  global.getName();
                        String tempUserChungPhoneFirstname =  "f_" + global.getName();
                        String tempUserChungPhoneLastname =  "l_" + global.getName();
                        String tempUserChungPhoneLanguage = getResources().getConfiguration().locale.getLanguage();
                        String FMC_token = global.FMCToken;
                        global.SendData_to_mSocketFORLOGIN(tempUserChungPhone, tempUserChungPhoneFirstname, tempUserChungPhoneLastname, tempUserChungPhoneLanguage, FMC_token);
                        Toast.makeText(getBaseContext(), "RELOGIN with FCM  token:" +  global.FMCToken, Toast.LENGTH_SHORT).show();
                    }
                });

*/

        //===GET BACK DATA WHEN OPEN BY NOTIFICATION TAP======///
        Intent intent = getIntent();
        if (intent != null) {

            String actionNotificationCall = intent.getStringExtra("action");
            String toNotificationCall = intent.getStringExtra("_to");
            String fromNotificationCall = intent.getStringExtra("_from");
            Log.d("CHUNG-", "CHUNG- =====WAKEUP========onNewIntent(1)====================" + actionNotificationCall);
            Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> GET BACK DATA WHEN OPEN BY NOTIFICATION TAP: " + actionNotificationCall);
            if (actionNotificationCall != null) {
                // Use the action data here
                //Toast.makeText(getBaseContext(), "Notification Call DATA: " +  actionNotificationCall + " " + toNotificationCall + " " + fromNotificationCall, Toast.LENGTH_SHORT).show();
               //show dialobox ok cuoc goi
                //show dialog ok connect or not
                RequestDialog connectionRequestDialog = new RequestDialog(this,
                        fromNotificationCall + " want connect with you ?", new DialogInterface.OnClickListener() {
                    @Override
                    //==> OK USER XAC NHAN BAM NUT OK ACCEPT CONNECT
                    public void onClick(DialogInterface dialog, int which) {
                        //thông báo socket tôi OK connect "accept_call"
                        global.SendData_to_mSocket_FOR_ACCEPT_CONNECT2USER( fromNotificationCall,toNotificationCall,true);

                        //khi qua trang khac thi bỏ ghe event receive_call socket của user khac ban qua
                        global.mSocket.off("receive_call");
                        global.mSocket.off("users");

                        //ghi lai user đang muốn call mình
                        global.setPeerWantTalkName(fromNotificationCall);
                        //======QUAN TRONG: chơi ăn gian===> đi thẳng vào luôn DI VAO TRANG CHAT VOICE ===//
                        setFragment(VoiceTranslationActivity.CONVERSATION_FRAGMENT);

                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    //==> OK USER KHONG CHIU BAM NUT HUY CANCEL-> reject CONNECT
                    public void onClick(DialogInterface dialog, int which) {
                        //thông báo socket tôi REJECT connect "REJECT accept_call"
                        global.SendData_to_mSocket_FOR_ACCEPT_CONNECT2USER(fromNotificationCall,toNotificationCall,false);
                    }
                });
                connectionRequestDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                });
                connectionRequestDialog.show();

            } else {
                // Handle case where action data is null
            }
        } else {
            // Handle case where intent is null
        }

        //try clear off all notification when app open
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }



    }

    /*
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String actionNotificationCall = intent.getStringExtra("action");
        String toNotificationCall = intent.getStringExtra("_to");
        String fromNotificationCall = intent.getStringExtra("_from");
        Log.d("CHUNG-", "CHUNG- =====WAKEUP========onNewIntent(2)====================" + actionNotificationCall);

        // Check if the activity was started by a new intent
        if (intent != null && intent.hasExtra("action")) {
            // Retrieve the data from the new intent
            String action = intent.getStringExtra("action");
            if (action != null) {
                // Handle the data
                // For example, update UI based on the action
            }
        }
    }
*/

    @Override
    protected void onStart() {
        onlyVoiceTranslationActivityAllow = true;

        Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> onStart");
        super.onStart();
        // when we return to the app's gui based on the service that was saved in the last closure we choose which fragment to start
        SharedPreferences msharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        int x = msharedPreferences.getInt("fragment", DEFAULT_FRAGMENT);

        //gọi fragment ra , fragment sô 1
        setFragment(0);


        ///XIN QUYEN MICRO//nếu micro ok quyền rôi thi hỏi tiep vi tri nguoi dung. và quyền đọc ghi file
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        }

        /*
        //xin quyền WRITE_EXTERNAL_STORAGE nếu chưa có
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //xin quyền đọc ghi file để load file json key api
            String[] REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            requestPermissions(REQUIRED_PERMISSIONS, 5);
        }
        //nếu đã có sẳn quyền
        else{

            //test chay tìm file json trong máy
            findJsonFiles_BYCHUNG(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()),
                    (KeyFileContainer.FilesListListener) new KeyFileContainer.FilesListListener(){
                        public void onSuccess(ArrayList<File> filesList) {
                            System.out.println(filesList);
                            if(!filesList.isEmpty()) {
                                String filenameKeyAPI = filesList.get(0).getName();
                                Log.d("CHUNG-", "CHUNG- LoadingActivity() -> GET JSON KEY FILE SUCCESS DONE!" + filenameKeyAPI);
                                saveAPIKEYFILE_BYCHUNG((File) filesList.get(0), new KeyFileContainer.FileOperationListener() {
                                    public void onSuccess() {
                                        Log.d("CHUNG-", "CHUNG- LoadingActivity() -> SAVE KEY FILE SUCCESS DONE!" + filenameKeyAPI);
                                    }

                                    public void onFailure() {
                                        Log.d("CHUNG-", "CHUNG- LoadingActivity() -> SAVE KEY FILE FAIL !" + filenameKeyAPI);
                                    }
                                });
                            }
                            else{
                                Log.d("CHUNG-", "CHUNG- LoadingActivity() -> GET JSON KEY FILE FAIL !");
                            }
                        }
                    }


            );
        }
*/


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            }
            case R.id.apiManagement: {
               Intent intent = new Intent(this, ApiManagementActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //tạo tool bar menu _ chính la cái menu ba chấm đi vao setting và api key view. nó nằm ở toàn cục app không trong các fragment con hay activity con
        //getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    public void setFragment(int fragmentName) {
        int currentFragment = fragmentName;
        Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> setFragment "+ currentFragment);
        //nếu là currentFragment = 0 -> gọi paringFragment
        //nếu là currentFragment = 1 -> gọi conversationFragment
        //nếu là currentFragment = 2 -> gọi walkieTalkieFragment
        switch (fragmentName) {
            //0 là đi tới PAIRING_FRAGMENT
            case PAIRING_FRAGMENT: {
                global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(1, "VoiceTranslationActivity -> setFragment -> PAIRING_FRAGMENT");
                // possible stop of the Conversation and WalkieTalkie Service
                stopConversationService();
                stopWalkieTalkieService();
                // possible setting of the fragment
                if (getCurrentFragment() != PAIRING_FRAGMENT) {
                    Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> new PairingFragment ");
                    PairingFragment paringFragment = new PairingFragment();
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    Bundle bundle = new Bundle();
                    paringFragment.setArguments(bundle);
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                    transaction.replace(R.id.fragment_container, paringFragment);
                    transaction.commit();
                    currentFragment = PAIRING_FRAGMENT;

                    //saveFragment();
                    //fragment=paringFragment;
                }
                break;
            }



            //1 là đi tới CONVERSATION_FRAGMENT
            case CONVERSATION_FRAGMENT: {
                global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(0, "VoiceTranslationActivity -> setFragment -> CONVERSATION_FRAGMENT");
                // possible setting of the fragment
                if (getCurrentFragment() != CONVERSATION_FRAGMENT) {
                    Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> new ConversationFragment ");
                    ConversationFragment conversationFragment = new ConversationFragment();
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("firstStart", true);
                    conversationFragment.setArguments(bundle);
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.replace(R.id.fragment_container, conversationFragment);
                    transaction.commit();
                    currentFragment = CONVERSATION_FRAGMENT;
                    //saveFragment();
                    //fragment= conversationFragment;
                }
                break;
            }





            //2 là đi tới WALKIE_TALKIE_FRAGMENT
            case WALKIE_TALKIE_FRAGMENT: {
                global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(0, "VoiceTranslationActivity -> setFragment -> WALKIE_TALKIE_FRAGMENT");
                // possible setting of the fragment
                if (getCurrentFragment() != WALKIE_TALKIE_FRAGMENT) {
                    WalkieTalkieFragment walkieTalkieFragment = new WalkieTalkieFragment();
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("firstStart", true);
                    walkieTalkieFragment.setArguments(bundle);
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.replace(R.id.fragment_container, walkieTalkieFragment);
                    transaction.commit();
                    currentFragment = WALKIE_TALKIE_FRAGMENT;
                    //saveFragment();
                    //fragment=walkieTalkieFragment;
                }
                break;
            }
        }
    }

    public void saveFragment() {
        new Thread("saveFragment") {
            @Override
            public void run() {
                super.run();
                //save fragment
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(VoiceTranslationActivity.this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("fragment", getCurrentFragment());
                editor.apply();
            }
        }.start();
    }

    public int getCurrentFragment() {
        if (currentFragment != -1) {
            return currentFragment;
        } else {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment != null) {
                if (currentFragment.getClass().equals(PairingFragment.class)) {
                    return PAIRING_FRAGMENT;
                }
                if (currentFragment.getClass().equals(ConversationFragment.class)) {
                    return CONVERSATION_FRAGMENT;
                }
                if (currentFragment.getClass().equals(WalkieTalkieFragment.class)) {
                    return WALKIE_TALKIE_FRAGMENT;
                }
            }
        }
        return -1;
    }

    public int startSearch() {
        //tạm thời bỏ tính năng search các thiết bị bluetooth xung quanh, vi đang dung socket
        //return BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED;

        if (global.getBluetoothCommunicator().isBluetoothLeSupported()) {
            if (Tools.hasPermissions(this, REQUIRED_PERMISSIONS)) {
                return global.getBluetoothCommunicator().startSearch();
            } else {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
                return NO_PERMISSIONS;
            }
        } else {
            return BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED;
        }


    }

    public int stopSearch(boolean tryRestoreBluetoothStatus) {
        return global.getBluetoothCommunicator().stopSearch(tryRestoreBluetoothStatus);
    }

    public boolean isSearching() {
        return global.getBluetoothCommunicator().isSearching();
    }


    ///=================HAM CONNECT KẾT NỐi user và đi vào fragment Conversation====/////////
    public void connect(Peer peer) {

        stopSearch(false);//stop search user gần đó

        ///HÀM QUAN TRONG: CONNECT BLUETOOTH/////OK THI mơi có cửa đi tiếp đến conversation
        global.getBluetoothCommunicator().connect(peer);
    }

    public void acceptConnection(Peer peer) {
        global.getBluetoothCommunicator().acceptConnection(peer);
    }

    public void rejectConnection(Peer peer) {
        global.getBluetoothCommunicator().rejectConnection(peer);
    }

    public ArrayList<GuiPeer> getConnectedPeersList() {
        return global.getBluetoothCommunicator().getConnectedPeersList();
    }

    public ArrayList<Peer> getConnectingPeersList() {
        return global.getBluetoothCommunicator().getConnectingPeers();
    }

    public void disconnect(Peer peer) {
        global.getBluetoothCommunicator().disconnect(peer);
    }



    /*@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Save the fragment's instance
        getSupportFragmentManager().putFragment(outState, "myFragmentName", fragment);
    }*/

    /**
     * Handles user acceptance (or denial) of our permission request.
     */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);





        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {

            //test chay tìm file json trong máy
            findJsonFiles_BYCHUNG(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()),
                    (KeyFileContainer.FilesListListener) new KeyFileContainer.FilesListListener(){
                        public void onSuccess(ArrayList<File> filesList) {
                            System.out.println(filesList);
                            if(!filesList.isEmpty()) {
                                String filenameKeyAPI = filesList.get(0).getName();
                                Log.d("CHUNG-", "CHUNG- LoadingActivity() -> GET JSON KEY FILE SUCCESS DONE!" + filenameKeyAPI);
                                saveAPIKEYFILE_BYCHUNG((File) filesList.get(0), new KeyFileContainer.FileOperationListener() {
                                    public void onSuccess() {
                                        Log.d("CHUNG-", "CHUNG- LoadingActivity() -> SAVE KEY FILE SUCCESS DONE!" + filenameKeyAPI);
                                    }

                                    public void onFailure() {
                                        Log.d("CHUNG-", "CHUNG- LoadingActivity() -> SAVE KEY FILE FAIL !" + filenameKeyAPI);
                                    }
                                });
                            }
                            else{
                                Log.d("CHUNG-", "CHUNG- LoadingActivity() -> GET JSON KEY FILE FAIL !");
                            }
                        }
                    }


            );

            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                notifyMissingSearchPermission();
                return;
            }
        }
        notifySearchPermissionGranted();
        //recreate();   // was called only if the grantResults were of length 0 or were neither PERMISSIONS_GRANTED nor PERMISSION_DENIED (I don't know what it is for anyway)



    }

    @Override
    //khi user bấm nút back hình cái cửa trên góc trên tay phải của app
    public void onBackPressed() {
        global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(1, "VoiceTranslationActivity -> onBackPressed");
        DialogInterface.OnClickListener confirmExitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                exitFromVoiceTranslation();

                //===bắn vô socket end_call ===//
                global.SendData_to_mSocket_FOR_END_CONNECT2USER(global.getName(),global.getPeerWantTalkName());


            }
        };

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            if (fragment instanceof ConversationFragment) {
                Fragment currentChildFragment = ((ConversationFragment) fragment).getCurrentFragment();
                if (currentChildFragment instanceof ConversationMainFragment) {
                    ConversationMainFragment conversationMainFragment = (ConversationMainFragment) currentChildFragment;
                    if (conversationMainFragment.isInputActive()) {
                        if (conversationMainFragment.isEditTextOpen()) {
                            conversationMainFragment.deleteEditText();
                        } else {
                            showConfirmExitDialog(confirmExitListener);
                        }
                    }
                } else {
                    showConfirmExitDialog(confirmExitListener);
                }
            } else if (fragment instanceof WalkieTalkieFragment) {
                WalkieTalkieFragment walkieTalkieFragment = (WalkieTalkieFragment) fragment;
                if (walkieTalkieFragment.isInputActive()) {
                    if (walkieTalkieFragment.isEditTextOpen()) {
                        walkieTalkieFragment.deleteEditText();
                    } else {
                        Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> setFragment "+ currentFragment);
                        setFragment(DEFAULT_FRAGMENT);
                    }
                }
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }


    }
    public void onBackPressed_NOTCALL_AGAIN() {
        global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(1, "VoiceTranslationActivity -> onBackPressed_NOTCALL_AGAIN");
        DialogInterface.OnClickListener confirmExitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                exitFromVoiceTranslation();
            }
        };

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            if (fragment instanceof ConversationFragment) {
                Fragment currentChildFragment = ((ConversationFragment) fragment).getCurrentFragment();
                if (currentChildFragment instanceof ConversationMainFragment) {
                    ConversationMainFragment conversationMainFragment = (ConversationMainFragment) currentChildFragment;
                    if (conversationMainFragment.isInputActive()) {
                        if (conversationMainFragment.isEditTextOpen()) {
                            conversationMainFragment.deleteEditText();
                        } else {
                            showConfirmExitDialogNOCANCEL(confirmExitListener);
                        }
                    }
                } else {
                    showConfirmExitDialog(confirmExitListener);
                }
            } else if (fragment instanceof WalkieTalkieFragment) {
                WalkieTalkieFragment walkieTalkieFragment = (WalkieTalkieFragment) fragment;
                if (walkieTalkieFragment.isInputActive()) {
                    if (walkieTalkieFragment.isEditTextOpen()) {
                        walkieTalkieFragment.deleteEditText();
                    } else {
                        Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> setFragment "+ currentFragment);
                        setFragment(DEFAULT_FRAGMENT);
                    }
                }
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }


    public void exitFromVoiceTranslation() {
        if (global.getBluetoothCommunicator().getConnectedPeersList().size() > 0) {
            global.getBluetoothCommunicator().disconnectFromAll();
        } else {
            Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> setFragment "+ currentFragment);
            setFragment(VoiceTranslationActivity.DEFAULT_FRAGMENT);
        }
    }


    // services management

    public void startConversationService(final Notification notification, final Global.ResponseListener responseListener) {
        final Intent intent = new Intent(this, ConversationService.class);
        global.getLanguage(false, new Global.GetLocaleListener() {
            @Override
            public void onSuccess(CustomLocale result) {
                intent.putExtra("notification", notification);
                startService(intent);
                responseListener.onSuccess();
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });

    }

    public void startWalkieTalkieService(final Notification notification, final Global.ResponseListener responseListener) {
        final Intent intent = new Intent(this, WalkieTalkieService.class);
        // initialization of the WalkieTalkieService
        global.getFirstLanguage(false, new Global.GetLocaleListener() {
            @Override
            public void onSuccess(CustomLocale result) {
                intent.putExtra("firstLanguage", result);
                global.getSecondLanguage(false, new Global.GetLocaleListener() {
                    @Override
                    public void onSuccess(CustomLocale result) {
                        intent.putExtra("secondLanguage", result);
                        intent.putExtra("notification", notification);
                        startService(intent);
                        responseListener.onSuccess();
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

    public synchronized void connectToConversationService(final VoiceTranslationService.VoiceTranslationServiceCallback callback, final ServiceCommunicatorListener responseListener) {
        // possible start of ConversationService
        startConversationService(buildNotification(CONVERSATION_FRAGMENT), new Global.ResponseListener() {
            @Override
            public void onSuccess() {
                CustomServiceConnection conversationServiceConnection = new CustomServiceConnection(new ConversationService.ConversationServiceCommunicator(connectionId));
                connectionId++;
                conversationServiceConnection.addCallbacks(callback, responseListener);
                conversationServiceConnections.add(conversationServiceConnection);
                bindService(new Intent(VoiceTranslationActivity.this, ConversationService.class), conversationServiceConnection, BIND_ABOVE_CLIENT);
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });
    }

    public synchronized void connectToWalkieTalkieService(final VoiceTranslationService.VoiceTranslationServiceCallback callback, final ServiceCommunicatorListener responseListener) {
        // possible start of WalkieTalkieService
        startWalkieTalkieService(buildNotification(WALKIE_TALKIE_FRAGMENT), new Global.ResponseListener() {
            @Override
            public void onSuccess() {
                CustomServiceConnection walkieTalkieServiceConnection = new CustomServiceConnection(new WalkieTalkieService.WalkieTalkieServiceCommunicator(connectionId));
                connectionId++;
                walkieTalkieServiceConnection.addCallbacks(callback, responseListener);
                walkieTalkieServiceConnections.add(walkieTalkieServiceConnection);
                bindService(new Intent(VoiceTranslationActivity.this, WalkieTalkieService.class), walkieTalkieServiceConnection, BIND_ABOVE_CLIENT);
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });
    }

    public void disconnectFromConversationService(ConversationService.ConversationServiceCommunicator conversationServiceCommunicator) {
        int index = -1;
        boolean found = false;
        for (int i = 0; i < conversationServiceConnections.size() && !found; i++) {
            if (conversationServiceConnections.get(i).getServiceCommunicator().equals(conversationServiceCommunicator)) {
                index = i;
                found = true;
            }
        }
        if (index != -1) {
            CustomServiceConnection serviceConnection = conversationServiceConnections.remove(index);
            unbindService(serviceConnection);
            serviceConnection.onServiceDisconnected();
        }
    }

    public void disconnectFromWalkieTalkieService(WalkieTalkieService.WalkieTalkieServiceCommunicator walkieTalkieServiceCommunicator) {
        int index = -1;
        boolean found = false;
        for (int i = 0; i < walkieTalkieServiceConnections.size() && !found; i++) {
            if (walkieTalkieServiceConnections.get(i).getServiceCommunicator().equals(walkieTalkieServiceCommunicator)) {
                index = i;
                found = true;
            }
        }
        if (index != -1) {
            CustomServiceConnection serviceConnection = walkieTalkieServiceConnections.remove(index);
            unbindService(serviceConnection);
            serviceConnection.onServiceDisconnected();
        }
    }

    public void stopConversationService() {
        stopService(new Intent(this, ConversationService.class));
    }

    public void stopWalkieTalkieService() {
        stopService(new Intent(this, WalkieTalkieService.class));
    }

    //notification
    private Notification buildNotification(int clickAction) {
        String channelID = "service_background_notification";
        String channelName = getResources().getString(R.string.notification_channel_name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        // creation of the click on the notification
        Intent resultIntent = new Intent(this, VoiceTranslationActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        // creation of the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID);
        if (clickAction == CONVERSATION_FRAGMENT) {
            builder.setContentTitle("Conversation")
                    .setContentText("Conversation mode is running...")
                    .setContentIntent(resultPendingIntent)
                    .setSmallIcon(R.drawable.mic_icon)
                    .setOngoing(true)
                    .setChannelId(channelID)
                    .build();
        } else {
            builder.setContentTitle("WalkieTalkie")
                    .setContentText("WalkieTalkie mode is running...")
                    .setContentIntent(resultPendingIntent)
                    .setSmallIcon(R.drawable.mic_icon)
                    .setOngoing(true)
                    .setChannelId(channelID)
                    .build();
        }
        return builder.build();
    }


    public void addCallback(Callback callback) {
        // in this way the listener will listen to both this activity and the communicator
        global.getBluetoothCommunicator().addCallback(callback);
        clientsCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        global.getBluetoothCommunicator().removeCallback(callback);
        clientsCallbacks.remove(callback);
    }

    private void notifyMissingSearchPermission() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < clientsCallbacks.size(); i++) {
                    clientsCallbacks.get(i).onMissingSearchPermission();
                }
            }
        });
    }

    private void notifySearchPermissionGranted() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < clientsCallbacks.size(); i++) {
                    clientsCallbacks.get(i).onSearchPermissionGranted();
                }
            }
        });
    }

    public CoordinatorLayout getFragmentContainer() {
        return fragmentContainer;
    }

    public static class Callback extends ConversationBluetoothCommunicator.Callback {
        public void onMissingSearchPermission() {
        }

        public void onSearchPermissionGranted() {
        }
    }


    public void findJsonFiles_BYCHUNG(final File dir,final KeyFileContainer.FilesListListener filesListListener) {
        new Thread() {
            public void run() {
                super.run();
                final ArrayList<File> list = new ArrayList<>();
                findJsonFiles_BYCHung(dir,list);
                System.out.println(list);
                mainHandler.post(new Runnable() {
                    public void run() {
                        filesListListener.onSuccess(list);
                    }
                });
            }
        }.start();
    }

    public void findJsonFiles_BYCHung(File dir, ArrayList<File> matchingSAFFiles) {
        String safPattern = ".json";
        File[] listFile = dir.listFiles();
        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {
                String filename = listFile[i].getName();
                if (listFile[i].isDirectory()) {
                    findJsonFiles_BYCHung(listFile[i], matchingSAFFiles);
                } else if (filename.endsWith(safPattern)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(dir.toString());
                    sb.append(File.separator);
                    sb.append(listFile[i].getName());
                    matchingSAFFiles.add(new File(sb.toString()));
                }
            }
        }
    }

    public void saveAPIKEYFILE_BYCHUNG(final File file, final KeyFileContainer.FileOperationListener responseListener) {
        new Thread() {
            public void run() {
                super.run();
                if (Tools.copyFile(file, new File(getFilesDir(), file.getName()))) {
                    global.setApiKeyFileName(file.getName());
                    global.resetApiToken();
                    mainHandler.post(new Runnable() {
                        public void run() {
                            responseListener.onSuccess();
                        }
                    });
                    return;
                }
                mainHandler.post(new Runnable() {
                    public void run() {
                        responseListener.onFailure();
                    }
                });
            }
        }.start();
    }

}



