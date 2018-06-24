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

import it.unimi.dsi.sux4j.mph.GOVMinimalPerfectHashFunction;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;

/**
 * This stores a word space in a dense matrix. Use for low-dimensional word 
 * embeddings only.
 * @author peter
 * @version 3.0
 */
public class DenseMatrix extends DISCO implements Serializable {
    
    /**
     * word space: stores word vectors (embeddings) for all words.
     */
    private final float[][] matrix;
    /**
     * subword ngram vectors (optional).
     */
    private final float[][] ngramMatrix;
    /**
     * stores most similar words for each word (optional, only for word spaces
     * of type SIM).
     */
    private int[][] simMatrix;
    /**
     * stores the similarity values for the simMatrix.
     */
    private float[][] simValues;
    // words
    // ACHTUNG: containsKey() funktioniert nicht! Stattdessen -1 für not contained!
    GOVMinimalPerfectHashFunction<CharSequence> word2indexMap; // word --> index in int[]
    private final int[] wordIndex2id; // word index --> ID  (=row number in matrix)
    private final int[] wordId2offset; // word ID --> offset in offset2word
    private final int[] frequencies; // word ID --> word frequency in corpus
    private final byte[] offset2word;
    // n-grams
    GOVMinimalPerfectHashFunction<CharSequence> ngram2indexMap; // ngram --> index
    private final int[] ngramIndex2id; // ngram index --> ID (=row number in ngramMatrix) 
    private final int[] ngramId2offset; // ngram ID --> offset in offset2ngram
    private final byte[] offset2ngram;
    private final int minN; // minimum ngram size (characters)
    private final int maxN;
    // disco.config
    private final ConfigFile config;
    
    // a DenseMatrix is of type COL if it only stores word vectors. In this case
    // simMatrix and simValues both are null.
    // If a DenseMatrix is of type SIM, then the numberOfSimilarWords most 
    // similar words for each word are stored in simMatrix, and the corresponding
    // similarity values in simValues.
    private DISCO.WordspaceType wordspaceType;
    private int numberOfSimilarWords; // = 0 if wordspaceType == COL
    
    public static final Charset UTF8 = Charset.forName("UTF-8");
    private static final long serialVersionUID = 20170125L;
    
    /**
     * Constructor to be used by <code>DenseMatrixFactory</code> only. To create
     * a <code>DenseMatrix</code> from a Lucene index word space use 
     * <code>DenseMatrixFactory.create</code> (or the command line interface). To
     * load a serialized <code>DenseMatrix</code> use <code>DenseMatrixFactory.load</code>.
     * @param matrix
     * @param ngramMatrix
     * @param simMatrix
     * @param simValues
     * @param word2indexMap
     * @param wordIndex2id
     * @param wordId2offset
     * @param frequencies
     * @param ngram2indexMap
     * @param ngramIndex2id
     * @param offset2word
     * @param ngramId2offset
     * @param offset2ngram
     * @param minN minimum ngram size or -1
     * @param maxN maximum ngram size or -1
     * @param config
     * @param wordspaceType
     * @param numberOfSimilarWords 
     */
    public DenseMatrix(float[][] matrix, float[][] ngramMatrix, int[][] simMatrix,
            float[][] simValues, 
            GOVMinimalPerfectHashFunction<CharSequence> word2indexMap, int[] wordIndex2id, 
            int[] wordId2offset, int[] frequencies, byte[] offset2word, 
            GOVMinimalPerfectHashFunction<CharSequence> ngram2indexMap,
            int[] ngramIndex2id, int[] ngramId2offset, byte[] offset2ngram, int minN,
            int maxN,
            ConfigFile config, DISCO.WordspaceType wordspaceType, 
            int numberOfSimilarWords){
        
        this.matrix = matrix;
        this.ngramMatrix = ngramMatrix;
        this.simMatrix = simMatrix;
        this.simValues = simValues;
        this.word2indexMap = word2indexMap;
        this.wordIndex2id = wordIndex2id;
        this.wordId2offset = wordId2offset;
        this.frequencies = frequencies;
        this.offset2word = offset2word;
        this.ngram2indexMap = ngram2indexMap;
        this.ngramIndex2id = ngramIndex2id;
        this.ngramId2offset = ngramId2offset;
        this.offset2ngram = offset2ngram;
        this.minN = minN;
        this.maxN = maxN;
        this.config = config;
        this.wordspaceType = wordspaceType;
        this.numberOfSimilarWords = numberOfSimilarWords;
    }
    
