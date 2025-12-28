package es.ifp.fairpay.utils;

import org.junit.Test;

/**
 * Pruebas unitarias para los métodos de la clase ValidationUtils.
 */
public class ValidationUtilsTest {

    // --- Pruebas para formatos de email VÁLIDOS ---

    @Test
    public void emailValidator_CorrectEmail_ReturnsTrue() {
        org.junit.Assert.assertTrue("El email 'test@example.com' debería ser válido", ValidationUtils.isValidEmail("test@example.com"));
    }

    @Test
    public void emailValidator_SubdomainEmail_ReturnsTrue() {
        org.junit.Assert.assertTrue("Un email con subdominio debería ser válido", ValidationUtils.isValidEmail("test@sub.example.co.uk"));
    }

    // --- Pruebas para formatos de email INVÁLIDOS ---

    @Test
    public void emailValidator_InvalidEmailNoAtSign_ReturnsFalse() {
        org.junit.Assert.assertFalse("Un email sin @ debería ser inválido", ValidationUtils.isValidEmail("test.example.com"));
    }

    @Test
    public void emailValidator_InvalidEmailNoTld_ReturnsFalse() {
        org.junit.Assert.assertFalse("Un email sin dominio de nivel superior debería ser inválido", ValidationUtils.isValidEmail("test@example"));
    }

    @Test
    public void emailValidator_EmptyString_ReturnsFalse() {
        org.junit.Assert.assertFalse("Una cadena vacía no es un email válido", ValidationUtils.isValidEmail(""));
    }

    @Test
    public void emailValidator_NullEmail_ReturnsFalse() {
        org.junit.Assert.assertFalse("Un valor nulo no es un email válido", ValidationUtils.isValidEmail(null));
    }
}
