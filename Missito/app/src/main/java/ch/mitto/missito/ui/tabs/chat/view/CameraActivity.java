package ch.mitto.missito.ui.tabs.chat.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.ch.mitto.missito.R;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Facing;
import com.otaliastudios.cameraview.Flash;
import com.otaliastudios.cameraview.SessionType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import ch.mitto.missito.util.MissitoConfig;

public class CameraActivity extends AppCompatActivity {

    private final String LOG_TAG = CameraActivity.class.getName();
    private final int AUDIO_RECORD_PERMISSION_REQUEST_CODE = 101;
    private final String IMAGE_EXTENSION = ".jpeg";
    private final String VIDEO_EXTENSION = ".mp4";
    public static final String FILEPATH_KEY = "filePath";
    public static final String CONTACT_PHONE_KEY = "contact_phone";
    private String contact;
    private String filepath;
    private Flash backFlash, frontFlash;
    private boolean backCameraOpensFirstTime = true;
    private boolean frontCameraOpensFirstTime = true;
    private boolean hasFrontFlash;
    private boolean hasBackFlash;

    private Handler customHandler = new Handler();

    private ProgressDialog dialog;

    @BindView(R.id.cameraView)
    CameraView cameraView;

    @BindView(R.id.textCounter)
    TextView textCounter;

    @BindView(R.id.hint)
    TextView hint;

    @BindView(R.id.flash_mode)
    ImageView flashView;

    @OnClick(R.id.flash_mode)
    public void onChangeFlashMode() {
        if (cameraView.getFlash().equals(Flash.AUTO)) {
            cameraView.setFlash(Flash.ON);
            flashView.setImageResource(R.drawable.ic_flash_on);
        } else if (cameraView.getFlash().equals(Flash.ON)) {
            cameraView.setFlash(Flash.OFF);
            flashView.setImageResource(R.drawable.ic_flash_off);
        } else {
            cameraView.setFlash(Flash.AUTO);
            flashView.setImageResource(R.drawable.ic_flash_auto);
        }

        //save new flashState for future restore while switching between cameras
        if (cameraView.getFacing() == Facing.BACK) {
            backFlash = cameraView.getFlash();
        } else {
            frontFlash = cameraView.getFlash();
        }
    }


    @BindView(R.id.facing_mode)
    ImageView facingView;

    @OnClick(R.id.facing_mode)
    public void onChangeFacing() {
        if (cameraView.getFacing().equals(Facing.BACK)) {
            cameraView.setFacing(Facing.FRONT);
            facingView.setImageResource(R.drawable.ic_camera_rear);
        } else {
            cameraView.setFacing(Facing.BACK);
            facingView.setImageResource(R.drawable.ic_camera_front);
        }
    }


    @BindView(R.id.action_camera)
    ImageView actionCamera;

    @OnClick(R.id.action_camera)
    public void onCapture() {
        if (cameraView.getSessionType().equals(SessionType.PICTURE)) {
            cameraView.capturePicture();
            actionCamera.setColorFilter(Color.YELLOW);
            filepath = MissitoConfig.getAttachmentsPath(contact) + getCurrentTimeStampForFileNaming();
        } else {
            cameraView.stopCapturingVideo();
        }
    }

    // @OnLongClick methods must have a 'boolean' return type.
    @OnLongClick(R.id.action_camera)
    public boolean takeVideo() {
        if (!isPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
            requestPermission(Manifest.permission.RECORD_AUDIO, AUDIO_RECORD_PERMISSION_REQUEST_CODE);
            return false;
        }
        cameraView.setSessionType(SessionType.VIDEO);
        filepath = MissitoConfig.getAttachmentsPath(contact) + getCurrentTimeStampForFileNaming();
        cameraView.startCapturingVideo(new File(filepath));
        startTime = SystemClock.uptimeMillis();
        customHandler.postDelayed(updateTimerThread, 0);
        textCounter.setVisibility(View.VISIBLE);
        hint.setVisibility(View.GONE);
        facingView.setVisibility(View.GONE);
        actionCamera.setImageResource(R.drawable.ic_camera_stop);
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeNoTitleBar_Fullscreen);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        contact = getIntent().getStringExtra(CONTACT_PHONE_KEY);

        if (!cameraExists(Facing.FRONT)) {
            facingView.setVisibility(View.GONE);
        }

        if (!cameraExists(Facing.BACK)) {
            cameraView.setFacing(Facing.FRONT);
            facingView.setVisibility(View.GONE);
        }


