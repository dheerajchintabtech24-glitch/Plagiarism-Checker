package com.plagchecker.api;

import com.plagchecker.core.*;
import com.plagchecker.model.AnalysisResult;
import com.plagchecker.preprocessing.TextPreprocessor;
import com.plagchecker.synonyms.SynonymMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Full plagiarism analysis pipeline — wires together all 5+ algorithms
 * and produces a rich AnalysisResult the frontend can render.
 */
@Service
public class PlagiarismAnalysisService {

    @Autowired private RabinKarp rabinKarp;
    @Autowired private Winnowing winnowing;
    @Autowired private Levenshtein levenshtein;
    @Autowired private SynonymMap synonymMap;

    public AnalysisResult analyze(String textA, String textB) {
        long startTime = System.nanoTime();

        // ---- Tokenize ----
        String processedA = TextPreprocessor.preprocess(textA);
        String processedB = TextPreprocessor.preprocess(textB);

        String[] wordsA = processedA.isEmpty() ? new String[0] : processedA.split("\\s+");
        String[] wordsB = processedB.isEmpty() ? new String[0] : processedB.split("\\s+");

        List<String> tokensA = Arrays.asList(wordsA);
        List<String> tokensB = Arrays.asList(wordsB);

        // ---- Sentence splitting ----
        List<String> sentencesA = splitSentences(textA);
        List<String> sentencesB = splitSentences(textB);

        // ---- Algorithm 1: Rabin-Karp ----
        double rkScore = rabinKarp.computeSimilarity(tokensA, tokensB) * 100.0;

        // ---- Algorithm 2: KMP (exact phrase match — ratio of matching sentences) ----
        double kmpScore = computeKmpScore(sentencesA, sentencesB);

        // ---- Algorithm 3: Winnowing ----
        double winScore = winnowing.computeSimilarity(tokensA, tokensB) * 100.0;

        // ---- Algorithm 4: Levenshtein ----
        double levScore = levenshtein.computeSimilarity(sentencesA, sentencesB) * 100.0;

        // ---- Algorithm 5: Cosine TF Similarity ----
        double cosScore = computeCosineSimilarity(tokensA, tokensB) * 100.0;

        // ---- Algorithm 6: Jaccard word-level ----
        double jacScore = computeJaccard(tokensA, tokensB) * 100.0;

        // ---- Synonym-aware bonus ----
        double synScore = synonymMap.synonymAwareSimilarity(tokensA, tokensB) * 100.0;

        // ---- Overall weighted score ----
        double overall = (rkScore * 0.20)
                        + (kmpScore * 0.10)
                        + (winScore * 0.20)
                        + (levScore * 0.20)
                        + (cosScore * 0.15)
                        + (jacScore * 0.05)
                        + (synScore * 0.10);
        overall = Math.min(overall, 100.0);

        // ---- Matched word count ----
        Set<String> setA = new HashSet<>(tokensA);
        Set<String> setB = new HashSet<>(tokensB);
        setA.retainAll(setB);
        int matchedWords = setA.size();

        // ---- Build matched segments from Rabin-Karp positions ----
        List<AnalysisResult.MatchedSegment> segments = buildMatchedSegments(tokensA, tokensB, wordsA);

        // ---- Sentence scores ----
        List<AnalysisResult.SentenceScore> sentenceScores = buildSentenceScores(sentencesA, sentencesB);

        // ---- Processing time ----
        long elapsed = System.nanoTime() - startTime;
        String processingTime = String.format("%.1fms", elapsed / 1_000_000.0);

        // ---- Assemble result ----
        AnalysisResult result = new AnalysisResult(overall);
        result.setRabinKarpScore(rkScore);
        result.setKmpScore(kmpScore);
        result.setWinnowingScore(winScore);
        result.setLevenshteinScore(levScore);
        result.setCosineSimilarity(cosScore);
        result.setJaccardSimilarity(jacScore);
        result.setTotalWordsA(wordsA.length);
        result.setTotalWordsB(wordsB.length);
        result.setMatchedWords(matchedWords);
        result.setProcessingTime(processingTime);
        result.setMatchedSegments(segments);
        result.setSentenceScores(sentenceScores);

        return result;
    }

