package com.mistasoup.chatprotect.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageUtils {
    
    // Character substitution map for leetspeak and similar variations
    private static final Map<Character, Character> CHAR_SUBSTITUTIONS = new HashMap<>();
    
    static {
        // Numbers to letters
        CHAR_SUBSTITUTIONS.put('0', 'o');
        CHAR_SUBSTITUTIONS.put('1', 'i');
        CHAR_SUBSTITUTIONS.put('3', 'e');
        CHAR_SUBSTITUTIONS.put('4', 'a');
        CHAR_SUBSTITUTIONS.put('5', 's');
        CHAR_SUBSTITUTIONS.put('6', 'g');
        CHAR_SUBSTITUTIONS.put('7', 't');
        CHAR_SUBSTITUTIONS.put('8', 'b');
        CHAR_SUBSTITUTIONS.put('9', 'g');
        
        // Special characters
        CHAR_SUBSTITUTIONS.put('@', 'a');
        CHAR_SUBSTITUTIONS.put('$', 's');
        CHAR_SUBSTITUTIONS.put('!', 'i');
        CHAR_SUBSTITUTIONS.put('+', 't');
        CHAR_SUBSTITUTIONS.put('*', 'x');
    }
    
    /**
     * Normalizes a message by removing special characters, converting to lowercase,
     * and replacing common substitutions
     */
    public static String normalizeMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        
        StringBuilder normalized = new StringBuilder();
        String lower = message.toLowerCase();
        
        for (char c : lower.toCharArray()) {
            // Replace known substitutions
            if (CHAR_SUBSTITUTIONS.containsKey(c)) {
                normalized.append(CHAR_SUBSTITUTIONS.get(c));
            } else if (Character.isLetterOrDigit(c)) {
                normalized.append(c);
            }
            // Skip all other characters (spaces, punctuation, etc.)
        }
        
        return normalized.toString();
    }
    
    /**
     * Calculates the Levenshtein distance between two strings
     */
    public static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Calculates similarity percentage between two strings (0-100)
     */
    public static double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null || s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }
        
        if (s1.equals(s2)) {
            return 100.0;
        }
        
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 100.0;
        }
        
        int distance = levenshteinDistance(s1, s2);
        return ((maxLength - distance) / (double) maxLength) * 100.0;
    }
    
    /**
     * Checks if a message is similar to any blocked word
     */
    public static boolean containsBlockedWord(String message, List<String> blockedWords) {
        // Allow very short messages to pass without blocking
        if (message.trim().length() <= 2) {
            return false;
        }
        
        String normalized = normalizeMessage(message);
        
        for (String blockedWord : blockedWords) {
            String normalizedBlocked = normalizeMessage(blockedWord);
            
            // Check if the normalized message contains the blocked word
            if (normalized.contains(normalizedBlocked)) {
                return true;
            }
            
            // Check for high similarity (catches variations)
            if (calculateSimilarity(normalized, normalizedBlocked) >= 80) {
                return true;
            }
            
            // Check if any word in the message is similar to the blocked word
            String[] words = message.toLowerCase().split("\\s+");
            for (String word : words) {
                // Skip very short words to avoid false matches
                if (word.trim().length() <= 2) {
                    continue;
                }
                
                String normalizedWord = normalizeMessage(word);
                if (normalizedWord.contains(normalizedBlocked) || 
                    normalizedBlocked.contains(normalizedWord)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}