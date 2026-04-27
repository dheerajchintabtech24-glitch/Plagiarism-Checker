package com.plagchecker.io;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

public class DocumentReader {

    /**
     * Extract text from an uploaded file (.txt, .pdf, .docx).
     */
    public static String extractFromMultipart(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) filename = "";
        filename = filename.toLowerCase();

        if (filename.endsWith(".pdf")) {
            return extractPdf(file.getInputStream());
        } else if (filename.endsWith(".docx")) {
            return extractDocx(file.getInputStream());
        } else {
            // Treat as plain text
            return new String(file.getBytes());
        }
    }

    private static String extractPdf(InputStream is) throws IOException {
        try (PDDocument doc = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private static String extractDocx(InputStream is) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            return doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }
}