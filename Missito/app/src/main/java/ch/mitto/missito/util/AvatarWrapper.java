package ch.mitto.missito.util;

import android.ch.mitto.missito.R;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.makeramen.roundedimageview.RoundedImageView;

import java.nio.charset.Charset;

import ch.mitto.missito.services.model.MissitoContact;

/**
 * This one wraps {@link RoundedImageView} and helps setting display options: color and initials.
 */

public class AvatarWrapper {
    private final Context context;
    private final RoundedImageView roundedImageView;
    private final TextView label;


    public AvatarWrapper(Context ctx, RoundedImageView roundedImageView, TextView label) {
        this.roundedImageView = roundedImageView;
        this.context = ctx;
        this.label = label;
    }

    private static final int[] cAvatarStubs = new int[]{
            R.drawable.avatar_green,
            R.drawable.avatar_blue,
            R.drawable.avatar_magenta,
            R.drawable.avatar_red,
            R.drawable.avatar_yellow,
    };

    public void update(final MissitoContact contact) {
        update(contact.phone, contact.avatarPath, contact.name);
    }

    public void update(String phone, final String avatarPath, final String name) {
        // pick background based on phone number
        CRC8 crc8 = new CRC8((short) 0x00);
        crc8.update(phone.getBytes(Charset.forName("UTF-8")));

        //set initial letter with shadow
        final int resPlaceholder = cAvatarStubs[crc8.getValue() % cAvatarStubs.length];
        if (TextUtils.isEmpty(avatarPath)) {
            displayDefault(resPlaceholder, name);
        } else {
            Glide.with(context)
                    .load(Uri.parse(avatarPath))
                    .apply(new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .placeholder(resPlaceholder)
                            .dontTransform()
                    )
                    .into(new SimpleTarget<Drawable>() {
                        @Override
                        public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                            roundedImageView.setImageDrawable(resource);
                            label.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            displayDefault(resPlaceholder, name);
                        }
                    });
        }
    }

    private void displayDefault(int resId, String title) {
        Glide.with(context).load(resId).into(roundedImageView);
        label.setText(Helper.getInitials(title));
        label.setVisibility(View.VISIBLE);
    }
}
