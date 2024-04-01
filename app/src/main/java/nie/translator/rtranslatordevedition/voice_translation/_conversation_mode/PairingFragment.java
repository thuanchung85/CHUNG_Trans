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
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import nie.translator.rtranslatordevedition.Global;
import nie.translator.rtranslatordevedition.R;
import nie.translator.rtranslatordevedition.settings.LanguagePreference;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
            Log.d("CHUNG-", String.format("CHUNG- PairingFragment - > mSocket() -> server reply -> %s ", args.toString()));
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
                        Log.d("CHUNG-", String.format("CHUNG- mSocket() -> server reply CO VAN DE-> %b ", success));
                        return;
                    }
                    String username = jsonObject.getString("username");
                    System.out.println(username);
                    long createdTime = jsonObject.getLong("__createdtime__");
                    System.out.println(createdTime);

                    JSONArray usersArray = new JSONArray(jsonObject.getString("users"));

                    //duyet loop qua các user trong usersArray
                    for (int each =0 ; each < usersArray.length(); each++) {

                        JSONObject userObject = usersArray.getJSONObject(each); // Assuming there's only one user
                        String userUsername = userObject.getString("username");
                        String userFirstname = userObject.getString("firstname");
                        String userLastname = userObject.getString("lastname");
                        String userPersonal_language = userObject.getString("personal_language");
                        String userOnline = userObject.getString("online");
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
                        }
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
                    voiceTranslationActivity.runOnUiThread(new Runnable() {

                                                               @Override
                                                               public void run() {
                                                                   listView = new PeerListAdapter(voiceTranslationActivity,
                                                                           new PairingArray(voiceTranslationActivity, arr_recentPeersFormWebSocket), callback);

                                                                   listViewGui.setAdapter(listView);

                                                               };
                                                           });

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

                    //chuyen lên main thread ui
                    voiceTranslationActivity.runOnUiThread(new Runnable() {

                                               @Override
                                               public void run() {
                                                   //show dialogbox ok connect or not
                                                   connectionRequestDialog = new RequestDialog(voiceTranslationActivity,
                                                           "SOME ONE ASK YOU JOIN ROOM: " + room + " ?",
                                                           15000, new DialogInterface.OnClickListener() {
                                                       @Override
                                                       public void onClick(DialogInterface dialog, int which) {
                                                           Log.d("CHUNG-", String.format("CHUNG- PairingFragment() -> connectionRequestDialog -> onlick OK GO"));

                                                           //chơi ăn gian===> đi thẳng vào luôn
                                                           voiceTranslationActivity.setFragment(VoiceTranslationActivity.CONVERSATION_FRAGMENT);
                                                       }
                                                   }, new DialogInterface.OnClickListener() {
                                                       @Override
                                                       public void onClick(DialogInterface dialog, int which) {
                                                           Log.d("CHUNG-", String.format("CHUNG- PairingFragment() -> connectionRequestDialog -> reject"));
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
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }
    };


    //khởi tạo websocket object
    private Socket mSocket;
    {
        try {
            String urlS = "http://27.74.249.34:8017";

            mSocket = IO.socket(urlS);
            Log.d("CHUNG-", "CHUNG- PairingFragment()  -> mSocket() PairingFragment -> DA TAO SUCCESSES!!"+ mSocket);



        } catch (URISyntaxException e) {
            Log.d("CHUNG-", "CHUNG- PairingFragment()  -> mSocket() PairingFragment -> FAIL ->  "+ e.getMessage());

        }
    }

    public void SendData_to_mSocketFORLOGIN(String usernamedata, String firstnamedata, String lastnamedata , String personal_languagedata) {

        String jsonString = String.format("{\"username\": \"%s\", \"firstname\": \"%s\", \"lastname\": \"%s\", \"personal_language\": \"%s\"}",usernamedata, firstnamedata, lastnamedata, personal_languagedata);
        //covert string to json
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            mSocket.emit("login", jsonObject);
            Log.d("CHUNG-", "CHUNG- PairingFragment() -> mSocket.emit(\"login\", jsonObject);");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void SendData_to_mSocket_FORCONNECT2USER(String fromUser, String toUser) {

        String jsonString = String.format("{\"from\": \"%s\", \"to\": \"%s\"}",fromUser, toUser);
        //covert string to json
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            mSocket.emit("call", jsonObject);
            Log.d("CHUNG-", "CHUNG- ConversationFragment() -> mSocket.emit(\"call\", jsonObject);");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public static final int CONNECTION_TIMEOUT = 5000;
    private RequestDialog connectionRequestDialog;
    private RequestDialog connectionConfirmDialog;
    private ConstraintLayout constraintLayout;
    private Peer confirmConnectionPeer;
    private WalkieTalkieButton walkieTalkieButton;
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
                    connectionRequestDialog = new RequestDialog(voiceTranslationActivity, getResources().getString(R.string.dialog_confirm_connection_request) + peer.getName() + " ?", 15000, new DialogInterface.OnClickListener() {
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
                        startSearch();
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
                startSearch();
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
        walkieTalkieButton = view.findViewById(R.id.buttonStart);
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

        // setting of listeners
        walkieTalkieButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (walkieTalkieButton.getState() == WalkieTalkieButton.STATE_SINGLE) {
                    Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> setFragment ");
                    voiceTranslationActivity.setFragment(VoiceTranslationActivity.WALKIE_TALKIE_FRAGMENT);
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
                                Toast.makeText(voiceTranslationActivity, "không thoải điều kiện là 1 PEER", Toast.LENGTH_SHORT).show();

                            }

                            if (item instanceof RecentPeer) {
                                RecentPeer recentPeer = (RecentPeer) item;
                                boolean isOnlineSocket = recentPeer.isAvailableSocket();
                                //nêu bluetook ok isAvailable = true
                                if (recentPeer.isAvailable()) {
                                    connect(recentPeer.getPeer());
                                }
                                //nếu không co bluetooth
                                else{
                                    //cố lấy user name cua peer mà mình muốn ket nối khi click vào
                                    RecentPeer peer = (RecentPeer) item;

                                    String nameOfpeer = peer.getName();
                                    global.setPeerWantTalkName(nameOfpeer);
                                    Log.d("CHUNG-", String.format("CHUNG- PairingFragment() -> listViewGui -> want to talk %s", nameOfpeer));

                                    if(isOnlineSocket == true) {
                                        //bật ra dialog box hỏi request connect websocket
                                        connectionRequestDialog = new RequestDialog(voiceTranslationActivity,
                                                "Do You want to connect" + peer.getName() + " ?",
                                                15000, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Log.d("CHUNG-", String.format("CHUNG- PairingFragment() -> connectionRequestDialog -> onlick OK GO"));

                                                SendData_to_mSocket_FORCONNECT2USER(global.getName(), peer.getName());

                                                //chơi ăn gian===> đi thẳng vào luôn
                                                voiceTranslationActivity.setFragment(VoiceTranslationActivity.CONVERSATION_FRAGMENT);
                                            }
                                        }, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Log.d("CHUNG-", String.format("CHUNG- PairingFragment() -> connectionRequestDialog -> reject"));
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


                                    //chơi ăn gian===> đi thẳng vào luôn
                                    //voiceTranslationActivity.setFragment(VoiceTranslationActivity.CONVERSATION_FRAGMENT);

                                    Toast.makeText(voiceTranslationActivity, "không thoải điều kiện là 1 RECENTPEER có recentPeer.isAvailable() = true", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else{
                                Toast.makeText(voiceTranslationActivity, "không thoải điều kiện là 1 RECENTPEER", Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            listView.getCallback().onClickNotAllowed(listView.getShowToast());
                        }
                    }
                }
            }
        });

        //===thữ lấy user name của user đang cai app=====//
        //String usernameCurrent = global.getName();

        //====TEST THU LIST VIEW CO HOLoAT DONG KHONG====//
        /*
        //tao object RecentPeer để add vào arr recentPeersArrayFormWebSocket, để dùng sau này
        RecentPeer recentPeer = new RecentPeer("TESTLISTVIEW",usernameCurrent);
        //add vao array
        arr_recentPeersFormWebSocket.add(recentPeer);
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
        listView = new PeerListAdapter(voiceTranslationActivity, new PairingArray(voiceTranslationActivity,
                arr_recentPeersFormWebSocket), callback);
        listViewGui.setAdapter(listView);*/

        ///====KHỞi Tạo SOCKET CONNECTION========//
        Log.d("CHUNG-", "CHUNG- PairingFragment() -> onCreate - > gọi mSocket.connect()");
        mSocket.on("users", onLoginCallBack);
        mSocket.on("receive_call", onReceive_call_CallBack);
        mSocket.connect();

        //bắn data vào websocket thông tin của user
        String tempUserChungPhone =  global.getName();
        String tempUserChungPhoneFirstname =  "f_" + global.getName();
        String tempUserChungPhoneLastname =  "l_" + global.getName();
        String tempUserChungPhoneLanguage = voiceTranslationActivity.getResources().getConfiguration().locale.getLanguage();
        String lang = global.getCurrentLanguageinPhone();

        if(lang != "") {
            if (lang.equals("Tiếng Hàn (Hàn Quốc)") || lang.equals("Korean (South Korea)") || lang.equals("한국어 (대한민국)") ) {
                tempUserChungPhoneLanguage = "ko";
            } else {
                if (lang.equals("Tiếng Việt (Việt Nam)") || lang.equals("Vietnamese (Vietnam)") || lang.equals("베트남어 (베트남)") ) {
                    tempUserChungPhoneLanguage = "vi";
                } else {
                    tempUserChungPhoneLanguage = "en";
                }
            }
        }
        else{
            tempUserChungPhoneLanguage = voiceTranslationActivity.getResources().getConfiguration().locale.getLanguage();
        }
        Toast.makeText(voiceTranslationActivity, lang + "->" + tempUserChungPhoneLanguage, Toast.LENGTH_SHORT).show();
        SendData_to_mSocketFORLOGIN(tempUserChungPhone, tempUserChungPhoneFirstname, tempUserChungPhoneLastname, tempUserChungPhoneLanguage);
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
            startSearch();
        }
    }

    @Override
    //chổ này kich hoạt hàm  startSearch() khi fragment resume
    public void onResume() {
        Log.d("CHUNG-", "CHUNG- PairingFragment() -> onResume -> resume PairingFragment ");
        super.onResume();
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
            startSearch();
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