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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.apache.lucene.index.CorruptIndexException;

/**
 * DISCO (Extracting DIStributionally Similar Words Using CO-occurrences) 
 * provides a number of methods for computing the distributional (i.e. semantic)
 * similarity between arbitrary words and text passages, for retrieving a word's
 * collocations or its corpus frequency. It also provides a method to retrieve
 * the semantically most similar words for a given word.<br>
 * It is important to keep in mind that there are two different types of word
 * spaces:
 * <ul>
 * <li><code>DISCO.WordspaceType.COL</code>: this type stores a 
 * <i>word vector</i> for each word. A word vector is the list of the 
 * significant co-occurrences of the word together with the type of 
 * co-occurrence (if any) and a significance value. The significant co-occurring
 * words of a word are also called its <i>collocations</i>. The type of 
 * co-occurrence can be a relative position in a context window, or a syntactic
 * relation</li>
 * <li><code>DISCO.WordspaceType.SIM</code>: this type stores the 
 * above word vectors, but also contains pre-computed lists of the most similar
 * words for each word. These words can be queried using the method 
 * <code>DISCO.similarWords()</code>. There are several methods in 
 * the DISCO API that only work with word spaces of type 
 * <code>DISCO.WordspaceType.SIM</code>.</li>
 * </ul>
 * DISCO supports two methods for storing word spaces, implemented by the two 
 * subclasses of the abstract <code>DISCO</code> class:
 * <ul>
 * <li><code>DISCOLuceneIndex</code>: this class uses 
 * <a href="https://lucene.apache.org">Lucene</a> to store a word space. It is 
 * intended for very high-dimensional word spaces (like the classic distributional
 * count vectors that work without any dimension reduction techniques) because it
 * stores them as a sparse matrix.</li>
 * <li><code>DenseMatrix</code>: this class stores a word space as a two-dimensional
 * array. It stores the full matrix and is therefore only feasible for predict
 * vectors (word embeddings) like the ones that are produced by word2vec.</li>
 * </ul>
 * Word spaces for several languages are available on the DISCO
 * <a href="http://www.linguatools.de/disco/disco-download_en.html">download
 * page</a>. You can also import word embeddings created with word2vec or 
 * <a href="https://github.com/facebookresearch/fastText">fastText</a> using 
 * <a href="http://www.linguatools.de/disco/disco-builder.html">DISCOBuilder</a>'s
 * <a href="http://www.linguatools.de/disco/disco-builder.html#import">import 
 * functionality</a>.<br>
 * <br>
 * DISCO is described in the following conference papers:
 * <ul>
 * <li>Peter Kolb: <a href="http://hdl.handle.net/10062/9731">Experiments on the
 * difference between semantic similarity and relatedness</a>. In <i>Proceedings
 * of the 17th Nordic Conference on Computational Linguistics - NODALIDA '09</i>,
 * Odense, Denmark, May 2009.</li>
 * <li>Peter Kolb: <a href="http://www.ling.uni-potsdam.de/~kolb/KONVENS2008-Kolb.pdf">
 * DISCOLuceneIndex: A Multilingual Database of Distributionally Similar Words</a>.
 * In <i>Tagungsband der 9. KONVENS</i>, Berlin, 2008.</li>
 * </ul>
 * @author peterkolb
 * @version 3.0
 */
public abstract class DISCO {
    
    /**
     * Available word space types (SIM = word space contains lists of
     * pre-computed similar words for each word, COL = word space contains only
     * word vectors).
     */
    public enum WordspaceType {
        /**
         * Word spaces of this type only store word vectors.
         */
        COL,
        /**
         * Word spaces of this type store word vectors and a pre-computed list
         * of the most similar words for each word. The similarity measure that 
         * was used in generating the pre-computed list of similar words is given
         * in the file <code>disco.config</code> in the word space directory. If it
         * is not given, the default value <code>SimilarityMeasure.KOLB</code>
         * was used.
         */
        SIM
    }
    /**
     * Available measures for vector comparison. 
     */
    public enum SimilarityMeasure { 
        /**
         * The well-known cosine vector similarity measure. This measure should
         * always be used with word spaces imported from word2vec.
         */
        COSINE, 
        /**
         * The vector similarity measure described in the paper <a 
         * href="http://hdl.handle.net/10062/9731">Experiments on the difference
         * between semantic similarity and relatedness</a>. Note that this measure
         * does not give usable results with word spaces imported from word2vec.
         */
        KOLB 
    }
    /**
     * This string is used as separator between a feature word and its relation.
     * It is a character from the Unicode private use area.
     */
    public final static String RELATION_SEPARATOR = "\uF8FF";
    
