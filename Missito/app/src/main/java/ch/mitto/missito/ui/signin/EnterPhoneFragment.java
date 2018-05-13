package ch.mitto.missito.ui.signin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.ch.mitto.missito.R;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import butterknife.Unbinder;
import ch.mitto.missito.ui.signin.Picker.Country;
import ch.mitto.missito.ui.signin.Picker.Utils;
import ch.mitto.missito.util.PrefsHelper;

import static com.google.i18n.phonenumbers.PhoneNumberUtil.*;

public class EnterPhoneFragment extends Fragment {

    private static final String LOG_TAG = EnterPhoneFragment.class.getSimpleName();
    private static final String KEY_COUNTRY = "country";
    private static final String PICKED_FLAG = "picked";
    private String badCode;

    private Listener listener;
    private Country country;
    private HashMap<String, Country> countriesByDialCode;
    private HashMap<String, Country> countriesByIsoCode = new HashMap<>();
    private int succesColor, errorColor;
    private boolean countrySelectedFromPicker;

    private Unbinder unbinder;

    @BindView(R.id.flag_img)
    ImageView flagImage;

    @BindView(R.id.country_name)
    TextView countryNameText;

    @BindView(R.id.phone_code_txt)
    EditText phoneCodeText;

    @BindView(R.id.phone_edit)
    EditText phoneEdit;

    @BindView(R.id.tv_agreement)
    TextView agreement;

    @BindView(R.id.enter_phone_btn)
    Button submit;

    @OnTextChanged({R.id.phone_edit})
    public void onPhoneNumberChanged() {
        submit.setEnabled(phoneEdit.getText().length() > 0 && country != null);
    }

    private boolean isChangeNowPhoneCodeText;

    @OnTextChanged({R.id.phone_code_txt})
    public void onCodeChanged() {

        if (!isChangeNowPhoneCodeText && phoneCodeText.isFocused() && phoneCodeText.getSelectionEnd() == 0) {
            isChangeNowPhoneCodeText = true;
            String currentCode = phoneCodeText.getText().toString();
            phoneCodeText.getText().clear();
            phoneCodeText.append("+" + currentCode);
            isChangeNowPhoneCodeText = false;
        }

        if (countrySelectedFromPicker) {
            setCountry(country);
            countrySelectedFromPicker = false;
            return;
        }

        String dialingCode = phoneCodeText.getText().toString();
        if (countriesByDialCode.containsKey(dialingCode)) {
            country = countriesByDialCode.get(dialingCode);
            setCountry(country);
        } else {
            showBadCountryCodeError();
        }
    }

    private Country findForIso(String isoCode) {
        return countriesByIsoCode.get(isoCode);
    }

    public EnterPhoneFragment() {
        // Required empty public constructor
    }

