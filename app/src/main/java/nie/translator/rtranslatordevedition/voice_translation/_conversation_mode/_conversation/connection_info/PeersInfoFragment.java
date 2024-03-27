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

package nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation.connection_info;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import nie.translator.rtranslatordevedition.Global;
import nie.translator.rtranslatordevedition.R;
import nie.translator.rtranslatordevedition.tools.FileLog;
import nie.translator.rtranslatordevedition.tools.Tools;
import nie.translator.rtranslatordevedition.tools.gui.RequestDialog;
import nie.translator.rtranslatordevedition.tools.gui.peers.GuiPeer;
import nie.translator.rtranslatordevedition.tools.gui.peers.Listable;
import nie.translator.rtranslatordevedition.tools.gui.peers.PeerListAdapter;
import nie.translator.rtranslatordevedition.tools.gui.peers.array.InfoArray;
import nie.translator.rtranslatordevedition.voice_translation.VoiceTranslationActivity;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.PairingFragment;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation.ConversationFragment;
import com.bluetooth.communicator.BluetoothCommunicator;
import com.bluetooth.communicator.Peer;
import com.bluetooth.communicator.tools.Timer;


//class này là lo xử lý về số người đang connect, nó hay dùng với class CONVERSATION_FRAGMENT
public class PeersInfoFragment extends Fragment {
    private RequestDialog connectionRequestDialog;
    private RequestDialog connectionConfirmDialog;
    private Peer confirmConnectionPeer;
    private ListView listViewGui;
    private boolean selected = false;
    @Nullable
    private PeerListAdapter listView;
    private TextView discoveryDescription;
    private TextView noPermissions;
    private TextView noBluetoothLe;
    private VoiceTranslationActivity voiceTranslationActivity;
    private Global global;
    private final Object lock = new Object();
    private VoiceTranslationActivity.Callback communicatorCallback;
    private Timer connectionTimer;
    private Peer connectingPeer;
    private ArrayList<Peer> disconnectingPeers = new ArrayList<>();