    // ---- Helper: KMP exact sentence matching ----
    private double computeKmpScore(List<String> sentA, List<String> sentB) {
        if (sentA.isEmpty() || sentB.isEmpty()) return 0.0;
        int matched = 0;
        for (String sa : sentA) {
            if (sa.trim().length() < 10) continue;
            for (String sb : sentB) {
                if (KMPMatcher.search(sb, sa) || KMPMatcher.search(sa, sb)) {
                    matched++;
                    break;
                }
            }
        }
        return sentA.isEmpty() ? 0.0 : (double) matched / sentA.size() * 100.0;
    }

    // ---- Helper: Cosine TF similarity ----
    private double computeCosineSimilarity(List<String> tokensA, List<String> tokensB) {
        Map<String, Integer> tfA = new HashMap<>(), tfB = new HashMap<>();
        for (String t : tokensA) tfA.merge(t, 1, Integer::sum);
        for (String t : tokensB) tfB.merge(t, 1, Integer::sum);

        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(tfA.keySet());
        allTerms.addAll(tfB.keySet());

        double dot = 0, magA = 0, magB = 0;
        for (String term : allTerms) {
            int a = tfA.getOrDefault(term, 0);
            int b = tfB.getOrDefault(term, 0);
            dot += a * b;
            magA += a * a;
            magB += b * b;
        }
        double denom = Math.sqrt(magA) * Math.sqrt(magB);
        return denom == 0 ? 0.0 : dot / denom;
    }

    // ---- Helper: Jaccard ----
    private double computeJaccard(List<String> tokensA, List<String> tokensB) {
        Set<String> setA = new HashSet<>(tokensA);
        Set<String> setB = new HashSet<>(tokensB);
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    // ---- Helper: Build matched segments from Rabin-Karp match positions ----
    private List<AnalysisResult.MatchedSegment> buildMatchedSegments(
            List<String> tokensA, List<String> tokensB, String[] rawWordsA) {

        List<AnalysisResult.MatchedSegment> segments = new ArrayList<>();
        try {
            int k = Math.min(5, Math.min(tokensA.size(), tokensB.size()));
            if (k < 2) return segments;

            List<int[]> positions = rabinKarp.findMatchedPositions(tokensA, tokensB, k);
            for (int[] pos : positions) {
                String matchedText = String.join(" ", tokensA.subList(pos[0], pos[1]));
                if (matchedText.length() < 8) continue;
                segments.add(new AnalysisResult.MatchedSegment(
                    matchedText, "EXACT", pos[0], pos[2]
                ));
            }
        } catch (Exception e) {
            // Graceful: return what we have
        }
        return segments;
    }

    // ---- Helper: Build sentence scores ----
    private List<AnalysisResult.SentenceScore> buildSentenceScores(
            List<String> sentA, List<String> sentB) {

        List<AnalysisResult.SentenceScore> results = new ArrayList<>();
        for (String sa : sentA) {
            if (sa.trim().length() < 10) continue;

            double bestSim = 0;
            String bestMatch = "";
            for (String sb : sentB) {
                double sim = levenshtein.sentenceSimilarity(sa, sb);
                if (sim > bestSim) {
                    bestSim = sim;
                    bestMatch = sb;
                }
            }

            String type;
            if (bestSim >= 0.95) type = "EXACT";
            else if (bestSim >= 0.80) type = "NEAR_EXACT";
            else if (bestSim >= 0.55) type = "PARAPHRASE";
            else type = "ORIGINAL";

            results.add(new AnalysisResult.SentenceScore(sa, bestMatch, bestSim, type));
        }
        return results;
    }

    // ---- Helper: Split text into sentences ----
    private List<String> splitSentences(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        String[] parts = text.split("(?<=[.!?])\\s+");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}