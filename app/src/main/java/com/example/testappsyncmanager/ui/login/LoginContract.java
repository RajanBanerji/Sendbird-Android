package com.example.testappsyncmanager.ui.login;

public interface LoginContract {
	interface View {
		void showProgress(boolean show);

		void onValidationSuccess(String userName);

		void onValidationError(String message);

		void onLoginSuccess(String message);

		void onLoginError(String message);
	}

	abstract class Presenter {
		public abstract void validateUser(String userName, String password);

		public abstract void authenticateUser(String userName);
	}
}
