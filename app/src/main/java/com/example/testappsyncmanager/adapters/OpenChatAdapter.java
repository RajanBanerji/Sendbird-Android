package com.example.testappsyncmanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testappsyncmanager.R;
import com.example.testappsyncmanager.utilities.DateUtils;
import com.example.testappsyncmanager.utilities.FileUtils;
import com.example.testappsyncmanager.utilities.ImageUtils;
import com.sendbird.android.AdminMessage;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.SendBird;
import com.sendbird.android.User;
import com.sendbird.android.UserMessage;

import java.util.ArrayList;
import java.util.List;

public class OpenChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int VIEW_TYPE_USER_MESSAGE = 10;
	private static final int VIEW_TYPE_FILE_MESSAGE = 20;
	private static final int VIEW_TYPE_ADMIN_MESSAGE = 30;

	private Context mContext;
	private List<BaseMessage> mMessageList;
	private OnItemClickListener mItemClickListener;
	private OnItemLongClickListener mItemLongClickListener;

	public interface OnItemClickListener {
		void onUserMessageItemClick(UserMessage message);

		void onFileMessageItemClick(FileMessage message);

		void onAdminMessageItemClick(AdminMessage message);
	}

	public interface OnItemLongClickListener {
		void onBaseMessageLongClick(BaseMessage message, int position);
	}


	public OpenChatAdapter(Context context) {
		mMessageList = new ArrayList<>();
		mContext = context;
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		mItemClickListener = listener;
	}

	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		mItemLongClickListener = listener;
	}

	public void setMessageList(List<BaseMessage> messages) {
		mMessageList = messages;
		notifyDataSetChanged();
	}

	public void addFirst(BaseMessage message) {
		mMessageList.add(0, message);
		notifyDataSetChanged();
	}

	public void addLast(BaseMessage message) {
		mMessageList.add(message);
		notifyDataSetChanged();
	}

	public void delete(long msgId) {
		for(BaseMessage msg : mMessageList) {
			if(msg.getMessageId() == msgId) {
				mMessageList.remove(msg);
				notifyDataSetChanged();
				break;
			}
		}
	}

	public void update(BaseMessage message) {
		BaseMessage baseMessage;
		for (int index = 0; index < mMessageList.size(); index++) {
			baseMessage = mMessageList.get(index);
			if(message.getMessageId() == baseMessage.getMessageId()) {
				mMessageList.remove(index);
				mMessageList.add(index, message);
				notifyDataSetChanged();
				break;
			}
		}
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if (viewType == VIEW_TYPE_USER_MESSAGE) {
			View view = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.list_item_open_chat_user, parent, false);
			return new UserMessageHolder(view);

		} else if (viewType == VIEW_TYPE_ADMIN_MESSAGE) {
			View view = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.list_item_open_chat_admin, parent, false);
			return new AdminMessageHolder(view);

		} else if (viewType == VIEW_TYPE_FILE_MESSAGE) {
			View view = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.list_item_open_chat_file, parent, false);
			return new FileMessageHolder(view);
		}
		return null;
	}

	@Override
	public int getItemViewType(int position) {
		if (mMessageList.get(position) instanceof UserMessage) {
			return VIEW_TYPE_USER_MESSAGE;
		} else if (mMessageList.get(position) instanceof AdminMessage) {
			return VIEW_TYPE_ADMIN_MESSAGE;
		} else if (mMessageList.get(position) instanceof FileMessage) {
			return VIEW_TYPE_FILE_MESSAGE;
		}
		return -1;
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		BaseMessage message = mMessageList.get(position);

		boolean isNewDay = false;

		if (position < mMessageList.size() - 1) {
			BaseMessage prevMessage = mMessageList.get(position + 1);

			if (!DateUtils.hasSameDate(message.getCreatedAt(), prevMessage.getCreatedAt())) {
				isNewDay = true;
			}

		} else if (position == mMessageList.size() - 1) {
			isNewDay = true;
		}

		switch (holder.getItemViewType()) {
			case VIEW_TYPE_USER_MESSAGE:
				((UserMessageHolder) holder).bind(mContext, (UserMessage) message, isNewDay,
						mItemClickListener, mItemLongClickListener, position);
				break;
			case VIEW_TYPE_ADMIN_MESSAGE:
				((AdminMessageHolder) holder).bind((AdminMessage) message, isNewDay,
						mItemClickListener);
				break;
			case VIEW_TYPE_FILE_MESSAGE:
				((FileMessageHolder) holder).bind(mContext, (FileMessage) message, isNewDay,
						mItemClickListener, mItemLongClickListener, position);
				break;
			default:
				break;
		}
	}

	@Override
	public int getItemCount() {
		return mMessageList.size();
	}

	private class UserMessageHolder extends RecyclerView.ViewHolder {
		TextView nicknameText, messageText, editedText, timeText, dateText;
		ImageView profileImage;

		UserMessageHolder(View itemView) {
			super(itemView);

			nicknameText = (TextView) itemView.findViewById(R.id.text_open_chat_nickname);
			messageText = (TextView) itemView.findViewById(R.id.text_open_chat_message);
			editedText = (TextView) itemView.findViewById(R.id.text_open_chat_edited);
			timeText = (TextView) itemView.findViewById(R.id.text_open_chat_time);
			profileImage = (ImageView) itemView.findViewById(R.id.image_open_chat_profile);
			dateText = (TextView) itemView.findViewById(R.id.text_open_chat_date);
		}

		public void bind(Context context, final UserMessage message, boolean isNewDay,
				  @Nullable final OnItemClickListener clickListener,
				  @Nullable final OnItemLongClickListener longClickListener, final int postion) {

			User sender = message.getSender();

			if (sender.getUserId().equals(SendBird.getCurrentUser().getUserId())) {
				nicknameText.setTextColor(ContextCompat.getColor(context, R.color.openChatNicknameMe));
			} else {
				nicknameText.setTextColor(ContextCompat.getColor(context, R.color.openChatNicknameOther));
			}

			if (isNewDay) {
				dateText.setVisibility(View.VISIBLE);
				dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
			} else {
				dateText.setVisibility(View.GONE);
			}

			nicknameText.setText(message.getSender().getNickname());
			messageText.setText(message.getMessage());
			timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

			if (message.getUpdatedAt() > 0) {
				editedText.setVisibility(View.VISIBLE);
			} else {
				editedText.setVisibility(View.GONE);
			}

			ImageUtils.displayRoundImageFromUrl(context, message.getSender().getProfileUrl(), profileImage);

			if (clickListener != null) {
				itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						clickListener.onUserMessageItemClick(message);
					}
				});
			}

			if (longClickListener != null) {
				itemView.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						longClickListener.onBaseMessageLongClick(message, postion);
						return true;
					}
				});
			}
		}
	}

	private class AdminMessageHolder extends RecyclerView.ViewHolder {
		TextView messageText, dateText;

		AdminMessageHolder(View itemView) {
			super(itemView);

			messageText = (TextView) itemView.findViewById(R.id.text_open_chat_message);
			dateText = (TextView) itemView.findViewById(R.id.text_open_chat_date);
		}

		void bind(final AdminMessage message, boolean isNewDay, final OnItemClickListener listener) {
			messageText.setText(message.getMessage());

			if (isNewDay) {
				dateText.setVisibility(View.VISIBLE);
				dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
			} else {
				dateText.setVisibility(View.GONE);
			}

			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					listener.onAdminMessageItemClick(message);
				}
			});
		}
	}

	private class FileMessageHolder extends RecyclerView.ViewHolder {
		TextView nicknameText, timeText, fileNameText, fileSizeText, dateText;
		ImageView profileImage, fileThumbnail;

		FileMessageHolder(View itemView) {
			super(itemView);

			nicknameText = itemView.findViewById(R.id.text_open_chat_nickname);
			timeText =  itemView.findViewById(R.id.text_open_chat_time);
			profileImage =  itemView.findViewById(R.id.image_open_chat_profile);
			fileNameText = itemView.findViewById(R.id.text_open_chat_file_name);
			fileSizeText =  itemView.findViewById(R.id.text_open_chat_file_size);
			fileThumbnail =  itemView.findViewById(R.id.image_open_chat_file_thumbnail);
			dateText =  itemView.findViewById(R.id.text_open_chat_date);
		}

		void bind(final Context context, final FileMessage message, boolean isNewDay,
				  @Nullable final OnItemClickListener clickListener,
				  @Nullable final OnItemLongClickListener longClickListener, final int position) {
			User sender = message.getSender();
			if (sender.getUserId().equals(SendBird.getCurrentUser().getUserId())) {
				nicknameText.setTextColor(ContextCompat.getColor(context, R.color.openChatNicknameMe));
			} else {
				nicknameText.setTextColor(ContextCompat.getColor(context, R.color.openChatNicknameOther));
			}

			if (isNewDay) {
				dateText.setVisibility(View.VISIBLE);
				dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
			} else {
				dateText.setVisibility(View.GONE);
			}

			ImageUtils.displayRoundImageFromUrl(context, message.getSender().getProfileUrl(), profileImage);

			fileNameText.setText(message.getName());
			fileSizeText.setText(FileUtils.toReadableFileSize(message.getSize()));
			nicknameText.setText(message.getSender().getNickname());
			if (message.getType().toLowerCase().startsWith("image")) {
				ArrayList<FileMessage.Thumbnail> thumbnails = (ArrayList<FileMessage.Thumbnail>) message.getThumbnails();

				if (thumbnails.size() > 0) {
					if (message.getType().toLowerCase().contains("gif")) {
						ImageUtils.displayGifImageFromUrl(context, message.getUrl(), fileThumbnail, thumbnails.get(0).getUrl(), fileThumbnail.getDrawable());
					} else {
						ImageUtils.displayImageFromUrl(context, thumbnails.get(0).getUrl(), fileThumbnail, fileThumbnail.getDrawable());
					}
				} else {
					if (message.getType().toLowerCase().contains("gif")) {
						ImageUtils.displayGifImageFromUrl(context, message.getUrl(), fileThumbnail, (String) null, fileThumbnail.getDrawable());
					} else {
						ImageUtils.displayImageFromUrl(context, message.getUrl(), fileThumbnail, fileThumbnail.getDrawable());
					}
				}

			} else if (message.getType().toLowerCase().startsWith("video")) {
				ArrayList<FileMessage.Thumbnail> thumbnails = (ArrayList<FileMessage.Thumbnail>) message.getThumbnails();

				if (thumbnails.size() > 0) {
					ImageUtils.displayImageFromUrlWithPlaceHolder(
							context, thumbnails.get(0).getUrl(), fileThumbnail, R.drawable.ic_file_message);
				} else {
					fileThumbnail.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_play));
				}

			} else {
				fileThumbnail.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_message));
			}

			if (clickListener != null) {
				itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						clickListener.onFileMessageItemClick(message);
					}
				});
			}

			if (longClickListener != null) {
				itemView.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						longClickListener.onBaseMessageLongClick(message, position);
						return true;
					}
				});
			}

		}
	}


}
