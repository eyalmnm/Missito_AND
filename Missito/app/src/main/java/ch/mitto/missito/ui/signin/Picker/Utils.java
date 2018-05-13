package ch.mitto.missito.ui.signin.Picker;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Utils {

    private static final String LOG_TAG = Utils.class.getSimpleName();

    public static JSONObject getCountriesJSON(Context context) {
        String resourceName = "countries";
        int resourceId = context.getResources().getIdentifier(
                resourceName, "raw", context.getApplicationContext().getPackageName());

        if (resourceId == 0) {
            return null;
        }

        InputStream stream = context.getResources().openRawResource(resourceId);

        try {
            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            String json = new String(buffer, "UTF-8");
            return new JSONObject(json);
        } catch (IOException | JSONException e) {
            Log.e(LOG_TAG, "Could not read json file", e);
        }

        return null;
    }

    public static List<Country> parseCountries(JSONObject jsonCountries) {
        List<Country> countries = new ArrayList<>();
        Iterator<String> iter = jsonCountries.keys();

        while (iter.hasNext()) {
            String key = iter.next();
            try {
                String value = (String) jsonCountries.get(key);
                countries.add(new Country(key, value));
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Could not parse countries from json file ", e);
            }
        }
        return countries;
    }

    public static Country getUserCountryInfo(Context context) {
        List<Country> allCountries = Utils.parseCountries(Utils.getCountriesJSON(context));
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager.getSimState() != 1) {
            String countryIsoCode = telephonyManager.getSimCountryIso();

            for (int i = 0; i < allCountries.size(); ++i) {
                Country country = allCountries.get(i);
                if (country.isoCode.equalsIgnoreCase(countryIsoCode)) {
                    country.isSelected = true;
                    return country;
                }
            }
        }

        return new Country("MD", "373");
    }
}
