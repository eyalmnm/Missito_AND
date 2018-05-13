package ch.mitto.missito.ui.signin.Picker;

import android.ch.mitto.missito.R;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

class HeaderViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.firstLetter)
    TextView header;

    public HeaderViewHolder(View itemView, ArrayList<Country> countries) {
        super(itemView);
        ButterKnife.bind(this, itemView);

        String name = countries.get(0).countryName;
        header.setText(String.valueOf(name.charAt(0)));
    }
}