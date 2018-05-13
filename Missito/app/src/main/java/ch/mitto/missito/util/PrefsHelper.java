package ch.mitto.missito.util;

import android.content.SharedPreferences;

import ch.mitto.missito.Application;
import ch.mitto.missito.net.ConnectionManager;

import static android.content.Context.MODE_PRIVATE;

public class PrefsHelper {

    private static String REPORT_KEYS_FLAG_KEY = "report_keys_flag";
    private static String REPORT_CLOUD_TOKEN_FLAG_KEY = "cloud_token_keys_flag";
    private static String LAST_REPORTED_OTPK_ID_KEY = "last_reported_otpk_id";
    private static String FIRST_CONTACT_STATUS_UPDATE_KEY = "first_contact_status_update";
    public static final String PREFS_NEXT_SMS_RESEND_ATTEMPT_TIME = "smsResendTimeout";
    public static final String PREFS_OTP_TOKEN = "otpToken";
    public static final String INVITE_DATE = "invite_date";
    public static final String PREFS_COUNTRY_CODE = "country_code";
    private static String DEVICE_ID = "device_id";
    private static final String ONLINE_STATUS_CHANGE_TIME = "online_status_change_time";
    private static final String PREFS_USERNAME = "username";

    private static SharedPreferences getPref() {
        String userId;
        if (Application.app.connectionManager != null &&
                Application.app.connectionManager.authState == ConnectionManager.AuthState.LOGGED_IN) {
            userId = Application.app.connectionManager.uid;
        } else {
            userId = "default";
        }
        return getPref(userId);
    }

    private static SharedPreferences getPref(String userId) {
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

    public static void saveReportKeysFlag(boolean flag) {
        SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(REPORT_KEYS_FLAG_KEY, flag);
        editor.apply();
    }

    public static boolean getReportKeysFlag() {
        return getPref().getBoolean(REPORT_KEYS_FLAG_KEY, false);
    }

    public static void saveLastReportedOtpkId(int id) {
        SharedPreferences.Editor editor = getEditor();
        editor.putInt(LAST_REPORTED_OTPK_ID_KEY, id);
        editor.apply();
    }

    public static int getLastReportedOtpkId() {
        return getPref().getInt(LAST_REPORTED_OTPK_ID_KEY, 0);
    }

    public static void setReportCloudTokenFlag(Boolean reported) {
        getDefaultPref().edit().putBoolean(REPORT_CLOUD_TOKEN_FLAG_KEY, reported).apply();
    }

    public static boolean getReportCloudTokenFlag() {
        return getDefaultPref().getBoolean(REPORT_CLOUD_TOKEN_FLAG_KEY, false);
    }

    public static boolean getFirstContactStatusUpdateFlag() {
        return getPref().getBoolean(FIRST_CONTACT_STATUS_UPDATE_KEY, true);
    }

    public static void setFirstContactStatusUpdateFlag(Boolean flag) {
        getPref().edit().putBoolean(FIRST_CONTACT_STATUS_UPDATE_KEY, flag).apply();
    }

    public static void setNextSmsResendTime(long timeout) {
        getDefaultPref().edit().putLong(PREFS_NEXT_SMS_RESEND_ATTEMPT_TIME, System.currentTimeMillis() + timeout).apply();
    }

    public static long getNextSmsResendTime() {
        return getDefaultPref().getLong(PREFS_NEXT_SMS_RESEND_ATTEMPT_TIME, 0);
    }

    public static void saveOtpToken(String token) {
        getDefaultPref().edit().putString(PREFS_OTP_TOKEN, token).apply();
    }

    public static String getOtpToken() {
        return getDefaultPref().getString(PREFS_OTP_TOKEN, "");
    }

    public static void setInvitationDate(long value) {
        getPref().edit().putLong(INVITE_DATE, value).apply();
    }

    public static long getInvitationDate() {
        return getPref().getLong(INVITE_DATE, -1);
    }

    public static void removeInvitationDate(){
        getPref().edit().remove(INVITE_DATE).apply();
    }

    public static void setCountryCode(String code){
        getDefaultPref().edit().putString(PREFS_COUNTRY_CODE, code).apply();
    }

    public static String getCountryCode(){
        return getDefaultPref().getString(PREFS_COUNTRY_CODE, "");
    }

    public static void removeCountryCode(){
        getDefaultPref().edit().remove(PREFS_COUNTRY_CODE).apply();
    }

    public static void setDeviceId(String userId, int deviceId){
        getPref(userId).edit().putInt(DEVICE_ID, deviceId).apply();
    }

    public static int getDeviceId(String userId){
        return getPref(userId).getInt(DEVICE_ID, 0);
    }

    public static void setOnlineStatusChangeTime(long value) {
        getPref().edit().putLong(ONLINE_STATUS_CHANGE_TIME, value).apply();
    }

    public static long getOnlineStatusChangeTime() {
        return getPref().getLong(ONLINE_STATUS_CHANGE_TIME, -1);
    }

    public static void saveUsername(String username) {
        getPref().edit().putString(PREFS_USERNAME, username).apply();
    }

    public static String getUsername(){
        return getPref().getString(PREFS_USERNAME, "");
    }

}
