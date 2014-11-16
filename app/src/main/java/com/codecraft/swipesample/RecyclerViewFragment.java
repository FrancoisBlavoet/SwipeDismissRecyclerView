package com.codecraft.swipesample;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codecraft.swipedismissrecyclerview.SwipeDismissRecyclerViewItemTouchListener;


public class RecyclerViewFragment extends Fragment {
    private MyAdapter mAdapter;

    public RecyclerViewFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_my, container, false);
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new SpaceItemDecoration(RecyclerView.VERTICAL, 10));

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter = new MyAdapter(getActivity()));

        SwipeDismissRecyclerViewItemTouchListener listener =
                new SwipeDismissRecyclerViewItemTouchListener(recyclerView,
                        getActivity(),
                        new SwipeDismissRecyclerViewItemTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }


                            public void onDismiss(RecyclerView recyclerView, RecyclerView.ViewHolder holder) {
                                mAdapter.remove(holder);
                            }
                        });
        recyclerView.addOnItemTouchListener(listener);
        recyclerView.setOnScrollListener(listener.makeScrollListener());

        return rootView;
    }

}