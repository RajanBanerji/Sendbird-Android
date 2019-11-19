package com.example.testappsyncmanager.ui.groupchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.testappsyncmanager.R;
import com.example.testappsyncmanager.utilities.PreferenceUtils;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.SendBirdException;

import java.util.ArrayList;
import java.util.List;



public class CreateNewCloseGroupChannelActivity extends AppCompatActivity
        implements SelectUserFragment.UsersSelectedListener, SelectDistinctFragment.DistinctSelectedListener {

    public static final String EXTRA_NEW_CHANNEL_URL = "EXTRA_NEW_CHANNEL_URL";

    static final int STATE_SELECT_USERS = 0;
    static final int STATE_SELECT_DISTINCT = 1;
    private Button mNextButton, mCreateCloseGroupBtn;
    private List<String> mSelectedIds;
    private boolean mIsDistinct = true;
    private int mCurrentState;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_close_group_channel);

        mSelectedIds = new ArrayList<>();

        if (savedInstanceState == null) {
            Fragment fragment = SelectUserFragment.newInstance();
            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction()
                    .replace(R.id.container_create_close_group_channel, fragment)
                    .commit();
        }

        mNextButton = findViewById(R.id.create_close_group_channel_next_btn);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentState == STATE_SELECT_USERS) {
                    Fragment fragment = SelectDistinctFragment.newInstance();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.container_create_close_group_channel, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            }
        });
        mNextButton.setEnabled(false);

        mCreateCloseGroupBtn = findViewById(R.id.create_close_group_channel_btn);
        mCreateCloseGroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentState == STATE_SELECT_USERS) {
                    mIsDistinct = PreferenceUtils.getGroupChannelDistinct();
                    createGroupChannel(mSelectedIds, mIsDistinct);
                }
            }
        });
        mCreateCloseGroupBtn.setEnabled(false);

        mToolbar = findViewById(R.id.create_close_group_channel_toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_left_white_24_dp);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void setState(int state) {
        if (state == STATE_SELECT_USERS) {
            mCurrentState = STATE_SELECT_USERS;
            mCreateCloseGroupBtn.setVisibility(View.VISIBLE);
            mNextButton.setVisibility(View.GONE);
        } else if (state == STATE_SELECT_DISTINCT){
            mCurrentState = STATE_SELECT_DISTINCT;
            mCreateCloseGroupBtn.setVisibility(View.VISIBLE);
            mNextButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUserSelected(boolean selected, String userId) {
        if (selected) {
            mSelectedIds.add(userId);
        } else {
            mSelectedIds.remove(userId);
        }

        if (mSelectedIds.size() > 0) {
            mCreateCloseGroupBtn.setEnabled(true);
        } else {
            mCreateCloseGroupBtn.setEnabled(false);
        }
    }

    @Override
    public void onDistinctSelected(boolean distinct) {
        mIsDistinct = distinct;
    }
    private void createGroupChannel(List<String> userIds, boolean distinct) {
        GroupChannel.createChannelWithUserIds(userIds, distinct, new GroupChannel.GroupChannelCreateHandler() {
            @Override
            public void onResult(GroupChannel groupChannel, SendBirdException e) {
                if (e != null) {
                    return;
                }
                Intent intent = new Intent();
                intent.putExtra(EXTRA_NEW_CHANNEL_URL, groupChannel.getUrl());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }


}