    public static EnterPhoneFragment newInstance(Country country, boolean picked) {
        EnterPhoneFragment phoneFragment = new EnterPhoneFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_COUNTRY, country);
        args.putBoolean(PICKED_FLAG, picked);
        phoneFragment.setArguments(args);
        return phoneFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(KEY_COUNTRY)) {
            country = (Country) getArguments().getSerializable(KEY_COUNTRY);
            countrySelectedFromPicker = getArguments().getBoolean(PICKED_FLAG);
        }
        List<Country> countries = Utils.parseCountries(Utils.getCountriesJSON(getContext()));

        countriesByDialCode = new HashMap<>(countries.size());
        for (Country c : countries) {
            countriesByDialCode.put(c.dialingCode, c);
            countriesByIsoCode.put(c.isoCode, c);
        }
        setupRepeatingCodes();
        succesColor = ContextCompat.getColor(getContext(), R.color.tundora);
        errorColor = ContextCompat.getColor(getContext(), R.color.orangeRed);
        badCode = getString(R.string.bad_dialing_code);
    }

    private void setupRepeatingCodes() {
        countriesByDialCode.put("+1", findForIso("US"));
        countriesByDialCode.put("+44", findForIso("GB"));
        countriesByDialCode.put("+590", findForIso("GP"));
        countriesByDialCode.put("+64", findForIso("NZ"));
        countriesByDialCode.put("+61", findForIso("AU"));
        countriesByDialCode.put("+212", findForIso("MA"));
        countriesByDialCode.put("+55", findForIso("BR"));
        countriesByDialCode.put("+358", findForIso("FI"));
        countriesByDialCode.put("+47", findForIso("NO"));
        countriesByDialCode.put("+7", findForIso("RU"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enter_phone, container, false);
        unbinder = ButterKnife.bind(this, view);
        listener.onHomeButtonShowRequest(false);
        phoneCodeText.setText(country == null ? badCode : country.dialingCode);
        phoneEdit.requestFocus();
        listener.onTitleChangeRequest(R.string.account_setup);
        agreement.setMovementMethod(LinkMovementMethod.getInstance());
        return view;
    }

    private void setCountry(Country c) {
        if (c == null) {
            showBadCountryCodeError();
            return;
        }
        countryNameText.setTextColor(succesColor);
        phoneCodeText.setTextColor(succesColor);
        countryNameText.setText(c.countryName);
        int id = getContext().getResources().getIdentifier("flag_" + c.isoCode.toLowerCase(), "drawable", getContext().getPackageName());
        flagImage.setImageResource(id == 0 ? R.drawable.no_flag : id);
        submit.setEnabled(phoneEdit.getText().length() > 0);
    }

    private void showBadCountryCodeError() {
        country = null;
        flagImage.setImageResource(R.drawable.no_flag);
        countryNameText.setText(badCode);
        countryNameText.setTextColor(errorColor);
        phoneCodeText.setTextColor(errorColor);
        submit.setEnabled(false);
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
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick({R.id.country_select_btn, R.id.country_name})
    void onSelectCountry() {
        listener.onCountrySelectPressed(country);
    }

    @OnClick(R.id.enter_phone_btn)
    void onPhoneSubmit() {
        PhoneNumberUtil phoneUtil = getInstance();
        String phoneNumber = phoneEdit.getText().toString().replaceAll("[^0-9]", "");
        phoneEdit.setText(phoneNumber);
        String phoneStr = phoneCodeText.getText().toString() + phoneNumber;
        try {
            Phonenumber.PhoneNumber phone = phoneUtil.parse(phoneStr, country.isoCode);
            PhoneNumberUtil.ValidationResult validationResult = phoneUtil.isPossibleNumberWithReason(phone);
            if (!phoneUtil.isValidNumber(phone)) {

                if (validationResult == ValidationResult.TOO_LONG) {
                    showWrongPhoneDialog(String.format(getResources().getString(R.string.too_long_number), country.countryName)).show();
                    return;
                } else if (validationResult == ValidationResult.TOO_SHORT) {
                    showWrongPhoneDialog(String.format(getResources().getString(R.string.too_short_number), country.countryName)).show();
                    return;
                }

                showWrongPhoneDialog(getResources().getString(R.string.incorrect_phone)).show();
                return;
            }
            phoneStr = phoneUtil.format(phone, PhoneNumberFormat.INTERNATIONAL);
        } catch (NumberParseException e) {
            showWrongPhoneDialog(String.format(getResources().getString(R.string.too_short_number), country.countryName)).show();
            return;
        }
        PrefsHelper.setCountryCode(countriesByDialCode.get(phoneCodeText.getText().toString()).isoCode);
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(phoneEdit.getWindowToken(), 0);

        listener.onOnPhoneNrEntered(this, phoneStr);
    }

    private Dialog showWrongPhoneDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, null);
        return builder.create();
    }

    public interface Listener {
        void onOnPhoneNrEntered(EnterPhoneFragment sender, String phoneNr);

        void onCountrySelectPressed(Country currentCountry);

        void onHomeButtonShowRequest(boolean show);

        void onTitleChangeRequest(int titleResId);

    }

    /*
     ________________________________________
     +590  :  2 countries
     Гваделупа(GP), Сен-Бартельми(BL)
     ________________________________________
     +64  :  2 countries
     Новая Зеландия(NZ), Питкэрн(PN)
     ________________________________________
     +61  :  3 countries
     О-в Рождества(CX), Австралия(AU), О-ва Херд и Макдональд(HM)
     ________________________________________
     +212  :  2 countries
     Западная Сахара(EH), Марокко(MA)
     ________________________________________
     +44  :  4 countries
     Великобритания(GB), Джерси(JE), Гернси(GG), О-в Мэн(IM)
     ________________________________________
     +262  :  3 countries
     Майотта(YT), Реюньон(RE), Французские Южные Территории(TF)
     ________________________________________
     +500  :  2 countries
     Южная Георгия и Южные Сандвичевы о-ва(GS), Фолклендские о-ва(FK)
     ________________________________________
     +672  :  2 countries
     О-в Норфолк(NF), Антарктида(AQ)
     ________________________________________
     +1  :  26 countries
     Сен-Мартен(MF), Виргинские о-ва (США)(VI), Американское Самоа(AS), Гуам(GU), Багамские о-ва(BS), Антигуа и Барбуда(AG), Сент-Люсия(LC), Сент-Китс и Невис(KN), Монтсеррат(MS), Канада(CA), Соединенные Штаты(US), Гренада(GD), Северные Марианские о-ва(MP), Тринидад и Тобаго(TT), Ангилья(AI), Виргинские о-ва (Британские)(VG), О-ва Тёркс и Кайкос(TC), Внешние малые о-ва (США)(UM), Барбадос(BB), Сент-Винсент и Гренадины(VC), Бермудские о-ва(BM), Синт-Мартен(SX), Доминика(DM), Ямайка(JM), Пуэрто-Рико(PR), Доминиканская Республика(DO)
     ________________________________________
     +599  :  2 countries
     Кюрасао(CW), Бонэйр, Синт-Эстатиус и Саба(BQ)
     ________________________________________
     +55  :  2 countries
     О-в Буве(BV), Бразилия(BR)
     ________________________________________
     +358  :  2 countries
     Финляндия(FI), Аландские о-ва(AX)
     ________________________________________
     +7  :  2 countries
     Россия(RU), Казахстан(KZ)
     ________________________________________
     +47  :  2 countries
     Шпицберген и Ян-Майен(SJ), Норвегия(NO)
     ________________________________________
     +965  :  2 countries
     Кувейт(KW), Каймановы о-ва(KY)

     */
}