    /**
     * Returns the type of the word space instance.
     * @return word space type
     */
    @Override
    public WordspaceType getWordspaceType(){
        
        return wordspaceType;
    }
    
    /**
     * 
     * @return vocabulary size. 
     */
    @Override
    public int numberOfWords(){
        
        return config.vocabularySize;
    }
    
    /**
     * 
     * @return dimensionality of the word space. 
     */
    @Override
    public int numberOfFeatureWords(){
        
        return config.numberFeatureWords;
    }
    
    @Override
    public int numberOfSimilarWords(){
        return numberOfSimilarWords;
    }
    
    /**
     * 
     * @param word
     * @return frequency of <code>word</code> in corpus. 0 if word not found.
     * @throws IOException 
     */
    @Override
    public int frequency(String word) throws IOException{
        int i = getMatrixRowNumber(word);
        if( i != -1 ){
            return frequencies[i];
        }else{
            return 0;
        }
    }
    
    /**
     * Only works with word spaces of type <code>DISCO.WordspaceType.SIM</code>.
     * @param word
     * @return list of similar words for <code>word</code> if these are stored
     * in the word space.
     * @throws IOException
     * @throws WrongWordspaceTypeException 
     */
    @Override
    public ReturnDataBN similarWords(String word) throws IOException, 
            WrongWordspaceTypeException{
        
        // check word space type
        if( wordspaceType != DISCO.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + " to word spaces of type "+wordspaceType);
        }
        
        int id = getMatrixRowNumber(word);
        if( id == -1 ){
            return null;
        }
        
        List<String> dsb = new LinkedList<>();
        List<Float> dsbSim = new LinkedList<>();
        for(int i = 0; i < numberOfSimilarWords; i++ ){
            if( simValues[id][i] == 0 ){
                break;
            }
            dsb.add( id2word(simMatrix[id][i]) );
            dsbSim.add( simValues[id][i] );
        }
        
