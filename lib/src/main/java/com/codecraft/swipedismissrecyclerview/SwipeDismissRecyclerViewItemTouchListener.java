/*
 * Copyright 2014 François Blavoet
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codecraft.swipedismissrecyclerview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * A {@link RecyclerView.OnItemTouchListener} that makes the list items in a {@link android.support.v7.widget.RecyclerView}
 * dismissable.
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * SwipeDismissRecyclerViewTouchListener touchListener =
 *         new SwipeDismissRecyclerViewTouchListener(
 *                 recyclerView,
 *                 new SwipeDismissRecyclerViewTouchListener.DismissCallbacks() {
 *                     @Override
 *                     public boolean canDismiss(int position) {
 *                         return true;
 *                     }
 *
 *                     @Override
 *                     public void onDismiss(RecyclerView recyclerView, RecyclerView.ViewHolder holder) {
 *                         mAdapter.remove(holder);
 *                     }
 *                 });
 * recyclerView.setOnTouchListener(touchListener);
 * recyclerView.setOnScrollListener(touchListener.makeScrollListener());
 * </pre>
 *
 * <p>This class Requires API level 12 or later due to use of {@link
 * android.view.ViewPropertyAnimator}.</p>
 *
 */
public class SwipeDismissRecyclerViewItemTouchListener implements RecyclerView.OnItemTouchListener {


    /**
     * The callback interface used by {@link SwipeDismissRecyclerViewItemTouchListener} to inform its client
     * about a successful dismissal of one or more list item positions.
     * Also used to inform the client when a list item is clicked.
     */
    public interface DismissCallbacks {
        /**
         * Called to determine whether the given position can be dismissed.
         */
        boolean canDismiss(int position);

        /**
         * Called when the user has indicated they she would like to dismiss one or more list item
         * positions.
         *
         * @param recyclerView the originating {@link android.support.v7.widget.RecyclerView}.
         * @param holder the {@link android.support.v7.widget.RecyclerView.ViewHolder} corresponding to the item being dismissed
         */
        void onDismiss(RecyclerView recyclerView, RecyclerView.ViewHolder holder);

    }

    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;


    // Fixed properties
    private final DismissCallbacks mCallbacks;
    private final RecyclerView mRecyclerView;

    // Transient properties
    private int mPointerId;
    private float mDownX;
    private float mDownY;
    private boolean mSwiping;
    private int mSwipingSlop;
    private VelocityTracker mVelocityTracker;
    private int mDownPosition;
    private RecyclerView.ViewHolder mDownHolder;
    private View mDownView;
    private int mDownViewWidth = 1; // 1 and not 0 to prevent dividing by zero
    private boolean mPaused;


    public SwipeDismissRecyclerViewItemTouchListener(RecyclerView recyclerView, Context context, DismissCallbacks callbacks) {
        ViewConfiguration vc = ViewConfiguration.get(context);
        mRecyclerView = recyclerView;
        mCallbacks = callbacks;
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = context.getResources().getInteger(
                android.R.integer.config_shortAnimTime);
    }


    /**
     * Enables or disables (pauses or resumes) watching for swipe-to-dismiss gestures.
     *
     * @param enabled Whether or not to watch for gestures.
     */
    public void setSwipeDismissEnabled(boolean enabled) {
        mPaused = !enabled;
    }




