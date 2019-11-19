package com.example.testappsyncmanager.ui.login;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.example.testappsyncmanager.R;
import com.example.testappsyncmanager.connection.ChatConnectionManager;
import com.example.testappsyncmanager.utilities.PreferenceUtils;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;

public class LoginPresenter extends LoginContract.Presenter {

	private final Context mContext;
	private LoginContract.View mView;

	public LoginPresenter(LoginContract.View view, Context context) {
		mView = view;
		mContext = context;
	}

	/**
	 * checking empty user login credentials
	 *
	 * @param userName
	 * @param password
	 */
	@Override
	public void validateUser(String userName, String password) {
		mView.showProgress(true);
		if (TextUtils.isEmpty(userName) || TextUtils.isEmpty(password)) {
			mView.showProgress(false);
			mView.onValidationError(mContext.getResources().getString(R.string.validation_failed_message));
		} else {
			mView.onValidationSuccess(userName);
		}
	}

	/**
	 * connecting user to sendBird chat sdk
	 *
	 * @param userName
	 */
	@Override
	public void authenticateUser(String userName) {
		ChatConnectionManager.login(userName, new SendBird.ConnectHandler() {
			@Override
			public void onConnected(User user, SendBirdException e) {
				mView.showProgress(false);
				if (e != null) {
					PreferenceUtils.setConnected(false);
					mView.onLoginError(String.format(mContext.getResources().getString(R.string.login_failed_message), e.getMessage()));
				} else {
					PreferenceUtils.setConnected(true);
					saveUserInfo(user);
					updateCurrentUserInfo(user);
					mView.onLoginSuccess(String.format(mContext.getResources().getString(R.string.loggedin_succesfully), user.getUserId()));
				}
			}
		});
	}

	private void saveUserInfo(User user) {
		PreferenceUtils.setNickname(user.getUserId());
		PreferenceUtils.setUserProfileUrl(user.getProfileUrl());
	}

	private void updateCurrentUserInfo(final User user) {
		SendBird.updateCurrentUserInfo(user.getUserId(), null, new SendBird.UserInfoUpdateHandler() {
			@Override
			public void onUpdated(SendBirdException e) {
				if (e != null) {
					Toast.makeText(mContext, "" + e.getCode() + ":" + e.getMessage(), Toast.LENGTH_SHORT).show();
					return;
				}
			}
		});
	}
}
