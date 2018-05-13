package ch.mitto.missito.ui.signin.Picker;

import android.ch.mitto.missito.R;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.Application;
import ch.mitto.missito.ui.common.fastscroll.RecyclerViewFastScroller;
import ch.mitto.missito.ui.signin.FilterableFragment;
import ch.mitto.missito.ui.tabs.interfaces.FilterableSection;
import ch.mitto.missito.util.MissitoLinearLayoutManager;
import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class CountryPicker extends Fragment implements CountryViewHolder.Listener, FilterableFragment {

    private static final String KEY_COUNTRY = "country";

    private CountryListAdapter adapter;
    private List<Country> countries;
    private Country currentCountry;
    private Listener listener;

    @BindView(R.id.container_missito)
    RelativeLayout listContainer;

    @BindView(R.id.empty_view)
    TextView emptyView;

    @BindView(R.id.country_list)
    RecyclerView countryList;

    @BindView(R.id.fastscroller)
    RecyclerViewFastScroller fastScrollerMissito;

    public static CountryPicker newInstance(Country country) {
        CountryPicker countryPicker = new CountryPicker();
        Bundle args = new Bundle();
        args.putSerializable(KEY_COUNTRY, country);
        countryPicker.setArguments(args);
        return countryPicker;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(KEY_COUNTRY)) {
            currentCountry = (Country) getArguments().getSerializable(KEY_COUNTRY);
        }

        listener.onHomeButtonShowRequest(true);

        countries = Utils.parseCountries(Utils.getCountriesJSON(this.getContext()));
        Collections.sort(countries, new Comparator<Country>() {
            @Override
            public int compare(Country country1, Country country2) {
                return country1.countryName.compareTo(country2.countryName);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(Application.app).inflate(R.layout.country_picker, null, false);
        ButterKnife.bind(this, view);
        setHasOptionsMenu(true);
        listener.onTitleChangeRequest(R.string.select_country);

        HashMap<Character, ArrayList<Country>> countriesByName = new HashMap<>();
        groupCountries(countriesByName);

        TreeSet<Character> treeSet = new TreeSet<>(countriesByName.keySet());
        adapter = new CountryListAdapter();
        for (Character character : treeSet) {
            adapter.addSection(new CountrySection(countriesByName.get(character), CountryPicker.this));
        }

        initRecyclerView(countryList, fastScrollerMissito, adapter);
        return view;
    }

    private void groupCountries(HashMap<Character, ArrayList<Country>> countriesByName) {
        for (Country country : countries) {
            if (currentCountry != null && country.isoCode.equals(currentCountry.isoCode)) {
                country.isSelected = true;
            }

            char firstChar = country.countryName.toLowerCase().charAt(0);
            ArrayList<Country> countries = countriesByName.get(firstChar);
            if (countries == null) {
                countries = new ArrayList<>();
                countriesByName.put(firstChar, countries);
            }
            countries.add(country);
        }
    }

    private void initRecyclerView(RecyclerView recyclerView,
                                  final RecyclerViewFastScroller fastScroller,
                                  final SectionedRecyclerViewAdapter adapter) {

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new MissitoLinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false, fastScroller, adapter));
        fastScroller.setRecyclerView(recyclerView);
        fastScroller.setViewsToUse(R.layout.recycler_view_fast_scroller__fast_scroller, R.id.fastscroller_bubble, R.id.fastscroller_handle);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement EnterPhoneFragment.Listener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onCountryClick(Country country) {
        listener.onCountrySelected(country);
    }

    @Override
    public void filter(String text) {
        for (Section section : adapter.getSectionsMap().values()) {
            ((FilterableSection) section).filter(text);
        }

        checkIfEmpty();
        adapter.notifyDataSetChanged();
    }

    private void checkIfEmpty() {
        boolean emptySearchResult = adapter.getItemCount() == 0;
        emptyView.setVisibility(emptySearchResult ? View.VISIBLE : View.GONE);
        listContainer.setVisibility(emptySearchResult ? View.INVISIBLE : View.VISIBLE);
    }

    public interface Listener {
        void onCountrySelected(Country country);

        void onTitleChangeRequest(int titleResId);

        void onHomeButtonShowRequest(boolean show);
    }
}