    public PeersInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        communicatorCallback = new VoiceTranslationActivity.Callback() {
            @Override
            public void onConnectionRequest(final GuiPeer peer) {
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
            public void onConnectionSuccess(GuiPeer peer) {
                super.onConnectionSuccess(peer);
                synchronized (lock) {
                    if (listView != null) {
                        int index = listView.indexOfPeer(peer.getUniqueName());
                        if (index != -1) {
                            listView.set(index, peer);
                        } else {
                            listView.add(peer);
                        }
                    }
                    connectingPeer = null;
                    resetConnectionTimer();
                    if (selected) {
                        startSearch();
                    }
                    disappearLoading();
                    activateInputs();
                }
            }

            @Override
            public void onConnectionFailed(GuiPeer peer, int errorCode) {
                super.onConnectionFailed(peer, errorCode);
                if (connectingPeer != null) {
                    if (connectionTimer != null && !connectionTimer.isFinished() && errorCode != BluetoothCommunicator.CONNECTION_REJECTED) {
                        // the timer has not expired and the connection has not been refused, so we try again
                        voiceTranslationActivity.connect(peer);
                    } else {
                        // the timer has expired, so the failure is reported
                        clearFoundPeers();
                        if (selected) {
                            startSearch();
                        }
                        disappearLoading();
                        activateInputs();
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
            public void onPeerFound(GuiPeer peer) {
                super.onPeerFound(peer);
                synchronized (lock) {
                    if (listView != null) {
                        BluetoothAdapter bluetoothAdapter = global.getBluetoothCommunicator().getBluetoothAdapter();
                        int index = listView.indexOfPeer(peer.getUniqueName());
                        if (index == -1) {
                            listView.add(peer);
                        } else {
                            Peer peer1 = (Peer) listView.get(index);
                            if (!peer1.isConnected() && !peer1.isReconnecting()) {
                                if (peer.isBonded(bluetoothAdapter)) {
                                    listView.set(index, peer);
                                } else if (peer1.isBonded(bluetoothAdapter)) {
                                    listView.set(index, listView.get(index));
                                } else {
                                    listView.set(index, peer);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onPeerLost(GuiPeer peer) {
                synchronized (lock) {
                    if (listView != null) {
                        int index = listView.indexOf(peer);
                        if (index != -1 && !((Peer) listView.get(index)).isConnected()) {
                            listView.remove(peer);
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
            public void onConnectionLost(GuiPeer peer) {
                super.onConnectionLost(peer);
                synchronized (lock) {
                    if (listView != null) {
                        int index = listView.indexOf(peer);
                        if (index != -1) {
                            listView.set(index, peer);
                        }
                    }
                }
            }

            @Override
            public void onConnectionResumed(GuiPeer peer) {
                super.onConnectionResumed(peer);
                synchronized (lock) {
                    if (listView != null) {
                        int index = listView.indexOf(peer);
                        if (index != -1) {
                            listView.set(index, peer);
                        }
                    }
                }
            }

            @Override
            public void onPeerUpdated(GuiPeer peer, GuiPeer newPeer) {
                synchronized (lock) {
                    if (listView != null) {
                        int index = listView.indexOf(peer);
                        if (index != -1) {
                            listView.set(index, newPeer);
                        }
                    }
                }
            }

            @Override
            public void onDisconnecting(GuiPeer peer) {
                super.onDisconnecting(peer);
                synchronized (lock) {
                    if (disconnectingPeers.size() == 0) {
                        deactivateInputs();
                        appearLoading();
                    }
                    disconnectingPeers.add(peer);
                }
            }

            @Override
            public void onDisconnected(GuiPeer peer, int peersLeft) {
                super.onDisconnected(peer, peersLeft);
                synchronized (lock) {
                    if (listView != null && peer != null) {
                        listView.remove(peer);
                    }
                    disconnectingPeers.remove(peer);
                    if (disconnectingPeers.size() == 0 && peersLeft > 0) {
                        disappearLoading();
                        activateInputs();
                    }
                    if (peersLeft == 0) {
                        Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> setFragment ");
                        voiceTranslationActivity.setFragment(VoiceTranslationActivity.DEFAULT_FRAGMENT);
                    }
                }
            }

            @Override
            public void onMissingSearchPermission() {
                super.onMissingSearchPermission();
                clearFoundPeers();
                if (noPermissions.getVisibility() != View.VISIBLE) {
                    // appearance of the written "permission is missing"
                    discoveryDescription.setVisibility(View.GONE);
                    noPermissions.setVisibility(View.VISIBLE);
                    // update listView link
                    ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) listViewGui.getLayoutParams();
                    layoutParams.bottomToTop = R.id.noPermission;
                    listViewGui.setLayoutParams(layoutParams);
                }

            }

            @Override
            public void onSearchPermissionGranted() {
                super.onSearchPermissionGranted();
                if (noPermissions.getVisibility() == View.VISIBLE) {
                    // disappearance of the written "permission is missing"
                    noPermissions.setVisibility(View.GONE);
                    discoveryDescription.setVisibility(View.VISIBLE);
                    // update listView link
                    ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) listViewGui.getLayoutParams();
                    layoutParams.bottomToTop = R.id.discoveryDescription;
                    listViewGui.setLayoutParams(layoutParams);

                    initializePeerList();
                } else {
                    //reset list view
                    clearFoundPeers();
                }
                if (selected) {
                    startSearch();
                }
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_peers_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listViewGui = view.findViewById(R.id.connection_list);
        discoveryDescription = view.findViewById(R.id.discoveryDescription);
        noPermissions = view.findViewById(R.id.noPermission);
        noBluetoothLe = view.findViewById(R.id.noBluetoothLe);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        voiceTranslationActivity = (VoiceTranslationActivity) requireActivity();
        global = (Global) voiceTranslationActivity.getApplication();
    }

    @Override
    public void onResume() {
        super.onResume();
        initializePeerList();
        voiceTranslationActivity.addCallback(communicatorCallback);
        // if you have the permission for the search it is activated from here, otherwise the permission will be requested at the time of the click or selection
        if (selected && Tools.hasPermissions(voiceTranslationActivity, VoiceTranslationActivity.REQUIRED_PERMISSIONS)) {
            startSearch();
        }
    }

    public void onSelected() {
        selected = true;
        startSearch();
    }

    public void onDeselected() {
        selected = false;
        stopSearch();
    }

    @Override
    public void onPause() {
        super.onPause();
        voiceTranslationActivity.removeCallback(communicatorCallback);
        if (selected) {
            stopSearch();
            communicatorCallback.onSearchStopped();
            if (connectingPeer != null) {
                voiceTranslationActivity.disconnect(connectingPeer);
                connectingPeer = null;
            }
        }
    }

    public void startSearch() {
        int result = voiceTranslationActivity.startSearch();
        if (result != BluetoothCommunicator.SUCCESS) {
            if (result == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED && noBluetoothLe.getVisibility() != View.VISIBLE) {
                // appearance of the bluetooth le missing sign
                listViewGui.setVisibility(View.GONE);
                discoveryDescription.setVisibility(View.GONE);
                noBluetoothLe.setVisibility(View.VISIBLE);
            } else if (result != VoiceTranslationActivity.NO_PERMISSIONS && result != BluetoothCommunicator.ALREADY_STARTED) {
                Toast.makeText(voiceTranslationActivity, getResources().getString(R.string.error_starting_search), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopSearch() {
        voiceTranslationActivity.stopSearch(true);
    }

    private void connect(final Peer peer) {
        connectingPeer = peer;
        confirmConnectionPeer = peer;
        connectionConfirmDialog = new RequestDialog(voiceTranslationActivity, getResources().getString(R.string.dialog_confirm_connection) + peer.getName() + "?", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deactivateInputs();
                appearLoading();
                voiceTranslationActivity.connect(peer);
                startConnectionTimer();
            }
        }, null);
        connectionConfirmDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                confirmConnectionPeer = null;
                connectionConfirmDialog = null;
            }
        });
        connectionConfirmDialog.show();
    }

    public void clearFoundPeers() {
        if (listView != null) {
            listView.clear();
        }
    }

    private void startConnectionTimer() {
        connectionTimer = new Timer(PairingFragment.CONNECTION_TIMEOUT);
        connectionTimer.start();
    }

    private void resetConnectionTimer() {
        if (connectionTimer != null) {
            connectionTimer.cancel();
            connectionTimer = null;
        }
    }

    public Peer getConfirmConnectionPeer() {
        return confirmConnectionPeer;
    }

    public RequestDialog getConnectionConfirmDialog() {
        return connectionConfirmDialog;
    }

    private void activateInputs() {
        // reactivate the click in the listView
        setListViewClickable(true, true);
    }

    private void deactivateInputs() {
        // deactivate the click in the listView
        setListViewClickable(false, true);
    }

    public void setListViewClickable(boolean isClickable, boolean showToast) {
        if (listView != null) {
            listView.setClickable(isClickable, showToast);
        }
    }

    private void appearLoading() {
        // loading appearance
        ConversationFragment conversationFragment = (ConversationFragment) getParentFragment();
        if (conversationFragment != null) {
            conversationFragment.appearLoading(null);
        }
    }

    private void disappearLoading() {
        // loading disappearance
        ConversationFragment conversationFragment = (ConversationFragment) getParentFragment();
        if (conversationFragment != null) {
            conversationFragment.disappearLoading(selected, null);
        }
    }

    private void initializePeerList() {
        ArrayList<GuiPeer> connectedPeers = voiceTranslationActivity.getConnectedPeersList();
        //nếu số người connect lớn hơn 0 thì chạy code if
        if (connectedPeers.size() > 0) {
            InfoArray connectedPeersInfo = new InfoArray(voiceTranslationActivity, connectedPeers);
            listView = new PeerListAdapter(voiceTranslationActivity, connectedPeersInfo, new PeerListAdapter.Callback() {
                @Override
                public void onClickExit(final Peer peer) {
                    super.onClickExit(peer);
                    if (listView != null && listView.isClickable()) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(voiceTranslationActivity);
                        builder.setCancelable(true);
                        builder.setMessage(getResources().getString(R.string.dialog_confirm_disconnection) + peer.getName() + "?");
                        builder.setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        voiceTranslationActivity.disconnect(peer);
                                        if (voiceTranslationActivity.getConnectedPeersList().size() == 0) {
                                            Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> setFragment ");
                                            voiceTranslationActivity.setFragment(VoiceTranslationActivity.DEFAULT_FRAGMENT);
                                        }
                                    }
                                });
                        builder.setNegativeButton(android.R.string.no, null);

                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else if (listView != null) {
                        listView.getCallback().onClickNotAllowed(listView.getShowToast());
                    }
                }

                @Override
                public void onClickNotAllowed(boolean showToast) {
                    super.onClickNotAllowed(showToast);
                    if (disconnectingPeers.size() > 0) {
                        Toast.makeText(voiceTranslationActivity, getResources().getString(R.string.error_cannot_interact_disconnection), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(voiceTranslationActivity, getResources().getString(R.string.error_cannot_interact_connection), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            listViewGui.setAdapter(listView);
            listViewGui.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    // start the pop up and then connect to the peer
                    if (listView.isClickable()) {
                        Listable item = listView.get(i);
                        if (item instanceof Peer) {
                            Peer peer = (Peer) item;
                            connect(peer);
                        }
                    } else {
                        listView.getCallback().onClickNotAllowed(listView.getShowToast());
                    }
                }
            });
        }

        //nếu số người bằng 0 thì đá về fragment default của VoiceTranslationActivity
        else
        {
            Log.d("CHUNG-", "CHUNG- VoiceTranslationActivity() -> setFragment về 0 do connectedPeers.size = 0 ");
            voiceTranslationActivity.setFragment(VoiceTranslationActivity.DEFAULT_FRAGMENT);
        }
    }
}
