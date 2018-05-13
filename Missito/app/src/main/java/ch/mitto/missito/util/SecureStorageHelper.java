package ch.mitto.missito.util;

import android.content.SharedPreferences;
import android.util.Base64;

import ch.mitto.missito.Application;
import ch.mitto.missito.net.ConnectionManager;

import static android.content.Context.MODE_PRIVATE;

// TODO: we must use secure storage here
public class SecureStorageHelper {

    private static String USER_ID_KEY = "user_id";
    private static String USER_TOKEN_KEY = "user_token";
    private static String REALM_KEY_KEY = "realm_key";

    private static SharedPreferences getPref() {
        String userId;
        if (Application.app.connectionManager != null &&
                Application.app.connectionManager.authState == ConnectionManager.AuthState.LOGGED_IN) {
            userId = Application.app.connectionManager.uid;
        } else {
            userId = "default";
        }
        return Application.app.getApplicationContext()
                .getSharedPreferences("missito_" + userId, MODE_PRIVATE);
    }

    private static SharedPreferences getDefaultPref() {
        return Application.app.getApplicationContext()
                .getSharedPreferences("missito_default", MODE_PRIVATE);
    }

    private static SharedPreferences.Editor getEditor() {
        return getPref().edit();
    }

    public static void saveUserId(String userId) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(USER_ID_KEY, userId);
        editor.apply();
    }

    public static void saveUserToken(String userToken) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(USER_ID_KEY, userToken);
        editor.apply();
    }

    public static String getUserId() {
        return getPref().getString(USER_ID_KEY, null);
    }

    public static String getUserToken() {
        return getPref().getString(USER_TOKEN_KEY, null);
    }

    public static void saveUserAuthData(String userId, String userToken) {
        SharedPreferences.Editor editor = getDefaultPref().edit();
        editor.putString(USER_ID_KEY, userId);
        editor.putString(USER_TOKEN_KEY, userToken);
        editor.apply();
    }

    public static void removeUserAuthData() {
        SharedPreferences.Editor editorDefault = getDefaultPref().edit();
        editorDefault.remove(USER_ID_KEY);
        editorDefault.remove(USER_TOKEN_KEY);
        editorDefault.apply();
    }

    public static byte[] getRealmKey(String userId) {
        String dataB64 = getPref().getString(REALM_KEY_KEY + "_" + userId, null);
        return dataB64 == null ? null : Base64.decode(dataB64, Base64.DEFAULT);
    }

    public static void saveRealmKey(String userId, byte[] key) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(REALM_KEY_KEY + "_" + userId, Base64.encodeToString(key, Base64.DEFAULT));
        editor.apply();
    }
}
