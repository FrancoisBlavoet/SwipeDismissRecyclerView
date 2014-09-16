package com.codecraft.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by François on 31/08/2014.
 */
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.TextViewHolder> {
    private List<Item> items = new ArrayList<Item>();

    public MyAdapter() {
        for (int i = 0 ; i < 20 ; i++) {
            items.add(new Item("Item n°" +i));
        }
    }

    @Override
    public TextViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_my, viewGroup, false);
        TextViewHolder holder = new TextViewHolder(view);
        view.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(TextViewHolder viewHolder, int i) {
        viewHolder.text.setText(items.get(i).getText());
        viewHolder.itemView.setAlpha(1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView text;

        public TextViewHolder(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(R.id.text);
        }


    }

    public void remove (RecyclerView.ViewHolder holder) {
        items.remove(holder.getPosition());
        notifyItemRemoved(holder.getPosition());
    }

}