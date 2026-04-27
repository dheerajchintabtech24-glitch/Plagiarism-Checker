package com.plagchecker.report;

import com.plagchecker.model.AnalysisResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ReportGenerator — Generates beautiful, printable HTML reports.
 * These reports can be saved as HTML files or printed to PDF via the browser.
 */
@Component
public class ReportGenerator {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm");

    public String generateHtmlReport(String labelA, String labelB, AnalysisResult r) {

        String timestamp = LocalDateTime.now().format(FMT);
        double sim = r.getOverallSimilarity();
        String verdict = r.getVerdict();
        String verdictColor = getVerdictColor(verdict);
        String verdictLabel = getVerdictLabel(verdict);

        StringBuilder sb = new StringBuilder();
        sb.append("""
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>PlagScan Pro — Analysis Report</title>
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Syne:wght@600;700;800&display=swap');
                body { font-family: 'Inter', -apple-system, sans-serif; background: #f8f9fc; color: #1a1a2e; padding: 40px; line-height: 1.6; }
                .container { max-width: 800px; margin: 0 auto; }
                .header { text-align: center; margin-bottom: 40px; }
                .logo { font-family: 'Syne', sans-serif; font-size: 28px; font-weight: 800; color: #6c5ce7; margin-bottom: 4px; }
                .sub { color: #888; font-size: 12px; }
                .meta { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 30px; }
                .meta-item { background: white; border: 1px solid #eee; border-radius: 10px; padding: 14px 18px; }
                .meta-label { font-size: 10px; text-transform: uppercase; color: #888; letter-spacing: 0.5px; margin-bottom: 4px; }
                .meta-value { font-weight: 600; font-size: 14px; }
                .score-card { background: white; border: 1px solid #eee; border-radius: 16px; padding: 32px; text-align: center; margin-bottom: 30px; }
                .score-big { font-family: 'Syne', sans-serif; font-size: 56px; font-weight: 800; }
                .verdict { display: inline-block; padding: 6px 18px; border-radius: 8px; font-size: 13px; font-weight: 600; margin-top: 8px; }
                .alg-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 30px; }
                .alg { background: white; border: 1px solid #eee; border-radius: 10px; padding: 14px; }
                .alg-name { font-size: 10px; text-transform: uppercase; color: #888; letter-spacing: 0.5px; margin-bottom: 4px; }
                .alg-val { font-family: 'Syne', sans-serif; font-size: 22px; font-weight: 700; }
                .alg-bar { height: 4px; background: #eee; border-radius: 4px; margin-top: 8px; overflow: hidden; }
                .alg-fill { height: 100%; border-radius: 4px; }
                .section-title { font-family: 'Syne', sans-serif; font-size: 16px; font-weight: 700; margin-bottom: 14px; }
                .sentence-list { background: white; border: 1px solid #eee; border-radius: 12px; overflow: hidden; margin-bottom: 30px; }
                .sent { padding: 12px 18px; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: start; gap: 12px; }
                .sent:last-child { border-bottom: none; }
                .sent-text { font-size: 12px; flex: 1; }
                .sent-match { font-size: 10px; color: #888; font-style: italic; margin-top: 3px; }
                .badge { padding: 3px 10px; border-radius: 6px; font-size: 10px; font-weight: 600; white-space: nowrap; }
                .b-exact { background: #ede9fe; color: #6c5ce7; }
                .b-near { background: #fee2e2; color: #ef4444; }
                .b-para { background: #fef3c7; color: #d97706; }
                .b-orig { background: #d1fae5; color: #059669; }
                .footer { text-align: center; color: #aaa; font-size: 11px; margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; }
                @media print { body { padding: 20px; } .container { max-width: 100%; } }
            </style>
        </head>
        <body>
        <div class="container">
            <div class="header">
                <div class="logo">⚡ PlagScan Pro</div>
                <div class="sub">Advanced Plagiarism Detection Report</div>
            </div>
        """);

        // Meta info
        sb.append(String.format("""
            <div class="meta">
                <div class="meta-item"><div class="meta-label">Document A</div><div class="meta-value">%s</div></div>
                <div class="meta-item"><div class="meta-label">Document B</div><div class="meta-value">%s</div></div>
                <div class="meta-item"><div class="meta-label">Generated</div><div class="meta-value">%s</div></div>
                <div class="meta-item"><div class="meta-label">Processing Time</div><div class="meta-value">%s</div></div>
            </div>
        """, esc(labelA), esc(labelB), timestamp, r.getProcessingTime() != null ? r.getProcessingTime() : "N/A"));

        // Score card
        sb.append(String.format("""
            <div class="score-card">
                <div class="score-big" style="color:%s">%.1f%%</div>
                <div class="verdict" style="background:%s20;color:%s">%s</div>
                <div style="margin-top:16px;color:#888;font-size:12px">
                    Words A: %d · Words B: %d · Matched: %d
                </div>
            </div>
        """, verdictColor, sim, verdictColor, verdictColor, verdictLabel,
                r.getTotalWordsA(), r.getTotalWordsB(), r.getMatchedWords()));

        // Algorithm scores
        sb.append("""
            <div class="section-title">Algorithm Breakdown</div>
            <div class="alg-grid">
        """);
        addAlgCard(sb, "Rabin-Karp", r.getRabinKarpScore(), "#6c5ce7");
        addAlgCard(sb, "KMP Exact", r.getKmpScore(), "#22d3a0");
        addAlgCard(sb, "Winnowing", r.getWinnowingScore(), "#22d3ee");
        addAlgCard(sb, "Levenshtein", r.getLevenshteinScore(), "#f59e0b");
        addAlgCard(sb, "Cosine TF", r.getCosineSimilarity(), "#a78bfa");
        addAlgCard(sb, "Jaccard", r.getJaccardSimilarity(), "#f87171");
        sb.append("</div>");

        // Sentence analysis
        List<AnalysisResult.SentenceScore> sentences = r.getSentenceScores();
        if (sentences != null && !sentences.isEmpty()) {
            sb.append("<div class=\"section-title\">Sentence Analysis</div><div class=\"sentence-list\">");
            for (AnalysisResult.SentenceScore s : sentences) {
                if (s.getSimilarity() < 0.1) continue;
                String badgeCls = switch (s.getType()) {
                    case "EXACT" -> "b-exact";
                    case "NEAR_EXACT" -> "b-near";
                    case "PARAPHRASE" -> "b-para";
                    default -> "b-orig";
                };
                sb.append(String.format("""
                    <div class="sent">
                        <div>
                            <div class="sent-text">%s</div>
                            %s
                        </div>
                        <div>
                            <span class="badge %s">%s</span>
                            <div style="text-align:center;font-size:10px;color:#888;margin-top:3px">%d%%</div>
                        </div>
                    </div>
                """, esc(s.getSentenceA()),
                        s.getBestMatchB() != null && !"ORIGINAL".equals(s.getType())
                                ? "<div class=\"sent-match\">↳ " + esc(truncate(s.getBestMatchB(), 120)) + "</div>" : "",
                        badgeCls, s.getType().replace("_", " "),
                        Math.round(s.getSimilarity() * 100)));
            }
            sb.append("</div>");
        }

        // Footer
        sb.append(String.format("""
            <div class="footer">
                Generated by PlagScan Pro v2.0 · %s · This report is for academic integrity purposes only.
            </div>
        </div>
        </body></html>
        """, timestamp));

        return sb.toString();
    }

    private void addAlgCard(StringBuilder sb, String name, double score, String color) {
        sb.append(String.format("""
            <div class="alg">
                <div class="alg-name">%s</div>
                <div class="alg-val" style="color:%s">%.1f%%</div>
                <div class="alg-bar"><div class="alg-fill" style="width:%.0f%%;background:%s"></div></div>
            </div>
        """, name, color, score, Math.min(score, 100), color));
    }

    private String getVerdictColor(String verdict) {
        return switch (verdict) {
            case "HIGH_PLAGIARISM" -> "#ef4444";
            case "MODERATE_PLAGIARISM" -> "#f59e0b";
            case "LOW_SIMILARITY" -> "#22d3ee";
            default -> "#22d3a0";
        };
    }

    private String getVerdictLabel(String verdict) {
        return switch (verdict) {
            case "HIGH_PLAGIARISM" -> "⚠ High Plagiarism Detected";
            case "MODERATE_PLAGIARISM" -> "◈ Moderate Similarity";
            case "LOW_SIMILARITY" -> "◎ Low Similarity";
            default -> "✓ Likely Original";
        };
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}