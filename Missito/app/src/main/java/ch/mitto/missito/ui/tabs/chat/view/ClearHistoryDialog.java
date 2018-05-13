package ch.mitto.missito.ui.tabs.chat.view;

import android.app.Dialog;
import android.ch.mitto.missito.R;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.RadioGroup;

import org.greenrobot.eventbus.EventBus;

import ch.mitto.missito.events.ClearHistoryEvent;
import ch.mitto.missito.ui.tabs.chat.ClearHistoryOption;
import ch.mitto.missito.util.RealmDBHelper;

/**
 * Created by usr1 on 10/31/17.
 */


public class ClearHistoryDialog extends DialogFragment {

    private static String staticPhone;

    public static ClearHistoryDialog newInstance(String phone) {
        staticPhone = phone;
        return new ClearHistoryDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view  = getActivity().getLayoutInflater().inflate(R.layout.dialog_clear_history, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.clean_history_dialog_title)
                .setView(view)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.clean),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ClearHistoryOption clearHistoryOption;
                                RadioGroup  rg = view.findViewById(R.id.clear_history_rdGroup);
                                switch (rg.getCheckedRadioButtonId()) {
                                    case R.id.last_hour_rdBtn:
                                        clearHistoryOption = ClearHistoryOption.LAST_HOUR;
                                        break;
                                    case R.id.last_day_rdBtn:
                                        clearHistoryOption = ClearHistoryOption.LAST_DAY;
                                        break;
                                    default:
                                        clearHistoryOption = ClearHistoryOption.ALL;
                                }
                                clearHistory(staticPhone, clearHistoryOption);
                            }
                        })
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        return builder.create();
    }


    private void clearHistory(String phone, ClearHistoryOption clearHistoryOption) {
        RealmDBHelper.clearChatHistory(phone, clearHistoryOption);
        EventBus.getDefault().post(new ClearHistoryEvent(phone));
    }


}
