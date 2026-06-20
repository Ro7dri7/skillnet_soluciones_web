package com.skillnet.service.producer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class GammaServiceDevPdfTest {

    @Test
    void buildDevPdf_acceptsPercentInTitle() throws Exception {
        GammaService service = new GammaService(null, null, null, null, null, null, null);
        Method method = GammaService.class.getDeclaredMethod("buildDevPdf", String.class);
        method.setAccessible(true);

        byte[] pdf = assertDoesNotThrow(
                () -> (byte[]) method.invoke(service, "100% ingresos en YouTube: guía 2026"));
        assertTrue(pdf.length > 50);
        String text = new String(pdf);
        assertTrue(text.startsWith("%PDF"));
        assertTrue(text.contains("100% ingresos"));
    }
}
