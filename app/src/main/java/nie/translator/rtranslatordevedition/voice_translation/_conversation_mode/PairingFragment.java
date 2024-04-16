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

package nie.translator.rtranslatordevedition.voice_translation._conversation_mode;
import static androidx.core.content.ContextCompat.getSystemService;
import static com.google.common.reflect.Reflection.getPackageName;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.app.Activity;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import nie.translator.rtranslatordevedition.Global;
import nie.translator.rtranslatordevedition.R;
import nie.translator.rtranslatordevedition.api_management.ApiManagementActivity;
import nie.translator.rtranslatordevedition.settings.LanguagePreference;
import nie.translator.rtranslatordevedition.settings.SettingsActivity;
import nie.translator.rtranslatordevedition.settings.UserImagePreference;
import nie.translator.rtranslatordevedition.settings.UserNamePreference;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.tools.FileLog;
import nie.translator.rtranslatordevedition.tools.Tools;
import nie.translator.rtranslatordevedition.tools.gui.RequestDialog;
import nie.translator.rtranslatordevedition.tools.gui.WalkieTalkieButton;
import nie.translator.rtranslatordevedition.tools.gui.animations.CustomAnimator;
import nie.translator.rtranslatordevedition.tools.gui.peers.GuiPeer;
import nie.translator.rtranslatordevedition.tools.gui.peers.Listable;
import nie.translator.rtranslatordevedition.tools.gui.peers.PeerListAdapter;
import nie.translator.rtranslatordevedition.tools.gui.peers.array.PairingArray;
import nie.translator.rtranslatordevedition.voice_translation.VoiceTranslationActivity;
import com.bluetooth.communicator.BluetoothCommunicator;
import com.bluetooth.communicator.Peer;
import com.bluetooth.communicator.tools.Timer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.User;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.recent_peer.RecentPeer;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.recent_peer.RecentPeersDataManager;

//đây là màn hình show các người dùng đang tìm thấy đươc xung quanh
public class PairingFragment extends PairingToolbarFragment {

    private LanguagePreference languagePreference;
    //======CUC SOCKET=====///

    List<RecentPeer> arr_recentPeersFormWebSocket = new ArrayList<RecentPeer>();
    //khởi tao websocket listener hứng data websocket trở về
    //khi login vao websocket và lấy thông tin các user khác
    private Emitter.Listener onLoginCallBack = new Emitter.Listener() {

        @Override
        //hàm websocket server tra ra data về
        public void call(final Object... args) {

            Log.d("CHUNG-", String.format("CHUNG- PairingFragment - > mSocket() ->onLoginCallBack server reply -> %s ", args.toString()));
            String argsReponse =  Arrays.toString(args);
            //covert json data từ server về data native android
            try {
                JSONArray jsonArray = new JSONArray(argsReponse);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    // Accessing data in the JSONObject
                    boolean success = jsonObject.getBoolean("success");
                    System.out.println(success);
                    if(success != true){
                        Log.d("CHUNG-", String.format("CHUNG- PairingFragment  mSocket() -> server reply CO VAN DE-> %b ", success));
                        return;
                    }

                    arr_recentPeersFormWebSocket.clear();



                    JSONArray usersArray = new JSONArray(jsonObject.getString("users"));

                    //duyet loop qua các user trong usersArray
                    for (int each =0 ; each < usersArray.length(); each++) {


                        JSONObject userObject = usersArray.getJSONObject(each); // Assuming there's only one user
                        String userUsername = userObject.getString("username");
                        String currentNameOfuser = global.getName();

                        //nếu trong boardcast có tên của chính user hiện tại thì bỏ qua, không cho vào array list của arr_recentPeersFormWebSocket
                        if(currentNameOfuser.equals(userUsername) ){
                            continue;
                        }



                        String userFirstname = userObject.getString("firstname");
                        String userLastname = userObject.getString("lastname");
                        String userPersonal_language = userObject.getString("personal_language");
                        String userOnline = userObject.getString("online");

                        //nếu user kia mà bị offline với bất kỳ lí do gì thì thông báo user này off
                        if(userUsername.equals(global.getPeerWantTalkName())){
                            //user nào mà có cờ == 1 thi là ok gọi đươc
                            if(!userOnline.equals("1")) {
                                int currentFrag = voiceTranslationActivity.getCurrentFragment();
                                if (currentFrag != VoiceTranslationActivity.PAIRING_FRAGMENT) {
                                    //quy tro ve PAIRING_FRAGMENT run main thread show back dialog box
                                    voiceTranslationActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            //user còn online trong room chat sẽ bị auto đá ra bởi lệnh dưới, do user kia tắt app
                                            voiceTranslationActivity.onBackPressed();
                                        }
                                    });
                                }
                            }
                            //user nào mà có cờ == 2 thì có nghĩa là bị kẹt đang busy hay sao đó, không gọi đươc
                            else{
                                if(userOnline.equals("2")){

                                }
                            }
                        }

                        boolean userSkip = userObject.getBoolean("skip");

                        double userCreatedTime = userObject.getDouble("__createdtime__");
                        double userUpdatedtime = userObject.getDouble("__updatedtime__");
                        int userActive = userObject.getInt("active");

                        Log.d("CHUNG-", "CHUNG- mSocket() -> onLoginCallBack server reply DATA->  "+
                                userUsername + " " + userFirstname + " " + userLastname + " " + userPersonal_language + " " + userSkip + " " + userCreatedTime + " " + userUpdatedtime + " " + userActive);

                        //tao object RecentPeer để add vào arr recentPeersArrayFormWebSocket, để dùng sau này
                        RecentPeer recentPeer;
                        //nếu user có online =1 thi bật setAvailableSocket()
                        if(userOnline != null && userOnline.equals("1")) {
                             recentPeer = new RecentPeer( userUsername, userUsername);
                            recentPeer.setAvailableSocket();
                        }
                        else{
                             recentPeer = new RecentPeer(userUsername, userUsername);

                             //check thêm coi tính trang userOnline có = 2 không nếu có thì để là busy màu đỏ
                            if(userOnline != null && userOnline.equals("2")) {
                                recentPeer.setAvailableSocket();
                                recentPeer.setBusy();
                            }
                        }
                        //trường hơp user không co username va unique name thì lấy deviceid đấp vào
                        //if(Objects.equals(recentPeer.getUniqueName(), "") ||
                                //(Objects.equals(recentPeer.getName(), ""))
                        //){
                           // recentPeer = new RecentPeer(userUsername, userUsername);
                        //}
                        //add vao array
                        boolean bis = recentPeer.isAvailableSocket();
                        System.out.println(bis);
                        arr_recentPeersFormWebSocket.add(recentPeer);

                    }

