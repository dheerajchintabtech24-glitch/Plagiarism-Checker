package com.plagchecker.core;

import com.plagchecker.preprocessing.TextPreprocessor;

public class SimilarityScorer {

    public static double score(String text1, String text2) {

        text1 = TextPreprocessor.preprocess(text1);
        text2 = TextPreprocessor.preprocess(text2);

        String[] words1 = text1.split(" ");
        String[] words2 = text2.split(" ");

        int matches = 0;

        for (String w1 : words1) {
            for (String w2 : words2) {
                if (w1.equalsIgnoreCase(w2)) {
                    matches++;
                }
            }
        }

        return (double) matches / (words1.length + words2.length) * 100;
    }
}