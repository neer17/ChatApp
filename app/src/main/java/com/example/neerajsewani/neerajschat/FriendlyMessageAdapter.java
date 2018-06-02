package com.example.neerajsewani.neerajschat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class FriendlyMessageAdapter extends ArrayAdapter<FriendlyMessage> {

    FriendlyMessageAdapter(Context context, List<FriendlyMessage> listItem){
        super(context, 0, listItem);
    }


    @Override
    public View getView(int position, View convertView,  ViewGroup parent) {
        View listItemView = convertView;

        if(listItemView == null){
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.item_layout, parent, false);
        }


        TextView messageTextView = listItemView.findViewById(R.id.textView);
        TextView usrnameTextView = listItemView.findViewById(R.id.textView2);
        ImageView imageView = listItemView.findViewById(R.id.imageView);

        FriendlyMessage currentMessage = getItem(position);

        boolean isPhoto = currentMessage.getmUrl() != null;

        if(isPhoto){
            Glide.with(imageView.getContext())
                    .load(currentMessage.getmUrl())
                    .into(imageView);

            imageView.setVisibility(View.VISIBLE);
            messageTextView.setVisibility(View.GONE);

        }
        else {
            messageTextView.setVisibility(View.VISIBLE);
            usrnameTextView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            messageTextView.setText(currentMessage.getmMessage());


        }
        usrnameTextView.setText(currentMessage.getmUsername());
        return listItemView;
    }
}
