package com.plagchecker.synonyms;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * SynonymMap — Built-in lightweight synonym dictionary.
 *
 * Used by the paraphrase detector to catch synonym substitutions
 * (e.g. "big" → "large", "fast" → "quick") that Levenshtein alone
 * cannot catch because the words are completely different.
 *
 * Each entry maps a canonical form → set of synonyms.
 * All words are stored lowercase and pre-stemmed.
 *
 * To extend: add entries in the static initializer below,
 * or load from a file (WordNet, or a custom .txt wordlist).
 */
@Component
public class SynonymMap {

    // canonical word → set of synonyms (including the canonical word itself)
    private static final Map<String, Set<String>> SYNONYM_GROUPS = new HashMap<>();
    // lookup: any synonym → canonical
    private static final Map<String, String> WORD_TO_CANONICAL = new HashMap<>();

    static {
        register("big",       "large", "huge", "enormous", "vast", "immense", "gigantic", "great");
        register("small",     "little", "tiny", "minute", "miniature", "compact", "petite");
        register("fast",      "quick", "rapid", "swift", "speedy", "brisk", "hasty", "fleet");
        register("slow",      "sluggish", "gradual", "leisurely", "unhurried", "ponderous");
        register("smart",     "intelligent", "clever", "bright", "brilliant", "sharp", "astute", "wise");
        register("show",      "display", "present", "exhibit", "demonstrate", "reveal", "illustrate");
        register("use",       "utilize", "employ", "apply", "implement", "leverage");
        register("make",      "create", "produce", "generate", "build", "construct", "develop", "form");
        register("get",       "obtain", "acquire", "gain", "receive", "retrieve", "fetch", "procure");
        register("give",      "provide", "offer", "supply", "deliver", "grant", "furnish", "yield");
        register("say",       "state", "claim", "assert", "declare", "mention", "note", "remark", "indicate");
        register("help",      "assist", "aid", "support", "facilitate", "enable");
        register("find",      "discover", "detect", "locate", "identify", "uncover");
        register("change",    "modify", "alter", "adjust", "transform", "revise", "update", "amend");
        register("improve",   "enhance", "boost", "strengthen", "upgrade", "refine", "advance");
        register("important", "significant", "crucial", "critical", "essential", "key", "vital", "major");
        register("problem",   "issue", "challenge", "difficulty", "obstacle", "concern");
        register("result",    "outcome", "consequence", "effect", "impact", "output");
        register("method",    "approach", "technique", "strategy", "procedure", "process", "way");
        register("study",     "research", "investigation", "analysis", "examination", "inquiry");
        register("increase",  "rise", "grow", "expand", "escalate", "surge", "climb", "gain");
        register("decrease",  "reduce", "decline", "drop", "fall", "diminish", "shrink", "lower");
        register("begin",     "start", "commence", "initiate", "launch", "introduce");
        register("end",       "finish", "conclude", "complete", "terminate", "stop", "cease");
        register("need",      "require", "demand", "necessitate", "call for");
        register("allow",     "permit", "enable", "authorize", "let");
        register("think",     "believe", "consider", "feel", "view", "regard", "perceive");
        register("show",      "demonstrate", "illustrate", "prove", "confirm", "indicate");
        register("data",      "information", "dataset", "facts", "evidence", "records");
        register("computer",  "machine", "system", "device", "processor");
        register("error",     "mistake", "fault", "flaw", "defect", "bug", "inaccuracy");
        register("fast",      "efficient", "optimized", "performant", "quick");
        register("text",      "content", "passage", "document", "material", "writing");
        register("similar",   "alike", "comparable", "equivalent", "related", "resembling");
        register("different", "distinct", "unlike", "dissimilar", "divergent", "varying");
        register("main",      "primary", "principal", "key", "central", "chief", "core");
        register("many",      "numerous", "multiple", "various", "several", "countless");
        register("new",       "novel", "recent", "latest", "modern", "current", "fresh");
        register("old",       "ancient", "traditional", "classic", "established", "historic");
    }

    private static void register(String canonical, String... synonyms) {
        Set<String> group = SYNONYM_GROUPS.computeIfAbsent(canonical, k -> new HashSet<>());
        group.add(canonical);
        group.addAll(Arrays.asList(synonyms));
        WORD_TO_CANONICAL.put(canonical, canonical);
        for (String syn : synonyms) {
            WORD_TO_CANONICAL.putIfAbsent(syn, canonical);
        }
    }

    /**
     * Check if two words are synonyms of each other.
     */
    public boolean areSynonyms(String a, String b) {
        a = a.toLowerCase(); b = b.toLowerCase();
        if (a.equals(b)) return true;
        String canonA = WORD_TO_CANONICAL.get(a);
        String canonB = WORD_TO_CANONICAL.get(b);
        return canonA != null && canonA.equals(canonB);
    }

    /**
     * Normalize a token to its canonical form (if known).
     */
    public String canonicalize(String word) {
        return WORD_TO_CANONICAL.getOrDefault(word.toLowerCase(), word.toLowerCase());
    }

    /**
     * Normalize an entire token list — replace synonyms with canonical forms.
     * This makes "fast algorithm" and "quick algorithm" compare as identical.
     */
    public List<String> normalizeWithSynonyms(List<String> tokens) {
        List<String> result = new ArrayList<>(tokens.size());
        for (String t : tokens) {
            result.add(canonicalize(t));
        }
        return result;
    }

    /**
     * Get all synonyms for a word (including itself).
     */
    public Set<String> getSynonyms(String word) {
        String canon = WORD_TO_CANONICAL.get(word.toLowerCase());
        if (canon == null) return Collections.singleton(word.toLowerCase());
        return Collections.unmodifiableSet(SYNONYM_GROUPS.getOrDefault(canon, Collections.singleton(word)));
    }

    /**
     * Compute synonym-aware similarity between two token lists.
     * Like Jaccard, but words that are synonyms count as a match.
     */
    public double synonymAwareSimilarity(List<String> tokensA, List<String> tokensB) {
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0;

        List<String> normA = normalizeWithSynonyms(tokensA);
        List<String> normB = normalizeWithSynonyms(tokensB);

        Set<String> setA = new HashSet<>(normA);
        Set<String> setB = new HashSet<>(normB);

        long intersection = setA.stream().filter(setB::contains).count();
        long union = setA.size() + setB.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }
}
