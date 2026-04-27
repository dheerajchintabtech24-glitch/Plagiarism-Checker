package com.plagchecker.batch;

import com.plagchecker.api.PlagiarismAnalysisService;
import com.plagchecker.model.AnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BatchAnalysisService {

    @Autowired
    private PlagiarismAnalysisService service;

    public AnalysisResult compareBatch(String[] texts) {

        double total = 0;
        int count = 0;

        for (int i = 0; i < texts.length; i++) {
            for (int j = i + 1; j < texts.length; j++) {
                total += service.analyze(texts[i], texts[j]).getOverallSimilarity();
                count++;
            }
        }

        double avg = (count == 0) ? 0 : total / count;

        return new AnalysisResult(avg);
    }
}