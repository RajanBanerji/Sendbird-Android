package com.example.testappsyncmanager.ui.login;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.testappsyncmanager.R;
import com.example.testappsyncmanager.databinding.ActivityLoginBinding;
import com.example.testappsyncmanager.ui.SelectChatTypeActivity;

public class LoginActivity extends AppCompatActivity implements LoginContract.View {
	private ActivityLoginBinding mBinding;
	private LoginPresenter mPresenter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBinding = DataBindingUtil.setContentView(this, R.layout.activity_login);
		initView();
		mPresenter = new LoginPresenter(this, this);
	}

	private void initView() {
		mBinding.progressBar.setVisibility(View.GONE);
	}

	public void buttonLogin(View view) {
		String userName = mBinding.etUserName.getText().toString();
		String userPassword = mBinding.etPassword.getText().toString();
		mPresenter.validateUser(userName, userPassword);
	}

	@Override
	public void showProgress(boolean show) {
		if (show) {
			mBinding.progressBar.setVisibility(View.VISIBLE);
		} else {
			mBinding.progressBar.setVisibility(View.GONE);
		}
	}

	@Override
	public void onValidationSuccess(String userName) {
		mPresenter.authenticateUser(userName);
	}

	@Override
	public void onValidationError(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onLoginSuccess(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
		SelectChatTypeActivity.intent(this);
		finish();
	}

	@Override
	public void onLoginError(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}
}
