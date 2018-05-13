package ch.mitto.missito.ui.signin.Picker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

import ch.mitto.missito.Application;

public class Country implements Serializable {
    public String isoCode;
    public String dialingCode;
    public String countryName;
    public boolean isSelected;

    public Country(String isoCode, String dialingCode) {
        this.isoCode = isoCode;
        this.dialingCode = String.format("+%s", dialingCode);
        countryName = getCountryNameFromIsoCode();
    }

    private String getCountryNameFromIsoCode() {
        return new Locale(Locale.getDefault().getLanguage(), isoCode).getDisplayCountry();
    }

    public static Country getCountryFromListForIso(ArrayList<Country> list, String isoCode) {
        for (Country c : list) {
            if (c.isoCode.equals(isoCode))
                return c;
        }
        return null;
    }

}
