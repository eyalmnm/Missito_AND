package ch.mitto.missito.ui.tabs.chat.message;

import android.ch.mitto.missito.R;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.SectionedViewHolder;

import java.util.EnumMap;

import butterknife.BindView;
import ch.mitto.missito.Application;
import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;

import static android.text.format.DateUtils.FORMAT_SHOW_TIME;

public class BaseChatMessageViewHolder extends SectionedViewHolder {

    private static final String LOG_TAG = BaseChatMessageViewHolder.class.getSimpleName();

    private static final EnumMap<ChatMessage.MessageInGroupType, float[]> INCOMING_CELL_RADII = new EnumMap<>(ChatMessage.MessageInGroupType.class);
    private static final EnumMap<ChatMessage.MessageInGroupType, float[]> OUTGOING_CELL_RADII = new EnumMap<>(ChatMessage.MessageInGroupType.class);

    public static float defaultChatCellTopMargin = Application.app.getResources().getDimension(R.dimen.chat_cell_top_margin);
    public static float dp8 = Application.app.getResources().getDimension(R.dimen.dp8);
    public static float dp30 = Application.app.getResources().getDimension(R.dimen.dp30);
    protected static float dp22 = Application.app.getResources().getDimension(R.dimen.dp22);

    protected ChatMessage message;

    static {
        float dp4 = Application.app.getResources().getDimension(R.dimen.dp4);

        OUTGOING_CELL_RADII.put(ChatMessage.MessageInGroupType.SINGLE, new float[]{dp22, dp22, dp22, dp22, dp22, dp22, dp22, dp22});
        OUTGOING_CELL_RADII.put(ChatMessage.MessageInGroupType.FIRST, new float[]{dp22, dp22, dp22, dp22, dp4, dp4, dp22, dp22});
        OUTGOING_CELL_RADII.put(ChatMessage.MessageInGroupType.MIDDLE, new float[]{dp22, dp22, dp4, dp4, dp4, dp4, dp22, dp22});
        OUTGOING_CELL_RADII.put(ChatMessage.MessageInGroupType.LAST, new float[]{dp22, dp22, dp4, dp4, dp22, dp22, dp22, dp22});

        INCOMING_CELL_RADII.put(ChatMessage.MessageInGroupType.SINGLE, new float[]{dp22, dp22, dp22, dp22, dp22, dp22, dp22, dp22});
        INCOMING_CELL_RADII.put(ChatMessage.MessageInGroupType.FIRST, new float[]{dp22, dp22, dp22, dp22, dp22, dp22, dp4, dp4});
        INCOMING_CELL_RADII.put(ChatMessage.MessageInGroupType.MIDDLE, new float[]{dp4, dp4, dp22, dp22, dp22, dp22, dp4, dp4});
        INCOMING_CELL_RADII.put(ChatMessage.MessageInGroupType.LAST, new float[]{dp4, dp4, dp22, dp22, dp22, dp22, dp22, dp22});
    }

    protected Context context;
    @BindView(R.id.date_txt)
    protected TextView dateText;
    @BindView(R.id.bubble)
    protected View bubble;

    public BaseChatMessageViewHolder(View itemView) {
        super(itemView);
        context = itemView.getContext();
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
        dateText.setText(DateUtils.formatDateTime(Application.app, message.date.getTime(), FORMAT_SHOW_TIME));
        roundCorners(message);
        ViewGroup.LayoutParams layoutParams = ((View) bubble.getParent()).getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) layoutParams).topMargin =
                    (int) (message.inGroupType == ChatMessage.MessageInGroupType.FIRST || message.inGroupType == ChatMessage.MessageInGroupType.SINGLE
                            ? dp8
                            : defaultChatCellTopMargin);
            ((View) bubble.getParent()).setLayoutParams(layoutParams);
        }
    }

    public void roundCorners(ChatMessage message) {
        float[] cornerRadii = getCornerRadiiFor(message);

        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(context.getResources().getColor(message.direction == ChatMessage.Direction.OUTGOING ? R.color.colorPrimary : R.color.athensGray));
        gradientDrawable.setCornerRadii(cornerRadii);
        bubble.setBackground(gradientDrawable);
    }

    protected float[] getCornerRadiiFor(ChatMessage message) {
        if (message.direction == ChatMessage.Direction.OUTGOING) {
            return OUTGOING_CELL_RADII.get(message.inGroupType);
        } else {
            return INCOMING_CELL_RADII.get(message.inGroupType);
        }
    }
}
