package ch.mitto.missito.util;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.google.common.io.ByteStreams;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import ch.mitto.missito.Application;


/**
 * Created by usr1 on 12/20/17.
 */

public class AESHelper {

    private static final String TAG = AESHelper.class.getSimpleName();

    private static Cipher cipher;

    private static Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        if (cipher == null)
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        return cipher;
    }

    public static byte[] encrypt(byte[] src, Key key) {
        try {
            //generate IV
            byte[] IVarray = new byte[16];
            new SecureRandom().nextBytes(IVarray);
            IvParameterSpec IVps = new IvParameterSpec(IVarray);

            //encrypt source
            Cipher cipher = getCipher();
            cipher.init(Cipher.ENCRYPT_MODE, key, IVps);
            byte[] encryptedAttach = cipher.doFinal(src);

            //concatenate IVkey and encrypted source
            byte[] encryptedFinal = new byte[IVarray.length + encryptedAttach.length];
            System.arraycopy(IVarray, 0, encryptedFinal, 0, IVarray.length);
            System.arraycopy(encryptedAttach, 0, encryptedFinal, IVarray.length, encryptedAttach.length);

            return encryptedFinal;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean encrypt(Key key, final InputStream input, final OutputStream output) {
        try {
            // initialize AES encryption
            Cipher encrypt = getCipher();

            //generate IV
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec IVps = new IvParameterSpec(iv);

            // write authentication and AES initialization data
            encrypt.init(Cipher.ENCRYPT_MODE, key, IVps);
            output.write(iv);

            // read data from input into buffer, encrypt and write to output
            byte[] buffer = new byte[16 * 1024];
            int numRead;
            byte[] encrypted = null;
            while ((numRead = input.read(buffer)) > 0) {
                encrypted = encrypt.update(buffer, 0, numRead);
                if (encrypted != null) {
                    output.write(encrypted);
                }
            }
            encrypted = encrypt.doFinal();

            if (encrypted != null) {
                output.write(encrypted);
            }

            return true;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            Log.e(TAG, "Failed to encrypt", e);
            return false;
        }
    }


    public static byte[] decrypt(byte[] src, Key key) {
        try {
            byte[] iv = Arrays.copyOfRange(src, 0, 16);
            byte[] encryptedSrc = Arrays.copyOfRange(src, 16, src.length);
            Cipher cipher = getCipher();
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            return cipher.doFinal(encryptedSrc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean decrypt(Key key, InputStream input, OutputStream output) {
        try {
            // initialize AES decryption
            byte[] iv = new byte[16];
            if (ByteStreams.read(input, iv, 0, 16) < 16)
                return false;
            Cipher decrypt = getCipher();
            decrypt.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));


            // read data from input into buffer, decrypt and write to output
            byte[] buffer = new byte[16 * 1024];
            int numRead;
            byte[] decrypted;
            while ((numRead = input.read(buffer)) > 0) {
                decrypted = decrypt.update(buffer, 0, numRead);
                if (decrypted != null) {
                    output.write(decrypted);
                }
            }

            decrypted = decrypt.doFinal();
            if (decrypted != null) {
                output.write(decrypted);
            }
            return true;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            Log.e(TAG, "Failed to decrypt", e);
            return false;
        }

    }

    public static boolean encrypt(String base64Key, File file, String companion) {
        File encryptedFile = new File(MissitoConfig.getAttachmentsPath(companion), file.getName() + ".enc");
        try {
            InputStream input = new FileInputStream(file);
            OutputStream output = new FileOutputStream(encryptedFile);
            return AESHelper.encrypt(AESHelper.decodeBase64Key(base64Key), input, output);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to encrypt file ", e);
            return false;
        }
    }

    public static boolean encrypt(String base64Key, Uri uri, String companion) {
        try {
            File encryptedFile = new File(MissitoConfig.getAttachmentsPath(companion), uri.getLastPathSegment() + ".enc");
            InputStream input = Application.app.getContentResolver().openInputStream(uri);
            OutputStream output = new FileOutputStream(encryptedFile);
            return AESHelper.encrypt(AESHelper.decodeBase64Key(base64Key), input, output);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to encrypt file ", e);
            return false;
        }
    }

    public static boolean decrypt(String keySecret, String filePath) {
        File encryptedFile = new File(filePath);
        File decryptedFile = new File(filePath + ".dec");
        boolean success;
        try {
            InputStream inputStream = new FileInputStream(encryptedFile);
            OutputStream outputStream = new FileOutputStream(decryptedFile);
            success = AESHelper.decrypt(AESHelper.decodeBase64Key(keySecret), inputStream, outputStream);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to decrypt file ", e);
            decryptedFile.delete();
            return false;
        }
        encryptedFile.delete();
        decryptedFile.renameTo(encryptedFile);
        return success;
    }

    public static String generateBase64Key() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.encodeToString(key, Base64.NO_WRAP);
    }

    public static Key decodeBase64Key(String base64encoded_key) {
        byte[] encryption_key = Base64.decode(base64encoded_key, Base64.DEFAULT);
        return new SecretKeySpec(encryption_key, "AES");
    }


}
