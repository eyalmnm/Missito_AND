package ch.mitto.missito.util;

import ch.mitto.missito.db.model.MessageRec;

/**
 * Created by usr1 on 10/24/17.
 */

public interface SendMessageListener {

    void attachmentUploaded(MessageRec message);

}
