package com.example.testappsyncmanager.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.ContentLoadingProgressBar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.testappsyncmanager.connection.ChatConnectionManager;
import com.example.testappsyncmanager.R;
import com.example.testappsyncmanager.utilities.PreferenceUtils;
import com.google.android.material.snackbar.Snackbar;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;

public class MainActivity extends AppCompatActivity {
    String userName="", chatName="", password="";
    Button btnJoin;
    CoordinatorLayout layoutSignIn;
    private ContentLoadingProgressBar mProgressBar;

    //Test Comment
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        layoutSignIn=findViewById(R.id.layout_sign_in);
        Button btnJoin = findViewById(R.id.btn_join);
        final EditText edit_user_name = findViewById(R.id.edit_user_name);
        final EditText edit_chat_name = findViewById(R.id.edit_chat_name);
        final EditText edit_password = findViewById(R.id.edit_password);
        btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userName = edit_user_name.getText().toString();
                // Remove all spaces from userID
                userName = userName.replaceAll("\\s", "");
                chatName = edit_chat_name.getText().toString();
                password = edit_password.getText().toString();
//                connectToSendBird(userName, chatName);
                boolean reply= authentication(userName, chatName, password);
                if(reply)
                {
                    Intent intent = new Intent(MainActivity.this, SelectChatTypeActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });


       /* FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
    }
    public boolean authentication(final String userId, final String chatName, final String password)
    {
        //btnJoin.setEnabled(false);

        ChatConnectionManager.login(userId, new SendBird.ConnectHandler() {
            @Override
            public void onConnected(User user, SendBirdException e) {


                if (e != null) {
                    // Error!
                    Toast.makeText(
                            MainActivity.this, "" + e.getCode() + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT)
                            .show();

                    // Show login failure snackbar
                    showSnackbar("Login to SendBird failed");
                    //btnJoin.setEnabled(true);
                    PreferenceUtils.setConnected(false);
                    return;
                }
                PreferenceUtils.setConnected(true);

                // Update the user's nickname
                updateCurrentUserInfo(userId);
                updateCurrentUserPushToken();

                // Proceed to MainActivity
                Intent intent = new Intent(MainActivity.this, SelectChatTypeActivity.class);
                startActivity(intent);
                finish();
            }
            });
        return true;
    }
    private void updateCurrentUserInfo(final String userNickname) {
        SendBird.updateCurrentUserInfo(userNickname, null, new SendBird.UserInfoUpdateHandler() {
            @Override
            public void onUpdated(SendBirdException e) {
                if (e != null) {
                    // Error!
                    Toast.makeText(
                            MainActivity.this, "" + e.getCode() + ":" + e.getMessage(),
                            Toast.LENGTH_SHORT)
                            .show();

                    // Show update failed snackbar
                    showSnackbar("Update user nickname failed");

                    return;
                }

                PreferenceUtils.setNickname(userNickname);
            }
        });
    }

            private void updateCurrentUserPushToken() {
//                PushNotificationForCurrentUser.registerPushTokenForCurrentUser(MainActivity.this, null);
            }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

            private void showSnackbar(String text) {
                Snackbar snackbar = Snackbar.make(layoutSignIn, text, Snackbar.LENGTH_SHORT);

                snackbar.show();
            }

            // Shows or hides the ProgressBar
            private void showProgressBar(boolean show) {
                if (show) {
                    mProgressBar.show();
                } else {
                    mProgressBar.hide();
                }
            }

    private void connectToSendBird(final String userId, final String userNickname) {
        // Show the loading indicator
        //showProgressBar(true);
        //btnJoin.setEnabled(false);

        ChatConnectionManager.login(userId, new SendBird.ConnectHandler() {
            @Override
            public void onConnected(User user, SendBirdException e) {
                // Callback received; hide the progress bar.
                showProgressBar(false);

                if (e != null) {
                    // Error!
                    Toast.makeText(
                            MainActivity.this, "" + e.getCode() + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT)
                            .show();

                    // Show login failure snackbar
                    showSnackbar("Login to SendBird failed");
                    //btnJoin.setEnabled(true);
                    PreferenceUtils.setConnected(false);
                    return;
                }

                PreferenceUtils.setConnected(true);

                // Update the user's nickname
                updateCurrentUserInfo(userNickname);
                updateCurrentUserPushToken();

                // Proceed to MainActivity
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

}
