package ch.mitto.missito.ui.tabs.chat.message.incoming;

import android.ch.mitto.missito.R;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.widget.FrameLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.mitto.missito.db.model.attach.ImageAttachRec;
import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;
import ch.mitto.missito.ui.tabs.chat.adapter.IncomingChatMessage;
import ch.mitto.missito.ui.tabs.chat.message.ChatCellHelper;
import ch.mitto.missito.ui.tabs.chat.message.ImageCellListener;
import ch.mitto.missito.ui.tabs.chat.view.ChatRoundedImageView;
import ch.mitto.missito.util.ImageHelper;

public class IncomingImageViewHolder extends IncomingMessageViewHolder {

    private ImageCellListener listener;
    private AsyncTask<Void, Void, Bitmap> thumbnailAsyncTask;

    @BindView(R.id.bubble)
    ChatRoundedImageView messageImg;

    @BindView(R.id.dots_menu)
    FrameLayout dotsMenu;

    @OnClick(R.id.img_forward)
    public void onForwardPressed() {
        if (listener != null) {
            listener.onForwardSelected(message.id);
        }
    }

    @OnClick(R.id.dots_menu)
    public void onDotsMenuPressed() {
        ChatCellHelper.imageMessageAlertDialog(context, message).show();
    }

    public IncomingImageViewHolder(View itemView, ImageCellListener listener) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        this.listener = listener;
    }

    public void setMessage(final IncomingChatMessage message) {
        super.setMessage(message);
        messageImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onImageMessageClick(message, getAdapterPosition());
            }
        });

        for (final ImageAttachRec missitoImage : message.attachmentRec.images) {
            if (thumbnailAsyncTask != null && !thumbnailAsyncTask.isCancelled()) {
                thumbnailAsyncTask.cancel(true);
            }
            thumbnailAsyncTask = ImageHelper.fromBase64(missitoImage.thumbnail, messageImg);
            thumbnailAsyncTask.execute();
            // TODO: remove when UI will be prepared for multiple images to display in one message
            break;
        }
        dotsMenu.setVisibility(message.attachmentRec.images.get(0).localFileURI == null ? View.GONE : View.VISIBLE);
    }

    @Override
    public void roundCorners(ChatMessage message) {
        messageImg.setCornerRadii(getCornerRadiiFor(message));
    }
}
