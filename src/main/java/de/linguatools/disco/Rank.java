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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * This class contains methods that are based on the rank a word occupies in the
 * similarity list of other words.
 * @author peter
 * @version 2.0
 */
public class Rank {
   
    /***************************************************************************
     * Computes the rank of w2 in the similarity list of w1. If w2 does not
     * occur in the similarity list of w1, the return value is 0.<br>
     * If w1 is not in the index, -1 is returned.<br>
     * <b>Important note</b>: This method does not work with word spaces of type
     * <code>DISCO.WordspaceType.COL</code> (use 
     * <code>de.linguatools.disco.Rank.rankCol</code> instead).
     * @param disco
     * @param w1 input word #1 (must be a single token).
     * @param w2 input word #2 (must be a single token).
     * @return rank (1...n) or 0 if w2 does not occur in similarity list of w1, 
     * or -1 if w1 not found.
     * @throws IOException
     * @throws WrongWordspaceTypeException if the <code>disco</code> word space
     * is not of type <code>DISCO.WordspaceType.SIM</code>.
     */
    public int rankSim(DISCO disco, String w1, String w2) throws IOException, 
            WrongWordspaceTypeException {
        
        // check word space type
        if( disco.getWordspaceType() != DISCO.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + "to word spaces of type "+disco.getWordspaceType());
        }
        
        ReturnDataBN res = disco.similarWords(w1);
        if (res == null) {
            return -1;
        }

