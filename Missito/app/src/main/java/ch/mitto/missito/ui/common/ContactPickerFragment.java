package ch.mitto.missito.ui.common;


import android.ch.mitto.missito.R;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.ui.common.ContactPicker.MarkListener;
import ch.mitto.missito.ui.common.fastscroll.RecyclerViewFastScroller;
import ch.mitto.missito.ui.tabs.contacts.ContactPickerAdapter;

/**
 * Created by usr1 on 3/13/18.
 */

public abstract class ContactPickerFragment extends Fragment implements MarkListener {
    @BindView(R.id.recyclerview_all)
    RecyclerView contactList;

    @BindView(R.id.fastscroller_all)
    RecyclerViewFastScroller fastScrollerAll;

    @BindView(R.id.done)
    AppCompatButton done;

    View rootView;

    @OnClick(R.id.done)
    void done() {
        ArrayList<String> selectedContacts = adapter.getSelectedContacts();
        if (!selectedContacts.isEmpty()) {
            listener.onContactsSelected(selectedContacts);
        }
    }

    protected ContactPickerListener listener;
    protected ContactPickerAdapter adapter;
    private String currentFilter = "";
    private MaterialSearchView searchView;
    private MenuItem searchMenuItem;
    private boolean allMarked;
    private int countOfMarkedItems;
    private boolean keyboardIsVisible;
    protected List<MissitoContact> contacts;

    private MaterialSearchView.OnQueryTextListener onSearchItemClickListener = new MaterialSearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            currentFilter = newText.toLowerCase();
            adapter.setItems(filterContacts(contacts, currentFilter));
            return true;
        }
    };


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (ContactPickerListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement ContactPickerListener");
        }
        listener.updateTitle(getString(getTitleRes()), true);
        contacts = new ArrayList<>();
        updateContacts();
        Collections.sort(contacts, new Comparator<MissitoContact>() {
            @Override
            public int compare(MissitoContact o1, MissitoContact o2) {
                return o1.name.compareTo(o2.name);
            }
        });
        adapter = new ContactPickerAdapter(contacts, this);
    }

    private List<MissitoContact> filterContacts(Collection<MissitoContact> contacts, String filter) {
        List<MissitoContact> result = new ArrayList<>();
        for (MissitoContact contact : contacts) {
            if (contact.name.toLowerCase().contains(filter) || contact.phone.contains(filter)) {
                result.add(contact);
            }
        }
        return result;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.contact_picker_fragment, container, false);
        ButterKnife.bind(this, rootView);
        initRecyclerView(contactList, fastScrollerAll, adapter);
        setHasOptionsMenu(true);
        searchView = getActivity().findViewById(R.id.search_view);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.contact_invite_menu, menu);
        searchMenuItem = menu.findItem(R.id.action_search);
        searchView.setMenuItem(searchMenuItem);
        searchView.setOnQueryTextListener(onSearchItemClickListener);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                break;
            case R.id.action_mark_all:
                allMarked = !allMarked;
                adapter.markAllItems(allMarked);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return super.onOptionsItemSelected(item);
    }

    private void initRecyclerView(RecyclerView recyclerView,
                                  final RecyclerViewFastScroller fastScroller,
                                  final ContactPickerAdapter adapter) {

        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false) {
            @Override
            public void onLayoutChildren(final RecyclerView.Recycler recycler, final RecyclerView.State state) {
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
        });
        fastScroller.setRecyclerView(recyclerView);
        fastScroller.setViewsToUse(R.layout.recycler_view_fast_scroller__fast_scroller, R.id.fastscroller_bubble, R.id.fastscroller_handle);
    }

    @Override
    public void onDestroyView() {
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
        listener = null;
        super.onDestroyView();
    }

    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {

            // navigation bar height
            int navigationBarHeight = 0;
            int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navigationBarHeight = getResources().getDimensionPixelSize(resourceId);
            }

            // status bar height
            int statusBarHeight = 0;
            resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = getResources().getDimensionPixelSize(resourceId);
            }

            // display window size for the app layout
            Rect rect = new Rect();
            getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);

            // screen height - (user app height + status + nav) ..... if non-zero, then there is a soft keyboard
            int keyboardHeight = rootView.getRootView().getHeight() - (statusBarHeight + navigationBarHeight + rect.height());

            keyboardIsVisible = keyboardHeight > 0;
            onChangeKeyboardState(keyboardIsVisible);

        }
    };


    @Override
    public void onUpdateCountOfMarkedItems(int countOfMarkedItems) {
        this.countOfMarkedItems = countOfMarkedItems ;
        invalidateDoneButton();
    }

    private void invalidateDoneButton(){
        done.setVisibility(countOfMarkedItems > 0 && !keyboardIsVisible ? View.VISIBLE : View.GONE);
        done.setText(String.format(getResources().getQuantityString(getDoneBtnTextRes(), (int)countOfMarkedItems), countOfMarkedItems));
    }

    protected void onChangeKeyboardState(boolean keyboardIsVisible) {
        invalidateDoneButton();
    }

    protected abstract int getTitleRes();

    protected abstract void updateContacts();

    protected abstract int getDoneBtnTextRes();
}
