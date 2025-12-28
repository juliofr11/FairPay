package es.ifp.fairpay.security;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.io.File;

import es.ifp.fairpay.data.security.EncryptManager;

/**
 * Prueba instrumentada para la clase EncryptManager.
 * Esta prueba se ejecuta en un dispositivo o emulador Android para verificar
 * el cifrado y descifrado de datos usando el AndroidKeystore.
 */
@RunWith(AndroidJUnit4.class)
public class EncryptManagerTest {

    private EncryptManager encryptManager;
    private Context context;
    private static final String TEST_ALIAS = "test_private_key_alias_for_instrumented_test";

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        encryptManager = new EncryptManager(context);
    }

    @After
    public void tearDown() {
        File testFile = new File(context.getFilesDir(), TEST_ALIAS);
        if (testFile.exists()) {
            testFile.delete();
        }
    }

    @Test
    public void encryptAndDecrypt_ReturnsOriginalData() throws Exception {
        String originalData = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";

        encryptManager.encryptAndSave(TEST_ALIAS, originalData);
        String decryptedData = encryptManager.loadAndDecrypt(TEST_ALIAS);

        assertNotNull("El dato descifrado no debería ser nulo", decryptedData);
        assertEquals("El dato descifrado debe ser igual al original", originalData, decryptedData);
    }

    @Test
    public void loadAndDecrypt_NonExistentAlias_ReturnsNull() throws Exception {
        String nonExistentAlias = "non_existent_alias_for_test";
        String decryptedData = encryptManager.loadAndDecrypt(nonExistentAlias);
        assertNull("El resultado debería ser nulo para un alias que no existe", decryptedData);
    }
}
