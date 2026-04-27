package com.plagchecker.search;

import com.plagchecker.api.PlagiarismAnalysisService;
import com.plagchecker.model.AnalysisResult;
import com.plagchecker.model.InternetScanResult;
import com.plagchecker.model.InternetScanResult.SourceMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for Internet plagiarism scanning.
 * <p>
 * OOP design:
 * <ul>
 *   <li>Extracts key phrases from the submitted text.</li>
 *   <li>Fires DuckDuckGo searches for each phrase via {@link WebFetcher}.</li>
 *   <li>Runs the full plagiarism pipeline (via {@link PlagiarismAnalysisService})
 *       on each source snippet vs. the input text.</li>
 *   <li>Aggregates results into an {@link InternetScanResult}.</li>
 * </ul>
 */
@Service
public class InternetScanService {

    /** Maximum search queries to fire per scan (keeps response time acceptable). */
    private static final int MAX_QUERIES = 4;

    /** Maximum web sources to analyse per scan. */
    private static final int MAX_SOURCES = 10;

    /** Minimum snippet length (chars) worth analysing. */
    private static final int MIN_SNIPPET_LEN = 40;

    @Autowired
    private PlagiarismAnalysisService analysisService;

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Scans the internet for potential plagiarism matches against
     * the supplied {@code inputText}.
     *
     * @param inputText the document text to check
     * @param fromDate  optional ISO date string "YYYY-MM-DD" to restrict
     *                  results to pages published after that date
     * @return a fully populated {@link InternetScanResult}
     */
    public InternetScanResult scan(String inputText, String fromDate) {
        long startNano = System.nanoTime();

        // 1. Extract key phrases to use as search queries
        List<String> queries = extractKeyPhrases(inputText, MAX_QUERIES);

        // 2. Collect unique search results from the web
        Map<String, WebFetcher.SearchResult> urlToResult = new LinkedHashMap<>();
        for (String query : queries) {
            List<WebFetcher.SearchResult> hits =
                    WebFetcher.search(query, fromDate, MAX_SOURCES);
            for (WebFetcher.SearchResult hit : hits) {
                urlToResult.putIfAbsent(hit.url, hit);
            }
        }

        // 3. Analyse each source snippet against the input text
        List<SourceMatch> matches = new ArrayList<>();
        for (WebFetcher.SearchResult sr : urlToResult.values()) {
            if (matches.size() >= MAX_SOURCES) break;

            // Prefer the snippet from DDG; fall back to fetching the page
            String sourceText = sr.snippet;
            if (sourceText == null || sourceText.length() < MIN_SNIPPET_LEN) {
                sourceText = WebFetcher.fetchPageText(sr.url);
                // Truncate to first 2000 chars for performance
                if (sourceText.length() > 2000) sourceText = sourceText.substring(0, 2000);
            }
            if (sourceText == null || sourceText.length() < MIN_SNIPPET_LEN) continue;

            // Run the full 7-algorithm plagiarism pipeline
            AnalysisResult ar = analysisService.analyze(inputText, sourceText);
            double sim = ar.getOverallSimilarity();

            // Only include sources with at least 5% similarity to reduce noise
            if (sim < 5.0) continue;

            String matchType = classifyMatch(sim);
            matches.add(new SourceMatch(sr.title, sr.url, sourceText, sim, matchType));
        }

        // 4. Sort by similarity descending
        matches.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

        // 5. Compute overall internet similarity = highest single-source score
        double overall = matches.isEmpty() ? 0.0 : matches.get(0).getSimilarity();

        // 6. Build and return the result object
        InternetScanResult result = new InternetScanResult(overall);
        result.setSources(matches);
        result.setSourcesChecked(urlToResult.size());
        result.setQueriesMade(queries.size());
        result.setFromDate(fromDate);

        long elapsed = System.nanoTime() - startNano;
        result.setProcessingTime(String.format("%.1fms", elapsed / 1_000_000.0));

        return result;
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    /**
     * Extracts up to {@code maxQueries} short key-phrase queries from the
     * input text. Strategy:
     * <ol>
     *   <li>Split into sentences.</li>
     *   <li>Pick the longest sentences (most content-dense).</li>
     *   <li>Truncate each to the first 10 words (good search query length).</li>
     * </ol>
     */
    private List<String> extractKeyPhrases(String text, int maxQueries) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        String[] sentences = text.split("(?<=[.!?])\\s+");

        return Arrays.stream(sentences)
                .map(String::trim)
                .filter(s -> s.length() > 30)
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .limit(maxQueries)
                .map(s -> {
                    // Take first 10 words as the search query
                    String[] words = s.split("\\s+");
                    int take = Math.min(10, words.length);
                    return Arrays.stream(words, 0, take)
                            .collect(Collectors.joining(" "));
                })
                .filter(q -> q.split("\\s+").length >= 3) // at least 3 words
                .distinct()
                .collect(Collectors.toList());
    }

    /** Classifies a similarity score into a human-readable match type. */
    private String classifyMatch(double sim) {
        if (sim >= 75) return "EXACT";
        if (sim >= 50) return "NEAR_EXACT";
        if (sim >= 25) return "PARAPHRASE";
        return "LOW";
    }
}
