package ch.mitto.missito.ui.tabs.chats;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.db.model.MessageRec;
import ch.mitto.missito.ui.tabs.chats.model.Chat;
import ch.mitto.missito.util.RealmDBHelper;

import static ch.mitto.missito.net.broker.MQTTConnectionManager.MSG_STATUS_UPDATE_EVENT;
import static ch.mitto.missito.net.broker.MQTTConnectionManager.NEW_STATUS_UPDATE_EVENT;
import static ch.mitto.missito.util.NotificationHelper.INCOMING_MESSAGE_SAVED;
import static ch.mitto.missito.util.NotificationHelper.KEY_MESSAGE;
import static ch.mitto.missito.util.NotificationHelper.NEW_TYPING_UID_KEY;
import static ch.mitto.missito.util.NotificationHelper.NEW_TYPING_NOTIFICATION;

public class ChatsFragment extends Fragment {

    private static final long REFRESH_INTERVAL = DateUtils.MINUTE_IN_MILLIS;

    @BindView(R.id.recyclerview_chats)
    RecyclerView recyclerView;
    private List<Chat> allChats;
    private String currentFilter = "";
    private ChatsAdapter adapter;
    MaterialSearchView.OnQueryTextListener onSearchItemClickListener = new MaterialSearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            currentFilter = newText.toLowerCase();
            adapter.setItems(filterChats(allChats, currentFilter));
            return true;
        }
    };
    private MaterialSearchView searchView;
    private Listener listener;
    private Handler typingHandler = new Handler();
    private Handler chatUpdateHandler;
    private HashMap<String, Runnable> typingRunnables = new HashMap<>();

    public ChatsFragment() {
        // Required empty public constructor
    }

    private BroadcastReceiver typingNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String from = intent.getExtras().getString(NEW_TYPING_UID_KEY);
            if (typingRunnables.containsKey(from)) {
                typingHandler.removeCallbacks(typingRunnables.get(from));
                typingRunnables.remove(from);
            }
            final Chat chat = adapter.getChat(from);
            if (from != null && chat != null) {
                chat.isTyping = true;
                adapter.notifyDataSetChanged();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        chat.isTyping = false;
                        adapter.notifyDataSetChanged();
                        typingRunnables.remove(from);
                    }
                };
                typingRunnables.put(from, runnable);
                typingHandler.postDelayed(runnable, 5000);
            }

        }
    };

    public static ChatsFragment newInstance() {
        return new ChatsFragment();
    }

    private BroadcastReceiver newMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final MessageRec incomingMessage = (MessageRec) intent.getExtras()
                    .getSerializable(KEY_MESSAGE);

            // TODO: should we consider deviceId?
            if (typingRunnables.containsKey(incomingMessage.senderUid)) {
                final Chat chat = adapter.getChat(incomingMessage.senderUid);
                chat.isTyping = false;
                adapter.notifyDataSetChanged();
                typingHandler.removeCallbacksAndMessages(typingRunnables.get(incomingMessage.senderUid));
                typingRunnables.remove(incomingMessage.senderUid);
            }

            updateChat();
        }
    };

    private BroadcastReceiver statusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateChat();
        }
    };

    private void updateChat() {
        allChats = RealmDBHelper.getChats();
        adapter.updateChats(allChats);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(newMessageReceiver,
                new IntentFilter(INCOMING_MESSAGE_SAVED));

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(typingNotificationReceiver,
                new IntentFilter(NEW_TYPING_NOTIFICATION));

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(statusUpdateReceiver,
                new IntentFilter(MSG_STATUS_UPDATE_EVENT));

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(statusUpdateReceiver,
                new IntentFilter(NEW_STATUS_UPDATE_EVENT));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_chats, container, false);
        ButterKnife.bind(this, rootView);
        startUpdateHandler();

        setHasOptionsMenu(true);
        searchView = (MaterialSearchView) getActivity().findViewById(R.id.search_view);

        allChats = RealmDBHelper.getChats();
        adapter = new ChatsAdapter(allChats, new ChatsAdapter.Listener() {
            @Override
            public void onChatSelected(Chat chat) {
                listener.onChatSelected(chat);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return rootView;
    }

    private List<Chat> filterChats(List<Chat> chats, String filter) {
        List<Chat> result = new ArrayList<>();
        for (Chat chat : chats) {
            if (chat.participants.get(0).name.toLowerCase().contains(filter)) {
                result.add(chat);
            }
        }
        return result;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement Listener");
        }
    }

    private void startUpdateHandler() {
        if (chatUpdateHandler != null) {
            return;
        }

        chatUpdateHandler = new Handler();
        chatUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                chatUpdateHandler.postDelayed(this, REFRESH_INTERVAL);
                if (adapter != null) {
                    updateChat();
                }
            }
        }, REFRESH_INTERVAL);
    }

    private void stopUpdateHandler() {
        if (chatUpdateHandler != null) {
            chatUpdateHandler.removeCallbacksAndMessages(null);
            chatUpdateHandler = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        searchView.setMenuItem(searchMenuItem);
        searchView.closeSearch();
        searchView.setOnQueryTextListener(onSearchItemClickListener);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            allChats = RealmDBHelper.getChats();
            adapter.setItems(allChats);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(newMessageReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(typingNotificationReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(statusUpdateReceiver);
        stopUpdateHandler();
    }

    public interface Listener {
        void onChatSelected(Chat chat);
    }
}