        cameraView.addCameraListener(new CameraListener() {


            @Override
            public void onCameraOpened(CameraOptions options) {
                super.onCameraOpened(options);
                if (takeVideoAfterGrantedPermissions) {
                    takeVideo();
                }

                //save flashState when the camera opens for the first time
                //or restore flashState after switching (back/front) camera
                if (cameraView.getFacing() == Facing.BACK) {
                    if (backCameraOpensFirstTime) {
                        backFlash = cameraView.getFlash();
                        hasBackFlash = cameraView.getFlash() != Flash.OFF;
                    } else {
                        cameraView.setFlash(backFlash);
                    }
                    backCameraOpensFirstTime = false;
                } else {
                    if (frontCameraOpensFirstTime) {
                        frontFlash = cameraView.getFlash();
                        hasFrontFlash = cameraView.getFlash() != Flash.OFF;
                    } else {
                        cameraView.setFlash(frontFlash);
                    }
                    frontCameraOpensFirstTime = false;
                }

                flashView.setVisibility(hasFlash() ? View.VISIBLE : View.GONE);
                cameraView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCameraClosed() {
                super.onCameraClosed();
                cameraView.setVisibility(View.GONE);
            }

            @Override
            public void onOrientationChanged(int orientation) {
                super.onOrientationChanged(orientation);
                // 90 to 270 and 270 to 90
                rotateViews(orientation + (int) (180 * Math.sin(orientation / 57.3)), facingView, flashView, textCounter);
            }

            @Override
            public void onCameraError(@NonNull CameraException exception) {
                super.onCameraError(exception);
                Toast.makeText(CameraActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onPictureTaken(byte[] jpeg) {
                super.onPictureTaken(jpeg);
                dialog = new ProgressDialog(CameraActivity.this);
                dialog.setMessage(getString(R.string.please_wait));
                dialog.show();
                new SavePhotoAsyncTask(filepath, jpeg) {
                    @Override
                    protected void onPostExecute(String photoPath) {
                        super.onPostExecute(photoPath);
                        dialog.hide();
                        actionCamera.clearColorFilter();
                        if (TextUtils.isEmpty(photoPath)) {
                            Toast.makeText(CameraActivity.this, R.string.failed_to_save_photo, Toast.LENGTH_LONG).show();
                        } else {
                            returnFile(photoPath);
                        }
                    }
                }.execute();

                try {
                    File filePhoto = new File(filepath);
                    FileOutputStream fileOuputStream = new FileOutputStream(filePhoto);
                    fileOuputStream.write(jpeg);
                    fileOuputStream.close();
                    String photoPath = filepath + IMAGE_EXTENSION;
                    filePhoto.renameTo(new File(photoPath));
                    returnFile(photoPath);
                } catch (FileNotFoundException e) {
                    Log.e(LOG_TAG, "File not found " + filepath, e);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "failed write fileOutputStream from file " + filepath, e);
                }
            }

            @Override
            public void onVideoTaken(File video) {
                super.onVideoTaken(video);
                textCounter.setVisibility(View.GONE);
                hint.setVisibility(View.VISIBLE);
                facingView.setVisibility(View.VISIBLE);
                actionCamera.setImageResource(R.drawable.ic_camera_action);
                String videoPath = video.getAbsoluteFile() + VIDEO_EXTENSION;
                video.renameTo(new File(videoPath));
                returnFile(videoPath);
            }
        });
    }


    boolean confirmSend = false;

    private void returnFile(String filepath) {
        Intent intent = new Intent();
        intent.putExtra(FILEPATH_KEY, filepath);
        setResult(RESULT_OK, intent);
        confirmSend = true;
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!confirmSend && filepath != null) {
            File file = new File(filepath);
            if (file.exists()) file.delete();
        }
        cameraView.stop();
        if (customHandler != null)
            customHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.destroy();
    }

    private boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isPermissionGranted(int[] grantResults) {
        return grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("NewApi")
    private void requestPermission(String permissionCode, int requestCode) {
        requestPermissions(new String[]{permissionCode}, requestCode);
    }


    boolean takeVideoAfterGrantedPermissions = false;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case AUDIO_RECORD_PERMISSION_REQUEST_CODE:
                if (isPermissionGranted(grantResults)) {
                    takeVideoAfterGrantedPermissions = true;
                } else {
                    Toast.makeText(this, R.string.no_permission_record_audio, Toast.LENGTH_SHORT).show();
                }
        }
    }


    private long startTime = SystemClock.uptimeMillis();
    private Runnable updateTimerThread = new Runnable() {

        public void run() {

            int secs = (int) ((SystemClock.uptimeMillis() - startTime) / 1000);
            int mins = secs / 60;

            secs = secs % 60;
            textCounter.setText(String.format("%02d", mins) + ":" + String.format("%02d", secs));
            customHandler.post(this);

        }

    };


    public static String getCurrentTimeStampForFileNaming() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date now = new Date();
        return sdfDate.format(now);
    }


    private void rotateViews(int rotation, View... views) {
        for (View view : views) {
            view.animate().rotation(rotation).setDuration(300).start();
        }
    }


    private boolean cameraExists(Facing facing) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP)
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == (facing == Facing.BACK ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT)) {
                    return true;
                }
            }
        else {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                for (String cameraId : manager.getCameraIdList()) {
                    CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                    Integer camera2Facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                    if (camera2Facing != null && camera2Facing == (facing == Facing.BACK ? CameraMetadata.LENS_FACING_BACK : CameraMetadata.LENS_FACING_FRONT)) {
                        return true;
                    }
                }
            } catch (CameraAccessException e) {
                return false;
            }
        }
        return false;
    }



    private boolean hasFlash() {
        return cameraView.getFacing() == Facing.BACK ? hasBackFlash : hasFrontFlash;
    }

    public class SavePhotoAsyncTask extends AsyncTask<Void, Void, String> {

        String filePath;
        byte[] jpeg;

        public SavePhotoAsyncTask(String filePath, byte[] jpeg) {
            this.filePath = filePath;
            this.jpeg = jpeg;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                File filePhoto = new File(filePath);
                FileOutputStream fileOuputStream = new FileOutputStream(filePhoto);
                fileOuputStream.write(jpeg);
                fileOuputStream.close();
                String photoPath = filePath + IMAGE_EXTENSION;
                filePhoto.renameTo(new File(photoPath));
                return photoPath;
            } catch (FileNotFoundException e) {
                Log.e(LOG_TAG, "File not found " + filePath, e);
            } catch (IOException e) {
                Log.e(LOG_TAG, "failed write fileOutputStream from file " + filePath, e);
            }
            return null;
        }
    }
}
