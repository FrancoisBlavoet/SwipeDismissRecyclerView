package com.codecraft.recyclerview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;

/**
 * A {@link android.view.View.OnTouchListener} that makes the list items in a {@landroid.support.v7.widget.RecyclerView}
 * dismissable.
 *
 * <p>After creating the listener, the caller should also call
 * {@android.support.v7.widget.RecyclerView#setOnScrollListener(android.support.v7.widget.RecyclerView.OnScrollListener)}, passing
 * in the scroll listener returned by {@link #makeScrollListener()}. If a scroll listener is
 * already assigned, the caller should still pass scroll changes through to this listener. This will
 * ensure that this {@link SwipeDismissRecyclerViewTouchListener} is paused during scrolling.</p>
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
 * <p>For a generalized {@link android.view.View.OnTouchListener} that makes any view dismissable,
 * see {@link SwipeDismissTouchListener}.</p>
 *
 * @see SwipeDismissTouchListener
 */
public class SwipeDismissRecyclerViewTouchListener implements View.OnTouchListener {
    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    // Fixed properties
    private RecyclerView mRecyclerView;
    private DismissCallbacks mCallbacks;
    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

    // Transient properties
    private float mDownX;
    private float mDownY;
    private boolean mSwiping;
    private int mSwipingSlop;
    private VelocityTracker mVelocityTracker;
    private int mDownPosition;
    private RecyclerView.ViewHolder mDownHolder;
    private View mDownView;
    private boolean mPaused;
    private boolean mIsDismissiblePosition;

    /**
     * The callback interface used by {@link SwipeDismissRecyclerViewTouchListener} to inform its client
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

        /**
         * called when the user has clicked on one the list items
         * @param recyclerView  The originating {@link android.support.v7.widget.RecyclerView}.
         * @param holder the {@link android.support.v7.widget.RecyclerView.ViewHolder} corresponding to the item being click
         */
        void onClick(RecyclerView recyclerView, RecyclerView.ViewHolder holder);
    }

    /**
     * Constructs a new swipe-to-dismiss touch listener for the given list view.
     *
     * @param recyclerView  The RecyclerView whose items should be dismissible.
     * @param callbacks The callback to trigger when the user has indicated that she would like to
     *                  dismiss one or more list items.
     */
    public SwipeDismissRecyclerViewTouchListener(RecyclerView recyclerView, DismissCallbacks callbacks) {
        ViewConfiguration vc = ViewConfiguration.get(recyclerView.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = recyclerView.getContext().getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        mRecyclerView = recyclerView;
        mCallbacks = callbacks;
    }

    /**
     * Enables or disables (pauses or resumes) watching for swipe-to-dismiss gestures.
     *
     * @param enabled Whether or not to watch for gestures.
     */
    public void setEnabled(boolean enabled) {
        mPaused = !enabled;
    }

    /**
     * Returns an {@link android.support.v7.widget.RecyclerView.OnScrollListener} to be added to the {@link
     * android.support.v7.widget.RecyclerView} using {@link android.support.v7.widget.RecyclerView#setOnScrollListener(android.support.v7.widget.RecyclerView.OnScrollListener)}.
     * If a scroll listener is already assigned, the caller should still pass scroll changes through
     * to this listener. This will ensure that this {@link com.codecraft.recyclerview.SwipeDismissRecyclerViewTouchListener} is
     * paused during list view scrolling.</p>
     *
     * @see SwipeDismissRecyclerViewTouchListener
     */
    public RecyclerView.OnScrollListener makeScrollListener() {
        return new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(int newState) {
                setEnabled(newState != RecyclerView.SCROLL_STATE_DRAGGING);
            }

            @Override
            public void onScrolled(int dx, int dy) {

            }
        };
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (mViewWidth < 2) {
            mViewWidth = mRecyclerView.getWidth();
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (mPaused) {
                    return false;
                }

                // TODO: ensure this is a finger, and set a flag

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
                    mDownX = motionEvent.getRawX();
                    mDownView.setPressed(true);
                    mDownY = motionEvent.getRawY();
                    mDownPosition = mRecyclerView.getChildPosition(mDownView);
                    mDownHolder = mRecyclerView.getChildViewHolder(mDownView);
                    mIsDismissiblePosition = mCallbacks.canDismiss(mDownPosition);
                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(motionEvent);
                }
                return false;
            }

            case MotionEvent.ACTION_CANCEL: {
                if (mVelocityTracker == null) {
                    break;
                }

                if (mDownView != null) {
                    mDownView.setPressed(false);
                    if (mSwiping) {
                        // cancel
                        mDownView.animate()
                                .translationX(0)
                                .alpha(1)
                                .setDuration(mAnimationTime)
                                .setListener(null);
                    }
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
                if (mVelocityTracker == null) {
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
                if (Math.abs(deltaX) > mViewWidth / 2 && mSwiping) {
                    dismiss = true;
                    dismissRight = deltaX > 0;
                } else if (mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity
                        && absVelocityY < absVelocityX && mSwiping) {
                    // dismiss only if flinging in the same direction as dragging
                    dismiss = (velocityX < 0) == (deltaX < 0);
                    dismissRight = mVelocityTracker.getXVelocity() > 0;
                }
                if (dismiss && mDownPosition != RecyclerView.NO_POSITION
                        && mIsDismissiblePosition) {
                    mDownView.setPressed(false);
                    // dismiss
                    final RecyclerView.ViewHolder viewHolder = mDownHolder;
                    mDownView.animate()
                            .translationX(dismissRight ? mViewWidth : -mViewWidth)
                            .alpha(0)
                            .setDuration(mAnimationTime)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mCallbacks.onDismiss(mRecyclerView, viewHolder);
                                }
                            });
                } else {
                    mDownView.setPressed(false);
                    // cancel
                    mDownView.animate()
                            .translationX(0)
                            .alpha(1)
                            .setDuration(mAnimationTime)
                            .setListener(null);

                    if (!mSwiping  && !mPaused) {
                        mDownView.performClick();
                        mDownView.playSoundEffect(SoundEffectConstants.CLICK);
                        mCallbacks.onClick(mRecyclerView, mDownHolder);
                    }

                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownY = 0;
                mDownView = null;
                mDownHolder = null;
                mDownPosition = RecyclerView.NO_POSITION;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null) {
                    break;
                }
                if (mPaused) {
                    if (mDownView != null) {
                        mDownView.setPressed(false);
                    }
                    break;
                }

                if (!mIsDismissiblePosition) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - mDownX;
                float deltaY = motionEvent.getRawY() - mDownY;
                if (Math.abs(deltaX) > mSlop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
                    mSwiping = true;
                    mSwipingSlop = (deltaX > 0 ? mSlop : -mSlop);
                    mRecyclerView.requestDisallowInterceptTouchEvent(true);

                    // Cancel RecyclerView's touch (un-highlighting the item)
                    MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (motionEvent.getActionIndex()
                                    << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    mRecyclerView.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }

                if (mSwiping) {
                    mDownView.setTranslationX(deltaX - mSwipingSlop);
                    mDownView.setAlpha(Math.max(0f, Math.min(1f,
                            1f - 2f * Math.abs(deltaX) / mViewWidth)));
                    return true;
                }
                break;
            }
        }
        return false;
    }

}
