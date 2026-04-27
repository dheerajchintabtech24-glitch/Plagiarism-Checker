package com.plagchecker.core;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Rabin-Karp Algorithm — Rolling Hash Substring Matching
 *
 * Generates k-gram hashes from both documents and finds intersecting hashes.
 * Time Complexity: O(n + m) average case
 * Used for: detecting copied passages of k consecutive words
 */
@Component
public class RabinKarp {

    private static final long BASE = 31L;
    private static final long MOD = 1_000_000_007L;
    private static final int DEFAULT_K = 5; // 5-word window

    /**
     * Compute similarity score [0.0 - 1.0] between two token lists.
     * Returns ratio of matched k-grams to total unique k-grams.
     */
    public double computeSimilarity(List<String> tokensA, List<String> tokensB) {
        return computeSimilarity(tokensA, tokensB, DEFAULT_K);
    }

    public double computeSimilarity(List<String> tokensA, List<String> tokensB, int k) {
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0;
        if (k > tokensA.size() || k > tokensB.size()) k = Math.min(tokensA.size(), tokensB.size());

        Set<Long> hashesA = computeHashes(tokensA, k);
        Set<Long> hashesB = computeHashes(tokensB, k);

        long intersection = hashesA.stream().filter(hashesB::contains).count();
        long union = hashesA.size() + hashesB.size() - intersection;

        return union == 0 ? 0.0 : (double) intersection / union;
    }

    /**
     * Find matched k-gram positions between two documents.
     * Returns list of [startA, endA] index pairs that match something in B.
     */
    public List<int[]> findMatchedPositions(List<String> tokensA, List<String> tokensB, int k) {
        List<int[]> matches = new ArrayList<>();
        if (tokensA.size() < k || tokensB.size() < k) return matches;

        // Build hash→positions map for B
        Map<Long, List<Integer>> hashToPosB = new HashMap<>();
        for (int i = 0; i <= tokensB.size() - k; i++) {
            long hash = hashKGram(tokensB, i, k);
            hashToPosB.computeIfAbsent(hash, x -> new ArrayList<>()).add(i);
        }

        // Slide over A, check each k-gram hash against B
        Set<Integer> covered = new HashSet<>();
        for (int i = 0; i <= tokensA.size() - k; i++) {
            if (covered.contains(i)) continue;
            long hash = hashKGram(tokensA, i, k);
            if (hashToPosB.containsKey(hash)) {
                // Verify actual content (collision safety)
                List<String> gramA = tokensA.subList(i, i + k);
                for (int posB : hashToPosB.get(hash)) {
                    List<String> gramB = tokensB.subList(posB, posB + k);
                    if (gramA.equals(gramB)) {
                        // Extend match as far as possible
                        int ext = k;
                        while (i + ext < tokensA.size() &&
                               posB + ext < tokensB.size() &&
                               tokensA.get(i + ext).equals(tokensB.get(posB + ext))) {
                            ext++;
                        }
                        matches.add(new int[]{i, i + ext, posB, posB + ext});
                        for (int m = i; m < i + ext; m++) covered.add(m);
                        break;
                    }
                }
            }
        }
        return matches;
    }

    /**
     * Compute rolling hash for all k-grams in a token list.
     */
    private Set<Long> computeHashes(List<String> tokens, int k) {
        Set<Long> hashes = new HashSet<>();
        for (int i = 0; i <= tokens.size() - k; i++) {
            hashes.add(hashKGram(tokens, i, k));
        }
        return hashes;
    }

    /**
     * Hash a k-gram starting at position i.
     * Uses polynomial rolling hash: sum(char[j] * BASE^j) mod MOD
     */
    private long hashKGram(List<String> tokens, int start, int k) {
        long hash = 0;
        long power = 1;
        for (int i = start; i < start + k; i++) {
            String token = tokens.get(i);
            for (char c : token.toCharArray()) {
                hash = (hash + (c - 'a' + 1) * power) % MOD;
                power = (power * BASE) % MOD;
            }
            // Separator between tokens
            hash = (hash + 27 * power) % MOD;
            power = (power * BASE) % MOD;
        }
        return hash;
    }
}
