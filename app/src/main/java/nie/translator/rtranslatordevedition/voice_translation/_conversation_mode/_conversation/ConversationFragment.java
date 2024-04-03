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

package nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import nie.translator.rtranslatordevedition.R;
import nie.translator.rtranslatordevedition.tools.gui.CustomFragmentPagerAdapter;
import nie.translator.rtranslatordevedition.tools.gui.animations.CustomAnimator;
import nie.translator.rtranslatordevedition.tools.gui.peers.PeerListAdapter;
import nie.translator.rtranslatordevedition.tools.gui.peers.array.PairingArray;
import nie.translator.rtranslatordevedition.voice_translation.VoiceTranslationActivity;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.PairingToolbarFragment;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation.connection_info.PeersInfoFragment;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation.main.ConversationMainFragment;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.recent_peer.RecentPeer;

///==Cổng thứ 3====///
//đây là ConversationFragment chổ người dùng connect và nói chuyên
public class ConversationFragment extends PairingToolbarFragment {
/*
    //=================khi nhận được login từ server trả về================//
    private List<RecentPeer> arr_recentPeersFormWebSocket = new ArrayList<RecentPeer>();
    private Emitter.Listener onReceive_loginCallBack = new Emitter.Listener() {
        @Override
        //hàm websocket server tra ra data về
        public void call(final Object... args) {
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
                        boolean userSkip = userObject.getBoolean("skip");

                        double userCreatedTime = userObject.getDouble("__createdtime__");
                        double userUpdatedtime = userObject.getDouble("__updatedtime__");
                        int userActive = userObject.getInt("active");

                        Log.d("CHUNG-", "CHUNG- mSocket() -> onLoginCallBack server reply DATA->  "+
                                userUsername + " " + userFirstname + " " + userLastname + " " + userPersonal_language + " " + userSkip + " " + userCreatedTime + " " + userUpdatedtime + " " + userActive);

                        //tao object RecentPeer để add vào arr recentPeersArrayFormWebSocket, để dùng sau này
                        RecentPeer recentPeer = new RecentPeer(userUsername,userLastname + userFirstname);
                        //add vao array
                        arr_recentPeersFormWebSocket.add(recentPeer);


                    }

                    System.out.println(arr_recentPeersFormWebSocket);
                    //OK LOGIN XONG GOI TIEP cai khac event "CALL" connect to user
                    SendData_to_mSocket_FORCONNECT2USER("Usertest1", "johnpham");


                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }
    };

    //=========== khi nhận được receive_call từ server trả về====///
    private Emitter.Listener onReceive_receive_callCallBack = new Emitter.Listener() {
        @Override
        //hàm websocket server tra ra data về
        public void call(final Object... args) {
            String argsReponse =  Arrays.toString(args);
            try {
                JSONArray jsonArray = new JSONArray(argsReponse);
                for (int i = 0; i < jsonArray.length(); i++){
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    // Accessing data in the JSONObject
                    String room = jsonObject.getString("room");
                    //emit vao room : join_room: {room: "123123123"}
                    //SendData_to_mSocket_FOR_JOINROOM(room);
                    //khi có room -> save lại
                    // khi vào man hình nói chuyên -> emit tiếp "send_message" : { message, username, to, createdtime }
                    SendData_to_mSocket_FOR_SENDMESSAGE("HELLO HELLO HELLO", "Usertest1", room, "10:10:10");

                    // lắng nghe : "receive_message"
                    String data = jsonObject.getString("data");

                    //

                }

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }
    };

    //======khi nhận được receive_message từ server gọi về
    private Emitter.Listener onReceive_receive_messageCallBack = new Emitter.Listener(){
        @Override
        //hàm websocket server tra ra data về
        public void call(final Object... args){
            String argsReponse =  Arrays.toString(args);
            try {
                JSONArray jsonArray = new JSONArray(argsReponse);
                //anh tự dich nhé.

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    };



    private Socket mSocket;
    {
        try {
            String urlS = "http://192.168.1.52:4000";

            mSocket = IO.socket(urlS);
            Log.d("CHUNG-", "CHUNG- PairingFragment()  -> mSocket() ConversationFragment -> DA TAO SUCCESSES!!"+ mSocket);



        } catch (URISyntaxException e) {
            Log.d("CHUNG-", "CHUNG- PairingFragment()  -> mSocket() ConversationFragment -> FAIL ->  "+ e.getMessage());

        }
    }

    //{"from": "johnpham", "to": "johnpham11"}
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

    public void SendData_to_mSocket_FORLOGIN(String usernamedata, String firstnamedata, String lastnamedata , String personal_languagedata) {

        String jsonString = String.format("{\"username\": \"%s\", \"firstname\": \"%s\", \"lastname\": \"%s\", \"personal_language\": \"%s\"}",usernamedata, firstnamedata, lastnamedata, personal_languagedata);
        //covert string to json
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            mSocket.emit("login", jsonObject);
            Log.d("CHUNG-", "CHUNG- ConversationFragment() -> mSocket.emit(\"login\", jsonObject);");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    //bắn vào socket thông tin event là send_message json là { message, username, to, createdtime }
    public void SendData_to_mSocket_FOR_SENDMESSAGE(String message, String username, String toRoom , String createdtime) {

        String jsonString = String.format("{\"message\": \"%s\", \"username\": \"%s\", \"to\": \"%s\", \"createdtime\": \"%s\"}",message, username, toRoom, createdtime);
        //covert string to json
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            mSocket.emit("send_message", jsonObject);
            Log.d("CHUNG-", "CHUNG- ConversationFragment() -> mSocket.emit(\"send_message\", jsonObject);");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void SendData_to_mSocket_FOR_JOINROOM(String roomName) {

        String jsonString = String.format("{\"room\": \"%s\"}",roomName);
        //covert string to json
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            mSocket.emit("join_room", jsonObject);
            Log.d("CHUNG-", "CHUNG- ConversationFragment() -> mSocket.emit(\"join_room\", jsonObject);");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
*/
//=============================================================================////


