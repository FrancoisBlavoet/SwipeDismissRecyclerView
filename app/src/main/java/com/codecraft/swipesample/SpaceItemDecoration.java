package com.codecraft.swipesample;


import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import static android.support.v7.widget.RecyclerView.HORIZONTAL;
import static android.support.v7.widget.RecyclerView.VERTICAL;

/**
 * @author François
 *         Simple ItemDecoration that only offsets items
 */
public class SpaceItemDecoration extends RecyclerView.ItemDecoration {

    private int mOrientation;
    private int mGapWidth;


    /**
     * @param orientation , one of {@link LinearLayoutManager#HORIZONTAL}, {@link LinearLayoutManager#VERTICAL}
     */
    public SpaceItemDecoration(int orientation, int gapWidth) {
        setOrientation(orientation);
        setGapWidth(gapWidth);
    }

    /**
     * @param orientation , one of {@link LinearLayoutManager#HORIZONTAL}, {@link LinearLayoutManager#VERTICAL}
     */
    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("invalid orientation");
        }
        mOrientation = orientation;
    }

    /**
     * @param gapWidth width of the gap between items, in px. Must be positive
     */
    public void setGapWidth(int gapWidth) {
        if (gapWidth < 0) {
            throw new IllegalArgumentException("invalid width");
        }
        mGapWidth = gapWidth;
    }

    @Override
    public void getItemOffsets(Rect outRect, int itemPosition, RecyclerView parent) {
        if (mOrientation == VERTICAL) {
            outRect.set(0, 0, 0, mGapWidth);
        } else {
            outRect.set(0, 0, mGapWidth, 0);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == VERTICAL) {
            outRect.set(0, 0, 0, mGapWidth);
        } else {
            outRect.set(0, 0, mGapWidth, 0);
        }
    }
}