    /**
     * Get <code>SimilarityMeasure</code> object from its String name.
     * @param simMeasure
     * @return SimilarityMeasure or null.
     */
    public static SimilarityMeasure getSimilarityMeasure(String simMeasure){
        
        if( simMeasure.equalsIgnoreCase("cosine") ){
            return SimilarityMeasure.COSINE;
        }else if( simMeasure.equalsIgnoreCase("Kolb") ){
            return SimilarityMeasure.KOLB;
        }else return null;
    }
    
    /**
     * Get VectorSimilarity class for a SimilarityMeasure.
     * @param simMeasure
     * @return 
     */
    public static VectorSimilarity getVectorSimilarity(SimilarityMeasure simMeasure){
        if( null == simMeasure ){
            return null;
        }
        switch (simMeasure) {
            case COSINE:
                return new CosineVectorSimilarity();
            case KOLB:
                return new KolbVectorSimilarity();
            default:
                return null;
        }
    }
    
    /**
     * Get type of this word space.
     * @return 
     */
    public abstract WordspaceType getWordspaceType();
    
    /**
     * Get number of words stored in this word space. In case of 
     * <code>DenseMatrix</code> this returns the value of <code>config.vocabularySize</code>,
     * which has to be equal to the number of rows in the similarity matrix.
     * For <code>DISCOLuceneIndex</code> this returns the number of documents in
     * the Lucene index.
     * @return 
     */
    public abstract int numberOfWords();
    
    /**
     * For <code>DISCOLuceneIndex</code> this returns the number of words that 
     * were used as features. Note that this is only equal to the dimensionality
     * of the word vectors if no positional or relational features were used.
     * See <a href="http://www.linguatools.de/disco/disco-builder.html#options">
     * options in disco.config</a> under <code>numberFeatureWords</code> for more
     * information.<br>
     * For <code>DenseMatrix</code> this is equal to the dimensionality of the 
     * word embedding (vector length).
     * @return 
     */
    public abstract int numberOfFeatureWords();
    
    /**
     * Get the number of similar words that are stored in word spaces of type SIM
     * for each word. 
     * @return number of similar words that are stored in the word space. For 
     * word spaces of type COL this value is always 0.  
     */
    public abstract int numberOfSimilarWords();
    
    
    /**
     * Get corpus frequency of <code>word</code>.
     * @param word
     * @return
     * @throws IOException 
     */
    public abstract int frequency(String word) throws IOException;
    
    /**
     * Returns the list of the most similar words for <code>word</code> (according
     * to <code>DISCO.semanticSimilarity</code>). Since the list of most similar
     * words for each word is only stored in word spaces of type
     * <code>WordspaceType.SIM</code> this does not work with word spaces of
     * type <code>WordspaceType.COL</code>.
     * @param word
     * @return
     * @throws IOException
     * @throws WrongWordspaceTypeException if called with a word space of type
     * <code>WordspaceType.COL</code>
     */
    public abstract ReturnDataBN similarWords(String word) throws IOException, 
            WrongWordspaceTypeException;
    
    /**
     * Computes the similarity between words <code>w1</code> and <code>w2</code>
     * by comparing their word vectors using the <code>vectorSimilarity</code>
     * measure of choice.
     * @param w1
     * @param w2
     * @param vectorSimilarity
     * @return
     * @throws IOException 
     */
    public abstract float semanticSimilarity(String w1, String w2, VectorSimilarity 
            vectorSimilarity) throws IOException;
    
    /**
     * Computes the similarity between words <code>w1</code> and <code>w2</code>
     * by comparing the set of the most similar words for <code>w1</code> with
     * the set of the most similar words for <code>w2</code>.<br>
     * The size of the set of similar words stored for each word in the word space
     * is given by the DISCOBuilder parameter <code>-nBest</code>. Default size
     * is 300.<br>
     * Only works with word spaces of type <code>WordspaceType.SIM</code>!
     * @param w1
     * @param w2
     * @param vectorSimilarity
     * @return
     * @throws IOException
     * @throws WrongWordspaceTypeException if called with a word space of type
     * <code>WordspaceType.COL</code>
     */
    public abstract float secondOrderSimilarity(String w1, String w2, VectorSimilarity 
            vectorSimilarity)
            throws IOException, WrongWordspaceTypeException;
    
    /**
     * Get word vector for <code>word</code> as map <code>feature - value</code>.
     * Features are either words or IDs.
     * @param word
     * @return map vector
     * @throws IOException 
     */
    public abstract Map<String,Float> getWordvector(String word)
            throws IOException;
    
