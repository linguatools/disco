/*******************************************************************************
 *   Copyright (C) 2007-2018 Peter Kolb
 *   peter.kolb@linguatools.org
 *
 *   Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *   use this file except in compliance with the License. You may obtain a copy
 *   of the License at 
 *   
 *        http://www.apache.org/licenses/LICENSE-2.0 
 *
 *   Unless required by applicable law or agreed to in writing, software 
 *   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 *   License for the specific language governing permissions and limitations
 *   under the License.
 *
 ******************************************************************************/
package de.linguatools.disco;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracting character n-grams from words. 
 * @author peterkolb
 * @version 3.0
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
