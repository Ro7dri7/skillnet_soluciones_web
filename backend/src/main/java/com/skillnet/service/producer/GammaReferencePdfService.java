package com.skillnet.service.producer;

import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GammaReferencePdfService {

    private static final int MAX_EXTRACT_CHARS = 80_000;

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sube un archivo PDF.");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        if (!name.endsWith(".pdf") && !contentType.contains("pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se aceptan archivos PDF.");
        }
        if (file.getSize() > 15 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El PDF de referencia debe pesar menos de 15 MB.");
        }
        try (InputStream input = file.getInputStream();
                PDDocument document = Loader.loadPDF(input.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document).trim();
            if (text.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No se pudo extraer texto del PDF. Prueba con un PDF con texto seleccionable.");
            }
            if (text.length() > MAX_EXTRACT_CHARS) {
                return text.substring(0, MAX_EXTRACT_CHARS);
            }
            return text;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el PDF de referencia.");
        }
    }
}