    /**
     * The second order word vector contains the <code>nBest</code> most similar
     * words for <code>word</code> as features (instead of the directly 
     * co-occuring words that you get with <code>getWordvector</code>).
     * @param word
     * @return
     * @throws IOException 
     * @throws de.linguatools.disco.WrongWordspaceTypeException when used with
     * word space that is not of type SIM. 
     */
    public abstract Map<String,Float> getSecondOrderWordvector(String word)
            throws IOException, WrongWordspaceTypeException;
    
    /**
     * Returns the collocations for the input word together with their 
     * significance values, ordered by significance value (highest significance
     * first). If the search word is not found in the index, the return value is 
     * <code>null</code>.<br>
     * Unlike the method <code>getWordvector()</code> this method summarizes the
     * words over the different relations.<br>
     * <b>Important note</b>: if used with a DenseMatrix or a DISCOLuceneIndex
     * that has IDs as features, the return values will be IDs and not words.
     * @param word
     * @return 
     * @throws IOException 
     */
    public abstract ReturnDataCol[] collocations(String word) throws IOException;
    
    /**
     * Writes word-frequency list to file. Iterates over all words in the word
     * space and outputs them together with their corpus frequency. 
     * @param outputFileName
     * @return 
     */
    public abstract int wordFrequencyList(String outputFileName);
    
    /**
     * Returns an iterator that iterates over all words in the word space (the
     * vocabulary). There is no special ordering of the words. The method 
     * <code>remove</code> is not supported. 
     * @return 
     * @throws java.io.IOException 
     */
    public abstract Iterator<String> getVocabularyIterator() throws IOException;
    
    /**
     * Returns the id-th word in the vocabulary.
     * @param id id has to be between 0 and <code>DISCO.numberOfWords() - 1</code>.
     * @return word with given id or <code>null</code> if id is not in the range 
     * 0..<code>DISCO.numberOfWords() - 1</code>.
     * @throws IOException 
     */
    public abstract String getWord(int id) throws IOException;
    
    // informations from disco.config:
    
    /**
     * Gets list of stopwords from the <code>disco.config</code> file in the 
     * word space.
     * @return stop words that were used in word space creation. 
     */
    public abstract String[] getStopwords();
    
    public abstract Map<String,Byte> getStopwordsHash();
    
    /**
     * Size of the underlying corpus.
     * @return 
     */
    public abstract long getTokenCount();
    /**
     * Get minimum frequency of tokens in corpus. 
     * @return 
     */
    public abstract int getMinFreq();
    /**
     * Get corpus frequency of the most frequent word in the word space (that 
     * was not filtered out by the stop word list that was used).
     * @return 
     */
    public abstract int getMaxFreq();
    
    /**
     * Load DISCOLuceneIndex or DenseMatrix from file into memory.
     * @param discoFile can be either Lucene index directory or serialized DenseMatrix
     * file.
     * @return 
     * @throws org.apache.lucene.index.CorruptIndexException 
     * @throws java.io.FileNotFoundException 
     * @throws de.linguatools.disco.CorruptConfigFileException 
     */
    public static DISCO load(String discoFile) throws CorruptIndexException, 
            IOException, FileNotFoundException, CorruptConfigFileException{
        
        DISCO disco;
        File f = new File(discoFile);
        if( f.isDirectory() ){
            disco = new DISCOLuceneIndex(discoFile, true);
        }else{
            disco = DenseMatrix.deserialize(f);
        }
        return disco;
    }
    
    /**
     * If <code>discoFile</code> is a <code>DISCOLuceneIndex</code> the index is
     * opened for reading but not loaded into memory.<br>
     * If <code>discoFile</code> is a <code>DenseMatrix</code> it is loaded into
     * memory. In this case <code>open</code> behaves exactly as <code>load</code>.
     * @param discoFile can be either Lucene index directory or serialized DenseMatrix
     * file.
     * @return 
     * @throws org.apache.lucene.index.CorruptIndexException 
     * @throws java.io.FileNotFoundException 
     * @throws de.linguatools.disco.CorruptConfigFileException 
     */
    public static DISCO open(String discoFile) throws CorruptIndexException, 
            IOException, FileNotFoundException, CorruptConfigFileException{
        
        DISCO disco;
        File f = new File(discoFile);
        if( f.isDirectory() ){
            disco = new DISCOLuceneIndex(discoFile, false);
        }else{
            disco = DenseMatrix.deserialize(f);
        }
        return disco;
    }
}
