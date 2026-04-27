package com.plagchecker.core;

import com.plagchecker.preprocessing.TextPreprocessor;

public class KMPMatcher {

    public static boolean search(String text, String pattern) {
        text = TextPreprocessor.preprocess(text);
        pattern = TextPreprocessor.preprocess(pattern);
        return text.contains(pattern);
    }
}