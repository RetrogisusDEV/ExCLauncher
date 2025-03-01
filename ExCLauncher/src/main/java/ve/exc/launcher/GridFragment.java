package ve.exc.launcher;


/*
 * Created by Thomas Barrasso on 9/11/12.
 * Copyright (c) 2012 Loupe Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Based on {@link android.app.ListFragment} but adapted for {@link GridView}.
 */
public class GridFragment extends Fragment {

    static final int INTERNAL_EMPTY_ID = 0x00ff0001;
    static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
    static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            mGrid.focusableViewAvailable(mGrid);
        }
    };

    final private AdapterView.OnItemClickListener mOnClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            onGridItemClick((GridView) parent, v, position, id);
        }
    };

    ListAdapter mAdapter;
    GridView mGrid;
    View mEmptyView;
    TextView mStandardEmptyView;
    View mProgressContainer;
    View mGridContainer;
    CharSequence mEmptyText;
    boolean mGridShown;

    public GridFragment() { }

    /**
     * Provide default implementation to return a simple grid view.  Subclasses
     * can override to replace with their own layout.  If doing so, the
     * returned view hierarchy <em>must</em> have a GridView whose id
     * is {@link android.R.id#list android.R.id.list} and can optionally
     * have a sibling view id {@link android.R.id#empty android.R.id.empty}
     * that is to be shown when the grid is empty.
     *
     * <p>If you are overriding this method with your own custom content,
     * consider including the standard layout {@link android.R.layout#list_content}
     * in your layout file, so that you continue to retain all of the standard
     * behavior of ListFragment.  In particular, this is currently the only
     * way to have the built-in indeterminant progress state be shown.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Context context = getActivity();

        FrameLayout root = new FrameLayout(context);

        // ------------------------------------------------------------------

        LinearLayout pframe = new LinearLayout(context);
        pframe.setId(INTERNAL_PROGRESS_CONTAINER_ID);
        pframe.setOrientation(LinearLayout.VERTICAL);
        pframe.setVisibility(View.GONE);
        pframe.setGravity(Gravity.CENTER);

        ProgressBar progress = new ProgressBar(context, null,
                android.R.attr.progressBarStyleLarge);
        pframe.addView(progress, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(pframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ------------------------------------------------------------------

        FrameLayout lframe = new FrameLayout(context);
        lframe.setId(INTERNAL_LIST_CONTAINER_ID);

        TextView tv = new TextView(getActivity());
        tv.setId(INTERNAL_EMPTY_ID);
        tv.setGravity(Gravity.CENTER);
        lframe.addView(tv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        GridView lv = new GridView(getActivity());
        lv.setId(android.R.id.list);
        lv.setDrawSelectorOnTop(false);
        lv.setColumnWidth(convertDpToPixels(60, getActivity()));
        lv.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        lv.setNumColumns(GridView.AUTO_FIT);
        lv.setHorizontalSpacing(convertDpToPixels(20, getActivity()));
        lv.setVerticalSpacing(convertDpToPixels(20, getActivity()));
        lv.setSmoothScrollbarEnabled(true);

        // disable overscroll
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            lv.setOverScrollMode(ListView.OVER_SCROLL_NEVER);
        }

        lframe.addView(lv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(lframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ------------------------------------------------------------------

        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        return root;
    }

    /**
     * Attach to grid view once the view hierarchy has been created.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureGrid();
    }

    /**
     * Detach from {@link GridView}
     */
    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mGrid = null;
        mGridShown = false;
        mEmptyView = mProgressContainer = mGridContainer = null;
        mStandardEmptyView = null;
        super.onDestroyView();
    }

    public static int convertDpToPixels(float dp, Context context){
        Resources resources = context.getResources();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                resources.getDisplayMetrics()
        );
    }

    /**
     * This method will be called when an item in the grid is selected.
     * Subclasses should override. Subclasses can call
     * getGridView().getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param g The {@link GridView} where the click happened
     * @param v The view that was clicked within the {@link GridView}
     * @param position The position of the view in the grid
     * @param id The row id of the item that was clicked
     */
    public void onGridItemClick(GridView g, View v, int position, long id) {

    }

    /**
     * Provide the cursor for the {@link GridView}.
     */
    public void setGridAdapter(ListAdapter adapter) {
        final boolean hadAdapter = (mAdapter != null);
        mAdapter = adapter;
        if (mGrid != null) {
            mGrid.setAdapter(adapter);
            if (!mGridShown && !hadAdapter) {
                // The grid was hidden, and previously didn't have an
                // adapter.  It is now time to show it.
                setGridShown(true, (getView().getWindowToken() != null));
            }
        }
    }

    /**
     * Set the currently selected grid item to the specified
     * position with the adapter's data
     *
     * @param position
     */
    public void setSelection(int position) {
        ensureGrid();
        mGrid.setSelection(position);
    }

    /**
     * Get the position of the currently selected grid item.
     */
    public int getSelectedItemPosition() {
        ensureGrid();
        return mGrid.getSelectedItemPosition();
    }

    /**
     * Get the cursor row ID of the currently selected grid item.
     */
    public long getSelectedItemId() {
        ensureGrid();
        return mGrid.getSelectedItemId();
    }

    /**
     * Get the activity's {@link GridView} widget.
     */
    public GridView getGridView() {
        ensureGrid();
        return mGrid;
    }

    /**
     * The default content for a ListFragment has a TextView that can
     * be shown when the grid is empty.  If you would like to have it
     * shown, call this method to supply the text it should use.
     */
    public void setEmptyText(CharSequence text) {
        ensureGrid();
        if (mStandardEmptyView == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        mStandardEmptyView.setText(text);
        if (mEmptyText == null) {
            mGrid.setEmptyView(mStandardEmptyView);
        }
        mEmptyText = text;
    }

    /**
     * Control whether the grid is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     *
     * <p>Applications do not normally need to use this themselves.  The default
     * behavior of ListFragment is to start with the grid not being shown, only
     * showing it once an adapter is given with {@link #setGridAdapter(ListAdapter)}.
     * If the grid at that point had not been shown, when it does get shown
     * it will be do without the user ever seeing the hidden state.
     *
     * @param shown If true, the grid view is shown; if false, the progress
     * indicator.  The initial value is true.
     */
    public void setGridShown(boolean shown) {
        setGridShown(shown, true);
    }

    /**
     * Like {@link #setGridShown(boolean)}, but no animation is used when
     * transitioning from the previous state.
     */
    public void setGridShownNoAnimation(boolean shown) {
        setGridShown(shown, false);
    }

    /**
     * Control whether the grid is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     *
     * @param shown If true, the grid view is shown; if false, the progress
     * indicator.  The initial value is true.
     * @param animate If true, an animation will be used to transition to the
     * new state.
     */
    private void setGridShown(boolean shown, boolean animate) {
        ensureGrid();
        if (mProgressContainer == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        if (mGridShown == shown) {
            return;
        }
        mGridShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mGridContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            } else {
                mProgressContainer.clearAnimation();
                mGridContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mGridContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mGridContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            } else {
                mProgressContainer.clearAnimation();
                mGridContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mGridContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Get the ListAdapter associated with this activity's {@link GridView}.
     */
    public ListAdapter getGridAdapter() {
        return mAdapter;
    }

    private void ensureGrid() {
        if (mGrid != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        if (root instanceof GridView) {
            mGrid = (GridView) root;
        } else {
            mStandardEmptyView = (TextView)root.findViewById(INTERNAL_EMPTY_ID);
            if (mStandardEmptyView == null) {
                mEmptyView = root.findViewById(android.R.id.empty);
            } else {
                mStandardEmptyView.setVisibility(View.GONE);
            }
            mProgressContainer = root.findViewById(INTERNAL_PROGRESS_CONTAINER_ID);
            mGridContainer = root.findViewById(INTERNAL_LIST_CONTAINER_ID);
            View rawGridView = root.findViewById(android.R.id.list);
            if (!(rawGridView instanceof GridView)) {
                if (rawGridView == null) {
                    throw new RuntimeException(
                            "Your content must have a GridView whose id attribute is " +
                                    "'android.R.id.list'");
                }
                throw new RuntimeException(
                        "Content has view with id attribute 'android.R.id.list' "
                                + "that is not a GridView class");
            }
            mGrid = (GridView) rawGridView;
            if (mEmptyView != null) {
                mGrid.setEmptyView(mEmptyView);
            } else if (mEmptyText != null) {
                mStandardEmptyView.setText(mEmptyText);
                mGrid.setEmptyView(mStandardEmptyView);
            }
        }
        mGridShown = true;
        mGrid.setOnItemClickListener(mOnClickListener);
        if (mAdapter != null) {
            ListAdapter adapter = mAdapter;
            mAdapter = null;
            setGridAdapter(adapter);
        } else {
            // We are starting without an adapter, so assume we won't
            // have our data right away and start with the progress indicator.
            if (mProgressContainer != null) {
                setGridShown(false, false);
            }
        }
        mHandler.post(mRequestFocus);
    }
}
