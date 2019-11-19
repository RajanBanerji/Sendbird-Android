package com.example.testappsyncmanager.utilities;

import android.content.Context;
import android.widget.Toast;

import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;

public class PushNotificationForCurrentUser {
	public static void registerPushTokenForCurrentUser(String token, final Context context) {
		SendBird.registerPushTokenForCurrentUser(token, new SendBird.RegisterPushTokenWithStatusHandler() {
			@Override
			public void onRegistered(SendBird.PushTokenRegistrationStatus pushTokenRegistrationStatus, SendBirdException e) {
				if (e != null) {
					Toast.makeText(context, "" + e.getCode() + ":" + e.getMessage(), Toast.LENGTH_SHORT).show();
					return;
				}

				if (pushTokenRegistrationStatus == SendBird.PushTokenRegistrationStatus.PENDING) {
					Toast.makeText(context, "Connection required to register push token.", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
}