    private ConstraintLayout constraintLayout;
    private ImageButton exitButton;
    private TabLayout tabLayout;
    private ViewPager pager;
    private CustomFragmentPagerAdapter pagerAdapter;
    private VoiceTranslationActivity.Callback communicatorCallback;
    private CustomAnimator animator = new CustomAnimator();
    private int pagerPosition = 0;

    public ConversationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        Log.d("CHUNG-", "CHUNG- ConversationFragment() -> onCreate ");
        communicatorCallback = new VoiceTranslationActivity.Callback() {
            @Override
            public void onSearchStarted() {
                if (!isLoadingVisible && !isLoadingAnimating) {
                    buttonSearch.setSearching(true, animator);
                }
            }

            @Override
            public void onSearchStopped() {
                if (!isLoadingVisible && !isLoadingAnimating) {
                    buttonSearch.setSearching(false, animator);
                }
            }
        };



        /*
        ///====KHỞi Tạo SOCKET CONNECTION========//
        ///STEP 3:
        // nhận về Event receive_call để nhận

        Log.d("CHUNG-", "CHUNG- PairingFragment() -> onCreate - > gọi mSocket.connect()");
        mSocket.on("login", onReceive_loginCallBack);
        mSocket.on("receive_call", onReceive_receive_callCallBack);
        mSocket.on("receive_message", onReceive_receive_messageCallBack);
        mSocket.connect();

        //bắn login trước tiên sau đó luồng login sẽ auto bắn tiếp các request khác vao socket
        String tempUserChungPhone = "Usertest1";
        String tempUserChungPhoneFirstname = "tester1Firstname";
        String tempUserChungPhoneLastname = "tester1Lastname";
        String tempUserChungPhoneLanguage = "vi";
        SendData_to_mSocket_FORLOGIN(tempUserChungPhone, tempUserChungPhoneFirstname, tempUserChungPhoneLastname, tempUserChungPhoneLanguage);
*/



    }// on create




    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_conversation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        constraintLayout = view.findViewById(R.id.container);
        exitButton = view.findViewById(R.id.exitButton);
        tabLayout = view.findViewById(R.id.tabsLayout);
        pager = view.findViewById(R.id.tabs);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Toolbar toolbar = voiceTranslationActivity.findViewById(R.id.toolbarConversation);
        voiceTranslationActivity.setActionBar(toolbar);
        // we give the constraint layout the information on the system measures (status bar etc.), which the fragmentContainer has, because they are not passed
        // to it if started with a Transaction and therefore it overlaps the status bar because fitsSystemWindows does not work
        WindowInsets windowInsets = voiceTranslationActivity.getFragmentContainer().getRootWindowInsets();
        if (windowInsets != null) {
            constraintLayout.dispatchApplyWindowInsets(windowInsets.replaceSystemWindowInsets(windowInsets.getSystemWindowInsetLeft(),windowInsets.getSystemWindowInsetTop(),windowInsets.getSystemWindowInsetRight(),0));
        }

        // insertion of the list of titles
        List<String> titles = new ArrayList<>();
        titles.add(getResources().getString(R.string.conversation));
        titles.add(getResources().getString(R.string.connection));

        //có 2 page trong adapter là "conversation", và "connection"
        //page chính là ConversationMainFragment xử lý âm thanh, nó được bỏ vào adapter để chuyển qua lại
        pagerAdapter = new CustomFragmentPagerAdapter(voiceTranslationActivity,
                getChildFragmentManager(), titles,
                new Fragment[]{new ConversationMainFragment()});
                //new Fragment[]{new ConversationMainFragment(), new PeersInfoFragment()});

        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.d("CHUNG-", "CHUNG- ConversationFragment() -> onPageScrolled()");
                if (position == 0 && positionOffset == 0 && pagerPosition == 1) {
                    pagerPosition = 0;
                    global.getBluetoothCommunicator().removeCallback(communicatorCallback);
                    ((PeersInfoFragment) pagerAdapter.getFragment(1)).onDeselected();
                    buttonSearch.setVisible(false,null);
                } else if (position == 1 && positionOffset == 0 && pagerPosition == 0) {
                    pagerPosition = 1;
                    global.getBluetoothCommunicator().addCallback(communicatorCallback);
                    ((PeersInfoFragment) pagerAdapter.getFragment(1)).onSelected();
                    if(!isLoadingVisible) {
                        buttonSearch.setVisible(true, null);
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {
                Log.d("CHUNG-", "CHUNG- ConversationFragment() -> onPageSelected()");
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.d("CHUNG-", "CHUNG- ConversationFragment() -> onPageScrollStateChanged()");
            }
        });

        tabLayout.setupWithViewPager(pager);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voiceTranslationActivity.onBackPressed();
            }
        });
    }

    public Fragment getCurrentFragment() {
        int position = pager.getCurrentItem();
        return pagerAdapter.getFragment(position);
    }

    @Override
    public void clearFoundPeers() {
        PeersInfoFragment peersInfoFragment = (PeersInfoFragment) pagerAdapter.getFragment(1);
        if (peersInfoFragment != null) {
            peersInfoFragment.clearFoundPeers();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pagerPosition == 1) {
            global.getBluetoothCommunicator().addCallback(communicatorCallback);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (pagerPosition == 1) {
            global.getBluetoothCommunicator().removeCallback(communicatorCallback);
        }
    }

    @Override
    protected void startSearch() {
        ((PeersInfoFragment) pagerAdapter.getFragment(1)).startSearch();
    }
}