                    System.out.println(arr_recentPeersFormWebSocket);
                    final PeerListAdapter.Callback callback = new PeerListAdapter.Callback() {
                        @Override
                        public void onFirstItemAdded() {
                            super.onFirstItemAdded();
                            discoveryDescription.setVisibility(View.GONE);
                            noDevices.setVisibility(View.GONE);
                            listViewGui.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLastItemRemoved() {
                            super.onLastItemRemoved();
                            //listViewGui.setVisibility(View.GONE);
                            if (noPermissions.getVisibility() != View.VISIBLE) {
                                discoveryDescription.setVisibility(View.VISIBLE);
                                //noDevices.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onClickNotAllowed(boolean showToast) {
                            super.onClickNotAllowed(showToast);
                            Toast.makeText(voiceTranslationActivity, getResources().getString(R.string.error_cannot_interact_connection), Toast.LENGTH_SHORT).show();
                        }
                    };
                    if(!arr_recentPeersFormWebSocket.isEmpty()) {
                        voiceTranslationActivity.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    listView = new PeerListAdapter(voiceTranslationActivity,
                                            new PairingArray(voiceTranslationActivity, arr_recentPeersFormWebSocket), callback);

                                    listViewGui.setAdapter(listView);
                                }
                                catch (ConcurrentModificationException e){

                                }
                            }

                            ;
                        });
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }
    };

    //khi user đang ở màn hinh paring thi nhận được tính hiệu đòi connect từ user khác
    private Emitter.Listener onReceive_call_CallBack = new Emitter.Listener() {
        @Override
        //hàm websocket server tra ra data về
        public void call(final Object... args) {
            Log.d("CHUNG-", String.format("CHUNG- PairingFragment - > mSocket() -> onReceive_call_CallBack -> %s ", args.toString()));
            String argsReponse =  Arrays.toString(args);
            //covert json data từ server về data native android
            try {
                JSONArray jsonArray = new JSONArray(argsReponse);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    // Accessing data in the JSONObject
                    String room = jsonObject.getString("room");
                    System.out.println(room);
                    String data = jsonObject.getString("data");
                    System.out.println(data);
                    //ta phải save lại ai là người đang call ta
                    JSONObject dataJS = jsonObject.getJSONObject("data");
                    String from = dataJS.getString("from");
                    //save lai người đang call cho mình
                    global.setPeerWantTalkName(from);

                    String to = dataJS.getString("to");
                    //kiểm tra lại "to" có phải là trùng tên của chính mình không, nêu đung thi ok là gọi mình
                    if(to.equals(global.getName())){
                        Boolean offlineCall = dataJS.getBoolean("offlineCall");
                        //nếu đang là cuộc call trong tình trạng OFFLINE
                        if(offlineCall == true){
                            //làm gi đó bật notification báo cho người offline biết có ai đang call mình
                            try {
                                String action = "offlineCall";
                                Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + voiceTranslationActivity.getPackageName() + "/" + R.raw.ringring);
                                MediaPlayer  mediaPlayer = MediaPlayer.create(voiceTranslationActivity, soundUri);
                                mediaPlayer.setVolume(0.1f,0.1f);
                                mediaPlayer.start();

                                RemoteViews contentView = new RemoteViews(voiceTranslationActivity.getPackageName(), R.layout.custom_push);
                                contentView.setImageViewResource(R.id.image, R.mipmap.ic_launcher);
                                contentView.setTextViewText(R.id.title, "Some Called You:");
                                contentView.setTextViewText(R.id.text,   from+ " " +  action + " "+ to + "." );
                                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                        .build();

                                String NOTIFICATION_CHANNEL_ID = "rTranslator_channel";
                                long[] pattern = {0, 1000, 500, 1000};
                                NotificationManager mNotificationManager = (NotificationManager) voiceTranslationActivity.getSystemService(Context.NOTIFICATION_SERVICE);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "rTranslator Notifications",
                                            NotificationManager.IMPORTANCE_HIGH);

                                    notificationChannel.setDescription("");
                                    notificationChannel.setSound(soundUri,audioAttributes);
                                    mNotificationManager.createNotificationChannel(notificationChannel);
                                }
                                // to display notification in DND Mode
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    NotificationChannel channel = mNotificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
                                    channel.canBypassDnd();

                                    channel.setSound(soundUri,audioAttributes);
                                    mNotificationManager.createNotificationChannel(channel);
                                }

                                Intent rTranlateActivity =  new Intent(voiceTranslationActivity.getApplicationContext(), VoiceTranslationActivity.class);
                                rTranlateActivity.putExtra("action", action);
                                rTranlateActivity.putExtra("_to", to);
                                rTranlateActivity.putExtra("_from", from);
                                rTranlateActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                PendingIntent pendingIntent = PendingIntent.getActivity(voiceTranslationActivity, 0, rTranlateActivity, PendingIntent.FLAG_UPDATE_CURRENT );
                                contentView.setOnClickPendingIntent(R.id.okButton, pendingIntent);
                                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(voiceTranslationActivity.getApplicationContext(), NOTIFICATION_CHANNEL_ID);
                                notificationBuilder.setAutoCancel(true)
                                        .setColor(ContextCompat.getColor(voiceTranslationActivity, R.color.primary))
                                        .setContentTitle("rTranslator")
                                        .setWhen(System.currentTimeMillis())
                                        .setSmallIcon(R.drawable.app_icon)
                                        .setContent(contentView)
                                        .setContentIntent(pendingIntent)
                                        .setSound(soundUri)
                                        .setAutoCancel(true);
                                Notification n = notificationBuilder.build();
                                n.sound = soundUri;
                                mNotificationManager.notify(1000, n);

                            }
                            catch (Exception e){
                                throw new RuntimeException(e);
                            }
                            break;
                        }


                        //=====nếu đang là cuộc call trong tình trang ONLINE========//
                        else{
                            //chuyển lên main thread ui để làm việc
                            voiceTranslationActivity.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    //chơi ringring thông báo cho người khác biết đang đươc gọi bởi ai đó
                                    //load ringring wav file
                                    mediaPlayer = MediaPlayer.create(voiceTranslationActivity, R.raw.ringring);
                                    mediaPlayer.setVolume(0.5f,0.5f);
                                    mediaPlayer.setLooping(true); // Enable looping
                                    mediaPlayer.start(); // Start playback
                                    //show dialogbox ok connect or not
                                    connectionRequestDialog = new RequestDialog(voiceTranslationActivity,
                                            room.split(":")[0] + " want connect with you ?", new DialogInterface.OnClickListener() {
                                        @Override
                                        //==> OK USER XAC NHAN BAM NUT OK ACCEPT CONNECT
                                        public void onClick(DialogInterface dialog, int which) {
                                            Log.d("CHUNG-", String.format("CHUNG- PairingFragment() -> connectionRequestDialog -> onlick OK GO"));

                                            //sound ring ring stop
                                            if(mediaPlayer !=null) {
                                                mediaPlayer.stop();
                                                mediaPlayer.release();
                                                mediaPlayer = null;
                                            }
                                            //thông báo socket tôi OK connect "accept_call"
                                            global.SendData_to_mSocket_FOR_ACCEPT_CONNECT2USER( global.getPeerWantTalkName(),global.getName(),true);

                                            //khi qua trang khac thi bỏ ghe event receive_call socket của user khac ban qua
                                            global.mSocket.off("receive_call");
                                            global.mSocket.off("users");

                                            //======QUAN TRONG: chơi ăn gian===> đi thẳng vào luôn DI VAO TRANG CHAT VOICE ===//
                                            voiceTranslationActivity.setFragment(VoiceTranslationActivity.CONVERSATION_FRAGMENT);

                                        }
                                    }, new DialogInterface.OnClickListener() {
                                        @Override
                                        //==> OK USER KHONG CHIU BAM NUT HUY CANCEL-> reject CONNECT
                                        public void onClick(DialogInterface dialog, int which) {
                                            Log.d("CHUNG-", String.format("CHUNG- PairingFragment() -> connectionRequestDialog -> reject"));
                                            if(mediaPlayer !=null) {
                                                mediaPlayer.stop();
                                                mediaPlayer.release();
                                                mediaPlayer = null;
                                            }
                                            //thông báo socket tôi REJECT connect "REJECT accept_call"
                                            global.SendData_to_mSocket_FOR_ACCEPT_CONNECT2USER( global .getPeerWantTalkName(),global.getName(),false);
                                        }
                                    });
                                    connectionRequestDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            connectionRequestDialog = null;
                                        }
                                    });
                                    connectionRequestDialog.show();
                                }
                            });
                            //============
                            break;
                        }
                    }





                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }
    };


    //khi user nhận duoc tín hiệu ok connect accept từ user khac
    private Emitter.Listener onReceive_accept_call_CallBack = new Emitter.Listener() {
        @Override
        //hàm websocket server tra ra data về
        public void call(final Object... args) {

            Log.d("CHUNG-", String.format("CHUNG- PairingFragment - > mSocket() -> onReceive_accept_call_CallBack -> %s ", args.toString()));
            String argsReponse =  Arrays.toString(args);

            if(!global.getApiKeyFileName().equals("")) {

                try {
                    JSONArray jsonArray = new JSONArray(argsReponse);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        // Accessing data in the JSONObject
                        JSONObject data = jsonObject.getJSONObject("data");

                        //lấy json object từ 1 json object cha
                        String OkIAMaccept = data.getString("accept");
                        System.out.println(OkIAMaccept);
                        //khi nhận đươc tin OK từ user phia bên kia thi ta show dialog box
                        if(OkIAMaccept.equals("true")){
                            //phải check coi có phải hàng của mình không, có tên mình trong message boardcast server trả về không
                            String myname= global.getName();
                            String fromname = data.getString("from");
                            String toname = data.getString("to");
                            if( myname.equals(toname) || myname.equals(fromname)) {
                                if (dialogWait != null) {
                                    dialogWait.dismiss();
                                }
                                voiceTranslationActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //sound ring ring stop
                                        if (mediaPlayer != null) {
                                            mediaPlayer.stop();
                                            mediaPlayer.release();
                                            mediaPlayer = null;
                                        }

                                        //khi qua trang khac thi bỏ ghe event receive_call socket của user khac ban qua
                                        global.mSocket.off("receive_call");
                                        global.mSocket.off("users");

                                        //===QUAN TRONG == di vào page chat voice====//
                                        voiceTranslationActivity.setFragment(VoiceTranslationActivity.CONVERSATION_FRAGMENT);
                                    }
                                });
                            }
                        }
                        //nếu nhận đươc từ chối từ phia bên kia thì show dialog box từ chối
                        else{
                            //phải check coi có phải hàng của mình không, có tên mình trong message boardcast server trả về không
                            String myname= global.getName();
                            String fromname = data.getString("from");
                            String toname = data.getString("to");
                            if( myname.equals(toname) || myname.equals(fromname)){
                                if(dialogWait != null) {
                                    dialogWait.dismiss();
                                }
                                voiceTranslationActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //===SHOW DIALOG BOX TU CHOI REJECT NHAN CALL====//
                                        DialogInterface.OnClickListener confirmExitListener = new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        };
                                        //creazione del dialog.
                                        AlertDialog.Builder builder = new AlertDialog.Builder(voiceTranslationActivity);
                                        builder.setCancelable(true);
                                        builder.setMessage("You call have been rejected!");
                                        builder.setPositiveButton(android.R.string.ok, confirmExitListener);


                                        AlertDialog dialog = builder.create();
                                        dialog.show();
                                    }
                                });
                            }

                        }

                    }
                }catch (JSONException e) {
                    throw new RuntimeException(e);
                }

            }
            else{
                Intent intent = new Intent(voiceTranslationActivity, ApiManagementActivity.class);
                startActivity(intent);

            }
        }
    };







    public static final int CONNECTION_TIMEOUT = 5000;
    private RequestDialog connectionRequestDialog;
    private RequestDialog connectionConfirmDialog;
    private ConstraintLayout constraintLayout;
    private Peer confirmConnectionPeer;
    private WalkieTalkieButton walkieTalkieButton;
    private WalkieTalkieButton apiKeyFileButton;
    private WalkieTalkieButton settingButton;
    private WalkieTalkieButton socketModeButton;

    private TextView userNameTextView;
    private ImageView current_user_image;
    private WalkieTalkieButton bluetoothModeButton;

    //true is Socket, false is BlueTooth
    private boolean currentMode_BlueOrSoc = true;

    private ListView listViewGui;
    private Timer connectionTimer;
    @Nullable
    private PeerListAdapter listView;
    private TextView discoveryDescription;
    private TextView noDevices;
    private TextView noPermissions;
    private TextView noBluetoothLe;
    private final Object lock = new Object();

    ///communicatorCallback là 1 abstract class cung i như interface, dùng để class mẹ gọi class con làm giùm gì đó
    private VoiceTranslationActivity.Callback communicatorCallback;
    private RecentPeersDataManager recentPeersDataManager;
    private CustomAnimator animator = new CustomAnimator();
    private Peer connectingPeer;

    private MediaPlayer mediaPlayer;

    AlertDialog dialogWait;

    final Handler handler = new Handler();
    final WeakReference<PairingFragment> activityRef = new WeakReference<>(this); // Replace 'this' with your activity instance

    public PairingFragment() {
        // Required empty public constructor
    }

    @Override
    //cũng giống như bên activity oncreate, chổ này khởi tạo các callback
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("CHUNG-", "CHUNG- PairingFragment() -> onCreate -> khởi tạo callback object communicatorCallback");
        //khởi tạo callback object communicatorCallback
        communicatorCallback = new VoiceTranslationActivity.Callback() {
            @Override
            //bắt đầu search cac nguoi dùng khác
            public void onSearchStarted() {
                Log.e("CHUNG-", "CHUNG- PairingFragment() communicatorCallback-> onSearchStarted call back ");
                buttonSearch.setSearching(true, animator);
            }

            @Override
            //stop search các người dùng khác
            public void onSearchStopped() {
                Log.e("CHUNG-", "CHUNG- PairingFragment() communicatorCallback-> onSearchStopped call back ");
                buttonSearch.setSearching(false, animator);
            }

            @Override
            //khi nhận đươc yêu cầu connect từ người khác
            public void onConnectionRequest(final GuiPeer peer) {
                Log.e("CHUNG-", "CHUNG- PairingFragment() communicatorCallback-> onConnectionRequest call back ");
                super.onConnectionRequest(peer);
                if (peer != null) {
                    String time = DateFormat.getDateTimeInstance().format(new Date());
                    FileLog.appendLog("\nnearby " + time + ": received connection request from:" + peer.getUniqueName());
                    connectionRequestDialog = new RequestDialog(voiceTranslationActivity, getResources().getString(R.string.dialog_confirm_connection_request) + peer.getName() + " ?",  new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            voiceTranslationActivity.acceptConnection(peer);
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            voiceTranslationActivity.rejectConnection(peer);
                        }
                    });
                    connectionRequestDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            connectionRequestDialog = null;
                        }
                    });
                    connectionRequestDialog.show();
                }
            }

            @Override
            //khi connect được với máy khác THANH CONG THI BẮn tính hiệu mở CONVERSATION_FRAGMENT cho activity cha voiceTranslationActivity
            public void onConnectionSuccess(GuiPeer peer) {
                Log.e("CHUNG-", "CHUNG- PairingFragment() communicatorCallback-> onConnectionSuccess call back ");
                super.onConnectionSuccess(peer);
                connectingPeer = null;
                resetConnectionTimer();

                //===QUAN TRONG===//
                voiceTranslationActivity.setFragment(VoiceTranslationActivity.CONVERSATION_FRAGMENT);
            }

            @Override
            //khi connect thất bại
            public void onConnectionFailed(GuiPeer peer, int errorCode) {
                Log.e("CHUNG-", "CHUNG- PairingFragment() communicatorCallback-> onConnectionFailed call back ");
                super.onConnectionFailed(peer, errorCode);
                if (connectingPeer != null) {
                    if (connectionTimer != null && !connectionTimer.isFinished() && errorCode != BluetoothCommunicator.CONNECTION_REJECTED) {
                        // the timer has not expired and the connection has not been refused, so we try again
                        voiceTranslationActivity.connect(peer);
                    } else {
                        // the timer has expired, so the failure is notified
                        clearFoundPeers();
                        //startSearch();
                        activateInputs();
                        disappearLoading(true, null);
                        connectingPeer = null;
                        if (errorCode == BluetoothCommunicator.CONNECTION_REJECTED) {
                            Toast.makeText(voiceTranslationActivity, peer.getName() + getResources().getString(R.string.error_connection_rejected), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(voiceTranslationActivity, getResources().getString(R.string.error_connection), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            //khi tìm thấy ai đó xung quanh
            public void onPeerFound(GuiPeer peer) {
                Log.e("CHUNG-", "CHUNG- PairingFragment() communicatorCallback-> onPeerFound call back ");
                super.onPeerFound(peer);
                synchronized (lock) {
                    if (listView != null) {
                        int recentIndex = listView.indexOfRecentPeer(peer.getUniqueName());
                        if (recentIndex == -1) {
                            BluetoothAdapter bluetoothAdapter = global.getBluetoothCommunicator().getBluetoothAdapter();
                            GuiPeer guiPeer = new GuiPeer(peer, null);
                            int index = listView.indexOfPeer(guiPeer.getUniqueName());
                            if (index == -1) {
                                listView.add(guiPeer);
                            } else {
                                Peer peer1 = (Peer) listView.get(index);
                                if (peer.isBonded(bluetoothAdapter)) {
                                    listView.set(index, guiPeer);
                                } else if (peer1.isBonded(bluetoothAdapter)) {
                                    listView.set(index, listView.get(index));
                                } else {
                                    listView.set(index, guiPeer);
                                }
                            }
                        } else {
                            RecentPeer recentPeer = (RecentPeer) listView.get(recentIndex);
                            recentPeer.setDevice(peer.getDevice());
                            listView.set(recentIndex, recentPeer);
                        }
                    }
                }
            }

            @Override
            //khi tìm thấy ai đó xung quanh update
            public void onPeerUpdated(GuiPeer peer, GuiPeer newPeer) {
                Log.e("CHUNG-", "CHUNG- PairingFragment() communicatorCallback-> onPeerUpdated call back ");
                super.onPeerUpdated(peer, newPeer);
                onPeerFound(newPeer);
            }

            @Override
            //khi mất kết nối với ai đó
            public void onPeerLost(GuiPeer peer) {
                Log.e("CHUNG-", "CHUNG- PairingFragment() communicatorCallback-> onPeerLost call back ");
                synchronized (lock) {
                    if (listView != null) {
                        int recentIndex = listView.indexOfRecentPeer(peer);  // because we don't have the name but only the address of the device
                        if (recentIndex == -1) {
                            listView.remove(new GuiPeer(peer, null));
                        } else {
                            RecentPeer recentPeer = (RecentPeer) listView.get(recentIndex);
                            recentPeer.setDevice(null);
                            listView.set(recentIndex, recentPeer);
                        }

                        if (peer.equals(getConfirmConnectionPeer())) {
                            RequestDialog requestDialog = getConnectionConfirmDialog();
                            if (requestDialog != null) {
                                requestDialog.cancel();
                            }
                        }
                    }
                }
            }

            @Override
            //khi bluetooth không support
            public void onBluetoothLeNotSupported() {
                Log.e("CHUNG-", "CHUNG- PairingFragment() communicatorCallback-> onBluetoothLeNotSupported call back ");
            }

            @Override
            //khi thiếu quyền Search nào đó trên điện thoại
            public void onMissingSearchPermission() {
                Log.e("CHUNG-", "CHUNG- PairingFragment() communicatorCallback-> onMissingSearchPermission call back ");
                super.onMissingSearchPermission();
                clearFoundPeers();
                if (noPermissions.getVisibility() != View.VISIBLE) {
                    // appearance of the written of missing permission
                    //listViewGui.setVisibility(View.GONE);
                    noDevices.setVisibility(View.GONE);
                    discoveryDescription.setVisibility(View.GONE);
                    noPermissions.setVisibility(View.VISIBLE);
                }
            }

            @Override
            //khi có quyền Search trên điện thoại
            public void onSearchPermissionGranted() {
                Log.e("CHUNG-", "CHUNG- PairingFragment() communicatorCallback-> onSearchPermissionGranted call back ");
                super.onSearchPermissionGranted();
                if (noPermissions.getVisibility() == View.VISIBLE) {
                    // disappearance of the written of missing permission
                    noPermissions.setVisibility(View.GONE);
                    //noDevices.setVisibility(View.VISIBLE);
                    discoveryDescription.setVisibility(View.VISIBLE);
                    initializePeerList();
                } else {
                    //reset list view
                    clearFoundPeers();
                }
                //startSearch();
            }
        };




    }



    //======================================================//
    //=================VUNG UI của FRAGMENT================//
    @Override
    //hàm này trong frament chính là lúc nó được khởi tạo vẽ ra trong bộ nhớ, không nên làm gì nhiều ở đây vì chính fragment còn chưa init xong
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("CHUNG-", "CHUNG- PairingFragment() -> onCreateView  -> fragment đang cấp bộ nhớ ");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pairing, container, false);
    }

    @Override
    //hàm này là khi fragment đã hoan thanh tạo ra trong app nên thưc hiện findViewById các UI member trong này
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d("CHUNG-", "CHUNG- PairingFragment() -> onViewCreated ->fragment onViewCreated completed ");
        super.onViewCreated(view, savedInstanceState);
        constraintLayout = view.findViewById(R.id.container);

        userNameTextView = view.findViewById(R.id.userNameText);
        current_user_image = view.findViewById(R.id.current_user_image);

        walkieTalkieButton = view.findViewById(R.id.buttonStart);

        apiKeyFileButton = view.findViewById(R.id.buttonStart2);

        settingButton = view.findViewById(R.id.buttonStart3);

        bluetoothModeButton = view.findViewById(R.id.buttonStart4);

        socketModeButton = view.findViewById(R.id.buttonStart5);

        listViewGui = view.findViewById(R.id.list_view);
        discoveryDescription = view.findViewById(R.id.discoveryDescription);
        noDevices = view.findViewById(R.id.noDevices);
        noPermissions = view.findViewById(R.id.noPermission);
        noBluetoothLe = view.findViewById(R.id.noBluetoothLe);



    }

    @Override
    //chổ này bắt đầu init list view các user gần đây
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("CHUNG-", "CHUNG- PairingFragment() -> onActivityCreated ->fragment onActivityCreated ");
        super.onActivityCreated(savedInstanceState);
        Toolbar toolbar = voiceTranslationActivity.findViewById(R.id.toolbarPairing);
        voiceTranslationActivity.setActionBar(toolbar);
        Context cc = voiceTranslationActivity.getApplication();
        recentPeersDataManager = ((Global) cc).getRecentPeersDataManager();
        // we give the constraint layout the information on the system measures (status bar etc.), which has the fragmentContainer,
        // because they are not passed to it if started with a Transaction and therefore it overlaps the status bar because it fitsSystemWindows does not work
        WindowInsets windowInsets = voiceTranslationActivity.getFragmentContainer().getRootWindowInsets();
        if (windowInsets != null) {
            constraintLayout.dispatchApplyWindowInsets(windowInsets.replaceSystemWindowInsets(windowInsets.getSystemWindowInsetLeft(),windowInsets.getSystemWindowInsetTop(),windowInsets.getSystemWindowInsetRight(),0));
        }

        //setup username od this current user
        userNameTextView.setText(global.getName());





        // setting of listeners
        //tma thời thay chức năng nút WalkieTalkieButton thành bật settting view
        walkieTalkieButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (walkieTalkieButton.getState() == WalkieTalkieButton.STATE_SINGLE) {
                    if(!global.getApiKeyFileName().equals("")) {
                        Log.d("CHUNG-", "CHUNG- PairingFragment() -> walkieTalkieButton ");
                        voiceTranslationActivity.setFragment(VoiceTranslationActivity.WALKIE_TALKIE_FRAGMENT);
                        global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(0, "PairingFragment -> walkieTalkieButton");
                    }
                    //chưa có api key file
                    else{
                        Intent intent = new Intent(voiceTranslationActivity, ApiManagementActivity.class);
                        startActivity(intent);
                    }
                }
            }
        });
        apiKeyFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (apiKeyFileButton.getState() == WalkieTalkieButton.STATE_SINGLE) {
                    Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> setFragment ");
                    //bật biến này lên để ngăn không cho nó kick hoạt onstop của voiceTranslationActivity, nếu không ai đang chat sẽ bị đá ra
                    voiceTranslationActivity.onlyVoiceTranslationActivityAllow = false;
                    //bật ApiManagementActivity để user điền api key
                    Intent intent = new Intent(voiceTranslationActivity, ApiManagementActivity.class);
                    startActivity(intent);
                }
            }
        });
        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (settingButton.getState() == WalkieTalkieButton.STATE_SINGLE) {
                    Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> setFragment ");
                    //bật biến này lên để ngăn không cho nó kick hoạt onstop của voiceTranslationActivity, nếu không ai đang chat sẽ bị đá ra
                    voiceTranslationActivity.onlyVoiceTranslationActivityAllow = false;
                    //tạm thơi dùng nút walki_talkie làm nut bat setting
                    Intent intent = new Intent(voiceTranslationActivity, SettingsActivity.class);
                    startActivity(intent);
                }
            }
        });

        bluetoothModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (settingButton.getState() == WalkieTalkieButton.STATE_SINGLE) {
                    currentMode_BlueOrSoc = false;
                    String tempUserChungPhoneLanguage = voiceTranslationActivity.getResources().getConfiguration().locale.getLanguage();
                    Toast.makeText(voiceTranslationActivity, "BLUETOOTH MODE Current language: "  + tempUserChungPhoneLanguage, Toast.LENGTH_SHORT).show();
                    //clear socket and diaconnect
                    Log.d("CHUNG-", "CHUNG- global.mSocket.disconnect()() ->bluetoothModeButton onClick");
                    //global.mSocket.disconnect();
                    arr_recentPeersFormWebSocket.clear();
                    listView = new PeerListAdapter(voiceTranslationActivity,
                            new PairingArray(voiceTranslationActivity, arr_recentPeersFormWebSocket), null);

                    listViewGui.setAdapter(listView);
                    //======
                    //an nut di sau khi bam
                    socketModeButton.setVisibility(view.VISIBLE);
                    bluetoothModeButton.setVisibility(view.GONE);

                    //==kich hoat bluetooth dò tìm user khac
                    //startSearch();

                }
            }
        });

        //KHI user tap on socket mode button
        socketModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (settingButton.getState() == WalkieTalkieButton.STATE_SINGLE) {
                    currentMode_BlueOrSoc = true;
                    //clear socket and disconnect
                    Log.d("CHUNG-", "CHUNG- global.mSocket.disconnect()() ->socketModeButton onClick");
                    //global.mSocket.disconnect();
                    arr_recentPeersFormWebSocket.clear();
                    listView = new PeerListAdapter(voiceTranslationActivity,
                            new PairingArray(voiceTranslationActivity, arr_recentPeersFormWebSocket), null);

                    listViewGui.setAdapter(listView);
                   //reconnect again
                   // global.mSocket.connect();
                    //bắn data vào websocket thông tin của user
                    String tempUserChungPhone =  global.getName();
                    String tempUserChungPhoneFirstname =  "f_" + global.getName();
                    String tempUserChungPhoneLastname =  "l_" + global.getName();
                    String tempUserChungPhoneLanguage = voiceTranslationActivity.getResources().getConfiguration().locale.getLanguage();

                    Toast.makeText(voiceTranslationActivity, "SOCKET MODE Current language: "  + tempUserChungPhoneLanguage, Toast.LENGTH_SHORT).show();
                    global.SendData_to_mSocketFORLOGIN(tempUserChungPhone, tempUserChungPhoneFirstname, tempUserChungPhoneLastname, tempUserChungPhoneLanguage, global.FMCToken);

                    //an nut di sau khi bam
                    socketModeButton.setVisibility(view.GONE);
                    bluetoothModeButton.setVisibility(view.VISIBLE);
                    noBluetoothLe.setVisibility(View.GONE);
                }
            }
        });

        // setting of array adapter
        initializePeerList();

        //=========tạo click event cho các item trong list view============//
        listViewGui.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("CHUNG-", String.format("CHUNG- PairingFragment() -> listViewGui -> onItemClick %s", i));
                synchronized (lock) {
                    if (listView != null) {
                        // start the pop up and then connect to the peer
                        if (listView.isClickable()) {
                            Listable item = listView.get(i);
                            //kiểm tra nếu item bi click đó thoải điều kiện là 1 PEER thì mới cho vào
                            if (item instanceof Peer) {
                                Peer peer = (Peer) item;
                                //thực hiện connect tới peer user
                                connect(peer);
                            }
                            else{


                            }

                            if (item instanceof RecentPeer) {
                                RecentPeer recentPeer = (RecentPeer) item;
                                boolean isOnlineSocket = recentPeer.isAvailableSocket();
                                //rào thêm nếu user đỏ thì bật false
                                if(recentPeer.isBusy()) {
                                    isOnlineSocket = false;
                                }
                                //nêu bluetook ok isAvailable = true
                                if (recentPeer.isAvailable()) {
                                    connect(recentPeer.getPeer());
                                }
                                //nếu không co bluetooth
                                else{
                                    //cố lấy user name cua peer mà mình muốn ket nối khi click vào
                                    RecentPeer peer = (RecentPeer) item;

                                    String nameOfpeer = peer.getName();

                                    //khi tap vào user nào đó thì save lai tên user đó vào máy local
                                    global.setPeerWantTalkName(nameOfpeer);

                                    Log.d("CHUNG-", String.format("CHUNG- PairingFragment() -> listViewGui -> want to talk %s", nameOfpeer));

                                    if(isOnlineSocket == true) {
                                        //bật ra dialog box hỏi request connect websocket
                                        connectionRequestDialog = new RequestDialog(voiceTranslationActivity, "Do you want to connect to " + peer.getName() + " ?",  new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Log.d("CHUNG-", String.format("CHUNG- PairingFragment() -> connectionRequestDialog -> onlick OK GO"));



                                                //chơi ăn gian===> đi thẳng vào luôn
                                                //check có file APIKEY chưa đã
                                                if(!global.getApiKeyFileName().equals("")) {
                                                   // voiceTranslationActivity.setFragment(VoiceTranslationActivity.CONVERSATION_FRAGMENT);
                                                    //chổ này mình không cho user vào chat ngay lâp tức, mà phải có sự OK từ user phía kia
                                                    //ta phải nghe event socket tra ve là accept hay reject

                                                    //emit call len serversocket
                                                    global.SendData_to_mSocket_FORCONNECT2USER(global.getName(), peer.getName());
                                                    //khi qua trang khac thi bỏ ghe event receive_call socket của chính mình
                                                    //global.mSocket.off("receive_call");

                                                    //show diaglog box wait for your friend accept
                                                    //===SHOW DIALOG BOX WAIT NHAN CALL====//
                                                    DialogInterface.OnClickListener confirmExitListenerWait = new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                        }
                                                    };
                                                    //creazione del dialog.
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(voiceTranslationActivity);
                                                    builder.setCancelable(true);
                                                    builder.setMessage("Please wait for you friend accept!");
                                                    builder.setPositiveButton(android.R.string.ok, confirmExitListenerWait);


                                                     dialogWait = builder.create();
                                                    dialogWait.show();
                                                }
                                               else{
                                                    Intent intent = new Intent(voiceTranslationActivity, ApiManagementActivity.class);
                                                    startActivity(intent);

                                                }
                                            }
                                        }, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Log.d("CHUNG-", String.format("CHUNG- PairingFragment() -> connectionRequestDialog -> CANCEL"));
                                            }
                                        });
                                        connectionRequestDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                connectionRequestDialog = null;
                                            }
                                        });
                                        connectionRequestDialog.show();
                                    }

                                    else {
                                        if(peer.isBusy){
                                            Toast.makeText(voiceTranslationActivity, "user is busy", Toast.LENGTH_SHORT).show();
                                        }
                                        else {
                                            //Toast.makeText(voiceTranslationActivity, "user is not available, so send notification to call", Toast.LENGTH_SHORT).show();
                                            //when user offline mode, we send notification for that user
                                            String me = global.getName();
                                            String friend = global.getPeerWantTalkName();
                                            //global.SendData_to_mSocket_FORCONNECT2USER(me,friend);

                                            //bật dialog hỏi
                                            connectionRequestDialog = new RequestDialog(voiceTranslationActivity, "user is not available, so send notification to call " + peer.getName() + " ?",  new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Log.d("CHUNG-", String.format("CHUNG- PairingFragment() -> connectionRequestDialog -> onlick OK GO"));



                                                    //chơi ăn gian===> đi thẳng vào luôn
                                                    //check có file APIKEY chưa đã
                                                    if(!global.getApiKeyFileName().equals("")) {
                                                        // voiceTranslationActivity.setFragment(VoiceTranslationActivity.CONVERSATION_FRAGMENT);
                                                        //chổ này mình không cho user vào chat ngay lâp tức, mà phải có sự OK từ user phía kia
                                                        //ta phải nghe event socket tra ve là accept hay reject

                                                        //emit call len serversocket
                                                        global.SendData_to_mSocket_FORCONNECT2USER(me,friend);
                                                        //khi qua trang khac thi bỏ ghe event receive_call socket của chính mình
                                                        //global.mSocket.off("receive_call");

                                                        //show diaglog box wait for your friend accept
                                                        //===SHOW DIALOG BOX WAIT NHAN CALL====//
                                                        DialogInterface.OnClickListener confirmExitListenerWait = new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                dialog.dismiss();
                                                            }
                                                        };
                                                        //creazione del dialog.
                                                        AlertDialog.Builder builder = new AlertDialog.Builder(voiceTranslationActivity);
                                                        builder.setCancelable(true);
                                                        builder.setMessage("Please wait for you friend accept!");
                                                        builder.setPositiveButton(android.R.string.ok, confirmExitListenerWait);


                                                        dialogWait = builder.create();
                                                        dialogWait.show();
                                                    }
                                                    else{
                                                        Intent intent = new Intent(voiceTranslationActivity, ApiManagementActivity.class);
                                                        startActivity(intent);

                                                    }
                                                }
                                            }, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Log.d("CHUNG-", String.format("CHUNG- PairingFragment() -> connectionRequestDialog -> CANCEL"));
                                                }
                                            });
                                            connectionRequestDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                @Override
                                                public void onCancel(DialogInterface dialog) {
                                                    connectionRequestDialog = null;
                                                }
                                            });
                                            connectionRequestDialog.show();

                                        }
                                    }
                                }
                            }
                            else{

                            }

                        } else {
                            listView.getCallback().onClickNotAllowed(listView.getShowToast());
                        }
                    }
                }
            }
        });



        ///====KHỞi Tạo SOCKET CONNECTION========//
        Log.d("CHUNG-", "CHUNG- PairingFragment() -> onCreate - > gọi mSocket.connect()");

        //global.mSocket.disconnect();
        //Log.d("CHUNG-", "CHUNG- PairingFragment() ->  global.mSocket.disconnect(); - >init pairring()");

        global.mSocket.off("users");
        global.mSocket.off("receive_call");
        global.mSocket.off("receive_accept_call");
        global.mSocket.on("users", onLoginCallBack);
        global.mSocket.on("receive_call", onReceive_call_CallBack);
        global.mSocket.on("receive_accept_call", onReceive_accept_call_CallBack);
        global.mSocket.connect();

        //bắn data vào websocket thông tin của user
        String tempUserChungPhone =  global.getName();
        String tempUserChungPhoneFirstname =  "f_" + global.getName();
        String tempUserChungPhoneLastname =  "l_" + global.getName();
        String tempUserChungPhoneLanguage = voiceTranslationActivity.getResources().getConfiguration().locale.getLanguage();
        String FMC_token = global.FMCToken;

        //LOGIN NOW! without FMC_token
        Toast.makeText(voiceTranslationActivity, "Current language: "  + tempUserChungPhoneLanguage, Toast.LENGTH_SHORT).show();
        global.SendData_to_mSocketFORLOGIN(tempUserChungPhone, tempUserChungPhoneFirstname, tempUserChungPhoneLastname, tempUserChungPhoneLanguage, FMC_token);
        //an nut di sau khi bam
        socketModeButton.setVisibility(View.GONE);


        //send socket update my status to online
        //delay khoan 3s để cho các fragment khac huy hoan toàn

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PairingFragment thisFragment = activityRef.get();
                if (thisFragment != null && thisFragment.isVisible()) {
                    // Do something after 5s = 5000ms
                    global.SendData_to_mSocket_FOR_UPDATE_STATUS_OF_USER(1, "PairingFragment -> onActivityCreated");
                }
            }
        }, 5000);

    }

    @Override
    //chổ này kich hoạt hàm  startSearch() khi fragment start
    public void onStart() {
        Log.d("CHUNG-", "CHUNG- PairingFragment() -> onStart -> start PairingFragment ");
        super.onStart();
        // release buttons and eliminate any loading
        activateInputs();
        disappearLoading(true, null);
        // if you don't have permission to search, activate from here
        if (!Tools.hasPermissions(voiceTranslationActivity, VoiceTranslationActivity.REQUIRED_PERMISSIONS)) {
            //startSearch();
        }


    }

    @Override
    //chổ này kich hoạt hàm  startSearch() khi fragment resume
    public void onResume() {
        Log.d("CHUNG-", "CHUNG- PairingFragment() -> onResume -> resume PairingFragment ");
        super.onResume();
        //thay hinh user
        File file = new File(this.voiceTranslationActivity.getFilesDir(), "user_image");
        if (file.exists()) {
            // Load the image file into a Bitmap
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

            if (bitmap != null) {
                // Set the Bitmap to the ImageView
                current_user_image.setImageBitmap(bitmap);
                current_user_image.setImageTintList(null);
                current_user_image.setClipToOutline(true);
            } else {
                // Handle case where bitmap is null (failed to decode bitmap)
                //current_user_image.setColorFilter(ContextCompat.getColor(voiceTranslationActivity, R.color.green), PorterDuff.Mode.SRC_IN);
            }
        } else {
            // Handle case where file does not exist
            //current_user_image.setColorFilter(ContextCompat.getColor(voiceTranslationActivity, R.color.green), PorterDuff.Mode.SRC_IN);
        }

        //restore status
        /*if (activity.getConnectingPeersList().size() > 0) {
            deactivateInputs();
            appearLoading(null);
        } else {
            clearFoundPeers();
            disappearLoading(null);
            activateInputs();
        }*/
        clearFoundPeers();

        //đây là lúc communicatorCallback tạo ở lúc oncreate đươc dùng, tạo callback cho activity cha cua fragment
        voiceTranslationActivity.addCallback(communicatorCallback);

        // if you have permission to search it is activated from here
        if (Tools.hasPermissions(voiceTranslationActivity, VoiceTranslationActivity.REQUIRED_PERMISSIONS)) {
            //gọi starSearch cac user xung quanh
            //startSearch();
        }


    }

    @Override
    public void onPause() {
        Log.d("CHUNG-", "CHUNG- PairingFragment() -> onPause -> pause PairingFragment ");
        super.onPause();
        voiceTranslationActivity.removeCallback(communicatorCallback);
        stopSearch();
        //communicatorCallback.onSearchStopped();
        if (connectingPeer != null) {
            voiceTranslationActivity.disconnect(connectingPeer);
            connectingPeer = null;
        }


    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        //khi qua trang khac thi bỏ connect socket củ
       // Log.d("CHUNG-", "CHUNG- PairingFragment() -> onDestroy - > gọi mSocket.disconnect()");
       // mSocket.off("receive_call");
       // mSocket.disconnect();


        //thoat app thi giải phóng bộ nhớ file wav
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void connect(final Peer peer) {
        Log.d("CHUNG-", "CHUNG- PairingFragment() -> connect(final Peer peer) -> connect peer!!! ");
        connectingPeer = peer;
        confirmConnectionPeer = peer;

        //khởi tạo dialog box nhắc nhở user ok thì tiếp
        connectionConfirmDialog = new RequestDialog(voiceTranslationActivity, getResources().getString(R.string.dialog_confirm_connection) + peer.getName() + "?", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deactivateInputs();
                appearLoading(null);
                voiceTranslationActivity.connect(peer);
                startConnectionTimer();
            }
        }, null);

        //add cancel cho dialog box khi user không muốn connect
        connectionConfirmDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                confirmConnectionPeer = null;
                connectionConfirmDialog = null;
            }
        });
        //show dialogbox
        connectionConfirmDialog.show();
    }

    @Override
    protected void startSearch() {
        Log.d("CHUNG-", "CHUNG- PairingFragment() -> startSearch() -> startSearch peer!!! ");

        int result = voiceTranslationActivity.startSearch();

        if (result != BluetoothCommunicator.SUCCESS) {
            if (result == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED && noBluetoothLe.getVisibility() != View.VISIBLE) {
                // appearance of the bluetooth le missing sign
                //listViewGui.setVisibility(View.GONE);
                noDevices.setVisibility(View.GONE);
                discoveryDescription.setVisibility(View.GONE);
                noBluetoothLe.setVisibility(View.VISIBLE);
            } else if (result != VoiceTranslationActivity.NO_PERMISSIONS && result != BluetoothCommunicator.ALREADY_STARTED) {
                Toast.makeText(voiceTranslationActivity, getResources().getString(R.string.error_starting_search), Toast.LENGTH_SHORT).show();
            }
        }


    }

    //=======================================================//
    //=================HELPPER FUNCTIONS================/////
    private void stopSearch() {
        voiceTranslationActivity.stopSearch(connectingPeer == null);
    }

    private void activateInputs() {
        /*animator.createAnimatorColor(walkieTalkieButton.getBackground(), GuiTools.getColor(requireContext(), R.color.gray), GuiTools.getColor(requireContext(), R.color.accent_white), getResources().getInteger(R.integer.durationLong)).start();
        animator.createAnimatorColor(walkieTalkieButtonIcon.getDrawable(), GuiTools.getColor(requireContext(), R.color.white), GuiTools.getColor(requireContext(), R.color.primary), getResources().getInteger(R.integer.durationLong)).start();*/
        walkieTalkieButton.show();
        //click reactivation of listView
        setListViewClickable(true, true);
        walkieTalkieButton.setState(WalkieTalkieButton.STATE_SINGLE);
    }

    private void deactivateInputs() {
        /*animator.createAnimatorColor(walkieTalkieButton.getBackground(), walkieTalkieButton.getBackgroundTintList().getDefaultColor(), GuiTools.getColor(requireContext(), R.color.gray), getResources().getInteger(R.integer.durationLong)).start();
        animator.createAnimatorColor(walkieTalkieButtonIcon.getDrawable(), GuiTools.getColor(requireContext(), R.color.primary), GuiTools.getColor(requireContext(), R.color.white), getResources().getInteger(R.integer.durationLong)).start();*/
        walkieTalkieButton.hide();
        //click deactivation of listView
        setListViewClickable(false, true);
        walkieTalkieButton.setState(WalkieTalkieButton.STATE_CONNECTING);
    }

    public Peer getConfirmConnectionPeer() {
        return confirmConnectionPeer;
    }

    public RequestDialog getConnectionConfirmDialog() {
        return connectionConfirmDialog;
    }

    private void startConnectionTimer() {
        connectionTimer = new Timer(CONNECTION_TIMEOUT);
        connectionTimer.start();
    }

    private void resetConnectionTimer() {
        if (connectionTimer != null) {
            connectionTimer.cancel();
            connectionTimer = null;
        }
    }

    private void initializePeerList() {
        Log.d("CHUNG-", "CHUNG- PairingFragment() -> initializePeerList -> khởi tạo peer list view ");
        final PeerListAdapter.Callback callback = new PeerListAdapter.Callback() {
            @Override
            public void onFirstItemAdded() {
                super.onFirstItemAdded();
                discoveryDescription.setVisibility(View.GONE);
                noDevices.setVisibility(View.GONE);
                listViewGui.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLastItemRemoved() {
                super.onLastItemRemoved();
               // listViewGui.setVisibility(View.GONE);
                if (noPermissions.getVisibility() != View.VISIBLE) {
                    discoveryDescription.setVisibility(View.VISIBLE);
                    //noDevices.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onClickNotAllowed(boolean showToast) {
                super.onClickNotAllowed(showToast);
                Toast.makeText(voiceTranslationActivity, getResources().getString(R.string.error_cannot_interact_connection), Toast.LENGTH_SHORT).show();
            }
        };

       RecentPeersDataManager.RecentPeersListener ll = new RecentPeersDataManager.RecentPeersListener(){
           @Override
           public void onRecentPeersObtained(ArrayList<RecentPeer> recentPeers) {
               if (recentPeers.size() > 0) {
                   Log.d("CHUNG-", "CHUNG- PairingFragment() -> RecentPeersListener -> onRecentPeersObtained recentPeers.size() > 0");
                   //recentPeers.add(1, new RecentPeer("IDOFCHUNGTEST_1123", "CHUNGTEST1123"));
                   //recentPeers.add(2, new RecentPeer("IDOFCHUNGTEST_3423", "NguyenThinh"));
                   //recentPeers.add(3, new RecentPeer("IDOFCHUNGTEST_1563", "ThiTHi"));
                   //recentPeers.add(4, new RecentPeer("IDOFCHUNGTEST_9562", "TruongSon"));
                   //recentPeers.add(arr_recentPeersFormWebSocket.get(0));
                   //recentPeers.add(arr_recentPeersFormWebSocket.get(1));
                   listView = new PeerListAdapter(voiceTranslationActivity, new PairingArray(voiceTranslationActivity,recentPeers), callback);
               } else {

                   Log.d("CHUNG-", "CHUNG- PairingFragment() -> RecentPeersListener -> onRecentPeersObtained recentPeers.size() <= 0");
                   listView = new PeerListAdapter(voiceTranslationActivity, new PairingArray(voiceTranslationActivity), callback);
               }
               //listViewGui.setAdapter(listView);
           }
        };

        recentPeersDataManager.getRecentPeers(ll);
    }

    public void clearFoundPeers() {
        if (listView != null) {
            listView.clear();
        }
    }

    public void setListViewClickable(boolean isClickable, boolean showToast) {
        if (listView != null) {
            listView.setClickable(isClickable, showToast);
        }
    }



}