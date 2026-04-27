package com.plagchecker.api;

import com.plagchecker.io.DocumentReader;
import com.plagchecker.model.AnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PlagiarismController {

    @Autowired 
    private PlagiarismAnalysisService analysisService;

    // ❌ REMOVE documentReader injection (not needed)

    @PostMapping("/analyze/text")
    public ResponseEntity<AnalysisResult> analyzeText(@RequestBody Map<String, String> body) {
        String textA = body.getOrDefault("textA", "").trim();
        String textB = body.getOrDefault("textB", "").trim();

        if (textA.isEmpty() || textB.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        AnalysisResult result = analysisService.analyze(textA, textB);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze/files")
    public ResponseEntity<AnalysisResult> analyzeFiles(
            @RequestParam("fileA") MultipartFile fileA,
            @RequestParam("fileB") MultipartFile fileB) {
        try {
            // ✅ FIX: call static methods properly
            String textA = DocumentReader.extractFromMultipart(fileA);
            String textB = DocumentReader.extractFromMultipart(fileB);

            if (textA.isBlank() || textB.isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            AnalysisResult result = analysisService.analyze(textA, textB);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "PlagScan Pro",
            "version", "2.0.0"
        ));
    }
}