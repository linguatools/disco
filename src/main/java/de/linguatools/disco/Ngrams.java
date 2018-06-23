/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.linguatools.disco;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracting character n-grams from words. 
 * @author peterkolb
 */
public class Ngrams {
    
    /**
     * Pads the <code>word</code> with boundary markers "&lt;" and "&gt;" and then
     * extracts all character n-grams for <code>n=[minN..maxN]</code>.
     * @param word
     * @param minN 0..n
     * @param maxN 1..n
     * @return all character n-grams from the word and the boundary markers for 
     * all specified n-gram sizes. 
     */
    public static List<String> extractAllNGramsFromWord(String word, int minN, 
            int maxN){
        
        // pad word with boundary markers
        word = "<" + word + ">";
        
        List<String> allNGrams = new ArrayList<>();
        
        for(int n = minN; n <= maxN; n++){
            addNGramsSizeN(word, n, allNGrams);
        }
        
        return allNGrams;
    }
    
    /**
     * Extracts all <code>n</code>-grams from the <code>word</code> and adds them
     * to <code>allNGrams</code>. 
     * @param word
     * @param n
     * @param allNGrams 
     */
    public static void addNGramsSizeN(String word, int n, List<String> allNGrams){
        
        if( n < 1 || word.length() < 1 ){
            return;
        }

        for(int i = 0; i < word.length()-(n-1); i++ ){
            String ngram = word.substring(i, i+n);
            allNGrams.add(ngram);
        }
    }
}
