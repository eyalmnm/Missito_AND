package ch.mitto.missito.util;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import ch.mitto.missito.ui.common.fastscroll.RecyclerViewFastScroller;

public class MissitoLinearLayoutManager extends LinearLayoutManager {

    private RecyclerView.Adapter adapter;
    private RecyclerViewFastScroller fastScroller;

    public MissitoLinearLayoutManager(Context context, int orientation, boolean reverseLayout, RecyclerViewFastScroller fastScroller, RecyclerView.Adapter adapter) {
        super(context, orientation, reverseLayout);
        this.adapter = adapter;
        this.fastScroller = fastScroller;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        //TODO if the items are filtered, considered hiding the fast scroller here
        final int firstVisibleItemPosition = findFirstVisibleItemPosition();
        if (firstVisibleItemPosition != 0) {
            // this avoids trying to handle un-needed calls
            if (firstVisibleItemPosition == -1)
                //not initialized, or no items shown, so hide fast-scroller
                fastScroller.setVisibility(View.GONE);
            return;
        }
        final int lastVisibleItemPosition = findLastVisibleItemPosition();
        int itemsShown = lastVisibleItemPosition - firstVisibleItemPosition + 1;
        //if all items are shown, hide the fast-scroller
        fastScroller.setVisibility(adapter.getItemCount() > itemsShown ? View.VISIBLE : View.GONE);
    }

}
