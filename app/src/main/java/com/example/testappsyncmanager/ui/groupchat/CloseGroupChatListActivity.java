package com.example.testappsyncmanager.ui.groupchat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.testappsyncmanager.R;
import com.example.testappsyncmanager.adapters.GroupChannelListAdapter;
import com.example.testappsyncmanager.connection.ChatConnectionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.GroupChannelListQuery;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;

import java.util.List;

public class CloseGroupChatListActivity extends AppCompatActivity {
	private androidx.appcompat.widget.Toolbar mToolbar;
	public static final String EXTRA_GROUP_CHANNEL_URL = "GROUP_CHANNEL_URL";
	private static final int INTENT_REQUEST_NEW_GROUP_CHANNEL = 302;

	private static final int CHANNEL_LIST_LIMIT = 20;
	private static final String CONNECTION_HANDLER_ID = "CONNECTION_HANDLER_GROUP_CHANNEL_LIST";
	private static final String CHANNEL_HANDLER_ID = "CHANNEL_HANDLER_GROUP_CHANNEL_LIST";

	private RecyclerView mRecyclerView;
	private LinearLayoutManager mLayoutManager;
	private GroupChannelListAdapter mChannelListAdapter;
	private FloatingActionButton mCreateChannelFab;
	private GroupChannelListQuery mChannelListQuery;
	private SwipeRefreshLayout mSwipeRefresh;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_chat_list);
		setupToolbar();
		if (savedInstanceState == null) {
			mRecyclerView = findViewById(R.id.closed_group_chat_recycler);
			mCreateChannelFab = findViewById(R.id.create_close_group_channel_list_btn);
			mSwipeRefresh = findViewById(R.id.refresh_close_group_channel_list);

			mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
				@Override
				public void onRefresh() {
					mSwipeRefresh.setRefreshing(true);
					refresh();
				}
			});

			mCreateChannelFab.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(CloseGroupChatListActivity.this, CreateNewCloseGroupChannelActivity.class);
					startActivityForResult(intent, INTENT_REQUEST_NEW_GROUP_CHANNEL);
				}
			});

			mChannelListAdapter = new GroupChannelListAdapter(this);
			mChannelListAdapter.load();

			initView();
		}

		String channelUrl = getIntent().getStringExtra("groupChannelUrl");
		if (channelUrl != null) {
			enterCloseGroupChannel(channelUrl);
		}
	}
	@Override
	public void onPause() {
		mChannelListAdapter.save();
		ChatConnectionManager.removeConnectionManagementHandler(CONNECTION_HANDLER_ID);
		SendBird.removeChannelHandler(CHANNEL_HANDLER_ID);
		super.onPause();
	}
	@Override
	public void onResume() {
		ChatConnectionManager.addConnectionManagementHandler(CONNECTION_HANDLER_ID, new ChatConnectionManager.ConnectionManagementHandler() {
			@Override
			public void onConnected(boolean reconnect) {
				refresh();
			}
		});

		SendBird.addChannelHandler(CHANNEL_HANDLER_ID, new SendBird.ChannelHandler() {
			@Override
			public void onMessageReceived(BaseChannel baseChannel, BaseMessage baseMessage) {
			}

			@Override
			public void onChannelChanged(BaseChannel channel) {
				mChannelListAdapter.clearMap();
				mChannelListAdapter.updateOrInsert(channel);
			}

			@Override
			public void onTypingStatusUpdated(GroupChannel channel) {
				mChannelListAdapter.notifyDataSetChanged();
			}
		});

		super.onResume();
	}
	private void setupToolbar() {
		mToolbar = findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setTitle(getResources().getString(R.string.group_channel_screen_title));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private void initView() {
		mLayoutManager = new LinearLayoutManager(this);
		mRecyclerView.setLayoutManager(mLayoutManager);
		mRecyclerView.setAdapter(mChannelListAdapter);
		mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

		mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				if (mLayoutManager.findLastVisibleItemPosition() == mChannelListAdapter.getItemCount() - 1) {
					loadNextChannelList();
				}
			}
		});
		mChannelListAdapter.setOnItemClickListener(new GroupChannelListAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(GroupChannel channel) {
				enterGroupChannel(channel);
			}
		});

		mChannelListAdapter.setOnItemLongClickListener(new GroupChannelListAdapter.OnItemLongClickListener() {
			@Override
			public void onItemLongClick(final GroupChannel channel) {
				showChannelOptionsDialog(channel);
			}
		});
	}

	void enterGroupChannel(GroupChannel channel) {
		final String channelUrl = channel.getUrl();
		enterCloseGroupChannel(channelUrl);
	}

	private void showChannelOptionsDialog(final GroupChannel channel) {
		String[] options;
		final boolean pushCurrentlyEnabled = channel.isPushEnabled();

		options = pushCurrentlyEnabled
				? new String[]{"Leave channel", "Turn push notifications OFF"}
				: new String[]{"Leave channel", "Turn push notifications ON"};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Channel options")
				.setItems(options, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {

							new AlertDialog.Builder(CloseGroupChatListActivity.this)
									.setTitle("Leave channel " + channel.getName() + "?")
									.setPositiveButton("Leave", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											leaveChannel(channel);
										}
									})
									.setNegativeButton("Cancel", null)
									.create().show();
						} else if (which == 1) {
							setChannelPushPreferences(channel, !pushCurrentlyEnabled);
						}
					}
				});
		builder.create().show();
	}

	private void setChannelPushPreferences(final GroupChannel channel, final boolean on) {

		channel.setPushPreference(on, new GroupChannel.GroupChannelSetPushPreferenceHandler() {
			@Override
			public void onResult(SendBirdException e) {
				if (e != null) {
					e.printStackTrace();
					Toast.makeText(CloseGroupChatListActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
					return;
				}

				String toast = on
						? "Push notifications have been turned ON"
						: "Push notifications have been turned OFF";

				Toast.makeText(CloseGroupChatListActivity.this, toast, Toast.LENGTH_SHORT)
						.show();
			}
		});
	}

	private void enterCloseGroupChannel(String channelUrl) {
		startActivity(new Intent(this, CloseGroupChatActivity.class).putExtra(CloseGroupChatListActivity.EXTRA_GROUP_CHANNEL_URL, channelUrl));
	}

	private void loadNextChannelList() {
		mChannelListQuery.next(new GroupChannelListQuery.GroupChannelListQueryResultHandler() {
			@Override
			public void onResult(List<GroupChannel> list, SendBirdException e) {
				if (e != null) {

					e.printStackTrace();
					return;
				}

				for (GroupChannel channel : list) {
					mChannelListAdapter.addLast(channel);
				}
			}
		});
	}


	private void leaveChannel(final GroupChannel channel) {
		channel.leave(new GroupChannel.GroupChannelLeaveHandler() {
			@Override
			public void onResult(SendBirdException e) {
				if (e != null) {
					return;
				}
				refresh();
			}
		});
	}

	private void refreshChannelList(int numChannels) {
		mChannelListQuery = GroupChannel.createMyGroupChannelListQuery();
		mChannelListQuery.setLimit(numChannels);
		mChannelListQuery.setIncludeEmpty(true);
		mChannelListQuery.next(new GroupChannelListQuery.GroupChannelListQueryResultHandler() {
			@Override
			public void onResult(List<GroupChannel> list, SendBirdException e) {
				if (e != null) {
					e.printStackTrace();
					return;
				}
				mChannelListAdapter.clearMap();
				mChannelListAdapter.setGroupChannelList(list);
			}
		});

		if (mSwipeRefresh.isRefreshing()) {
			mSwipeRefresh.setRefreshing(false);
		}
	}

	private void refresh() {
		refreshChannelList(CHANNEL_LIST_LIMIT);
	}

	interface onBackPressedListener {
		boolean onBack();
	}

	private onBackPressedListener mOnBackPressedListener;

	public void setOnBackPressedListener(onBackPressedListener listener) {
		mOnBackPressedListener = listener;
	}

	@Override
	public void onBackPressed() {
		if (mOnBackPressedListener != null && mOnBackPressedListener.onBack()) {
			return;
		}
		super.onBackPressed();
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

	void setActionBarTitle(String title) {
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle(title);
		}
	}
}
