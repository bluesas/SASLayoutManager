package sas.layoutmanager;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import java.util.ArrayList;

public class TopBottomLinearLayoutManager extends LinearLayoutManager {

    protected OrientationHelper mOrientationHelper;

    protected ArrayList<Integer> topPositionList = new ArrayList<>();
    protected SparseArray<View> currentTopViewList = new SparseArray<>();

    public TopBottomLinearLayoutManager(Context context) {
        super(context);
        topPositionList.add(1);
        topPositionList.add(3);
    }

    public TopBottomLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public TopBottomLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(orientation);
        mOrientationHelper = OrientationHelper.createOrientationHelper(this, orientation);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        layoutTopViews(recycler, findFirstVisibleItemPosition(), findLastVisibleItemPosition());
    }

    protected void layoutTopViews(RecyclerView.Recycler recycler, int firstVisiblePosition, int lastVisiblePosition) {
        int start = 0;
        SparseArray<Integer> rangeArray = new SparseArray<>();
        int first = firstVisiblePosition;
        int last = lastVisiblePosition;
        for (int position : topPositionList) {
            rangeArray.put(position, getRange(position, first, last));
        }
        for (int position : topPositionList) {

            View topView = null;
            int positionArea = rangeArray.get(position);
            if (positionArea == RANGE_IN) {
                topView = findViewByPositionTraversal(position);
                if (topView == null) {
                    topView = recycler.getViewForPosition(position);
                }
                if (topView == null) {
                    continue;
                }
                int topPos = mOrientationHelper.getDecoratedStart(topView);
                if (topPos <= start) {
                    start += setViewToTop(start, topView);
                    currentTopViewList.put(position, topView);
                }
            } else if (positionArea == RANGE_OUT_UP) {
                topView = recycler.getViewForPosition(position);
                start += setViewToTop(start, topView);
                currentTopViewList.put(position, topView);
            }
        }

        topSpaceUsed = start;
    }

    protected int setViewToTop(int currentTopBottom, View topView) {
        addView(topView);
        topView.setBackgroundColor(Color.parseColor("#40ff0000"));
        measureChildWithMargins(topView, 0, 0);
        int height = mOrientationHelper.getDecoratedMeasurement(topView);

        int left = 0;
        int right = getWidth();
        int top = currentTopBottom;
        int bottom = top + height;
        layoutDecorated(topView, left, top, right, bottom);
        return height;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        Pair<Integer, Integer> firstAndLastPair = findFirstAndLastVisibleItemPosition();
        removeAndRecycleViews(currentTopViewList, recycler);
        ArrayList<View> viewsCovered = findViewCoveredByTopViews(firstAndLastPair.first, firstAndLastPair.second);
        if (viewsCovered != null && !viewsCovered.isEmpty()) {
            removeAndRecycleViews(viewsCovered, recycler);
        }
        int res = super.scrollVerticallyBy(dy, recycler, state);

        layoutTopViews(recycler, firstAndLastPair.first, firstAndLastPair.second);

        return res;
    }

    /**
     * Should call after remove top views
     * @param first
     * @param last
     */
    protected ArrayList<View> findViewCoveredByTopViews(int first, int last) {
        if (first > last) {
            return null;
        }
        ArrayList<View> views = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            int bottom = mOrientationHelper.getDecoratedEnd(view);
            if (bottom < topSpaceUsed) {
                views.add(view);
            }
        }

        return views;
    }

    protected int topSpaceUsed = 0;
    @Override
    public int getPaddingTop() {
        return super.getPaddingTop();
    }

    protected int bottomSpaceUsed = 0;
    @Override
    public int getPaddingBottom() {
        return super.getPaddingBottom();
    }

    protected SparseArray<View> layoutAndScrollTopViews(int scrollOffset, RecyclerView.Recycler recycler, ArrayList<Integer> viewPositions, int first, int last) {
        int start = 0;
        SparseArray<View> topViews = new SparseArray<>();
        if (viewPositions == null || viewPositions.isEmpty()) {
            return topViews;
        }
        for (int pos : viewPositions) {
            View view = null;
            if (pos <= first) {
                view = recycler.getViewForPosition(pos);
                if (view != null) {
                    start += setViewToTop(scrollOffset, view);
                    topViews.put(pos, view);
                }
            } else if (pos <= last){
                view = findViewByPositionTraversal(pos);
                if (view == null) {
                    continue;
                }
                int topPos = mOrientationHelper.getDecoratedStart(view);
                if (topPos < start) {
                    mOrientationHelper.offsetChild(view, -scrollOffset);
                    start += mOrientationHelper.getDecoratedMeasurement(view);
                    topViews.put(pos, view);
                }
            }
        }

        return topViews;
    }

    public View findViewByPositionTraversal(int position) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (getPosition(child) == position) {
                return child;
            }
        }
        return null;
    }

    public SparseArray<View> findItemPositionArray() {
        SparseArray<View> viewIndexArray = new SparseArray<>();
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            viewIndexArray.put(getPosition(view), view);
        }
        return viewIndexArray;
    }

    @Override
    public int findFirstVisibleItemPosition() {
        if (currentTopViewList != null && currentTopViewList.size() >= 0) {
            SparseArray<View> viewSparseArray = findItemPositionArray();
            for (int i = 0; i < viewSparseArray.size(); i++) {
                int pos = viewSparseArray.keyAt(i);
                if (currentTopViewList.indexOfKey(pos) < 0) {
                    return pos;
                }
            }
        }
        return super.findFirstVisibleItemPosition();
    }

    @Override
    public int findFirstCompletelyVisibleItemPosition() {
        if (currentTopViewList != null && currentTopViewList.size() >= 0) {
            SparseArray<View> viewSparseArray = findItemPositionArray();
            for (int i = 0; i < viewSparseArray.size(); i++) {
                int pos = viewSparseArray.keyAt(i);
                View view = viewSparseArray.valueAt(i);
                if (currentTopViewList.indexOfKey(pos) < 0
                        && mOrientationHelper.getDecoratedStart(view) >= 0) {
                    return pos;
                }
            }
        }

        return super.findFirstCompletelyVisibleItemPosition();
    }

    @Override
    public int findLastVisibleItemPosition() {
        if (currentTopViewList != null && currentTopViewList.size() >= 0) {
            SparseArray<View> viewSparseArray = findItemPositionArray();
            for (int i = viewSparseArray.size() - 1; i >= 0; i--) {
                int pos = viewSparseArray.keyAt(i);
                if (currentTopViewList.indexOfKey(pos) < 0) {
                    return pos;
                }
            }
        }
        return super.findLastVisibleItemPosition();
    }

    @Override
    public int findLastCompletelyVisibleItemPosition() {
        if (currentTopViewList != null && currentTopViewList.size() >= 0) {
            SparseArray<View> viewSparseArray = findItemPositionArray();
            for (int i = viewSparseArray.size() - 1; i >= 0; i--) {
                int pos = viewSparseArray.keyAt(i);
                View view = viewSparseArray.valueAt(i);
                if (currentTopViewList.indexOfKey(pos) < 0
                        && mOrientationHelper.getDecoratedEnd(view) <= getHeight()) {
                    return pos;
                }
            }
        }
        return super.findLastCompletelyVisibleItemPosition();
    }

    public ArrayList<View> findViewListByPositionListTraversal(ArrayList<Integer> positionList) {
        final int childCount = getChildCount();
        ArrayList<View> resultList = new ArrayList<>();
        if (positionList != null && !positionList.isEmpty()) {
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (positionList.contains(getPosition(child))) {
                    resultList.add(child);
                }
            }
        }
        return resultList;
    }

    protected Pair<Integer, Integer> findFirstAndLastVisibleItemPosition() {
        SparseArray<View> viewSparseArray = findItemPositionArray();
        int first = -1;
        int last = -1;
        for (int i = 0; i < viewSparseArray.size(); i++) {
            int pos = viewSparseArray.keyAt(i);
            if (currentTopViewList.indexOfKey(pos) < 0) {
                if (first > pos || first < 0) {
                    first = pos;
                }
                if (last < pos || last < 0) {
                    last = pos;
                }
            }
        }

        return new Pair<>(first, last);
    }

    protected void removeAndRecycleViews(SparseArray<View> views, RecyclerView.Recycler recycler) {
        if (views == null || views.size() <= 0) {
            return;
        }
        for (int i = 0; i < views.size(); i++) {
//            removeAndRecycleView(views.valueAt(i), recycler);
            removeView(views.valueAt(i));
        }

    }

    protected void removeAndRecycleViews(ArrayList<View> views, RecyclerView.Recycler recycler) {
        if (views == null || views.size() <= 0) {
            return;
        }
        for (int i = 0; i < views.size(); i++) {
//            removeAndRecycleView(views.get(i), recycler);
            removeView(views.get(i));
        }

    }

    protected SparseArray<View> getOutTopViews(SparseArray<View> views, int first, int last) {
        SparseArray<View> outViews = new SparseArray<>();
        for (int i = 0; i < views.size(); i++) {
            int position = views.keyAt(i);
            View view = views.valueAt(i);
            if (position < first || position > last) {
                outViews.put(i, view);
            }
        }
        return outViews;
    }

    protected static final int RANGE_IN = 0;
    protected static final int RANGE_OUT_UP = 1;
    protected static final int RANGE_OUT_DOWN = 2;

    protected int getRange(int position, int first, int last) {
        if (position < first) {
            return RANGE_OUT_UP;
        } else if (position > last) {
            return RANGE_OUT_DOWN;
        } else {
            return RANGE_IN;
        }
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        super.onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsAdded(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsRemoved(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsUpdated(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
        super.onItemsUpdated(recyclerView, positionStart, itemCount, payload);
    }

    @Override
    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        super.onItemsMoved(recyclerView, from, to, itemCount);
    }

}
