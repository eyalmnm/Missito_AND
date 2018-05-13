package ch.mitto.missito.ui.tabs.contacts;

import android.app.Activity;
import android.ch.mitto.missito.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.miguelcatalan.materialsearchview.MaterialSearchView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.Application;
import ch.mitto.missito.services.model.MissitoContact;
import ch.mitto.missito.net.ConnectionChangeReceiver;
import ch.mitto.missito.net.broker.MQTTConnectionManager;
import ch.mitto.missito.net.webapi.APIRequests;
import ch.mitto.missito.ui.MainActivity;
import ch.mitto.missito.ui.MainActivityAccess;
import ch.mitto.missito.ui.tabs.chat.ChatActivity;
import ch.mitto.missito.ui.tabs.interfaces.FilterableSection;
import ch.mitto.missito.services.Contacts;
import ch.mitto.missito.util.Helper;
import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

import static ch.mitto.missito.util.NotificationHelper.INCOMING_MESSAGE_SAVED;

/**
 * Fragment for displaying a list of contacts.
 * <p>
 * Depending on the Switch it will display either Missito contacts or all contacts from this device.
 */
public class ContactListFragment extends Fragment {

    private static final String LOG_TAG = ContactListFragment.class.getSimpleName();
    private static final long REFRESH_INTERVAL = DateUtils.MINUTE_IN_MILLIS;
    private static final String SECTION_CONTACTS = "contactsSection";
    private static final String SECTION_PROFILE = "profileSection";
    private MainActivityAccess listener;
    private Handler handler;


    @BindView(R.id.recyclerview_missito)
    RecyclerView recyclerViewMissito;

    private MaterialSearchView searchView;
    private String currentFilter = "";
    private SectionedRecyclerViewAdapter adapter;
    public APIRequests apiRequests;
    private MaterialSearchView.OnQueryTextListener queryTextListener = new MaterialSearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            currentFilter = newText.toLowerCase();
            FilterableSection section = (FilterableSection) adapter.getSection(SECTION_CONTACTS);
            section.filter(currentFilter);
            adapter.notifyDataSetChanged();
            return true;
        }
    };
    private MenuItem searchMenuItem;
    private BroadcastReceiver permissionGrantedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Contacts contacts = Application.app.contacts;
            if (!contacts.contactsLoaded() && Helper.canReadContacts(getContext())) {
                contacts.loadSystemContacts();
                FilterableSection section = (FilterableSection) adapter.getSection(SECTION_CONTACTS);
                section.filter(currentFilter);
                adapter.notifyDataSetChanged();
            }
        }
    };

    private BroadcastReceiver statusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ContactsSection section = (ContactsSection) adapter.getSection(SECTION_CONTACTS);
            section.setItems(Application.app.contacts.missitoContacts);
            adapter.notifyDataSetChanged();
        }
    };

    private BroadcastReceiver networkStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(ConnectionChangeReceiver.CONNECTION_STATUS_UPDATE, false)) {
                adapter.notifyDataSetChanged();
            }
        }
    };

    private BroadcastReceiver newMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            adapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ContactListFragment() {
    }

    public static ContactListFragment newInstance() {
        return new ContactListFragment();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact_list, container, false);
        ButterKnife.bind(this, rootView);
        apiRequests = Application.app.connectionManager.apiRequests;
        startUpdateHandler();

        setHasOptionsMenu(true);
        searchView = (MaterialSearchView) getActivity().findViewById(R.id.search_view);
        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                showHeaders(false);
            }

            @Override
            public void onSearchViewClosed() {
                showHeaders(true);
            }
        });

        adapter = new SectionedRecyclerViewAdapter();
        adapter.addSection(SECTION_PROFILE, new ProfileSection());
        adapter.addSection(SECTION_CONTACTS, new ContactsSection(Application.app.contacts.missitoContacts, new ContactItemViewHolder.Listener() {
            @Override
            public void onContactClick(MissitoContact contact) {
                Log.d(LOG_TAG, "ContactAttachment click for " + contact.name);
                if (!contact.phone.isEmpty()) {
                    ChatActivity.start(getContext(), contact);
                }
            }
        }));

        recyclerViewMissito.setAdapter(adapter);
        recyclerViewMissito.setLayoutManager(new LinearLayoutManager(getContext()));

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(permissionGrantedReceiver,
                new IntentFilter(MainActivity.PERMISSIONS_GRANTED_EVT));

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(statusUpdateReceiver,
                new IntentFilter(MQTTConnectionManager.NEW_STATUS_UPDATE_EVENT));

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(networkStatusReceiver,
                new IntentFilter(ConnectionChangeReceiver.BROADCAST_NETWORK_STATUS));

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(newMessageReceiver,
                new IntentFilter(INCOMING_MESSAGE_SAVED));

        return rootView;
    }

    private void showHeaders(boolean hasHeader) {
        ContactsSection section = (ContactsSection) adapter.getSection(SECTION_CONTACTS);
        section.setHasHeader(hasHeader);
        Section profile = adapter.getSection(SECTION_PROFILE);
        profile.setVisible(hasHeader);
        adapter.notifyDataSetChanged();
    }

    private void startUpdateHandler() {
        if (handler != null) {
            return;
        }

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, REFRESH_INTERVAL);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        }, REFRESH_INTERVAL);
    }

    private void stopUpdateHandler() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(permissionGrantedReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(statusUpdateReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(networkStatusReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(newMessageReceiver);
        stopUpdateHandler();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_contacts, menu);
        MenuItem item = menu.findItem(R.id.action_invite);
        Helper.setMenuItemColor(getContext(), item, android.R.color.white);

        searchMenuItem = menu.findItem(R.id.action_search);
        searchView.setMenuItem(searchMenuItem);
        searchView.closeSearch();
        searchView.setOnQueryTextListener(queryTextListener);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_invite) {
            listener.onInviteCalled(this);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (MainActivityAccess) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MainActivityAccess");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}
