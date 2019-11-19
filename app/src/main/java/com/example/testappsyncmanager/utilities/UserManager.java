package com.example.testappsyncmanager.utilities;

import com.example.testappsyncmanager.models.User;

public class UserManager {
	public static User getUserInfo() {
		User user = new User();
		user.userProfileUrl = PreferenceUtils.getUserProfileUrl();
		user.userName = PreferenceUtils.getNickname();
		return user;
	}
}
