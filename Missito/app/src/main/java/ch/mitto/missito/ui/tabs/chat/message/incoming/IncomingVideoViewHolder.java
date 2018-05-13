package ch.mitto.missito.ui.tabs.chat.message.incoming;

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
import ch.mitto.missito.ui.tabs.chat.adapter.IncomingChatMessage;
import ch.mitto.missito.ui.tabs.chat.message.ChatCellHelper;
import ch.mitto.missito.ui.tabs.chat.message.VideoCellListener;
import ch.mitto.missito.ui.tabs.chat.view.ChatRoundedImageView;
import ch.mitto.missito.util.Helper;
import ch.mitto.missito.util.ImageHelper;

public class IncomingVideoViewHolder extends IncomingMessageViewHolder {

    private VideoCellListener listener;
    private AsyncTask<Void, Void, Bitmap> thumbnailAsyncTask;

    private VideoAttachRec missitoVideo;
    private IncomingChatMessage message;

    @BindView(R.id.bubble)
    ChatRoundedImageView image;

    @BindView(R.id.progress)
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

    public IncomingVideoViewHolder(View itemView, VideoCellListener listener) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        this.listener = listener;
    }

    public void setMessage(final IncomingChatMessage message) {
        super.setMessage(message);
        this.message = message;
        missitoVideo = message.attachmentRec.video.get(0);

        if (thumbnailAsyncTask != null && !thumbnailAsyncTask.isCancelled()) {
            thumbnailAsyncTask.cancel(true);
        }
        thumbnailAsyncTask = ImageHelper.fromBase64(missitoVideo.thumbnail, image);
        thumbnailAsyncTask.execute();
        progressBar.setVisibility(message.isLoading ? View.VISIBLE : View.GONE);
        if (missitoVideo.localFileURI != null) {
            playButton.setImageResource(R.drawable.ic_play_white);
        } else {
            playButton.setImageResource(R.drawable.ic_download);
        }
        playButton.setVisibility(message.isLoading ? View.GONE : View.VISIBLE);
        dotsMenu.setVisibility(message.isLoading || missitoVideo.localFileURI == null ? View.GONE : View.VISIBLE);
    }

    @OnClick(R.id.btn_play)
    public void onPlayClick() {
        if (missitoVideo.localFileURI != null) {
            Helper.startVideoPlayer(missitoVideo.localFileURI, image.getContext(), false);
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
