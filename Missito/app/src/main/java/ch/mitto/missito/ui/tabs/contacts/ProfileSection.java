package ch.mitto.missito.ui.tabs.contacts;

import android.ch.mitto.missito.R;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;

public class ProfileSection extends StatelessSection {

    public ProfileSection() {
        super(new SectionParameters.Builder(R.layout.item_profile)
                .build());
    }

    @Override
    public int getContentItemsTotal() {
        return 1;
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new ProfileItemViewHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ProfileItemViewHolder) holder).onBindViewHolder();
    }
}
