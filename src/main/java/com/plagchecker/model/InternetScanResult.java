package com.plagchecker.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the full result of an Internet plagiarism scan.
 * <p>
 * Models the OOP pattern from {@link AnalysisResult} — encapsulates
 * all matched online sources, an overall similarity score,
 * a plain-language verdict, and performance metadata.
 */
public class InternetScanResult {

    /** Highest similarity found across all checked sources (0–100). */
    private double overallInternetSimilarity;

    /** Plain-language verdict: HIGH_PLAGIARISM / MODERATE / LOW / ORIGINAL. */
    private String verdict;

    /** How many online sources were actually checked. */
    private int sourcesChecked;

    /** How many search queries were fired to the web. */
    private int queriesMade;

    /** Wall-clock time for the whole scan. */
    private String processingTime;

    /** Optional date filter that was applied (may be null). */
    private String fromDate;

    /** Top matching sources, sorted by similarity descending. */
    private List<SourceMatch> sources = new ArrayList<>();

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------

    public InternetScanResult() {
    }

    public InternetScanResult(double overallSimilarity) {
        this.overallInternetSimilarity = overallSimilarity;
        this.verdict = generateVerdict(overallSimilarity);
    }

    // ----------------------------------------------------------------
    // Verdict helper
    // ----------------------------------------------------------------

    private static String generateVerdict(double sim) {
        if (sim >= 75)
            return "HIGH_PLAGIARISM";
        if (sim >= 40)
            return "MODERATE_PLAGIARISM";
        if (sim >= 15)
            return "LOW_SIMILARITY";
        return "ORIGINAL";
    }

    // ----------------------------------------------------------------
    // Getters / Setters
    // ----------------------------------------------------------------

    public double getOverallInternetSimilarity() {
        return overallInternetSimilarity;
    }

    public void setOverallInternetSimilarity(double v) {
        this.overallInternetSimilarity = v;
        this.verdict = generateVerdict(v);
    }

    public String getVerdict() {
        return verdict;
    }

    public int getSourcesChecked() {
        return sourcesChecked;
    }

    public void setSourcesChecked(int n) {
        this.sourcesChecked = n;
    }

    public int getQueriesMade() {
        return queriesMade;
    }

    public void setQueriesMade(int n) {
        this.queriesMade = n;
    }

    public String getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(String t) {
        this.processingTime = t;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String d) {
        this.fromDate = d;
    }

    public List<SourceMatch> getSources() {
        return sources;
    }

    public void setSources(List<SourceMatch> s) {
        this.sources = s;
    }

    // ----------------------------------------------------------------
    // Inner class — one matched online source
    // ----------------------------------------------------------------

    /**
     * Represents a single online source that was checked against the
     * submitted text.
     */
    public static class SourceMatch {

        private String title;
        private String url;
        /** The snippet / extracted text fetched from this source. */
        private String snippet;
        /** Similarity score (0–100) against the submitted text. */
        private double similarity;
        /** Match type label: EXACT / NEAR_EXACT / PARAPHRASE / LOW */
        private String matchType;

        public SourceMatch() {
        }

        public SourceMatch(String title, String url, String snippet,
                double similarity, String matchType) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
            this.similarity = similarity;
            this.matchType = matchType;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String t) {
            this.title = t;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String u) {
            this.url = u;
        }

        public String getSnippet() {
            return snippet;
        }

        public void setSnippet(String s) {
            this.snippet = s;
        }

        public double getSimilarity() {
            return similarity;
        }

        public void setSimilarity(double d) {
            this.similarity = d;
        }

        public String getMatchType() {
            return matchType;
        }

        public void setMatchType(String m) {
            this.matchType = m;
        }
    }
}
