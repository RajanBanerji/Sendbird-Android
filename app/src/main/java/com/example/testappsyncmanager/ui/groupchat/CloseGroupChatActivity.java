package com.example.testappsyncmanager.ui.groupchat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testappsyncmanager.R;
import com.example.testappsyncmanager.adapters.CloseGroupChatAdapter;
import com.example.testappsyncmanager.connection.ChatConnectionManager;
import com.example.testappsyncmanager.ui.openchat.ShowImageActivity;
import com.example.testappsyncmanager.utilities.BasicUtils;
import com.example.testappsyncmanager.utilities.FileUtils;
import com.example.testappsyncmanager.utilities.PreferenceUtils;
import com.example.testappsyncmanager.utilities.UrlPreviewInfo;
import com.google.android.material.snackbar.Snackbar;
import com.sendbird.android.AdminMessage;
import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.PreviousMessageListQuery;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.UserMessage;
import com.sendbird.syncmanager.SendBirdSyncManager;
import com.sendbird.syncmanager.handler.CompletionHandler;

import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;


public class CloseGroupChatActivity extends AppCompatActivity {
    private static final String LOG_TAG = CloseGroupChatActivity.class.getSimpleName();
    private static final int CHANNEL_LIST_LIMIT = 20;
    private static final String CONNECTION_HANDLER_GROUP_CHAT = "CONNECTION_HANDLER_GROUP_CHAT";
    private static final int NORMAL_STATE = 0;
    private static final int EDIT_STATE = 1;
    private static final String STATE_CHANNEL_URL = "STATE_CHANNEL_URL";
    private static final int INTENT_REQUEST_CHOOSE_MEDIA = 301;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 13;
    public static final String EXTRA_CHANNEL_URL = "EXTRA_CHANNEL_URL";

    private InputMethodManager mInputMethodManager;
    private HashMap<BaseChannel.SendFileMessageWithProgressHandler, FileMessage> mFileProgressHandlerMap;
    private RelativeLayout mGroupChatParentLayout;
    private RecyclerView mRecyclerView;
    private CloseGroupChatAdapter mCloseGroupChatAdapter;
    private LinearLayoutManager mLayoutManager;
    private EditText mEditTxt;
    private Button mSendBtn;
    private ImageButton mUploadFileBtn;
    private View mCurrentEventLayout;
    private TextView mCurrentEventText;

    private GroupChannel mChannel;
    private String mChannelUrl;

    private boolean mIsTyping;

