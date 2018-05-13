package ch.mitto.missito.ui.signin.Picker;

import android.ch.mitto.missito.R;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class CountryViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.country_name)
    TextView name;

    @BindView(R.id.country_code)
    TextView code;

    private Country country;
    private CountryViewHolder.Listener listener;

    @OnClick(R.id.countryContainer)
    public void onCountrySelect() {
        listener.onCountryClick(country);
    }

    @BindView(R.id.selected)
    ImageView selected;

    @BindView(R.id.container)
    LinearLayout container;

    public CountryViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void update(Country country, CountryViewHolder.Listener listener) {
        this.country = country;
        this.listener = listener;
        name.setText(country.countryName);
        code.setText(country.dialingCode);
        container.setBackgroundResource(country.isSelected ? R.color.athensGray : android.R.color.white);
        selected.setVisibility(country.isSelected ? View.VISIBLE : View.GONE);
    }

    public interface Listener {
        void onCountryClick(Country country);
    }
}
