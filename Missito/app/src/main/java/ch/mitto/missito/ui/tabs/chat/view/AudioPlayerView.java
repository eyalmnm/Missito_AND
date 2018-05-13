package ch.mitto.missito.ui.tabs.chat.view;

import android.animation.ObjectAnimator;
import android.ch.mitto.missito.R;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.db.model.attach.AudioAttachRec;
import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;
import ch.mitto.missito.ui.tabs.chat.message.AudioPlayerDownloadListener;
import ch.mitto.missito.util.MediaPlayerSingleton;

public class AudioPlayerView extends RelativeLayout implements MediaPlayerSingleton.Listener {

    private static final String LOG_TAG = AudioPlayerView.class.getSimpleName();

    @BindView(R.id.seekbar)
    public SeekBar seekBar;

    @BindView(R.id.play_btn)
    ImageButton play;

    @BindView(R.id.time_txt)
    TextView time;

    @Nullable
    @BindView(R.id.audio_download_progress)
    ProgressBar downloadProgressBar;

    private Handler handler = new Handler();
    private AudioAttachRec audio;
    private ChatMessage audioMessage;
    private MediaPlayerSingleton player;
    private boolean isPlaying;
    private boolean prepared;

    private Runnable seekBarUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (prepared && isPlaying) {
                seekBar.clearAnimation();
                ObjectAnimator animation = ObjectAnimator.ofInt(seekBar, "progress", seekBar.getProgress(), player.getCurrentPosition());
                animation.setDuration(50);
                animation.setInterpolator(new LinearInterpolator());
                animation.start();

                setTimeLabel(player.getCurrentPosition());
                updateUiWithDelay(50);
            }
        }
    };

    public AudioPlayerView(Context context) {
        super(context);
    }

    public AudioPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AudioPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public int getAudioDuration(Context context) {
        if (audio.localFileURI == null) {
            return 0;
        }
        MediaPlayer player = MediaPlayer.create(context, Uri.parse(audio.localFileURI));
        int duration = 0;
        if (player != null) {
            duration = player.getDuration();
            player.release();
        }
        return duration;
    }

    @Override
    public void onDataSourceChange() {
        stopPlaying();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
    }

    //TODO: remove 'final int position'. Chat cell (or its subviews) should not know anything about cell's position in adapter
    public void setMessage(final ChatMessage audioMessage, final AudioPlayerDownloadListener downloadListener, final int position) {
        audio = audioMessage.attachmentRec.audio.get(0);
        this.audioMessage = audioMessage;
        int audioDuration = getAudioDuration(getContext());
        seekBar.setMax(audioDuration);

        if (downloadProgressBar != null) {
            downloadProgressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
        }

        if (audioMessage.isLoading) {
            play.setImageDrawable(null);
            setProgressBarVisibility(View.VISIBLE);
        } else {
            if (audioMessage.attachmentRec.audio.get(0).localFileURI != null) {
                play.setImageResource(R.drawable.ic_play);
            } else{
                play.setImageResource(R.drawable.ic_download);
            }
            setProgressBarVisibility(View.GONE);
        }

        seekBar.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        seekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (prepared) {
                        updateSeekBar(progress, false);
                    } else {
                        preparePlayerFromPoint(progress);
                        updateUiWithDelay();
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (prepared) {
                    pausePlaying();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (prepared) {
                    resumePlaying();
                }
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!audioMessage.isLoading) {
                    if (audio.localFileURI == null) {
                        audioMessage.isLoading = true;
                        downloadListener.onAudioDownloadRequested(audioMessage, position);
                        play.setImageDrawable(null);
                        setProgressBarVisibility(View.VISIBLE);
                    } else {
                        if (prepared) {
                            updateSeekBar(player.getCurrentPosition(), false);
                        }
                        onPlay(isPlaying);
                    }
                }
            }
        });

        setTimeLabel(audioDuration);
    }

    private void setProgressBarVisibility(int visibility) {
        if (downloadProgressBar != null) {
            downloadProgressBar.setVisibility(visibility);
        }
    }

    private void setTimeLabel(int audioDuration) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(audioDuration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(audioDuration)
                - TimeUnit.MINUTES.toSeconds(minutes);

        time.setText(String.format(getContext().getString(R.string.time_format), minutes, seconds));
    }

    private void updateSeekBar(int progress, boolean updateHandler) {
        player.seekTo(progress);
        handler.removeCallbacks(seekBarUpdateRunnable);

        setTimeLabel(player.getCurrentPosition());
        if (updateHandler) {
            updateUiWithDelay();
        } else {
            seekBar.setProgress(player.getCurrentPosition());
        }
    }

    private void onPlay(boolean isPlaying) {
        if (!isPlaying) {
            if (!prepared) {
                startPlaying();
            } else {
                resumePlaying();
            }
        } else {
            pausePlaying();
        }
    }

    private void startPlaying() {
        try {
            player = MediaPlayerSingleton.getInstance();
            player.setDataSource(getContext(), audio);
            player.setOnDataSourceChangeListener(this);

            player.prepare();
            play.setImageResource(R.drawable.ic_pause);
            seekBar.setMax(player.getDuration());

            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    prepared = true;
                    player.start();
                    updateUiWithDelay();
                    isPlaying = true;
                }
            });

        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlaying();
            }
        });
    }

    public void stopPlaying() {
        isPlaying = false;
        play.setImageResource(R.drawable.ic_play);
        handler.removeCallbacks(seekBarUpdateRunnable);
        if (prepared) {
            player.stop();
            player.reset();
            prepared = false;
        }

        seekBar.clearAnimation();
        seekBar.setProgress(0);

        int audioDuration = getAudioDuration(getContext());
        setTimeLabel(audioDuration);
    }

    private void resumePlaying() {
        isPlaying = true;
        play.setImageResource(R.drawable.ic_pause);
        handler.removeCallbacks(seekBarUpdateRunnable);
        player.start();
        updateUiWithDelay();
    }

    private void pausePlaying() {
        isPlaying = false;
        seekBar.clearAnimation();
        play.setImageResource(R.drawable.ic_play);
        handler.removeCallbacks(seekBarUpdateRunnable);
        player.pause();
    }

    private void preparePlayerFromPoint(int progress) {

        try {
            player = MediaPlayerSingleton.getInstance();
            player.setDataSource(getContext(), audio);
            player.setOnDataSourceChangeListener(this);

            player.prepare();
            play.setImageResource(R.drawable.ic_pause);
            seekBar.setMax(player.getDuration());
            seekBar.setProgress(progress);
            player.seekTo(progress);
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    prepared = true;
                    player.start();
                    isPlaying = true;
                    updateUiWithDelay(20);
                }
            });

            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    stopPlaying();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateUiWithDelay(int... params) {
        handler.postDelayed(seekBarUpdateRunnable, params.length > 0 ? params[0] : 0);
    }
}
