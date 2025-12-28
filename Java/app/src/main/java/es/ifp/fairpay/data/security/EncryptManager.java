package es.ifp.fairpay.data.security;

import android.content.Context;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class EncryptManager {

    private final Context context;
    private final MasterKey masterKey;

    public EncryptManager(Context context) throws GeneralSecurityException, IOException {
        this.context = context.getApplicationContext();
        this.masterKey = new MasterKey.Builder(this.context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
    }

    public void encryptAndSave(String alias, String dataToEncrypt) throws GeneralSecurityException, IOException {
        File file = new File(context.getFilesDir(), alias);

        // Si el archivo ya existe de un intento de registro anterior, lo borramos para evitar error de duplicado.
        if (file.exists()) {
            file.delete();
        }

        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        byte[] fileContent = dataToEncrypt.getBytes(StandardCharsets.UTF_8);
        /*
        * Usamos try-with-resources para asegurarnos de que el OutputStream se cierre correctamente.
        * Con java.io.OutpuStream, nos aseguramos de que únicamente exista aquí este código
        * Y evitar duplicados intentando abrir el archivo dos veces.
        */
        try(java.io.OutputStream outputStream = encryptedFile.openFileOutput()){
          outputStream.write(fileContent);
          outputStream.flush();
        }

    }

    public String loadAndDecrypt(String alias) throws GeneralSecurityException, IOException {
        File file = new File(context.getFilesDir(), alias);

        if (!file.exists()) {
            return null;
        }

        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        byte[] fileContent;
        try(InputStream inputStream = encryptedFile.openFileInput();
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream()){
                byte[] buffer = new byte[1024];
                int bytesRead;
                while((bytesRead = inputStream.read(buffer)) != -1){
                    byteStream.write(buffer, 0, bytesRead);
                }
                fileContent = byteStream.toByteArray();
            }
        return new String(fileContent, StandardCharsets.UTF_8);
    }
}
