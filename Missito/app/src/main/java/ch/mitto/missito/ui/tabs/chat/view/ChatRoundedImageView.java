package ch.mitto.missito.ui.tabs.chat.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * Class {@link ChatRoundedImageView} extends standard {@link AppCompatImageView}
 * to change shape of a view to match appropriate {@link ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage.MessageInGroupType}
 * set to {@link ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage}
 */

public class ChatRoundedImageView extends AppCompatImageView {

    private Path path = new Path();
    private float[] cornerRadii;

    {
        setWillNotDraw(false);
    }

    public ChatRoundedImageView(Context context) {
        super(context);
    }

    public ChatRoundedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChatRoundedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setCornerRadii(float[] cornerRadii) {
        this.cornerRadii = cornerRadii;
        int w = getWidth();
        int h = getHeight();
        if (w > 0 && h > 0) {
            setupPath(w, h);
        }
        invalidate();
    }

    private void setupPath(int width, int height) {
        path.reset();
        RectF rectF = new RectF(0, 0, width, height);
        path.addRoundRect(rectF, cornerRadii, Path.Direction.CW);
        path.close();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (isCornerRadiiSet()) {
            setupPath(w, h);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isCornerRadiiSet()) {
            canvas.clipPath(path);
        }
        super.onDraw(canvas);
    }

    private boolean isCornerRadiiSet() {
        return cornerRadii != null;
    }
}
