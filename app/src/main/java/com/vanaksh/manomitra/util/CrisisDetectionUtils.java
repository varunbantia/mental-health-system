package com.vanaksh.manomitra.util;

import java.util.Arrays;
import java.util.List;

public class CrisisDetectionUtils {
    
    // Simple list of crisis-related keywords
    private static final List<String> CRISIS_KEYWORDS = Arrays.asList(
            "suicide", "kill myself", "end it all", "self harm", "cutting", 
            "jump off", "overdose", "die", "hopeleess", "no reason to live"
    );

    /**
     * Detects if the given text contains any crisis-related keywords.
     * @param text The input text to check.
     * @return true if keywords are detected, false otherwise.
     */
    public static boolean detectCrisis(String text) {
        if (text == null || text.isEmpty()) return false;
        
        String lowerText = text.toLowerCase();
        for (String keyword : CRISIS_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
