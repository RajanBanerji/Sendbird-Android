package com.example.testappsyncmanager.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.testappsyncmanager.R;
import com.example.testappsyncmanager.models.User;
import com.example.testappsyncmanager.ui.groupchat.CloseGroupChatListActivity;
import com.example.testappsyncmanager.ui.openchat.OpenChatListActivity;
import com.example.testappsyncmanager.utilities.ImageUtils;
import com.example.testappsyncmanager.utilities.UserManager;

public class SelectChatTypeActivity extends AppCompatActivity {
	private DrawerLayout mDrawerLayout;
	private Toolbar mToolbar;
	private ActionBarDrawerToggle mDrawerToggle;

	public static void intent(Context context) {
		Intent intent = new Intent(context, SelectChatTypeActivity.class);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_chat_type);
		setupToolbar();
		setDrawerLayout();
		setClickListener();
		findViewById(R.id.btn_open_chat).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SelectChatTypeActivity.this, OpenChatListActivity.class);
				startActivity(intent);
			}
		});

		findViewById(R.id.btn_group_chat).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SelectChatTypeActivity.this, CloseGroupChatListActivity.class);
				startActivity(intent);
			}
		});
	}

	private void setupToolbar() {
		mToolbar = findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setTitle(getString(R.string.screen_title_select_chat_type));
	}

	private void setDrawerLayout() {
		mDrawerLayout = findViewById(R.id.drawer_layout);
		setupDrawerToggle();
		setUserProfileOnDrawerLayout();
	}

	private void setupDrawerToggle() {
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.app_name, R.string.app_name);
		mDrawerToggle.syncState();
	}

	private void setUserProfileOnDrawerLayout() {
		User userInfo = UserManager.getUserInfo();
		ImageUtils.displayRoundImageFromUrl(this,String.valueOf(userInfo.userProfileUrl),(AppCompatImageView) findViewById(R.id.ivProfilePic));
		((AppCompatTextView) findViewById(R.id.tvUserName)).setText(String.valueOf(userInfo.userName));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void setClickListener() {
		findViewById(R.id.ivProfilePic).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Toast.makeText(SelectChatTypeActivity.this, getResources().getString(R.string.open_user_profile), Toast.LENGTH_SHORT).show();
			}
		});
	}
}
