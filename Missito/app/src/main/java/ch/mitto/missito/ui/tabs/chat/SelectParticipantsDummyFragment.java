package ch.mitto.missito.ui.tabs.chat;


import android.ch.mitto.missito.R;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.ui.MainActivity;

public class SelectParticipantsDummyFragment extends Fragment {

    @BindView(R.id.list)
    ListView list;

    public SelectParticipantsDummyFragment() {
    }

    public static SelectParticipantsDummyFragment newInstance() {
        return new SelectParticipantsDummyFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MainActivity) getActivity()).getSupportActionBar().setTitle("Select participants");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fargment_list_dummy, container, false);
        ButterKnife.bind(this, view);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, new String[]{"Person1", "Person2", "Person3", "Person4", "Person5", "Person6",});
        list.setAdapter(adapter);
        return view;
    }
}
