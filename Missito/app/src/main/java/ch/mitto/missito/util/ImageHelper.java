package ch.mitto.missito.util;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.net.UnknownHostException;

import ch.mitto.missito.Application;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ImageHelper {

    public static final String IMG_DIR = "missitoImg";
    private static final int THUMBNAIL_MAX_WIDTH = 224;     // Width and Height are in dp. Will be converted to pixels later.
    private static final int THUMBNAIL_MAX_HEIGHT = 300;
    private static final String MIME_JPG = "image/jpeg";

    private static File getImgCacheDir(Context context) {
        File directory = new File(context.getFilesDir(), IMG_DIR);

        if (!directory.exists()) {
            directory.mkdir();
        }

        return directory;
    }

    public static String formLocalURI(Context context, String fileName) {
        File imgCache = ImageHelper.getImgCacheDir(context);
        File img = new File(imgCache, fileName);

        if (img.exists() && img.isFile()) {
            return img.toURI().toString();
        }

        return null;
    }

    private static byte[] compressBitmap(String mimeType, Bitmap bitmap, int quality) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (MIME_JPG.equals(mimeType)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
        } else {
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, byteArrayOutputStream);
        }

        return byteArrayOutputStream.toByteArray();
    }

    public static final OkHttpClient okHttpClient = new OkHttpClient.Builder().addInterceptor(
            new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Response response;
                    Request request = chain.request()
                            .newBuilder()
                            .addHeader("Authorization", "Bearer " + Application.app.connectionManager.backendToken)
                            .build();
                    try {
                        response = chain.proceed(request);
                        Log.d(ImageHelper.class.getSimpleName(), "intercept: " + response.body());

                        // body = decrypt(response.body().bytes())
                        // response = new Response.Builder().body(body)
                        //         .code(response.code())
                        //         .message(response.message())
                        //         .build()
                        // return response
                    } catch (SocketTimeoutException | UnknownHostException e) {
                        e.printStackTrace();
                        throw new IOException(e);
                    }
                    return response;
                }
            }
    ).build();

    public static ImagePipelineConfig getFrescoConfig(Context context) {
        DiskCacheConfig diskCacheConfig = DiskCacheConfig.newBuilder(context)
                .setBaseDirectoryName(ImageHelper.IMG_DIR).build();

        return OkHttpImagePipelineConfigFactory
                .newBuilder(context, okHttpClient)
                .setDownsampleEnabled(true)
                .setMainDiskCacheConfig(diskCacheConfig)
                .build();
    }

    @SuppressLint("StaticFieldLeak")
    public static AsyncTask<Void, Void, Bitmap> fromBase64(String base64, final ImageView img) {
        return new BitmapFromBase64AsyncTask(base64) {
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                img.setImageBitmap(bitmap);
            }
        };
    }

    public static class CopyFileAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private File destFile, srcFile;

        public CopyFileAsyncTask(File srcFile, File destFile) {
            this.srcFile = srcFile;
            this.destFile = destFile;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                if (!destFile.exists()) {
                    destFile.createNewFile();
                }
                FileUtils.copyFile(srcFile, destFile);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public static class Base64FromBitmapAsyncTask extends AsyncTask<Void, Void, String> {

        private final Uri uri;
        private final Bitmap bitmap;

        public Base64FromBitmapAsyncTask(Uri uri, Bitmap bitmap) {
            super();
            this.uri = uri;
            this.bitmap = bitmap;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                if (bitmap == null) {
                    return null;
                }

                ContentResolver contentResolver = Application.app.getContentResolver();
                String mimeType = contentResolver.getType(uri);

                byte[] bytes = compressBitmap(mimeType, bitmap, 40);
                return Base64.encodeToString(bytes, Base64.NO_WRAP);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static class BitmapFromBase64AsyncTask extends AsyncTask<Void, Void, Bitmap> {

        private final String base64;

        public BitmapFromBase64AsyncTask(String base64) {
            super();
            this.base64 = base64;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decoded);
                Bitmap bitmap = BitmapFactory.decodeStream(byteArrayInputStream);
                if (!isCancelled() && bitmap != null) {
                    float maxWidth = Helper.dipToPixels(Application.app, THUMBNAIL_MAX_WIDTH);
                    float maxHeight = Helper.dipToPixels(Application.app, THUMBNAIL_MAX_HEIGHT);

                    if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight) {
                        bitmap = Helper.downscale(bitmap, maxWidth, maxHeight);
                    } else {
                        bitmap = Helper.upscale(bitmap, maxWidth, maxHeight);
                    }
                }
                return bitmap;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
        }
    }

    public static class CreateThumbnailAsyncTask extends AsyncTask<Void, Void, String> {

        public interface OnFinishedListener {
            void onFinished(CreateThumbnailAsyncTask task, String base64Thumbnail);
        }

        public OnFinishedListener listener;
        private File imageFile;
        private Bitmap bitmap;

        public CreateThumbnailAsyncTask(File imageFile, OnFinishedListener listener) {
            super();
            this.imageFile = imageFile;
            this.listener = listener;
        }

        public CreateThumbnailAsyncTask(Bitmap bitmap, OnFinishedListener listener) {
            super();
            this.bitmap = bitmap;
            this.listener = listener;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                String contentType = MIME_JPG;
                int maxWidth = (int) Helper.dipToPixels(Application.app, THUMBNAIL_MAX_WIDTH);
                int maxHeight = (int) Helper.dipToPixels(Application.app, THUMBNAIL_MAX_HEIGHT);

                if (bitmap == null) {
                    String filename = imageFile.getName();
                    contentType = URLConnection.guessContentTypeFromName(filename);

                    BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
                    bitmapFactoryOptions.inJustDecodeBounds = true;

                    // Getting size of bitmap
                    BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bitmapFactoryOptions);

                    // Calculate inSampleSize
                    bitmapFactoryOptions.inSampleSize = calculateInSampleSize(bitmapFactoryOptions,
                            maxWidth,
                            maxHeight);

                    // Decode bitmap with inSampleSize set
                    bitmapFactoryOptions.inJustDecodeBounds = false;
                    bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bitmapFactoryOptions);
                }

                if (imageFile != null) {
                    ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
                    switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            bitmap = rotate(bitmap, 90);
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            bitmap = rotate(bitmap, 270);
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            bitmap = rotate(bitmap, 180);
                    }
                }

                Bitmap scaledBitmap = bitmap;

                if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight) {
                    scaledBitmap = Helper.downscale(bitmap, maxWidth, maxHeight);
                }

                byte[] byteArray = compressBitmap(contentType, scaledBitmap, 40);
                return Base64.encodeToString(byteArray, Base64.DEFAULT);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String base64Thumbnail) {
            super.onPostExecute(base64Thumbnail);
            if (listener != null) {
                listener.onFinished(this, base64Thumbnail);
            }
        }

    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix mtx = new Matrix();
        mtx.postRotate(degree);
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}