        // create return object
        ReturnDataBN res = new ReturnDataBN();
        res.words = new String[ dsb.size() ];
        res.values = new float[ dsbSim.size() ];
        for( int i = 0; i < dsb.size(); i++ ){
            res.words[i] = dsb.get(i);
            res.values[i] = dsbSim.get(i);
        }
        return res;
    }
    
    @Override
    public float semanticSimilarity(String w1, String w2, VectorSimilarity 
            vectorSimilarity) throws IOException{
        
        int id1 = getMatrixRowNumber(w1);
        if( id1 == -1 ){
            return -2;
        }
        int id2 = getMatrixRowNumber(w2);
        if( id2 == -1 ){
            return -2;
        }
        
        return (float)vectorSimilarity.computeSimilarity(matrix[id1], matrix[id2]);
    }
    
    @Override
    public float secondOrderSimilarity(String w1, String w2, VectorSimilarity 
            vectorSimilarity)
            throws IOException, WrongWordspaceTypeException{
        
        // check word space type
        if( wordspaceType != DISCOLuceneIndex.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + "to word spaces of type "+wordspaceType);
        }
        
        // look up words
        int id1 = getMatrixRowNumber(w1);
        if( id1 == -1 ){
            return -2;
        }
        int id2 = getMatrixRowNumber(w2);
        if( id2 == -1 ){
            return -2;
        }
        
        // build mapVectors (for efficiency reasons the words are not inserted
        // as keys but their IDs are used)
        Map<String,Float> v1 = new HashMap();
        for(int i = 0; i < numberOfSimilarWords; i++ ){
            if( simValues[id1][i] > 0 ){
                v1.put(String.valueOf(simMatrix[id1][i]), simValues[id1][i]);
            }else{
                break;
            }
        }
        Map<String,Float> v2 = new HashMap();
        for(int i = 0; i < numberOfSimilarWords; i++ ){
            if( simValues[id2][i] > 0 ){
                v2.put(String.valueOf(simMatrix[id2][i]), simValues[id2][i]);
            }else{
                break;
            }
        }
        // compute similarity
        return (float)vectorSimilarity.computeSimilarity(v1, v2);
    }
    
    /**
     * Returns a word embedding converted to a sparse vector. 
     * Note that word vectors in a dense matrix only contain IDs as keys and not
     * words.<br>
     * To get a dense vector (float array) use <code>getWordEmbedding</code> instead.
     * @param word
     * @return sparse vector.
     * @throws IOException 
     */
    @Override
    public Map<String,Float> getWordvector(String word)
            throws IOException{

        int id = getMatrixRowNumber(word);
        if( id == -1 ){
            return null;
        }

        Map<String,Float> wv = new HashMap<>();
        for( int i = 0; i < config.numberFeatureWords; i++ ){
            wv.put(String.valueOf(i), matrix[id][i]);
        }
        
        return wv;
    }
    
    /**
     * The second order word vector contains words as keys, namely the most
     * similar words for <code>word</code>.
     * @param word
     * @return second order word vector or <code>null</code> if <code>word</code>
     * not found.
     * @throws WrongWordspaceTypeException 
     */
    @Override
    public Map<String,Float> getSecondOrderWordvector(String word) throws 
            WrongWordspaceTypeException{
        
        // check word space type
        if( wordspaceType != DISCOLuceneIndex.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + "to word spaces of type "+wordspaceType);
        }
        // lookup word
        int id = getMatrixRowNumber(word);
        if( id == -1 ){
            return null;
        }
        // build second order vector
        Map<String,Float> v = new HashMap();
        for(int i = 0; i < numberOfSimilarWords; i++ ){
            if( simValues[id][i] > 0 ){
                v.put(id2word(simMatrix[id][i]), simValues[id][i]);
            }else{
                break;
            }
        }
        return v;
    }
    
    public int[] getSecondOrderWordvector(int id){
        return simMatrix[id];
    }
    
    @Override
    public ReturnDataCol[] collocations(String word) throws IOException{
        
        int id = getMatrixRowNumber(word);
        if( id == -1 ){
            return null;
        }
        ReturnDataCol[] res = new ReturnDataCol[config.numberFeatureWords];
        for( int i = 0; i < config.numberFeatureWords; i++ ){
            res[i].word = String.valueOf(i); // feature = column ID
            res[i].value = matrix[id][i];
            res[i].relation = 0;
        }
        // sortiere Array ReturnDataCol[] nach hoechstem Signifikanzwert
        Arrays.sort(res);
        return res;
    }
    
    @Override
    public int wordFrequencyList(String outputFileName){
     
        try {
            // öffne Ausgabedatei
            PrintWriter fw = new PrintWriter(outputFileName, "UTF-8");
            for( int i = 0; i < frequencies.length; i++ ){
                // Wort und Frequenz in Ausgabe schreiben
                fw.println( id2word(i)+"\t"+frequencies[i]);
            }
            fw.close();
        } catch (IOException ex) {
            System.out.println(DenseMatrix.class.getName()+": "+ex);
            return -1;
        }
        return frequencies.length;
    }
    
    @Override
    public String[] getStopwords() throws FileNotFoundException, IOException,
            CorruptConfigFileException{
        
        return config.stopwords.trim().split("\\s+");
    }
    
    @Override
    public long getTokenCount(){
        
        return config.tokencount;
    }
    
    @Override
    public int getMinFreq(){
        
        return config.minFreq;
    }
    
    @Override
    public int getMaxFreq(){
        
        return config.maxFreq;
    }
    
    @Override
    public Iterator<String> getVocabularyIterator(){
        
        return new VocabularyIterator();
    }
    
    class VocabularyIterator implements Iterator<String>{
        
        private int i;
        
        public VocabularyIterator(){
            
            i = 0;
        }
        
        @Override
        public boolean hasNext(){
            
            if( i < config.vocabularySize ){
                return true;
            }else{
                return false;
            }
        }
        
        @Override
        public String next(){
            
            int buffer = i;
            i++;
            return id2word( buffer );
        }
        
        @Override
        public void remove(){
            
        }
    }
    
    @Override
    public String getWord(int id) throws IOException{
        
        if( id >= config.vocabularySize ){
            return null;
        }
        return id2word(id);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // additional methods specific to DenseMatrix
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * 
     * @param word
     * @return word ID (equal to row index in matrix) or -1 if word not found.
     * <b>WARNING</b>: if this <code>DenseMatrix</code> was created from a 
     * <code>DISCOLuceneIndex</code> then the returned ID can be larger than the
     * number of rows in the matrix!
     * For accessing a matrix row, always use <code>getMatrixRowNumber(String)</code>
     * instead.
     */
    public int getWordId(String word){
        // containsKey() funktioniert nicht bei GOVHash weil es immer true ist
        // aber -1 für false steht!
        long id = word2indexMap.getLong(word);
        if( id != -1 ){
            return wordIndex2id[ (int) id ];
        }else{
            return -1;
        }
    }
    
    /**
     * 
     * @param word 
     * @return word ID in the range <code>0..config.vocabularySize</code> or -1
     * if <code>word</code> is not found in the <code>word2indexMap</code> or its
     * ID is larger than <code>config.vocabularySize-1</code>. The ID is equal
     * to the row index in <code>matrix</code> (the matrix row is the 
     * <code>word</code>'s word vector).<br>
     * The ID that you get with this method can be safely used to retrieve word
     * vectors with <code>getWordVector(int)</code>.
     */
    public int getMatrixRowNumber(String word){
        long id = word2indexMap.getLong(word);
        if( id != -1  ){
            int row = wordIndex2id[ (int) id ];
            if( row >= matrix.length ){
                return -1;
            }else{
                return row;
            }
        }else{
            return -1;
        }
    }
    
    /**
     * 
     * @param id
     * @return dense vector (the matrix row no. <code>id</code>) or 
     * <code>null</code> if <code>id &lt; 0</code> or <code>id &gt; 
     * config.vocabularySize-1</code>.
     */
    public float[] getWordVector(int id){
        if( id < 0 || id >= matrix.length ){
            return null;
        }else{
            return matrix[id];
        }
    }
    
    /**
     * Get embedding vector for <code>word</code>. If word is not found (out of 
     * vocabulary) then <code>null</code> is returned unless this word space stores
     * subword information (n-grams). In this case a word embedding is computed
     * on the fly from <code>word</code>'s character n-grams. 
     * @param word 
     * @return embedding vector or <code>null</code>.
     */
    public float[] getWordEmbedding(String word){
        
        long id = word2indexMap.getLong(word);
        if( id != -1  ){
            int row = wordIndex2id[ (int) id ];
            if( row < matrix.length ){
                return matrix[row];
            }
        }
        // if word not found in matrix AND this is a DenseMatrix with subwords
        // then create word embedding on the fly
        if( ngram2indexMap == null ){
            return null;
        }else{
            return Subword.getEmbeddingForOov(word, this);
        }
    }
    
    /**
     * 
     * @param ngram
     * @return ngram ID (equal to row index in ngramMatrix) or -1 if ngram not
     * found (or this word space does not contain any ngrams).
     */
    private int getNgramId(String ngram){
        if( ngram2indexMap == null ){
            return -1;
        }
        if( ngram2indexMap.containsKey(ngram) ){
            return ngramIndex2id[ (int) ngram2indexMap.getLong(ngram) ];
        }else{
            return -1;
        }
    }
    
    /**
     * 
     * @param ngram
     * @return ngram vector or null if either ngram not found or this word space
     * does not contain any ngrams.
     */
    public float[] getNgramVector(String ngram){
        
        int id = getNgramId(ngram);
        if( id == -1 ){
            return null;
        }
        return ngramMatrix[id];
    }
    
    /**
     * 
     * @return minimum ngram size in characters or -1 if no ngrams are stored. 
     */
    public int getMinN(){
        return minN;
    }
    
    /**
     * 
     * @return maximum ngram size in characters or -1 if no ngrams are stored. 
     */
    public int getMaxN(){
        return maxN;
    }
    
    public ConfigFile getConfig(){
        return config;
    }
    
    public void setWordspaceType(WordspaceType type){
        this.wordspaceType = type;
    }
    
    public void setNumberOfSimilarWords(int n){
        this.numberOfSimilarWords = n;
    }
    
    public void setSimMatrix(int[][] simMatrix){
        this.simMatrix = simMatrix;
    }
    
    public void setSimValues(float[][] simValues){
        this.simValues = simValues;
    }
    
    private String id2word(int id){
        
        int offset = wordId2offset[ id ];
        
        int wordBytesSize = (int) offset2word[offset] & 0xFF;
        wordBytesSize += (int) (offset2word[offset+1] & 0xFF) << 8;
        String word = new String(
                ArrayUtils.subarray(offset2word, offset + 2, offset + 2 + wordBytesSize),
                UTF8);
        return word;
    }
    
    /**
     * Compute the <code>max</code> most similar words for word with ID 
     * <code>wordId</code>. This is done by comparing the matrix row for 
     * <code>wordId</code> with all other matrix rows.
     * @param wordId
     * @param max
     * @return 
     */
    public List<ReturnDataCol> getMostSimilar(int wordId, int max){
        
        List<ReturnDataCol> similarWords = new ArrayList<>();
        
        for( int k = 0; k < config.vocabularySize; k++ ){
            if( k == wordId ){
                continue;
            }
            float sim = (float) DISCO.getVectorSimilarity(SimilarityMeasure.COSINE)
                    .computeSimilarity(getWordVector(wordId), getWordVector(k));
            if( sim <= 0.0F ){
                continue;
            }
            similarWords.add(new ReturnDataCol( id2word(k), sim));
        }
        
        Collections.sort(similarWords);
        
        if( similarWords.size() < max ){
            max = similarWords.size();
        }
        return similarWords.subList(0, max);
    }
    
    public void printMostSimilar(String w, int max){
        
        int i = getWordId(w);
        if( i == -1 ){
            return;
        }
        
        List<ReturnDataCol> similarWords = new ArrayList<>();
        
        for( int k = 0; k < config.vocabularySize; k++ ){
            if( k == i ){
                continue;
            }
            float sim = (float) DISCO.getVectorSimilarity(SimilarityMeasure.COSINE)
                    .computeSimilarity(getWordVector(i), getWordVector(k));
            if( sim <= 0.0F ){
                continue;
            }
            similarWords.add(new ReturnDataCol( id2word(k), sim));
        }
        
        Collections.sort(similarWords);
        int out = 0;
        for( ReturnDataCol data : similarWords ){
            System.out.println(data.word+"\t"+data.value);
            out++;
            if( out >= max ){
                break;
            }
        }
    }
    
    /**
     * Serialize <code>DenseMatrix</code> object to file.
     * @param denseMatrix
     * @param outputPath 
     */
    public static void serialize(DenseMatrix denseMatrix, String outputPath){
        
        try(
            OutputStream file = new FileOutputStream(outputPath);
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutput output = new ObjectOutputStream(buffer);
        ){
            output.writeObject(denseMatrix);
        }catch(IOException ex){
            System.err.println("Cannot serialize DenseMatrix: "+ex);
        }
    }
    
    /**
     * Deserialize <code>DenseMatrix</code> object from file.    
     * @param serializedDenseMatrixPath
     * @return 
     */
    public static DenseMatrix deserialize(File serializedDenseMatrixPath){
        
        DenseMatrix denseMatrix = null;
        
        try(
            InputStream file = new FileInputStream(serializedDenseMatrixPath);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream (buffer);
          ){
            denseMatrix = (DenseMatrix) input.readObject();
        }catch(ClassNotFoundException | IOException ex){
            System.err.println("Cannot deserialize DenseMatrix: "+ex);
        }
        
        if( denseMatrix.simMatrix != null ){
            denseMatrix.numberOfSimilarWords = denseMatrix.simMatrix[0].length;
        }
        
        return denseMatrix;
    }

}