    public RecyclerView.OnScrollListener makeScrollListener() {

        return new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                setSwipeDismissEnabled(newState != RecyclerView.SCROLL_STATE_DRAGGING);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            }
        };
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        return handleTouch(motionEvent);
    }

    @Override
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        handleTouch(motionEvent);
    }

    private boolean handleTouch( MotionEvent motionEvent) {

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (mPaused) {
                    return false;
                }

                // Find the child view that was touched (perform a hit test)
                Rect rect = new Rect();
                int childCount = mRecyclerView.getChildCount();
                int[] coords = new int[2];
                mRecyclerView.getLocationOnScreen(coords);
                int x = (int) motionEvent.getRawX() - coords[0];
                int y = (int) motionEvent.getRawY() - coords[1];
                View child;
                for (int i = 0; i < childCount; i++) {
                    child = mRecyclerView.getChildAt(i);
                    child.getHitRect(rect);
                    if (rect.contains(x, y)) {
                        mDownView = child;
                        break;
                    }
                }

                if (mDownView != null) {
                    mPointerId = motionEvent.getPointerId(motionEvent.getActionIndex()); // TODO check on on Down, reinit on on UP

                    mDownX = motionEvent.getRawX();
                    mDownY = motionEvent.getRawY();
                    mDownPosition = mRecyclerView.getChildPosition(mDownView);
                    mDownViewWidth = mDownView.getWidth();
                    mDownHolder = mRecyclerView.getChildViewHolder(mDownView);
                    mDownHolder.setIsRecyclable(false);
                    if (mCallbacks.canDismiss(mDownPosition)) {
                        mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.addMovement(motionEvent);
                    } else {
                        mDownView = null;
                    }
                }
                return false;
            }

            case MotionEvent.ACTION_CANCEL: {
                if (mVelocityTracker == null) {
                    break;
                }

                if (mDownView != null && mSwiping) {
                    // cancel
                    mDownView.animate()
                            .translationX(0)
                            .alpha(1)
                            .setDuration(mAnimationTime)
                            .setListener(null);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownY = 0;
                mDownView = null;
                mDownPosition = RecyclerView.NO_POSITION;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null
                        || mPointerId != motionEvent.getPointerId(motionEvent.getActionIndex())) {
                    break;
                }

                float deltaX = motionEvent.getRawX() - mDownX;
                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = mVelocityTracker.getXVelocity();
                float absVelocityX = Math.abs(velocityX);
                float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                boolean dismiss = false;
                boolean dismissRight = false;
                if (Math.abs(deltaX) > mDownView.getWidth() / 2 && mSwiping) {
                    dismiss = true;
                    dismissRight = deltaX > 0;
                } else if (mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity
                        && absVelocityY < absVelocityX && mSwiping) {
                    // dismiss only if flinging in the same direction as dragging
                    dismiss = (velocityX < 0) == (deltaX < 0);
                    dismissRight = mVelocityTracker.getXVelocity() > 0;
                }
                if (dismiss && mDownPosition != RecyclerView.NO_POSITION) {
                    // dismiss
                    final RecyclerView.ViewHolder viewHolder = mDownHolder;
                    final View animatedView = mDownView;
                    mDownView.animate()
                            .translationX(dismissRight ? mDownViewWidth : -mDownViewWidth)
                            .alpha(0)
                            .setDuration(mAnimationTime)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    animatedView.setTranslationX(0);
                                    viewHolder.setIsRecyclable(true);
                                    mCallbacks.onDismiss(mRecyclerView, viewHolder);
                                }
                            });
                } else {
                    // cancel
                    mDownView.animate()
                            .translationX(0)
                            .alpha(1)
                            .setDuration(mAnimationTime)
                            .setListener(null);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownY = 0;
                mDownView = null;
                mDownHolder = null;
                mDownPosition = RecyclerView.NO_POSITION;
                mPointerId = MotionEvent.INVALID_POINTER_ID;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null || mPaused
                        || mPointerId != motionEvent.getPointerId(motionEvent.getActionIndex())) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - mDownX;
                float deltaY = motionEvent.getRawY() - mDownY;
                if (Math.abs(deltaX) > mSlop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
                    mSwiping = true;
                    mSwipingSlop = (deltaX > 0 ? mSlop : -mSlop);
                }

                if (mSwiping) {
                    mDownView.setTranslationX(deltaX - mSwipingSlop);
                    mDownView.setAlpha(Math.max(0f, Math.min(1f,
                            1f - 2f * Math.abs(deltaX) / mDownViewWidth)));
                    return true;
                }
                break;
            }
        }
        return false;
    }

}