        for (int i = 0; i < res.words.length; i++) {
            if (res.words[i].equals(w2)) {
                return i + 1;
            }
        }
        return 0;
    }

    /***************************************************************************
     * Computes the rank of w2 among the collocations of w1. If w2 does not
     * occur as collocation of w1, the return value is 0. If w1 is not found in
     * the index, the return value is -1.<br>
     * See <a href="DISCO.html#collocations(java.lang.String)">DISCOLuceneIndex.collocations</a>
     * for more information.<br>
     * <b>Important note:</b> this method can only be used with class <code>DISCOLuceneIndex</code>
     * but not with <code>DenseMatrix</code>!
     * @param disco 
     * @param w1 input word #1 (must be a single token).
     * @param w2 input word #2 (must be a single token).
     * @return rank (1...n) or 0 if w2 not a collocation of w1, or -1 if w1 not
     * in index.
     * @throws IOException
     */
    public int rankCol(DISCOLuceneIndex disco, String w1, String w2) throws IOException {
        
        ReturnDataCol[] res = disco.collocations(w1);
        if( res == null) return -1;
        
        for (int i = 0; i < res.length; i++) {
            if (res[i].word.equals(w2)) {
                return i + 1;
            }
        }
        // if w2 is not among the collocates of w1, return 0
        return 0;
    }

    /***************************************************************************
     * Finds the words in the index in whose similarity or collocation lists the
     * <code>words</code> rank highest. There must be at least one word in
     * <code>words</code>.<br>
     * For each word <code>v</code> from the word space vocabulary, the method
     * looks up on which rank the input words occur among the similar words of
     * <code>v</code>. The score of <code>v</code> is the product of these
     * ranks. The lower the rank product, the more similar <code>v</code> is to
     * <code>words</code>.<br>
     * For low frequency words this method often gives better results than
     * de.linguatools.disco.similarWords().<br>
     * <b>Warning</b>: This operation is very time consuming! It should only be
     * used if the word space has been loaded into main memory.<br>
     * <b>Important note</b>: This method does not work with word spaces of type
     * <code>DISCO.WordspaceType.COL</code> (use 
     * <code>de.linguatools.disco.Rank.highestRankingCol</code> instead).
     * @param disco DISCO word space (in-memory)
     * @param words set of input words (all words must be single tokens).
     * @return ArrayList with similar words, most similar first (i.e. lowest rank
     * product first). If none of the <code>words</code> was found in the index,
     * the return value is null.
     * @throws IOException
     * @throws WrongWordspaceTypeException
     */
    public ArrayList<WordAndRank> highestRankingSim(DISCO disco, Set<String> words)
            throws IOException, WrongWordspaceTypeException {

        // check word space type
        if( disco.getWordspaceType() != DISCO.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + "to word spaces of type "+disco.getWordspaceType());
        }
        
        // store words from the Set in Hash
        HashMap wordsHash = new HashMap();
        for(Iterator it = words.iterator(); it.hasNext(); ){
            wordsHash.put(it.next(), 1);
        }

        ArrayList<WordAndRank> result = new ArrayList<>();
        
        // run trough list of all words v in index
        Iterator<String> iterator = disco.getVocabularyIterator();
        while ( iterator.hasNext() ) {
            String v = iterator.next();
            // leave out words from the input set
            if ( wordsHash.containsKey(v) ) {
                continue;
            }
            // look up rank of input words in the list of the most similar words
            // of v
            long rankProdukt = 1;
            long r;
            for (String inputWord : words) {
                r = rankSim(disco, v, inputWord);
                if( r >= 1 ){
                    rankProdukt = rankProdukt * r;
                }
            }
            if( rankProdukt > 1 ){
                result.add(new WordAndRank(v, rankProdukt));
            }
        }
        
        // sort result Vector by rank (lowest first)
        Collections.sort(result);
        // return sorted Vector
        return result;
    }

    /***************************************************************************
     * Finds the words in the index in whose collocation lists the
     * <code>words</code> rank highest. There must be at least one word in
     * <code>words</code>.<br>
     * For each word <code>v</code> from the word space vocabulary, the method
     * looks up on which rank the input words occur among the collocations of
     * <code>v</code>. The score of <code>v</code> is the product of these
     * ranks. The lower the rank product, the more similar <code>v</code> is to
     * <code>words</code>.<br>
     * For low frequency words this method often gives better results than
     * de.linguatools.disco.similarWords().<br>
     * <b>Warning</b>: This operation is very time consuming! It should only be
     * used if the word space has been loaded into main memory.<br>
     * <b>Important note</b>: This method only works with class 
     * <code>DISCOLuceneIndex</code> but not with <code>DenseMatrix</code>.
     * @param disco DISCO word space (in-memory)
     * @param words set of input words (all words must be single tokens).
     * @return ArrayList with similar words, most similar first (i.e. lowest rank
     * product first). If none of the <code>words</code> was found in the index,
     * the return value is null.
     * @throws IOException
     * @throws WrongWordspaceTypeException
     */
    public ArrayList<WordAndRank> highestRankingCol(DISCOLuceneIndex disco, 
            Set<String> words)
            throws IOException, WrongWordspaceTypeException {

        // store words from the Set in Hash
        HashMap wordsHash = new HashMap();
        for(Iterator it = words.iterator(); it.hasNext(); ){
            wordsHash.put(it.next(), 1);
        }

        ArrayList<WordAndRank> result = new ArrayList<>();
        
        // run trough list of all words v in index
        Iterator<String> iterator = disco.getVocabularyIterator();
        while ( iterator.hasNext() ) {
            String v = iterator.next();
            // leave out words from the input set
            if ( wordsHash.containsKey(v) ) {
                continue;
            }
            // look up rank of input words in the list of the most similar words
            // of v
            long rankProdukt = 1;
            long r;
            for (String inputWord : words) {
                r = rankCol(disco, v, inputWord);
                if( r >= 1 ){
                    rankProdukt = rankProdukt * r;
                }
            }
            if( rankProdukt > 1 ){
                result.add(new WordAndRank(v, rankProdukt));
            }
        }
        
        // sort result Vector by rank (lowest first)
        Collections.sort(result);
        // return sorted Vector
        return result;
    }
    
    
    /***************************************************************************
     * data structure.
     */
    public class WordAndRank implements Comparable<WordAndRank>{

        String word;
        long rank;

        // Konstruktor
        public WordAndRank(String word, long rank) {
            this.word = word;
            this.rank = rank;
        }
        
        @Override
        public int compareTo(WordAndRank other) {
            int retval = 0;
            if (rank < other.rank) {
                retval = -1;
            }
            if (rank > other.rank) {
                retval = 1;
            }
            return (retval);
        }
        
    }

}
