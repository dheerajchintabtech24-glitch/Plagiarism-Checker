package com.plagchecker.entity;

import java.time.LocalDateTime;

/**
 * Representing a single plagiarism analysis check.
 * Stored in memory for history tracking during the current session.
 */
public class PlagiarismCheck {

    private Long id;
    private String labelA;
    private String labelB;
    private double overallSimilarity;
    private double rabinKarpScore;
    private double kmpScore;
    private double winnowingScore;
    private double levenshteinScore;
    private double cosineSimilarity;
    private double jaccardSimilarity;
    private String verdict;
    private int totalWordsA;
    private int totalWordsB;
    private int matchedWords;
    private int matchCount;
    private String processingTime;
    private String analysisMode; // TEXT, FILE, URL
    private String inputTextA;
    private String inputTextB;
    private LocalDateTime createdAt;

    public PlagiarismCheck() {
        this.createdAt = LocalDateTime.now();
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLabelA() { return labelA; }
    public void setLabelA(String labelA) { this.labelA = labelA; }

    public String getLabelB() { return labelB; }
    public void setLabelB(String labelB) { this.labelB = labelB; }

    public double getOverallSimilarity() { return overallSimilarity; }
    public void setOverallSimilarity(double overallSimilarity) { this.overallSimilarity = overallSimilarity; }

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

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public int getTotalWordsA() { return totalWordsA; }
    public void setTotalWordsA(int totalWordsA) { this.totalWordsA = totalWordsA; }

    public int getTotalWordsB() { return totalWordsB; }
    public void setTotalWordsB(int totalWordsB) { this.totalWordsB = totalWordsB; }

    public int getMatchedWords() { return matchedWords; }
    public void setMatchedWords(int matchedWords) { this.matchedWords = matchedWords; }

    public int getMatchCount() { return matchCount; }
    public void setMatchCount(int matchCount) { this.matchCount = matchCount; }

    public String getProcessingTime() { return processingTime; }
    public void setProcessingTime(String processingTime) { this.processingTime = processingTime; }

    public String getAnalysisMode() { return analysisMode; }
    public void setAnalysisMode(String analysisMode) { this.analysisMode = analysisMode; }

    public String getInputTextA() { return inputTextA; }
    public void setInputTextA(String inputTextA) { this.inputTextA = inputTextA; }

    public String getInputTextB() { return inputTextB; }
    public void setInputTextB(String inputTextB) { this.inputTextB = inputTextB; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

