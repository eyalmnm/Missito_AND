package ch.mitto.missito.ui.signin.Picker;

import android.ch.mitto.missito.R;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;

import ch.mitto.missito.ui.tabs.interfaces.FilterableSection;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;

class CountrySection extends StatelessSection implements FilterableSection {
    private ArrayList<Country> countries = new ArrayList<>();
    private ArrayList<Country> filteredList = new ArrayList<>();
    private CountryViewHolder.Listener listener;

    public CountrySection(ArrayList<Country> countries, CountryViewHolder.Listener listener) {
        super(new SectionParameters.Builder(R.layout.country_item)
                .headerResourceId(R.layout.contact_group_header)
                .build());
        this.countries = countries;
        this.listener = listener;
        this.filteredList.addAll(countries);
    }

    @Override
    public int getContentItemsTotal() {
        return filteredList.size();
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new CountryViewHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((CountryViewHolder) holder).update(filteredList.get(position), listener);
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new HeaderViewHolder(view, filteredList);
    }

    public String getFirstLetter() {
        return String.valueOf(countries.get(0).countryName.charAt(0));
    }

    @Override
    public void filter(String query) {
        if (TextUtils.isEmpty(query)) {
            filteredList = new ArrayList<>(countries);
            this.setVisible(true);
        } else {
            filteredList.clear();
            for (Country value : countries) {
                if (value.countryName.toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(value);
                }
            }

            this.setVisible(!filteredList.isEmpty());
        }
    }
}