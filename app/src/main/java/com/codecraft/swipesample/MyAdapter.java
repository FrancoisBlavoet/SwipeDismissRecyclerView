package com.codecraft.swipesample;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.codecraft.swipedismissrecyclerview.SwipeDismissTouchListener;

import java.util.ArrayList;
import java.util.List;


public class MyAdapter extends RecyclerView.Adapter<MyAdapter.TextViewHolder> implements SwipeDismissTouchListener.DismissCallbacks {
    private final Context mContext;
    private List<Item> items = new ArrayList<Item>();

    public MyAdapter(Context context) {
        for (int i = 0 ; i < 100 ; i++) {
            items.add(new Item("Item n°" +i));
        }
        mContext = context;
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

    @Override
    public boolean canDismiss(Object token) {
        return true;
    }

    @Override
    public void onDismiss(View view, Object token) {
        items.remove(((TextViewHolder)token).getPosition());
        notifyItemRemoved(((TextViewHolder)token).getPosition());
    }

    public class TextViewHolder extends RecyclerView.ViewHolder  {
        public TextView text;

        public TextViewHolder(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(R.id.text);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(mContext,
                            "clicked " + text.getText(),
                            Toast.LENGTH_SHORT).show();
                }
            });
           // itemView.setOnTouchListener(new SwipeDismissTouchListener(itemView,this, MyAdapter.this));
        }


    }

    public void remove (RecyclerView.ViewHolder holder) {
        items.remove(holder.getPosition());
        notifyItemRemoved(holder.getPosition());
    }

}