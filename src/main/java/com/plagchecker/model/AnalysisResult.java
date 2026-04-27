package com.plagchecker.model;

import java.util.*;

public class AnalysisResult {

    private double overallSimilarity;
    private String verdict;

    // Individual algorithm scores (0-100)
    private double rabinKarpScore;
    private double kmpScore;
    private double winnowingScore;
    private double levenshteinScore;
    private double cosineSimilarity;
    private double jaccardSimilarity;

    // Stats
    private int totalWordsA;
    private int totalWordsB;
    private int matchedWords;
    private String processingTime;

    // Matched segments for side-by-side diff
    private List<MatchedSegment> matchedSegments = new ArrayList<>();

    // Sentence-level scores
    private List<SentenceScore> sentenceScores = new ArrayList<>();

    public AnalysisResult(double similarity) {
        this.overallSimilarity = similarity;
        this.verdict = generateVerdict(similarity);
    }

    public double getOverallSimilarity() { return overallSimilarity; }
    public void setOverallSimilarity(double overallSimilarity) {
        this.overallSimilarity = overallSimilarity;
        this.verdict = generateVerdict(overallSimilarity);
    }

    public String getVerdict() { return verdict; }

    public double getRabinKarpScore() { return rabinKarpScore; }
    public void setRabinKarpScore(double rabinKarpScore) { this.rabinKarpScore = rabinKarpScore; }

    public double getKmpScore() { return kmpScore; }
    public void setKmpScore(double kmpScore) { this.kmpScore = kmpScore; }

    public double getWinnowingScore() { return winnowingScore; }
    public void setWinnowingScore(double winnowingScore) { this.winnowingScore = winnowingScore; }

    public double getLevenshteinScore() { return levenshteinScore; }
    public void setLevenshteinScore(double levenshteinScore) { this.levenshteinScore = levenshteinScore; }

    public double getCosineSimilarity() { return cosineSimilarity; }
    public void setCosineSimilarity(double cosineSimilarity) { this.cosineSimilarity = cosineSimilarity; }

    public double getJaccardSimilarity() { return jaccardSimilarity; }
    public void setJaccardSimilarity(double jaccardSimilarity) { this.jaccardSimilarity = jaccardSimilarity; }

    public int getTotalWordsA() { return totalWordsA; }
    public void setTotalWordsA(int totalWordsA) { this.totalWordsA = totalWordsA; }

    public int getTotalWordsB() { return totalWordsB; }
    public void setTotalWordsB(int totalWordsB) { this.totalWordsB = totalWordsB; }

    public int getMatchedWords() { return matchedWords; }
    public void setMatchedWords(int matchedWords) { this.matchedWords = matchedWords; }

    public String getProcessingTime() { return processingTime; }
    public void setProcessingTime(String processingTime) { this.processingTime = processingTime; }

    public List<MatchedSegment> getMatchedSegments() { return matchedSegments; }
    public void setMatchedSegments(List<MatchedSegment> matchedSegments) { this.matchedSegments = matchedSegments; }

    public List<SentenceScore> getSentenceScores() { return sentenceScores; }
    public void setSentenceScores(List<SentenceScore> sentenceScores) { this.sentenceScores = sentenceScores; }

    private String generateVerdict(double similarity) {
        if (similarity > 75) return "HIGH_PLAGIARISM";
        if (similarity > 40) return "MODERATE_PLAGIARISM";
        if (similarity > 15) return "LOW_SIMILARITY";
        return "ORIGINAL";
    }

    // ---------- Inner classes ----------

    public static class MatchedSegment {
        private String text;
        private String matchType; // EXACT, NEAR_EXACT, PARAPHRASE
        private int positionA;
        private int positionB;

        public MatchedSegment() {}
        public MatchedSegment(String text, String matchType, int positionA, int positionB) {
            this.text = text;
            this.matchType = matchType;
            this.positionA = positionA;
            this.positionB = positionB;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getMatchType() { return matchType; }
        public void setMatchType(String matchType) { this.matchType = matchType; }
        public int getPositionA() { return positionA; }
        public void setPositionA(int positionA) { this.positionA = positionA; }
        public int getPositionB() { return positionB; }
        public void setPositionB(int positionB) { this.positionB = positionB; }
    }

    public static class SentenceScore {
        private String sentenceA;
        private String bestMatchB;
        private double similarity;
        private String type; // EXACT, NEAR_EXACT, PARAPHRASE, ORIGINAL

        public SentenceScore() {}
        public SentenceScore(String sentenceA, String bestMatchB, double similarity, String type) {
            this.sentenceA = sentenceA;
            this.bestMatchB = bestMatchB;
            this.similarity = similarity;
            this.type = type;
        }

        public String getSentenceA() { return sentenceA; }
        public void setSentenceA(String sentenceA) { this.sentenceA = sentenceA; }
        public String getBestMatchB() { return bestMatchB; }
        public void setBestMatchB(String bestMatchB) { this.bestMatchB = bestMatchB; }
        public double getSimilarity() { return similarity; }
        public void setSimilarity(double similarity) { this.similarity = similarity; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}