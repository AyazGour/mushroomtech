package com.mushroomtech.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    
    private List<ChatMessage> chatMessages;
    private SimpleDateFormat timeFormat;
    
    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        
        holder.senderName.setText(message.getSender());
        holder.messageText.setText(message.getMessage());
        holder.timestamp.setText(timeFormat.format(new Date(message.getTimestamp())));
        
        // Style based on sender
        if (message.isFromUser()) {
            holder.itemView.setBackgroundResource(R.drawable.user_message_background);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.ai_message_background);
        }
    }
    
    @Override
    public int getItemCount() {
        return chatMessages.size();
    }
    
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView senderName;
        TextView messageText;
        TextView timestamp;
        
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            senderName = itemView.findViewById(R.id.senderName);
            messageText = itemView.findViewById(R.id.messageText);
            timestamp = itemView.findViewById(R.id.timestamp);
        }
    }
} 