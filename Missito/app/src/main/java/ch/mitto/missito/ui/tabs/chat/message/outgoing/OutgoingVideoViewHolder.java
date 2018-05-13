package ch.mitto.missito.ui.tabs.chat.message.outgoing;

import android.ch.mitto.missito.R;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.mitto.missito.db.model.attach.VideoAttachRec;
import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;
import ch.mitto.missito.ui.tabs.chat.adapter.OutgoingChatMessage;
import ch.mitto.missito.ui.tabs.chat.message.ChatCellHelper;
import ch.mitto.missito.ui.tabs.chat.message.VideoCellListener;
import ch.mitto.missito.ui.tabs.chat.view.ChatRoundedImageView;
import ch.mitto.missito.util.Helper;
import ch.mitto.missito.util.ImageHelper;

public class OutgoingVideoViewHolder extends OutgoingMessageViewHolder {

    private VideoCellListener listener;

    private VideoAttachRec missitoVideo;
    private OutgoingChatMessage message;

    @BindView(R.id.bubble)
    ChatRoundedImageView image;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.btn_play)
    ImageButton playButton;

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
        ChatCellHelper.videoMessageAlertDialog(context, message).show();
    }

    private AsyncTask<Void, Void, Bitmap> thumbnailAsyncTask;

    public OutgoingVideoViewHolder(View itemView, VideoCellListener listener) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        this.listener = listener;
    }

    public void setMessage(final OutgoingChatMessage message) {
        super.setMessage(message);

        this.message = message;

        missitoVideo = message.attachmentRec.video.get(0);

        if (thumbnailAsyncTask != null && !thumbnailAsyncTask.isCancelled()) {
            thumbnailAsyncTask.cancel(true);
        }
        thumbnailAsyncTask = ImageHelper.fromBase64(missitoVideo.thumbnail, image);
        thumbnailAsyncTask.execute();
        progressBar.setVisibility(message.isLoading ? View.VISIBLE : View.GONE);
        playButton.setVisibility(message.isLoading ? View.GONE : View.VISIBLE);
        dotsMenu.setVisibility(message.isLoading || missitoVideo.localFileURI == null ? View.GONE : View.VISIBLE);
    }

    @OnClick(R.id.btn_play)
    public void onPlayClick() {
        if (missitoVideo.localFileURI != null) {
            Helper.startVideoPlayer(missitoVideo.localFileURI, image.getContext(), true);
        } else {
            listener.onVideoDownloadRequested(message, getAdapterPosition());
            progressBar.setVisibility(View.VISIBLE);
            playButton.setVisibility(View.GONE);
            message.isLoading = true;
        }
    }

    @Override
    public void roundCorners(ChatMessage message) {
        image.setCornerRadii(getCornerRadiiFor(message));
    }
}
