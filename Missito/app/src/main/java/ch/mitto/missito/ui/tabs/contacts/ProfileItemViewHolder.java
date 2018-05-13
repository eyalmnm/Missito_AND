package ch.mitto.missito.ui.tabs.contacts;

import android.ch.mitto.missito.R;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.Application;
import ch.mitto.missito.ui.common.inputName.InputNameActivity;
import ch.mitto.missito.util.Helper;
import ch.mitto.missito.util.PrefsHelper;

public class ProfileItemViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.profileAvatar)
    RoundedImageView avatar;

    @BindView(R.id.name)
    TextView name;

    @BindView(R.id.phone)
    TextView phone;

    ProfileItemViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void onBindViewHolder() {
        // TODO Replace avatar and name with real data
        avatar.setImageResource(R.mipmap.ic_launcher);
        String username = PrefsHelper.getUsername();
        name.setText(username.isEmpty() ? Application.app.getString(R.string.missito_user) : username);
        phone.setText(String.format("%s", Helper.addPlus(Application.app.connectionManager.uid)));
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.getContext().startActivity(new Intent(view.getContext(), InputNameActivity.class));
            }
        });
    }
}
