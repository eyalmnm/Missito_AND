package ch.mitto.missito.net.webapi;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.damnhandy.uri.template.UriTemplate;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.mitto.missito.model.message.ContactData;
import ch.mitto.missito.Application;
import ch.mitto.missito.net.model.AttachmentSpec;
import ch.mitto.missito.net.model.CloudTokenUpdate;
import ch.mitto.missito.net.model.ContactsStatusUpdateRequest;
import ch.mitto.missito.net.model.InviteRequest;
import ch.mitto.missito.net.model.MessageIdResponse;
import ch.mitto.missito.net.model.OTPCheckResponse;
import ch.mitto.missito.net.model.OTPReqResponse;
import ch.mitto.missito.net.model.OutgoingMessage;
import ch.mitto.missito.net.model.ProfileSettings;
import ch.mitto.missito.net.model.StatusUpdateRequest;
import ch.mitto.missito.net.model.UpdateUserNameObj;
import ch.mitto.missito.net.model.UpdatedMessages;
import ch.mitto.missito.security.signal.model.IdentityData;
import ch.mitto.missito.security.signal.model.NewSessionData;
import ch.mitto.missito.security.signal.model.OTPKeysData;
import ch.mitto.missito.security.signal.model.SignedPreKeyData;
import ch.mitto.missito.ui.MainActivity;
import ch.mitto.missito.util.Helper;

public class APIRequests {

    private static final String LOG_TAG = APIRequests.class.getSimpleName();

    private String baseURL;
    private RequestQueue queue;
    private String backendToken;

    public APIRequests(Context ctx, String baseURL) {
        this.baseURL = baseURL;
        queue = Volley.newRequestQueue(ctx);
    }

    public void setBackendToken(String backendToken) {
        this.backendToken = backendToken;
    }

    private abstract class AuthorizedJsonRequest<T> extends JsonRequest<T> {

