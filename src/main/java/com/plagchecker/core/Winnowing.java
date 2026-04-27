package com.plagchecker.core;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Winnowing Algorithm — Document Fingerprinting
 *
 * This is the algorithm used by Stanford's MOSS plagiarism detection system.
 * Robust against reordering, minor insertions, and deletions.
 *
 * Steps:
 *  1. Generate all k-grams
 *  2. Hash each k-gram (Rabin-Karp style)
 *  3. Apply sliding window of size w → select minimum hash in each window
 *  4. Collect the selected hashes as the document's "fingerprint"
 *  5. Fingerprint overlap = similarity
 *
 * Time Complexity: O(n) for fingerprinting, O(|FA| + |FB|) for comparison
 */
@Component
public class Winnowing {

    private static final int K = 5;  // k-gram size
    private static final int W = 4;  // window size
    private static final long BASE = 31L;
    private static final long MOD = 1_000_000_007L;

    /**
     * Compute Jaccard similarity of document fingerprints.
     */
    public double computeSimilarity(List<String> tokensA, List<String> tokensB) {
        Set<Long> fpA = fingerprint(tokensA);
        Set<Long> fpB = fingerprint(tokensB);

        if (fpA.isEmpty() || fpB.isEmpty()) return 0.0;

        long intersection = fpA.stream().filter(fpB::contains).count();
        long union = fpA.size() + fpB.size() - intersection;

        return union == 0 ? 0.0 : (double) intersection / union;
    }

    /**
     * Generate the winnowed fingerprint set for a document.
     */
    public Set<Long> fingerprint(List<String> tokens) {
        if (tokens.size() < K) return new HashSet<>();

        // Step 1: hash all k-grams
        List<Long> kgramHashes = new ArrayList<>();
        for (int i = 0; i <= tokens.size() - K; i++) {
            kgramHashes.add(hashKGram(tokens, i));
        }

        // Step 2: winnow — select minimum hash in each window of size W
        Set<Long> fingerprints = new HashSet<>();
        if (kgramHashes.size() < W) {
            fingerprints.addAll(kgramHashes);
            return fingerprints;
        }

        long prevMin = Long.MAX_VALUE;
        int prevMinPos = -1;

        for (int i = 0; i <= kgramHashes.size() - W; i++) {
            // Find min in current window
            long windowMin = Long.MAX_VALUE;
            int windowMinPos = i;

            for (int j = i; j < i + W; j++) {
                if (kgramHashes.get(j) < windowMin) {
                    windowMin = kgramHashes.get(j);
                    windowMinPos = j;
                }
            }

            // Add to fingerprint if it's a new minimum or position changed
            if (windowMin != prevMin || windowMinPos != prevMinPos) {
                fingerprints.add(windowMin);
                prevMin = windowMin;
                prevMinPos = windowMinPos;
            }
        }

        return fingerprints;
    }

    /**
     * Get fingerprint overlap details: which hashes match.
     */
    public FingerprintResult compareFingerprints(List<String> tokensA, List<String> tokensB) {
        Set<Long> fpA = fingerprint(tokensA);
        Set<Long> fpB = fingerprint(tokensB);

        Set<Long> shared = new HashSet<>(fpA);
        shared.retainAll(fpB);

        long union = fpA.size() + fpB.size() - shared.size();
        double similarity = union == 0 ? 0.0 : (double) shared.size() / union;

        return new FingerprintResult(fpA.size(), fpB.size(), shared.size(), similarity);
    }

    private long hashKGram(List<String> tokens, int start) {
        long hash = 0;
        long power = 1;
        for (int i = start; i < start + K; i++) {
            for (char c : tokens.get(i).toCharArray()) {
                hash = (hash + (c - 'a' + 1) * power) % MOD;
                power = (power * BASE) % MOD;
            }
            hash = (hash + 27 * power) % MOD;
            power = (power * BASE) % MOD;
        }
        return hash;
    }

    public static class FingerprintResult {
        public final int sizeA;
        public final int sizeB;
        public final int sharedCount;
        public final double similarity;

        public FingerprintResult(int sizeA, int sizeB, int sharedCount, double similarity) {
            this.sizeA = sizeA;
            this.sizeB = sizeB;
            this.sharedCount = sharedCount;
            this.similarity = similarity;
        }
    }
}