    private int mCurrentState = NORMAL_STATE;
    private BaseMessage mEditMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_close_group_chat);
        setupToolbar();

        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mFileProgressHandlerMap = new HashMap<>();

        if (savedInstanceState != null) {
            mChannelUrl = savedInstanceState.getString(STATE_CHANNEL_URL);
        } else {
            mChannelUrl = getIntent().getStringExtra(CloseGroupChatListActivity.EXTRA_GROUP_CHANNEL_URL);
        }

        mCloseGroupChatAdapter = new CloseGroupChatAdapter(this);
        setUpChatListAdapter();

        mCloseGroupChatAdapter.load(mChannelUrl);

        mGroupChatParentLayout = findViewById(R.id.group_chat_parent_layout);
        mRecyclerView = findViewById(R.id.recycler_view);

        mCurrentEventLayout = findViewById(R.id.layout_group_chat_current_event);
        mCurrentEventText = findViewById(R.id.text_group_chat_current_event);

        mEditTxt = findViewById(R.id.edittext_group_chat_message);
        mSendBtn = findViewById(R.id.button_group_chat_send);
        mUploadFileBtn = findViewById(R.id.button_group_chat_upload);

        mEditTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    mSendBtn.setEnabled(true);
                } else {
                    mSendBtn.setEnabled(false);
                }
            }
        });

        mSendBtn.setEnabled(false);
        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentState == EDIT_STATE) {
                    String userInput = mEditTxt.getText().toString();
                    if (userInput.length() > 0) {
                        if (mEditMessage != null) {
                            editChatMsg(mEditMessage, userInput);
                        }
                    }
                    setState(NORMAL_STATE, null, -1);
                } else {
                    String userInput = mEditTxt.getText().toString();
                    if (userInput.length() > 0) {
                        sendUserMessage(userInput);
                        mEditTxt.setText("");
                    }
                }
            }
        });

        mUploadFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestMedia();
            }
        });

        mIsTyping = false;
        mEditTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!mIsTyping) {
                    setTypingStatus(true);
                }

                if (s.length() == 0) {
                    setTypingStatus(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        setUpRecyclerView();
    }

    private void setUpRecyclerView() {
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mCloseGroupChatAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (mLayoutManager.findLastVisibleItemPosition() == mCloseGroupChatAdapter.getItemCount() - 1) {
                    mCloseGroupChatAdapter.loadPreviousMessages(CHANNEL_LIST_LIMIT, null);
                }
            }
        });
    }

    private void setUpChatListAdapter() {
        mCloseGroupChatAdapter.setItemClickListener(new CloseGroupChatAdapter.OnItemClickListener() {
            @Override
            public void onMsgItmClick(UserMessage message) {
                if (mCloseGroupChatAdapter.isFailedMessage(message)) {
                    retryFailedMessage(message);
                    return;
                }
                if (mCloseGroupChatAdapter.isTempMessage(message)) {
                    return;
                }


                if (message.getCustomType().equals(CloseGroupChatAdapter.URL_PREVIEW_CUSTOM_TYPE)) {
                    try {
                        UrlPreviewInfo info = new UrlPreviewInfo(message.getData());
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(info.getUrl()));
                        startActivity(browserIntent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFileMessageItemClick(FileMessage message) {
                if (mCloseGroupChatAdapter.isFailedMessage(message)) {
                    retryFailedMessage(message);
                    return;
                }
                if (mCloseGroupChatAdapter.isTempMessage(message)) {
                    return;
                }


                onFileMessageClicked(message);
            }
        });

        mCloseGroupChatAdapter.setItemLongClickListener(new CloseGroupChatAdapter.OnItemLongClickListener() {
            @Override
            public void onUserMessageItemLongClick(UserMessage message, int position) {
                showMessageOptionsDialog(message, position);
            }

            @Override
            public void onFileMessageItemLongClick(FileMessage message) {
            }

            @Override
            public void onAdminMessageItemLongClick(AdminMessage message) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SendBirdSyncManager.setup(this, PreferenceUtils.getUserId(), new CompletionHandler() {
            @Override
            public void onCompleted(SendBirdException e) {
                ChatConnectionManager.addConnectionManagementHandler(CONNECTION_HANDLER_GROUP_CHAT, new ChatConnectionManager.ConnectionManagementHandler() {
                    @Override
                    public void onConnected(boolean reconnect) {
                        refreshFirst();
                    }
                });
            }
        });
        ChatConnectionManager.addConnectionManagementHandler(CONNECTION_HANDLER_GROUP_CHAT, new ChatConnectionManager.ConnectionManagementHandler() {
            @Override
            public void onConnected(boolean reconnect) {
                refreshFirst();
            }
        });

    }

    private void setupToolbar() {
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
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

    private void refresh() {
        loadInitialMessageList(CHANNEL_LIST_LIMIT);
    }

    private void refreshFirst() {
        enterChannel(mChannelUrl);
    }

    private void enterChannel(String channelUrl) {
        GroupChannel.getChannel(channelUrl, new GroupChannel.GroupChannelGetHandler() {
            @Override
            public void onResult(final GroupChannel groupChannel, SendBirdException e) {
                mChannel = groupChannel;
                if (e != null) {
                    // Error!
                    e.printStackTrace();
                    return;
                }
                refresh();
            }
        });
    }

    private void loadInitialMessageList(int numMessages) {

        PreviousMessageListQuery mPrevMessageListQuery = mChannel.createPreviousMessageListQuery();
        mPrevMessageListQuery.load(numMessages, true, new PreviousMessageListQuery.MessageListQueryResult() {
            @Override
            public void onResult(List<BaseMessage> list, SendBirdException e) {
                if (e != null) {
                    // Error!
                    e.printStackTrace();
                    return;
                }

                mCloseGroupChatAdapter.setMessageList(list);
            }
        });

    }

    private void showMessageOptionsDialog(final BaseMessage message, final int position) {
        String[] options = new String[]{"Edit message", "Delete message"};

        AlertDialog.Builder builder = new AlertDialog.Builder(CloseGroupChatActivity.this);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    setState(EDIT_STATE, message, position);
                } else if (which == 1) {
                    deleteChatMsg(message);
                }
            }
        });
        builder.create().show();
    }

    private void setState(int state, BaseMessage editingMessage, final int position) {
        switch (state) {
            case NORMAL_STATE:
                mCurrentState = NORMAL_STATE;
                mEditMessage = null;

                mUploadFileBtn.setVisibility(View.VISIBLE);
                mSendBtn.setText("SEND");
                mEditTxt.setText("");
                break;

            case EDIT_STATE:
                mCurrentState = EDIT_STATE;
                mEditMessage = editingMessage;

                mUploadFileBtn.setVisibility(View.GONE);
                mSendBtn.setText("SAVE");
                String messageString = ((UserMessage) editingMessage).getMessage();
                if (messageString == null) {
                    messageString = "";
                }
                mEditTxt.setText(messageString);
                if (messageString.length() > 0) {
                    mEditTxt.setSelection(0, messageString.length());
                }

                mEditTxt.requestFocus();
                mEditTxt.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mInputMethodManager.showSoftInput(mEditTxt, 0);

                        mRecyclerView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mRecyclerView.scrollToPosition(position);
                            }
                        }, 500);
                    }
                }, 100);
                break;
        }
    }

    private void retryFailedMessage(final BaseMessage message) {
        new AlertDialog.Builder(CloseGroupChatActivity.this)
                .setMessage("Retry?")
                .setPositiveButton(R.string.resend_message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            if (message instanceof UserMessage) {
                                String userInput = ((UserMessage) message).getMessage();
                                sendUserMessage(userInput);
                            } else if (message instanceof FileMessage) {
                                Uri uri = mCloseGroupChatAdapter.getTempFileMessageUri(message);
                                sendFileWithThumbnail(uri);
                            }
                            mCloseGroupChatAdapter.removeFailedMessage(message);
                        }
                    }
                })
                .setNegativeButton(R.string.delete_message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            mCloseGroupChatAdapter.removeFailedMessage(message);
                        }
                    }
                }).show();
    }

    private void requestMedia() {
        if (ContextCompat.checkSelfPermission(CloseGroupChatActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermissions();
        } else {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                intent.setType("*/*");
                String[] mimeTypes = {"image/*", "video/*"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            } else {
                intent.setType("image/* video/*");
            }

            intent.setAction(Intent.ACTION_GET_CONTENT);

            startActivityForResult(Intent.createChooser(intent, "Select Media"), INTENT_REQUEST_CHOOSE_MEDIA);

            SendBird.setAutoBackgroundDetection(false);
        }
    }

    private void requestStoragePermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(CloseGroupChatActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(mGroupChatParentLayout, "Storage access permissions are required to upload/download files.",
                    Snackbar.LENGTH_LONG)
                    .setAction("Okay", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    PERMISSION_WRITE_EXTERNAL_STORAGE);
                        }
                    })
                    .show();
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
    }

    private void onFileMessageClicked(FileMessage message) {
        String type = message.getType().toLowerCase();
        if (type.startsWith("image")) {
            Intent i = new Intent(CloseGroupChatActivity.this, ShowImageActivity.class);
            i.putExtra("url", message.getUrl());
            i.putExtra("type", message.getType());
            startActivity(i);
        } else if (type.startsWith("video")) {
            Toast.makeText(this, "Media player not found....", Toast.LENGTH_SHORT).show();
        } else {
            showDownloadConfirmDialog(message);
        }
    }

    private void showDownloadConfirmDialog(final FileMessage message) {
        if (ContextCompat.checkSelfPermission(CloseGroupChatActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            requestStoragePermissions();
        } else {
            new AlertDialog.Builder(CloseGroupChatActivity.this)
                    .setMessage("Download file?")
                    .setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                FileUtils.downloadFile(CloseGroupChatActivity.this, message.getUrl(), message.getName());
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null).show();
        }

    }

    private void sendUserMessageWithUrl(final String text, String url) {
        if (mChannel == null) {
            return;
        }

        new BasicUtils.UrlPreviewAsyncTask() {
            @Override
            protected void onPostExecute(UrlPreviewInfo info) {
                if (mChannel == null) {
                    return;
                }

                UserMessage tempUserMessage = null;
                BaseChannel.SendUserMessageHandler handler = new BaseChannel.SendUserMessageHandler() {
                    @Override
                    public void onSent(UserMessage userMessage, SendBirdException e) {
                        if (e != null) {
                            Toast.makeText(
                                    CloseGroupChatActivity.this,
                                    "Send failed with error " + e.getCode() + ": " + e.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                            mCloseGroupChatAdapter.markMessageFailed(userMessage.getRequestId());
                            return;
                        }

                        mCloseGroupChatAdapter.markMessageSent(userMessage);
                    }
                };

                try {
                    String jsonString = info.toJsonString();
                    tempUserMessage = mChannel.sendUserMessage(text, jsonString, CloseGroupChatAdapter.URL_PREVIEW_CUSTOM_TYPE, handler);
                } catch (Exception e) {
                    tempUserMessage = mChannel.sendUserMessage(text, handler);
                }

                mCloseGroupChatAdapter.addFirst(tempUserMessage);
            }
        }.execute(url);
    }

    private void sendUserMessage(String text) {
        if (mChannel != null) {


            List<String> urls = BasicUtils.extractUrls(text);
            if (urls.size() > 0) {
                sendUserMessageWithUrl(text, urls.get(0));
                return;
            }

            UserMessage tempUserMessage = mChannel.sendUserMessage(text, new BaseChannel.SendUserMessageHandler() {
                @Override
                public void onSent(UserMessage userMessage, SendBirdException e) {
                    if (e != null) {
                        Toast.makeText(
                                CloseGroupChatActivity.this,
                                "Send failed with error " + e.getCode() + ": " + e.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                        mCloseGroupChatAdapter.markMessageFailed(userMessage.getRequestId());
                        return;
                    }
                    mCloseGroupChatAdapter.markMessageSent(userMessage);
                }
            });
            mCloseGroupChatAdapter.addFirst(tempUserMessage);
        }
    }

    private void setTypingStatus(boolean typing) {
        if (mChannel == null) {
            return;
        }

        if (typing) {
            mIsTyping = true;
            mChannel.startTyping();
        } else {
            mIsTyping = false;
            mChannel.endTyping();
        }
    }

    private void sendFileWithThumbnail(Uri uri) {
        if (mChannel != null) {


        List<FileMessage.ThumbnailSize> thumbnailSizes = new ArrayList<>();
        thumbnailSizes.add(new FileMessage.ThumbnailSize(240, 240));
        thumbnailSizes.add(new FileMessage.ThumbnailSize(320, 320));

        Hashtable<String, Object> info = FileUtils.getFileInfo(CloseGroupChatActivity.this, uri);

        if (info == null) {
            Toast.makeText(CloseGroupChatActivity.this, "Extracting file information failed.", Toast.LENGTH_LONG).show();
            return;
        }

        final String path = (String) info.get("path");
        final File file = new File(path);
        final String name = file.getName();
        final String mime = (String) info.get("mime");
        final int size = (Integer) info.get("size");

        if (path.equals("")) {
            Toast.makeText(CloseGroupChatActivity.this, "File must be located in local storage.", Toast.LENGTH_LONG).show();
        } else {
            BaseChannel.SendFileMessageWithProgressHandler progressHandler = new BaseChannel.SendFileMessageWithProgressHandler() {
                @Override
                public void onProgress(int bytesSent, int totalBytesSent, int totalBytesToSend) {
                    FileMessage fileMessage = mFileProgressHandlerMap.get(this);
                    if (fileMessage != null && totalBytesToSend > 0) {
                        int percent = (totalBytesSent * 100) / totalBytesToSend;
                        mCloseGroupChatAdapter.setFileProgressPercent(fileMessage, percent);
                    }
                }

                @Override
                public void onSent(FileMessage fileMessage, SendBirdException e) {
                    if (e != null) {
                        Toast.makeText(CloseGroupChatActivity.this, "" + e.getCode() + ":" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        mCloseGroupChatAdapter.markMessageFailed(fileMessage.getRequestId());
                        return;
                    }

                    mCloseGroupChatAdapter.markMessageSent(fileMessage);
                }
            };
            FileMessage tempFileMessage = mChannel.sendFileMessage(file, name, mime, size, "", null, thumbnailSizes, progressHandler);

            mFileProgressHandlerMap.put(progressHandler, tempFileMessage);

            mCloseGroupChatAdapter.addTempFileMessageInfo(tempFileMessage, uri);
            mCloseGroupChatAdapter.addFirst(tempFileMessage);
        }
        }
    }

    private void editChatMsg(final BaseMessage message, String editedMessage) {
        if (mChannel!= null) {

            mChannel.updateUserMessage(message.getMessageId(), editedMessage, null, null, new BaseChannel.UpdateUserMessageHandler() {
                @Override
                public void onUpdated(UserMessage userMessage, SendBirdException e) {
                    if (e != null) {
                        Toast.makeText(CloseGroupChatActivity.this, "Error " + e.getCode() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mCloseGroupChatAdapter.loadLatestMessages(CHANNEL_LIST_LIMIT, new BaseChannel.GetMessagesHandler() {
                        @Override
                        public void onResult(List<BaseMessage> list, SendBirdException e) {
                            mCloseGroupChatAdapter.markAllMessagesAsRead();
                        }
                    });
                }
            });
        }
    }

    private void deleteChatMsg(final BaseMessage message) {
        if (mChannel != null) {
            mChannel.deleteMessage(message, new BaseChannel.DeleteMessageHandler() {
                @Override
                public void onResult(SendBirdException e) {
                    if (e != null) {
                        Toast.makeText(CloseGroupChatActivity.this, "Error " + e.getCode() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mCloseGroupChatAdapter.loadLatestMessages(CHANNEL_LIST_LIMIT, new BaseChannel.GetMessagesHandler() {
                        @Override
                        public void onResult(List<BaseMessage> list, SendBirdException e) {
                            mCloseGroupChatAdapter.markAllMessagesAsRead();
                        }
                    });
                }
            });
        }


    }
}