        public AuthorizedJsonRequest(int method, String url, String requestBody, Response.Listener<T> listener, final Response.ErrorListener errorListener) {
            super(method, url, requestBody, listener, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (error.networkResponse != null && error.networkResponse.statusCode == 403) {
                        logoutAndOpenAuthPage();
                    }
                    errorListener.onErrorResponse(error);
                }
            });
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> headers = super.getHeaders();
            if (backendToken != null) {
                headers = new HashMap<>(headers);
                headers.put("Authorization", "Bearer " + backendToken);
            }
            return headers;
        }
    }

    private class APIJsonRequest<T> extends AuthorizedJsonRequest<T> {

        private Class<T> type;

        public APIJsonRequest(int method, String url, String requestBody,
                              Response.Listener<T> listener,
                              Response.ErrorListener errorListener, Class<T> type) {
            super(method, url, requestBody, listener, errorListener);
            this.type = type;
        }

        @Override
        protected Response<T> parseNetworkResponse(NetworkResponse response) {
            String json;
            try {
                json = new String(
                        response.data,
                        HttpHeaderParser.parseCharset(response.headers));
            } catch (Exception e) {
                return Response.error(new VolleyError(e));
            }
            T result = new Gson().fromJson(json, type);
            return Response.success(result, null);
        }
    }

    private class APISimpleRequest extends AuthorizedJsonRequest<Void> {

        public APISimpleRequest(int method, String url, String requestBody,
                                Response.Listener<Void> listener,
                                Response.ErrorListener errorListener) {
            super(method, url, requestBody, listener, errorListener);
        }

        @Override
        protected Response<Void> parseNetworkResponse(NetworkResponse response) {
            if (response.statusCode != 200) {
                return Response.error(new VolleyError("Status code: " + response.statusCode));
            }
            return Response.success(null, null);
        }
    }


    public class AuthRequest<T> extends Request<T> {

        private final Response.Listener<T> listener;
        private Class<T> type;
        private Map<String, String> params;

        public AuthRequest(String url, Map<String, String> params,
                           Response.Listener<T> listener,
                           Response.ErrorListener errorListener, Class<T> type) {
            super(Request.Method.POST, url, errorListener);
            this.type = type;
            this.listener = listener;
            this.params = params;
        }

        @Override
        public Map<String, String> getParams() {
            return params;
        }

        @Override
        protected void deliverResponse(T response) {
            listener.onResponse(response);
        }

        @Override
        protected VolleyError parseNetworkError(VolleyError volleyError) {
            Log.e(LOG_TAG, "parseNetworkError", volleyError);
            return super.parseNetworkError(volleyError);
        }

        @Override
        protected Response<T> parseNetworkResponse(NetworkResponse response) {
            String json;
            try {
                json = new String(
                        response.data,
                        HttpHeaderParser.parseCharset(response.headers));
            } catch (Exception e) {
                return Response.error(new VolleyError(e));
            }
            T result = new Gson().fromJson(json, type);
            return Response.success(result, null);
        }
    }


    private <T> void enqueueJsonRequest(int method, String url, Response.Listener<T> onResult,
                                        Response.ErrorListener onError, Class<T> type) {
        enqueueJsonRequest(method, url, onResult, onError, type, new DefaultRetryPolicy());

    }

    private <T> void enqueueJsonRequest(int method, String url, String body, Response.Listener<T> onResult,
                                        Response.ErrorListener onError, Class<T> type) {
        enqueueJsonRequest(method, url, body, onResult, onError, type, new DefaultRetryPolicy());
    }


    private <T> void enqueueJsonRequest(int method, String url, Response.Listener<T> onResult,
                                        Response.ErrorListener onError, Class<T> type, RetryPolicy retryPolicy) {

        Log.d(LOG_TAG, "Requesting: " + url + " data: none");
        enqueueRequest(new APIJsonRequest<T>(method, url, null, onResult, onError, type).setRetryPolicy(retryPolicy));

    }

    private <T> void enqueueJsonRequest(int method, String url, String body, Response.Listener<T> onResult,
                                        Response.ErrorListener onError, Class<T> type, RetryPolicy retryPolicy) {

        Log.d(LOG_TAG, "Requesting: " + url + " data: " + body);
        enqueueRequest(new APIJsonRequest<T>(method, url, body, onResult, onError, type).setRetryPolicy(retryPolicy));
    }

    private <T> void enqueueAuthRequest(String url, Map<String, String> params, Response.Listener<T> onResult,
                                        final Response.ErrorListener onError, Class<T> type) {

        Log.d(LOG_TAG, "Requesting: " + url + " data: " + params);
        enqueueRequest(new AuthRequest<>(url, params, onResult, onError, type));
    }

    private void enqueueSimpleRequest(int method, String url, Response.Listener<Void> onResult,
                                      Response.ErrorListener onError) {

        Log.d(LOG_TAG, "Requesting: " + url + " data: none");
        enqueueRequest(new APISimpleRequest(method, url, null, onResult, onError));

    }

    private void enqueueSimpleRequest(int method, String url, String body, Response.Listener<Void> onResult,
                                      Response.ErrorListener onError) {
        Log.d(LOG_TAG, "Requesting: " + url + " data: " + body);
        enqueueRequest(new APISimpleRequest(method, url, body, onResult, onError));
    }

    private <T> void enqueueRequest(Request<T> request) {
        queue.add(request);
    }

    private String urlencode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "Can't urlencode " + str);
            return "";
        }
    }

    // Auth requests

    public void requestOTP(String phone,
                           Response.Listener<OTPReqResponse> onResult, Response.ErrorListener onError) {


        enqueueAuthRequest(
                baseURL + "/otp/request",
                prepareOTPRequestParams(phone),
                onResult, onError, OTPReqResponse.class);

    }

    public void checkOTP(String token, String code, int deviceId,
                         Response.Listener<OTPCheckResponse> onResult, Response.ErrorListener onError) {

        enqueueAuthRequest(
                baseURL + "/otp/check",
                prepareOTPCheckParams(token, code, deviceId),
                onResult, onError, OTPCheckResponse.class);

    }

    // Update User name
    public static String USER_PROFILE_API = "/userProfile";
    public void updateUserName(UpdateUserNameObj updateUserNameObj,Response.Listener<Void> onResult, Response.ErrorListener onError) {

        enqueueSimpleRequest(Request.Method.PUT,
                baseURL + USER_PROFILE_API,
                new Gson().toJson(updateUserNameObj, UpdateUserNameObj.class),
                onResult, onError);
    }

    // Get User Name
    public void getUserName(Response.Listener<UpdateUserNameObj> onResult, Response.ErrorListener onError) {
        enqueueJsonRequest(Request.Method.GET,
                UriTemplate.fromTemplate(baseURL + USER_PROFILE_API)
                        .expand(),
                onResult, onError, UpdateUserNameObj.class);
    }

    public void uploadAttachment(AttachmentSpec attachmentSpec, final File file, AsyncHttpResponseHandler handler) {
        if (file.exists()) {

            RequestParams params = new RequestParams();
            for (Map.Entry<String, String> x : attachmentSpec.uploadFields.entrySet()) {
                params.put(x.getKey(), x.getValue());
            }
            params.put("Content-Type", "application/octet-stream");
            try {
                params.put("file", file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            AsyncHttpClient client = new AsyncHttpClient();
            client.post(attachmentSpec.uploadURL, params, handler);
        }
    }

    private Map<String, String> prepareOTPRequestParams(String phone) {
        Map<String, String> params = new HashMap<>(1);
        params.put("phone", phone);
        return params;
    }

    private Map<String, String> prepareOTPCheckParams(String token, String code, int deviceId) {
        Map<String, String> params = new HashMap<>(3);
        params.put("token", token);
        params.put("code", code);
        if (deviceId != 0) {
            params.put("deviceId", Integer.toString(deviceId));
        }
        return params;
    }


    // Old requests

    public void storeIdentity(IdentityData identityData,
                              Response.Listener<Void> onResult, Response.ErrorListener onError) {

        String body = new Gson().toJson(identityData);
        enqueueSimpleRequest(Request.Method.PUT,
                baseURL + "/identity",
                body, onResult, onError);
    }

    public void storeSignedPreKey(SignedPreKeyData signedPreKeyData,
                                  Response.Listener<Void> onResult, Response.ErrorListener onError) {

        String body = new Gson().toJson(signedPreKeyData);
        enqueueSimpleRequest(Request.Method.PUT,
                baseURL + "/signedPreKey",
                body, onResult, onError);
    }

    public void storeOTPKeys(OTPKeysData otpKeysData,
                             Response.Listener<Void> onResult, Response.ErrorListener onError) {

        String body = new Gson().toJson(otpKeysData);
        enqueueSimpleRequest(Request.Method.POST,
                baseURL + "/otpk",
                body, onResult, onError);
    }


    public void requestNewSession(String destUid, int destDeviceId,
                                  Response.Listener<NewSessionData> onResult, Response.ErrorListener onError) {

        enqueueJsonRequest(Request.Method.GET,
                UriTemplate.fromTemplate(baseURL + "/session{?uid,deviceId}")
                        .set("uid", Helper.removePlus(destUid))
                        .set("deviceId", destDeviceId)
                        .expand(),
                onResult, onError, NewSessionData.class);
    }

    public void sendMessage(OutgoingMessage message, Response.Listener<MessageIdResponse> onResult, Response.ErrorListener onError) {

        enqueueJsonRequest(Request.Method.POST,
                baseURL + "/p2p",
                new Gson().toJson(message, OutgoingMessage.class),
                onResult,
                onError,
                MessageIdResponse.class,
                new DefaultRetryPolicy(
                        DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                        0,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                ));
    }

    public void attach(String destUid, int destDeviceId, long byteSize, Response.Listener<AttachmentSpec> onResult, Response.ErrorListener onError) {

        enqueueJsonRequest(Request.Method.GET,
                UriTemplate.fromTemplate(baseURL + "/attach{?size,destUid,destDeviceId}")
                        .set("size", byteSize)
                        .set("destUid", Helper.removePlus(destUid))
                        .set("destDeviceId", destDeviceId)
                        .expand(),
                null,
                onResult, onError, AttachmentSpec.class);
    }

    public void addContacts(List<ContactData> contacts, Response.Listener<String> onResult, Response.ErrorListener onError) {
        List<ContactData> contactsToSend = new ArrayList<>();
        for (ContactData contactData : contacts) {
            contactsToSend.add(new ContactData(Helper.removePlus(contactData.phone),
                    contactData.firstName, contactData.lastName));
        }
        String templateString = baseURL + "/contacts";
        JSONObject body = new JSONObject();
        String json = new Gson().toJson(contactsToSend);
        try {
            body.put("contacts", new JSONArray(json));
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not add contacts to json", e);
            //TODO: throw an error here
        }
        enqueueJsonRequest(Request.Method.POST,
                UriTemplate.fromTemplate(templateString)
                        .expand(),
                body.toString(),
                onResult, onError, String.class);
    }


    public void setMessagesStatus(List<String> delivered, List<String> seen, Response.Listener<UpdatedMessages> onResult, Response.ErrorListener onError) {
        StatusUpdateRequest statusUpdate = new StatusUpdateRequest(delivered, seen);

        String templateString = baseURL + "/message/status";
        String body = new Gson().toJson(statusUpdate);
        enqueueJsonRequest(Request.Method.PUT,
                UriTemplate.fromTemplate(templateString)
                        .expand(),
                body,
                onResult, onError, UpdatedMessages.class);
    }

    public void setContactStatus(List<String> normal,
                                 List<String> blocked,
                                 List<String> muted,
                                 Response.Listener<String> onResult, Response.ErrorListener onError) {
        ContactsStatusUpdateRequest request = new ContactsStatusUpdateRequest(Helper.removePlus(normal),
                Helper.removePlus(blocked), Helper.removePlus(muted));

        String templateString = baseURL + "/contacts/status";
        String body = new Gson().toJson(request);
        enqueueJsonRequest(Request.Method.PUT,
                UriTemplate.fromTemplate(templateString)
                        .expand(),
                body,
                onResult, onError, String.class);
    }

    public void sendInvites(String lang, ArrayList<String> phones, Response.Listener<String> onResult, Response.ErrorListener onError) {
        List<String> cleanedPhones = Helper.removePlus(phones);
        InviteRequest request = new InviteRequest(lang, new ArrayList<>(cleanedPhones));

        String body = new Gson().toJson(request);
        enqueueJsonRequest(Request.Method.POST,
                baseURL + "/invite",
                body,
                onResult, onError, String.class);
    }

    public void updateCloudToken(Response.Listener<String> onResult, Response.ErrorListener onError) {
        CloudTokenUpdate tokenUpdate = new CloudTokenUpdate(
                FirebaseInstanceId.getInstance().getToken(),
                "google",
                Helper.getDeviceId());

        String body = new Gson().toJson(tokenUpdate);
        enqueueJsonRequest(Request.Method.PUT,
                baseURL + "/cloudtoken",
                body,
                onResult, onError, String.class);
    }

    public void deleteCloudToken(Response.Listener<String> onResult, Response.ErrorListener onError) {
        String templateString = baseURL + "/cloudtoken{?deviceId}";
        enqueueJsonRequest(Request.Method.DELETE,
                UriTemplate.fromTemplate(templateString)
                        .set("deviceId", Helper.getDeviceId())
                        .expand(),
                onResult, onError, String.class);
    }

    public void getProfileSettings(Response.Listener<ProfileSettings> onResult, Response.ErrorListener onError) {
        enqueueJsonRequest(Request.Method.GET,
                UriTemplate.fromTemplate(baseURL + "/profile")
                        .expand(),
                null,
                onResult, onError, ProfileSettings.class);
    }

    public void toggleStatusVisibility(boolean visible, Response.Listener<String> onResult, Response.ErrorListener onError) {
        ProfileSettings profileSettings = new ProfileSettings();
        profileSettings.presenceStatus = visible;

        String body = new Gson().toJson(profileSettings);

        Log.d(LOG_TAG, "toggleStatusVisibility: " + body);

        enqueueJsonRequest(Request.Method.PUT,
                UriTemplate.fromTemplate(baseURL + "/profile")
                        .expand(),
                body,
                onResult, onError, String.class);
    }

    public void allowSendMessageSeenStatus(boolean allow, Response.Listener<String> onResult, Response.ErrorListener onError) {
        ProfileSettings profileSettings = new ProfileSettings();
        profileSettings.messageStatus = allow;

        String body = new Gson().toJson(profileSettings);

        Log.d(LOG_TAG, "allowSendMessageSeenStatus: " + body);

        enqueueJsonRequest(Request.Method.PUT,
                UriTemplate.fromTemplate(baseURL + "/profile")
                        .expand(),
                body,
                onResult, onError, String.class);
    }

    private void logoutAndOpenAuthPage(){
        Application.app.connectionManager.logout();
        Intent intent = new Intent(Application.app, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Application.app.startActivity(intent);
    }
}


//    static let BASE_URL = "http://195.154.162.228:8082"
//
//
////    # store signed pre key
////    curl "http://missito:8080/signature_key/store?uid=55" -d "{\"id\":223, \"data\":\"some-data\", \"signature\":\"signofit\"}"
////
////    # store otp keys
////    curl "http://missito:8080/otp/store?uid=55" -d "{\"start_id\":0, \"otp_keys\":[\"key0\",\"key1\",\"key2\",\"key3\"]}"
////
////    #request session with user 55
////    curl "http://missito:8080/request_session?uid=55"
////    # reply: {"identity":{"identity_public_key":"identity-key-data","reg_id":123},"signed_pre_key_public":{"id":223,"data":"some-data","signature":"signofit"},"otp":{"id":0,"data":"key0"}}
//
