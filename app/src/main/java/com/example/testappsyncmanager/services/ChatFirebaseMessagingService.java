package com.example.testappsyncmanager.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.testappsyncmanager.ui.groupchat.CloseGroupChatListActivity;
import com.example.testappsyncmanager.R;
import com.example.testappsyncmanager.utilities.PreferenceUtils;
import com.example.testappsyncmanager.utilities.PushNotificationForCurrentUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class ChatFirebaseMessagingService extends FirebaseMessagingService {
	private static final String TAG = "TestFirebaseMsgService";

	@Override
	public void onNewToken(@NonNull String s) {
		super.onNewToken(s);
		sendRegistrationToServer(s);
	}

	public void onMessageReceived(RemoteMessage remoteMessage) {
		if (remoteMessage.getData().size() > 0) {
			Log.d(TAG, "Message data payload: " + remoteMessage.getData());
		}
		if (remoteMessage.getNotification() != null) {
			Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
		}
		String channelUrl = null;
		try {
			JSONObject sendBird = new JSONObject(remoteMessage.getData().get("sendbird"));
			JSONObject channel = (JSONObject) sendBird.get("channel");
			channelUrl = (String) channel.get("channel_url");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		sendNotification(this, remoteMessage.getData().get("message"), channelUrl);

	}

	public static void sendNotification(Context context, String messageBody, String channelUrl) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		final String CHANNEL_ID = "CHANNEL_ID";
		if (Build.VERSION.SDK_INT >= 26) {
			NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "CHANNEL_NAME", NotificationManager.IMPORTANCE_HIGH);
			notificationManager.createNotificationChannel(mChannel);
		}

		Intent intent = new Intent(context, CloseGroupChatListActivity.class);
		intent.putExtra("groupChannelUrl", channelUrl);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_launcher_background)
				.setColor(Color.parseColor("#7469C4"))
				.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_foreground))
				.setContentTitle(context.getResources().getString(R.string.app_name))
				.setAutoCancel(true)
				.setSound(defaultSoundUri)
				.setPriority(Notification.PRIORITY_MAX)
				.setDefaults(Notification.DEFAULT_ALL)
				.setContentIntent(pendingIntent);

		if (PreferenceUtils.getNotificationsShowPreviews()) {
			notificationBuilder.setContentText(messageBody);
		} else {
			notificationBuilder.setContentText("Somebody sent you a message.");
		}

		notificationManager.notify(0 , notificationBuilder.build());
	}

	private void sendRegistrationToServer(String token) {
		PushNotificationForCurrentUser.registerPushTokenForCurrentUser(token, ChatFirebaseMessagingService.this);
	}
}
