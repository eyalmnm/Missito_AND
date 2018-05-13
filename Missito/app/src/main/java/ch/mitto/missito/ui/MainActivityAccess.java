package ch.mitto.missito.ui;

import android.content.Context;
import android.support.v4.app.Fragment;

/**
 * Interface for accessing the {@link MainActivity} from inside fragments.
 *
 * Yeah, a fragment could call {@link Fragment#getActivity()} and cast it to {@link MainActivity},
 * but we'll try using {@link Fragment#onAttach(Context)} and {@link Fragment#onDetach()} for this.
 *
 * This will reduce some cast/access code and will expose only a subset of methods
 */

public interface MainActivityAccess {
    /**
     * Update title and home button display
     *
     * @param title
     * @param homeButtonFlag
     */
    void updateTitle(String title, boolean homeButtonFlag);

    /**
     * When someone has triggered "Invite" process.
     * @param sender
     */
    void onInviteCalled(Fragment sender);
}
