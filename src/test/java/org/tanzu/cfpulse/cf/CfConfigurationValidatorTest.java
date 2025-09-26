package org.tanzu.cfpulse.cf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CfConfigurationValidatorTest {

    @Test
    void testValidateConfiguration_WithValidConfig_ShouldNotThrowException() {
        // Given
        CfConfigurationValidator validator = new CfConfigurationValidator(
                "https://api.example.com",
                "testuser",
                "testpass",
                "testorg",
                "testspace"
        );

        // When & Then
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(validator, "validateConfiguration");
        });
    }

    @Test
    void testValidateConfiguration_WithMissingApiHost_ShouldThrowException() {
        // Given
        CfConfigurationValidator validator = new CfConfigurationValidator(
                "",
                "testuser",
                "testpass",
                "testorg",
                "testspace"
        );

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ReflectionTestUtils.invokeMethod(validator, "validateConfiguration");
        });
        assertTrue(exception.getMessage().contains("Cloud Foundry configuration is incomplete"));
    }

    @Test
    void testValidateConfiguration_WithMissingUsername_ShouldThrowException() {
        // Given
        CfConfigurationValidator validator = new CfConfigurationValidator(
                "https://api.example.com",
                "",
                "testpass",
                "testorg",
                "testspace"
        );

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ReflectionTestUtils.invokeMethod(validator, "validateConfiguration");
        });
        assertTrue(exception.getMessage().contains("Cloud Foundry configuration is incomplete"));
    }

    @Test
    void testValidateConfiguration_WithMissingPassword_ShouldThrowException() {
        // Given
        CfConfigurationValidator validator = new CfConfigurationValidator(
                "https://api.example.com",
                "testuser",
                "",
                "testorg",
                "testspace"
        );

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ReflectionTestUtils.invokeMethod(validator, "validateConfiguration");
        });
        assertTrue(exception.getMessage().contains("Cloud Foundry configuration is incomplete"));
    }

    @Test
    void testValidateConfiguration_WithMissingOrgAndSpace_ShouldNotThrowException() {
        // Given
        CfConfigurationValidator validator = new CfConfigurationValidator(
                "https://api.example.com",
                "testuser",
                "testpass",
                "",
                ""
        );

        // When & Then
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(validator, "validateConfiguration");
        });
    }
}
