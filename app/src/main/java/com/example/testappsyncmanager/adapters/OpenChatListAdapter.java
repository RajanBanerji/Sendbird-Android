package com.example.testappsyncmanager.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testappsyncmanager.R;
import com.sendbird.android.OpenChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OpenChatListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private List<OpenChannel> mChannelList;
	private Context mContext;
	private OnItemClickListener mItemClickListener;
	private OnItemLongClickListener mItemLongClickListener;

	public interface OnItemClickListener {
		void onItemClick(OpenChannel channel);
	}

	public interface OnItemLongClickListener {
		void onItemLongPress(OpenChannel channel);
	}

	public OpenChatListAdapter(Context context) {
		mContext = context;
		mChannelList = new ArrayList<>();
	}

	@Override
	public ChannelHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.activity_open_group_list_item, parent, false);
		Random rnd = new Random();
		int currentStrokeColor = Color.argb(25, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
		view.setBackgroundColor(currentStrokeColor);
		return new ChannelHolder(view);
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		((ChannelHolder) holder).bind(mContext, mChannelList.get(position), position, mItemClickListener, mItemLongClickListener);
	}

	@Override
	public int getItemCount() {
		return mChannelList.size();
	}

	public void setOpenChannelList(List<OpenChannel> channelList) {
		mChannelList = channelList;
		notifyDataSetChanged();
	}

	public void addLast(OpenChannel channel) {
		mChannelList.add(channel);
		notifyDataSetChanged();
	}

	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		mItemLongClickListener = listener;
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		mItemClickListener = listener;
	}

	private class ChannelHolder extends RecyclerView.ViewHolder {
		private String[] colorList = {"#ff2de3e1", "#ff35a3fb", "#ff805aff", "#ffcf47fb", "#ffe248c3"};

		TextView nameText, participantCountText;
		ImageView coloredDecorator;

		ChannelHolder(View itemView) {
			super(itemView);
			nameText = itemView.findViewById(R.id.text_open_channel_list_name);
			participantCountText = itemView.findViewById(R.id.text_open_channel_list_participant_count);
			coloredDecorator = itemView.findViewById(R.id.image_open_channel_list_decorator);
		}

		void bind(final Context context, final OpenChannel channel, int position, @Nullable final OnItemClickListener clickListener, @Nullable final OnItemLongClickListener longClickListener) {
			nameText.setText(channel.getName());

			String participantCount = String.format(context.getResources()
					.getString(R.string.participant_count), channel.getParticipantCount());
			participantCountText.setText(participantCount);

			coloredDecorator.setBackgroundColor(Color.parseColor(colorList[position % colorList.length]));

			if (clickListener != null) {
				itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						clickListener.onItemClick(channel);
					}
				});
			}

			if (longClickListener != null) {
				itemView.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						longClickListener.onItemLongPress(channel);

						return true;
					}
				});
			}
		}

	}
}
