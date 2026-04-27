package com.plagchecker.core;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Levenshtein Edit Distance — Fuzzy Sentence Matching
 *
 * Compares every sentence in A against every sentence in B.
 * Uses normalized edit distance to detect paraphrased content.
 * Time Complexity: O(|A| * |B| * len_a * len_b)
 */
@Component
public class Levenshtein {

    private static final double PARAPHRASE_THRESHOLD = 0.65; // if similarity >= this → paraphrase

    /**
     * Compute overall document similarity using sentence-level edit distances.
     */
    public double computeSimilarity(List<String> sentencesA, List<String> sentencesB) {
        if (sentencesA.isEmpty() || sentencesB.isEmpty()) return 0.0;

        double totalSim = 0.0;
        int count = 0;

        for (String sA : sentencesA) {
            if (sA.length() < 15) continue; // skip very short sentences
            double bestMatch = 0.0;
            for (String sB : sentencesB) {
                double sim = sentenceSimilarity(sA, sB);
                if (sim > bestMatch) bestMatch = sim;
                if (bestMatch >= 0.99) break; // exact match found
            }
            totalSim += bestMatch;
            count++;
        }

        return count == 0 ? 0.0 : totalSim / count;
    }

    /**
     * Find all paraphrased sentence pairs.
     */
    public List<ParaphraseMatch> findParaphrases(List<String> sentencesA, List<String> sentencesB) {
        List<ParaphraseMatch> matches = new ArrayList<>();

        for (int i = 0; i < sentencesA.size(); i++) {
            String sA = sentencesA.get(i);
            if (sA.length() < 15) continue;

            double bestSim = 0.0;
            int bestJ = -1;

            for (int j = 0; j < sentencesB.size(); j++) {
                double sim = sentenceSimilarity(sA, sentencesB.get(j));
                if (sim > bestSim) {
                    bestSim = sim;
                    bestJ = j;
                }
            }

            if (bestSim >= PARAPHRASE_THRESHOLD && bestJ >= 0) {
                String type = bestSim >= 0.95 ? "EXACT" : bestSim >= 0.80 ? "NEAR_EXACT" : "PARAPHRASE";
                matches.add(new ParaphraseMatch(sA, sentencesB.get(bestJ), bestSim, type, i, bestJ));
            }
        }

        return matches;
    }

    /**
     * Normalized similarity between two sentences [0.0–1.0].
     * 1.0 = identical, 0.0 = completely different.
     */
    public double sentenceSimilarity(String a, String b) {
        a = a.toLowerCase().trim();
        b = b.toLowerCase().trim();
        if (a.equals(b)) return 1.0;
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;
        int dist = editDistance(a, b);
        return 1.0 - (double) dist / maxLen;
    }

    /**
     * Standard Levenshtein edit distance between two strings.
     * Uses O(min(|a|, |b|)) space optimization.
     */
    public int editDistance(String a, String b) {
        if (a.equals(b)) return 0;
        if (a.isEmpty()) return b.length();
        if (b.isEmpty()) return a.length();

        // Ensure a is the shorter string
        if (a.length() > b.length()) {
            String tmp = a; a = b; b = tmp;
        }

        int[] prev = new int[a.length() + 1];
        int[] curr = new int[a.length() + 1];

        for (int i = 0; i <= a.length(); i++) prev[i] = i;

        for (int j = 1; j <= b.length(); j++) {
            curr[0] = j;
            for (int i = 1; i <= a.length(); i++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                curr[i] = Math.min(
                    Math.min(prev[i] + 1, curr[i - 1] + 1),
                    prev[i - 1] + cost
                );
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }

        return prev[a.length()];
    }

    /**
     * Word-level edit distance (operates on token arrays).
     * More meaningful for sentence comparison than char-level.
     */
    public int wordEditDistance(String[] a, String[] b) {
        int m = a.length, n = b.length;
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = a[i - 1].equals(b[j - 1]) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[m][n];
    }

    public static class ParaphraseMatch {
        public final String sentenceA;
        public final String sentenceB;
        public final double similarity;
        public final String type;
        public final int indexA;
        public final int indexB;

        public ParaphraseMatch(String sentenceA, String sentenceB, double similarity,
                               String type, int indexA, int indexB) {
            this.sentenceA = sentenceA;
            this.sentenceB = sentenceB;
            this.similarity = similarity;
            this.type = type;
            this.indexA = indexA;
            this.indexB = indexB;
        }
    }
}
