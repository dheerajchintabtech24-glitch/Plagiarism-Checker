package com.plagchecker.api;

import com.plagchecker.history.AnalysisHistoryStore;
import com.plagchecker.io.DocumentReader;
import com.plagchecker.model.AnalysisResult;
import com.plagchecker.model.InternetScanResult;
import com.plagchecker.report.ReportGenerator;
import com.plagchecker.search.InternetScanService;
import com.plagchecker.synonyms.SynonymMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/v2")
@CrossOrigin(origins = "*")
public class ExtendedController {

    @Autowired private PlagiarismAnalysisService analysisService;
    @Autowired private AnalysisHistoryStore historyStore;
    @Autowired private ReportGenerator reportGenerator;
    @Autowired private SynonymMap synonymMap;
    @Autowired private InternetScanService internetScanService;

    // ---------------- ANALYSIS ----------------

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody Map<String, String> body) {

        String textA  = body.getOrDefault("textA", "").trim();
        String textB  = body.getOrDefault("textB", "").trim();
        String labelA = body.getOrDefault("labelA", "Document A");
        String labelB = body.getOrDefault("labelB", "Document B");

        if (textA.isEmpty() || textB.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "textA and textB are required"));
        }

        AnalysisResult result = analysisService.analyze(textA, textB);
        AnalysisHistoryStore.HistoryEntry entry = historyStore.save(
                labelA, labelB, result, "TEXT", textA, textB, null);

        return ResponseEntity.ok(Map.of(
                "historyId", entry.id,
                "result", result
        ));
    }

    @PostMapping("/analyze/files")
    public ResponseEntity<?> analyzeFiles(
            @RequestParam("fileA") MultipartFile fileA,
            @RequestParam("fileB") MultipartFile fileB) {

        try {
            String textA = DocumentReader.extractFromMultipart(fileA);
            String textB = DocumentReader.extractFromMultipart(fileB);

            AnalysisResult result = analysisService.analyze(textA, textB);

            String nameA = fileA.getOriginalFilename() != null ? fileA.getOriginalFilename() : "File A";
            String nameB = fileB.getOriginalFilename() != null ? fileB.getOriginalFilename() : "File B";

            AnalysisHistoryStore.HistoryEntry entry = historyStore.save(
                    nameA, nameB, result, "FILE", textA, textB, null);

            return ResponseEntity.ok(Map.of(
                    "historyId", entry.id,
                    "result", result
            ));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ---------------- BATCH ----------------

    @PostMapping("/batch")
    public ResponseEntity<?> batchCompare(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, String> documents = (Map<String, String>) body.get("documents");

        if (documents == null || documents.size() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "Need at least 2 documents"));
        }

        List<String> names = new ArrayList<>(documents.keySet());
        List<String> texts = new ArrayList<>();
        for (String name : names) texts.add(documents.get(name));

        int n = names.size();
        double[][] matrix = new double[n][n];
        List<Map<String, Object>> flagged = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            matrix[i][i] = 100.0;
            for (int j = i + 1; j < n; j++) {
                AnalysisResult r = analysisService.analyze(texts.get(i), texts.get(j));
                double sim = r.getOverallSimilarity();
                matrix[i][j] = sim;
                matrix[j][i] = sim;

                if (sim > 40) {
                    Map<String, Object> pair = new LinkedHashMap<>();
                    pair.put("docA", names.get(i));
                    pair.put("docB", names.get(j));
                    pair.put("similarity", sim);
                    flagged.add(pair);
                }
            }
        }

        // Sort flagged pairs by similarity descending
        flagged.sort((a, b) -> Double.compare((double) b.get("similarity"), (double) a.get("similarity")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentNames", names);
        result.put("similarityMatrix", matrix);
        result.put("flaggedPairs", flagged);

        return ResponseEntity.ok(result);
    }

    // ---------------- REPORT ----------------

    @PostMapping("/report/html")
    public ResponseEntity<byte[]> exportHtmlReport(@RequestBody Map<String, String> body) {

        String textA  = body.getOrDefault("textA", "").trim();
        String textB  = body.getOrDefault("textB", "").trim();
        String labelA = body.getOrDefault("labelA", "Document A");
        String labelB = body.getOrDefault("labelB", "Document B");

        if (textA.isEmpty() || textB.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        AnalysisResult result = analysisService.analyze(textA, textB);
        String html = reportGenerator.generateHtmlReport(labelA, labelB, result);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setContentDispositionFormData("attachment", "plagscan-report.html");

        return ResponseEntity.ok().headers(headers).body(html.getBytes());
    }

    // ---------------- HISTORY ----------------

    @GetMapping("/history")
    public ResponseEntity<List<AnalysisHistoryStore.HistoryEntry>> getHistory() {
        return ResponseEntity.ok(historyStore.getAll());
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Map<String, Object>> deleteHistory(@PathVariable long id) {
        boolean deleted = historyStore.delete(id);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearHistory() {
        historyStore.clearAll();
        return ResponseEntity.ok(Map.of("status", "cleared"));
    }

    @GetMapping("/history/summary")
    public ResponseEntity<AnalysisHistoryStore.HistorySummary> getHistorySummary() {
        return ResponseEntity.ok(historyStore.getSummary());
    }

    // ---------------- SYNONYMS ----------------

    @GetMapping("/synonyms/check")
    public ResponseEntity<Map<String, Object>> checkSynonyms(
            @RequestParam String a, @RequestParam String b) {

        return ResponseEntity.ok(Map.of(
                "wordA", a,
                "wordB", b,
                "areSynonyms", synonymMap.areSynonyms(a, b),
                "canonicalA", synonymMap.canonicalize(a),
                "canonicalB", synonymMap.canonicalize(b)
        ));
    }

    @GetMapping("/synonyms")
    public ResponseEntity<Map<String, Object>> getSynonyms(@RequestParam String word) {
        return ResponseEntity.ok(Map.of(
                "word", word,
                "canonical", synonymMap.canonicalize(word),
                "synonyms", synonymMap.getSynonyms(word)
        ));
    }

    // ---------------- INTERNET SCAN ----------------

    /**
     * POST /api/v2/internet-scan
     * Body: { "text": "...", "fromDate": "YYYY-MM-DD" (optional) }
     *
     * Scans the internet for plagiarism matches against the submitted text.
     * Uses DuckDuckGo search — no API key required.
     */
    @PostMapping("/internet-scan")
    public ResponseEntity<?> internetScan(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "").trim();
        String fromDate = body.getOrDefault("fromDate", "").trim();

        if (text.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "text is required"));
        }
        if (text.split("\\s+").length < 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please provide at least 5 words to scan"));
        }

        InternetScanResult result = internetScanService.scan(
                text,
                fromDate.isEmpty() ? null : fromDate
        );
        return ResponseEntity.ok(result);
    }
}