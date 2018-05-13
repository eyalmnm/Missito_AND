package ch.mitto.missito.ui.common;

import java.util.ArrayList;

/**
 * Created by usr1 on 3/13/18.
 */

public interface ContactPickerListener {
    void onContactsSelected(ArrayList<String> phones);
    void updateTitle(String title, boolean homeButtonFlag);
}
