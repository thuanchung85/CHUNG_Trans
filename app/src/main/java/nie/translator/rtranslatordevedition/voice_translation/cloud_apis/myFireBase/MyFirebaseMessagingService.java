package nie.translator.rtranslatordevedition.voice_translation.cloud_apis.myFireBase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import nie.translator.rtranslatordevedition.Global;
import nie.translator.rtranslatordevedition.R;
import nie.translator.rtranslatordevedition.voice_translation.VoiceTranslationActivity;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private Global global;

    @Override
    public void onNewToken(String s) {
        global = (Global) getApplication();

        Log.d("CHUNG-",  global.getName() + "CHUNG- MyFirebaseMessagingService() -> onNewToken" + s);


        // update token FMC whenever refesh by firebase
        String msg = "FMC TOKEN: -> \n" + s;
        global.FMCToken = s;
        Log.w("CHUNG", " FCM  token REFESH:" +  global.FMCToken);

        //RE LOGIN NOW! with FMC_token
        String tempUserChungPhone =  global.getName();
        String tempUserChungPhoneFirstname =  "f_" + global.getName();
        String tempUserChungPhoneLastname =  "l_" + global.getName();
        String tempUserChungPhoneLanguage = getResources().getConfiguration().locale.getLanguage();
        String FMC_token = global.FMCToken;
        global.SendData_to_mSocketFORLOGIN(tempUserChungPhone, tempUserChungPhoneFirstname, tempUserChungPhoneLastname, tempUserChungPhoneLanguage, FMC_token);
       if(global.FMCToken != null) {
           try {
               Toast.makeText(getBaseContext(), "REFESH  FCM  token by onNewToken:" + global.FMCToken, Toast.LENGTH_SHORT).show();
           }
           catch (RuntimeException e){

           }
       }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        System.out.println(remoteMessage);
        global = (Global) getApplication();
        Log.w("CHUNG", "CHUNG FCM  onMessageReceived REFESH:" +  global.getName());

        Toast.makeText(getBaseContext(), "<<<<<<onMessageReceived: " , Toast.LENGTH_SHORT).show();

        Map<String, String> params = remoteMessage.getData();
        JSONObject object = new JSONObject(params);
        try {
            String action = object.getString("action");
            String to = object.getString("_to");
            String from = object.getString("_from");


            Log.w("CHUNG-", String.format("CHUNG- MyFirebaseMessagingService() -> onMessageReceived" + object));

            String NOTIFICATION_CHANNEL_ID = "rTranslator_channel";

            long pattern[] = {0, 1000, 500, 1000};

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "rTranslator Notifications",
                        NotificationManager.IMPORTANCE_HIGH);

                notificationChannel.setDescription("");
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Color.RED);
                notificationChannel.setVibrationPattern(pattern);
                notificationChannel.enableVibration(true);
                mNotificationManager.createNotificationChannel(notificationChannel);
            }

            // to diaplay notification in DND Mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = mNotificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
                channel.canBypassDnd();
            }

            Intent rTranlateActivity =  new Intent(this.getApplicationContext(), VoiceTranslationActivity.class);
            rTranlateActivity.putExtra("action", action);
            rTranlateActivity.putExtra("_to", to);
            rTranlateActivity.putExtra("_from", from);
            rTranlateActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, rTranlateActivity, PendingIntent.FLAG_UPDATE_CURRENT );

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

            notificationBuilder.setAutoCancel(true)
                    .setColor(ContextCompat.getColor(this, R.color.primary))
                    .setContentTitle(getString(R.string.app_name))
                    //remoteMessage.getNotification().getBody()
                    .setContentText( "data: "+ "\n" + " | " + action + " | "+ to + " | " + from)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);



            mNotificationManager.notify(1000, notificationBuilder.build());

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }
}
