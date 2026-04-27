package com.plagchecker.history;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.plagchecker.entity.PlagiarismCheck;
import com.plagchecker.model.AnalysisResult;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * AnalysisHistoryStore — Persists analysis history in memory.
 *
 * Reverted from database persistence to in-memory storage.
 * All analysis results are stored in a thread-safe list.
 */
@Component
public class AnalysisHistoryStore {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String HISTORY_FILE = "history.json";

    private final List<PlagiarismCheck> history = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @PostConstruct
    public void init() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try {
                List<PlagiarismCheck> loaded = mapper.readValue(file, new TypeReference<List<PlagiarismCheck>>() {});
                history.addAll(loaded);
                
                // Initialize idGenerator to max found ID + 1
                long maxId = loaded.stream()
                        .mapToLong(c -> c.getId() != null ? c.getId() : 0)
                        .max()
                        .orElse(0);
                idGenerator.set(maxId + 1);
                
                System.out.println("Loaded " + history.size() + " history entries from " + HISTORY_FILE);
            } catch (IOException e) {
                System.err.println("Failed to load history from file: " + e.getMessage());
            }
        }
    }

    private void persist() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(HISTORY_FILE), history);
        } catch (IOException e) {
            System.err.println("Failed to persist history: " + e.getMessage());
        }
    }

    /**
     * Save an analysis result to memory.
     */
    public HistoryEntry save(String labelA, String labelB, AnalysisResult result) {
        return save(labelA, labelB, result, "TEXT", null, null, null);
    }

    /**
     * Save with additional metadata.
     */
    public HistoryEntry save(String labelA, String labelB, AnalysisResult result,
                             String mode, String inputTextA, String inputTextB,
                             String matchedSourcesJson) {
        PlagiarismCheck check = new PlagiarismCheck();
        check.setId(idGenerator.getAndIncrement());
        check.setLabelA(labelA != null ? labelA.substring(0, Math.min(labelA.length(), 255)) : "Document A");
        check.setLabelB(labelB != null ? labelB.substring(0, Math.min(labelB.length(), 255)) : "Document B");
        check.setOverallSimilarity(result.getOverallSimilarity());
        check.setRabinKarpScore(result.getRabinKarpScore());
        check.setKmpScore(result.getKmpScore());
        check.setWinnowingScore(result.getWinnowingScore());
        check.setLevenshteinScore(result.getLevenshteinScore());
        check.setCosineSimilarity(result.getCosineSimilarity());
        check.setJaccardSimilarity(result.getJaccardSimilarity());
        check.setVerdict(result.getVerdict());
        check.setTotalWordsA(result.getTotalWordsA());
        check.setTotalWordsB(result.getTotalWordsB());
        check.setMatchedWords(result.getMatchedWords());
        check.setMatchCount(result.getMatchedSegments() != null ? result.getMatchedSegments().size() : 0);
        check.setProcessingTime(result.getProcessingTime() != null ? result.getProcessingTime() : "N/A");
        check.setAnalysisMode(mode);

        // Store truncated input text for reference
        if (inputTextA != null) {
            check.setInputTextA(inputTextA.length() > 5000 ? inputTextA.substring(0, 5000) : inputTextA);
        }
        if (inputTextB != null) {
            check.setInputTextB(inputTextB.length() > 5000 ? inputTextB.substring(0, 5000) : inputTextB);
        }
        // matchedSourcesJson logic removed for internet scan removal

        history.add(0, check); // Add to beginning (most recent first)
        
        // Keep only last 100
        synchronized (history) {
            if (history.size() > 100) {
                history.remove(history.size() - 1);
            }
        }

        persist();
        return toHistoryEntry(check);
    }

    /**
     * Get all history entries (most recent first).
     */
    public List<HistoryEntry> getAll() {
        synchronized (history) {
            return history.stream()
                    .map(this::toHistoryEntry)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Delete a history entry by ID.
     */
    public boolean delete(long id) {
        synchronized (history) {
            boolean removed = history.removeIf(check -> check.getId() != null && check.getId() == id);
            if (removed) persist();
            return removed;
        }
    }

    /**
     * Clear all history.
     */
    public void clearAll() {
        history.clear();
        persist();
    }

    /**
     * Get summary statistics.
     */
    public HistorySummary getSummary() {
        synchronized (history) {
            int total = history.size();
            if (total == 0) {
                return new HistorySummary(0, 0.0, 0, 0, 0);
            }

            double sumSim = history.stream().mapToDouble(PlagiarismCheck::getOverallSimilarity).sum();
            int highCount = (int) history.stream().filter(c -> "HIGH_PLAGIARISM".equals(c.getVerdict())).count();
            int modCount = (int) history.stream().filter(c -> "MODERATE_PLAGIARISM".equals(c.getVerdict())).count();
            int origCount = (int) history.stream().filter(c -> "ORIGINAL".equals(c.getVerdict())).count();

            return new HistorySummary(
                    total,
                    Math.round((sumSim / total) * 100.0) / 100.0,
                    highCount,
                    modCount,
                    origCount
            );
        }
    }

    private HistoryEntry toHistoryEntry(PlagiarismCheck check) {
        return new HistoryEntry(
                check.getId(),
                check.getLabelA(),
                check.getLabelB(),
                check.getOverallSimilarity(),
                check.getVerdict(),
                check.getMatchCount(),
                check.getProcessingTime(),
                check.getCreatedAt() != null ? check.getCreatedAt().format(FMT) : "N/A",
                check.getAnalysisMode()
        );
    }

    // ---- Data classes ----

    public static class HistoryEntry {
        public final long id;
        public final String labelA;
        public final String labelB;
        public final double similarity;
        public final String verdict;
        public final int matchCount;
        public final String processingTime;
        public final String timestamp;
        public final String mode;

        public HistoryEntry(long id, String labelA, String labelB, double similarity,
                            String verdict, int matchCount, String processingTime,
                            String timestamp, String mode) {
            this.id = id;
            this.labelA = labelA;
            this.labelB = labelB;
            this.similarity = similarity;
            this.verdict = verdict;
            this.matchCount = matchCount;
            this.processingTime = processingTime;
            this.timestamp = timestamp;
            this.mode = mode;
        }
    }

    public static class HistorySummary {
        public final int total;
        public final double averageSimilarity;
        public final int highPlagiarism;
        public final int moderate;
        public final int original;

        public HistorySummary(int total, double avg, int high, int mod, int orig) {
            this.total = total;
            this.averageSimilarity = avg;
            this.highPlagiarism = high;
            this.moderate = mod;
            this.original = orig;
        }
    }
}