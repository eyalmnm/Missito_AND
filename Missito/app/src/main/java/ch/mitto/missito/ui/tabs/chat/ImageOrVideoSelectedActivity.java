package ch.mitto.missito.ui.tabs.chat;

import android.ch.mitto.missito.R;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.imagepipeline.image.ImageInfo;
import com.stfalcon.frescoimageviewer.drawee.ZoomableDraweeView;

import java.io.File;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;

/**
 * ImageOrVideoSelectedActivity takes {@link java.io.File} that points to selected image and shows it
 * stretched to fullscreen (when possible) giving the ability to add text message and decide
 * whether to send selected image or not
 * Also {@link ZoomableDraweeView} allows to zoom image
 */

public class ImageOrVideoSelectedActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    public static final String IMAGE_FILE_KEY = "img_file_key";
    public static final String IS_FROM_CAMERA_KEY = "is_from_camera_key";
    private static final String LOG_TAG = ImageOrVideoSelectedActivity.class.getSimpleName();
    private final int MAX_PROGRESS = 200;
    private final int UPDATE_INTERVAL_PROGRESS = 50; //milis => 20fps
    private int heightControllerPx;
    private Handler mHandler = new Handler();
    private File file;
    private boolean createdFromCamera;

    @BindView(R.id.result_image)
    ZoomableDraweeView resultPhotoDraweeView;

    @BindView(R.id.result_video)
    VideoView resultVideoView;

    @OnTouch(R.id.result_video)
    public boolean hideShowController(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (videoController.getTranslationY() == 0) {
                videoController.animate().translationY(heightControllerPx).setDuration(300);
            } else {
                videoController.animate().translationY(0).setDuration(300);
            }
        }
        return true;
    }


    @BindView(R.id.video_controller)
    LinearLayout videoController;
    @BindView(R.id.pause_play_video)
    ImageView pausePlayBtn;

    @OnClick(R.id.pause_play_video)
    public void pauseOrPlay() {
        if (resultVideoView.isPlaying()) {
            mHandler.removeCallbacks(updateTimeTask);
            resultVideoView.pause();
            pausePlayBtn.setImageResource(R.drawable.ic_play_video_result);
        } else {
            updateProgressBar();
            resultVideoView.start();
            pausePlayBtn.setImageResource(R.drawable.ic_pause_video_result);

        }
    }

    @BindView(R.id.seekBarController)
    SeekBar seekBarController;
    @BindView(R.id.timeVideo)
    TextView timeInfoVideoView;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    boolean confirmSend = false;

    @OnClick(R.id.send_button)
    public void send() {
        Intent data = new Intent();
        data.putExtra(IMAGE_FILE_KEY, file.getAbsolutePath());
        setResult(RESULT_OK, data);
        confirmSend = true;
        finish();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeNoTitleBar_Fullscreen);
        setContentView(R.layout.activity_image_preview);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("");
        }
        heightControllerPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
        seekBarController.setOnSeekBarChangeListener(this);
        seekBarController.setProgress(0);
        seekBarController.setMax(MAX_PROGRESS);

        String filePath = getIntent().getStringExtra(IMAGE_FILE_KEY);
        createdFromCamera = getIntent().getBooleanExtra(IS_FROM_CAMERA_KEY, false);
        file = new File(filePath);
        if (!file.exists()) {
            finish();
            return;
        }
        String mimeTypeCamera = URLConnection.guessContentTypeFromName(filePath);
        if (mimeTypeCamera != null && mimeTypeCamera.startsWith("image")) {
            showImage(file);
        } else {
            prepareVideoView(file);
        }

    }

    private void prepareVideoView(File file) {
        resultVideoView.setVisibility(View.VISIBLE);
        videoController.setVisibility(View.VISIBLE);
        resultVideoView.setClickable(true);
        resultVideoView.setVideoPath(file.getAbsolutePath());
        setThumbnail();
        resultVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                updateTimerView();
            }
        });
        resultVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                pausePlayBtn.setImageResource(R.drawable.ic_play_video_result);
            }
        });
    }

    private void setThumbnail() {
        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(thumb);
        resultVideoView.setBackground(bitmapDrawable);
    }


    private void showImage(File file) {
        resultPhotoDraweeView.setVisibility(View.VISIBLE);
        PipelineDraweeControllerBuilder controller = Fresco.newDraweeControllerBuilder();
        controller.setUri(Uri.fromFile(file));
        controller.setOldController(resultPhotoDraweeView.getController());
        controller.setControllerListener(new BaseControllerListener<ImageInfo>() {
            @Override
            public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                super.onFinalImageSet(id, imageInfo, animatable);
                if (imageInfo == null || resultPhotoDraweeView == null) {
                    return;
                }
                resultPhotoDraweeView.update(imageInfo.getWidth(), imageInfo.getHeight());
            }
        });
        resultPhotoDraweeView.setController(controller.build());
    }


    private void updateProgressBar() {
        mHandler.postDelayed(updateTimeTask, UPDATE_INTERVAL_PROGRESS);
        resultVideoView.setBackground(null); //clear thumbnail
    }

    private Runnable updateTimeTask = new Runnable() {
        @Override
        public void run() {
            updateTimerView();
            int progress = getProgressPercentage(resultVideoView.getCurrentPosition(), resultVideoView.getDuration());
            seekBarController.setProgress(progress);
            mHandler.postDelayed(this, UPDATE_INTERVAL_PROGRESS);
        }
    };

    private void updateTimerView() {
        int currentPos = resultVideoView.getCurrentPosition() / 1000;
        int duration = resultVideoView.getDuration() / 1000;
        String progressStr = String.format("%d:%02d / %d:%02d", currentPos / 60, currentPos % 60,
                duration / 60, duration % 60);
        timeInfoVideoView.setText(progressStr);

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(updateTimeTask);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int currentPosition = progressToTimer(seekBar.getProgress(), resultVideoView.getDuration());
        resultVideoView.seekTo(currentPosition);
        updateProgressBar();
    }


    public int getProgressPercentage(long currentDuration, long totalDuration) {
        Double progress = (((double) currentDuration) / totalDuration) * MAX_PROGRESS;
        return progress.intValue();
    }


    public int progressToTimer(int progress, int totalDuration) {
        return (int) ((((double) progress) / MAX_PROGRESS) * totalDuration);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        confirmSend = true;
        outState.putInt("position", resultVideoView.getCurrentPosition());
        outState.putBoolean("isPlaying", resultVideoView.isPlaying());
        outState.putString("filePath", file.getAbsolutePath());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        confirmSend = false;
        file = new File(savedInstanceState.getString("filePath"));
        int position = savedInstanceState.getInt("position");
        boolean isPlaying = savedInstanceState.getBoolean("isPlaying");
        if (resultVideoView.getVisibility() == View.VISIBLE) {
            resultVideoView.seekTo(position);
            if (isPlaying) {
                resultVideoView.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!confirmSend && createdFromCamera) {
            file.delete();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
        }
        return (super.onOptionsItemSelected(menuItem));
    }
}
