package com.example.testappsyncmanager.ui.openchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testappsyncmanager.R;
import com.example.testappsyncmanager.adapters.OpenChatListAdapter;
import com.example.testappsyncmanager.connection.ChatConnectionManager;
import com.example.testappsyncmanager.utilities.PreferenceUtils;
import com.sendbird.android.OpenChannel;
import com.sendbird.android.OpenChannelListQuery;
import com.sendbird.android.SendBirdException;
import com.sendbird.syncmanager.SendBirdSyncManager;
import com.sendbird.syncmanager.handler.CompletionHandler;

import java.util.List;

public class OpenChatListActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private OpenChatListAdapter mOpenChatListAdapter;
    public static final String OPEN_CHANNEL_URL = "OPEN_CHANNEL_URL";
    private static final int CHANNEL_LIST_LIMIT = 10;
    private static final String CONNECTION_HANDLER_ID = "CONNECTION_HANDLER_ID";
    private OpenChannelListQuery mOpenChannelListQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_chat_list);
        setupToolbar();
        mRecyclerView = findViewById(R.id.recycler_open_chat_list);
        mOpenChatListAdapter = new OpenChatListAdapter(this);
        initView();
    }

    private void setupToolbar() {
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.open_chat_list_screen_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initView() {
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mOpenChatListAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, 0));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (mLayoutManager.findLastVisibleItemPosition() == mOpenChatListAdapter.getItemCount() - 1) {
                    loadNextChannelList();
                }
            }
        });
        mOpenChatListAdapter.setOnItemClickListener(new OpenChatListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(OpenChannel channel) {
                String channelUrl = channel.getUrl();
                startActivity(new Intent(OpenChatListActivity.this, OpenChatActivity.class).putExtra(OPEN_CHANNEL_URL, channelUrl));
            }
        });

        mOpenChatListAdapter.setOnItemLongClickListener(new OpenChatListAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongPress(OpenChannel channel) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        SendBirdSyncManager.setup(this, PreferenceUtils.getUserId(), new CompletionHandler() {
            @Override
            public void onCompleted(SendBirdException e) {
                ChatConnectionManager.addConnectionManagementHandler(CONNECTION_HANDLER_ID, new ChatConnectionManager.ConnectionManagementHandler() {
                    @Override
                    public void onConnected(boolean reconnect) {
                        refreshChannelList();
                    }
                });
            }
        });
    }

    private void refreshChannelList() {
        mOpenChannelListQuery = OpenChannel.createOpenChannelListQuery();
        mOpenChannelListQuery.setLimit(CHANNEL_LIST_LIMIT);
        mOpenChannelListQuery.next(new OpenChannelListQuery.OpenChannelListQueryResultHandler() {
            @Override
            public void onResult(List<OpenChannel> list, SendBirdException e) {
                if (e != null) {
                    e.printStackTrace();
                    return;
                }
                mOpenChatListAdapter.setOpenChannelList(list);
            }
        });
    }

    private void loadNextChannelList() {
        if (mOpenChannelListQuery != null) {
            mOpenChannelListQuery.next(new OpenChannelListQuery.OpenChannelListQueryResultHandler() {
                @Override
                public void onResult(List<OpenChannel> list, SendBirdException e) {
                    if (e != null) {
                        e.printStackTrace();
                        return;
                    }

                    for (OpenChannel channel : list) {
                        mOpenChatListAdapter.addLast(channel);
                    }
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
