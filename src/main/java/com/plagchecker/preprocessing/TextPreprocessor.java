package com.plagchecker.preprocessing;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TextPreprocessor — Advanced text preprocessing pipeline.
 *
 * Features:
 *  - Text normalization (lowercase, punctuation removal)
 *  - Stopword removal
 *  - Sentence segmentation
 *  - Tokenization
 *  - N-gram / shingling generation
 */
public class TextPreprocessor {

    // Common English stopwords
    private static final Set<String> STOPWORDS = Set.of(
        "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "is", "was", "are", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "shall", "can", "it", "its", "this", "that",
        "these", "those", "i", "you", "he", "she", "we", "they", "me", "him",
        "her", "us", "them", "my", "your", "his", "our", "their", "not", "no",
        "if", "then", "than", "when", "while", "as", "so", "very", "just",
        "about", "also", "more", "most", "some", "any", "all", "each", "every",
        "from", "into", "over", "after", "before", "between", "under", "above",
        "up", "down", "out", "off", "such", "only", "own", "same", "too",
        "which", "who", "whom", "what", "where", "how", "there", "here"
    );

    /**
     * Basic preprocessing: lowercase + remove punctuation + normalize whitespace.
     */
    public static String preprocess(String text) {
        if (text == null || text.isBlank()) return "";
        return text.toLowerCase()
                   .replaceAll("[^a-zA-Z0-9 ]", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    /**
     * Advanced preprocessing with stopword removal.
     */
    public static String preprocessAdvanced(String text) {
        if (text == null || text.isBlank()) return "";
        String[] words = preprocess(text).split("\\s+");
        return Arrays.stream(words)
                .filter(w -> !w.isEmpty() && !STOPWORDS.contains(w))
                .collect(Collectors.joining(" "));
    }

    /**
     * Tokenize text into a list of cleaned words.
     */
    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        String processed = preprocess(text);
        if (processed.isEmpty()) return Collections.emptyList();
        return Arrays.asList(processed.split("\\s+"));
    }

    /**
     * Tokenize with stopword removal.
     */
    public static List<String> tokenizeWithoutStopwords(String text) {
        return tokenize(text).stream()
                .filter(w -> !STOPWORDS.contains(w))
                .collect(Collectors.toList());
    }

    /**
     * Split text into sentences using regex.
     */
    public static List<String> splitSentences(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        String[] parts = text.split("(?<=[.!?])\\s+");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Generate n-grams (shingles) from a list of tokens.
     *
     * @param tokens List of tokens
     * @param n Size of each n-gram
     * @return Set of n-gram strings
     */
    public static Set<String> generateNGrams(List<String> tokens, int n) {
        Set<String> ngrams = new LinkedHashSet<>();
        if (tokens.size() < n) return ngrams;

        for (int i = 0; i <= tokens.size() - n; i++) {
            String ngram = String.join(" ", tokens.subList(i, i + n));
            ngrams.add(ngram);
        }
        return ngrams;
    }

    /**
     * Generate character-level n-grams.
     */
    public static Set<String> generateCharNGrams(String text, int n) {
        Set<String> ngrams = new LinkedHashSet<>();
        String clean = preprocess(text).replaceAll("\\s+", "");
        if (clean.length() < n) return ngrams;

        for (int i = 0; i <= clean.length() - n; i++) {
            ngrams.add(clean.substring(i, i + n));
        }
        return ngrams;
    }

    /**
     * Calculate n-gram similarity (Jaccard) between two texts.
     * This is the primary plagiarism detection technique using shingling.
     */
    public static double ngramSimilarity(String text1, String text2, int n) {
        List<String> tokens1 = tokenize(text1);
        List<String> tokens2 = tokenize(text2);

        Set<String> ngrams1 = generateNGrams(tokens1, n);
        Set<String> ngrams2 = generateNGrams(tokens2, n);

        if (ngrams1.isEmpty() || ngrams2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(ngrams1);
        intersection.retainAll(ngrams2);

        Set<String> union = new HashSet<>(ngrams1);
        union.addAll(ngrams2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Check if a word is a stopword.
     */
    public static boolean isStopword(String word) {
        return STOPWORDS.contains(word.toLowerCase());
    }
}