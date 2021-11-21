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

import de.linguatools.disco.DISCO.SimilarityMeasure;
import de.linguatools.disco.DISCO.WordspaceType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class provides support for compositional distributional semantics.
 * There are methods to compute the similarity between multi-word terms,
 * phrases and sentences or even paragraphs based on composition of the vectors
 * of individual words.<br>
 * The methods in this class come in two versions: for dense vectors (float arrays) 
 * as well as sparse vectors (maps).
 * @author peter
 * @version 2.0
 */
public class Compositionality {
    
    /**
     * Implemented methods of vector composition.
     */
    public enum VectorCompositionMethod {
        /**
         * Simple vector addition.
         */
        ADDITION, 
       /**
        * Vector subtraction.
        */
        SUBTRACTION,
        /**
         * Entry-wise multiplication.
         */
        MULTIPLICATION, 
        /**
         * Parameterized combination of addition and multiplication, cf. 
         * equation (11) in J. Mitchell and M. Lapata: Vector-based Models of
         * Semantic Composition. Proceedings of ACL-08: HLT.
         */
        COMBINED, 
        /**
         * Dilate word vector u along the direction of word vector v: 
         *  <blockquote>v' = u ° v<br>
         *     = (lambda-1)(u*v/u*u)*u + v</blockquote>
         * If SimilarityMeasures.COSINE is used, the following formula can be
         * used instead:
         *  <blockquote>v' = (u*u)v + (lambda-1)(u*v)u</blockquote>
         * where * is the dot product (Skalarprodukt).<br>
         * Contrary to the other composition methods, this operation is not
         * symmetric.<br>
         * See chapter 4 of J. Mitchell: Composition in Distributional Models of
         * Semantics. PhD, Edinburgh, 2011.
         */
        DILATION,
        /**
         * Vector extrema combines vectors by choosing for each vector dimension
         * the value that has the highest distance from zero, i.e. the highest 
         * absolute value.
         * See G. Forgues et al. (2014): Bootstrapping Dialog Systems with Word
         * Embeddings. NIPS 2014.
         */
        EXTREMA;
    }
    
    /**
     * The following formula is used:
     * <blockquote>(wv1*wv1)wv2 + (lambda-1)(wv1*wv2)wv1</blockquote>
     * The default value (if lambda is null) for lambda is 2.0.<br>
     * This composition method only works with the SimilarityMeasures.COSINE
     * similarity measure. 
     * @param wv1
     * @param wv2
     * @param lambda
     * @return 
     */
    public static Map<String,Float> composeVectorsByDilation(
            Map<String,Float> wv1, Map<String,Float> wv2, Float lambda){
        
        if( lambda == null) lambda = 2.0F;
        
        float a = SparseVector.dotProduct(wv1, wv2);
        Map<String,Float> f1 = SparseVector.mul(wv2, a);
        Map<String,Float> f2 = SparseVector.mul(wv1, a*(lambda-1));
        return SparseVector.add(f1, f2);
    }
    
    /**
     * The following formula is used:
     * <blockquote>(wv1*wv1)wv2 + (lambda-1)(wv1*wv2)wv1</blockquote>
     * The default value (if lambda is null) for lambda is 2.0.<br>
     * This composition method only works with the SimilarityMeasures.COSINE
     * similarity measure. 
     * @param wv1
     * @param wv2
     * @param lambda
     * @return 
     */
    public static float[] composeVectorsByDilation(float[] wv1, float[] wv2, Float lambda){
        
        if( lambda == null) lambda = 2.0F;
        
        float a = DenseVector.dotProduct(wv1, wv2);
        float[] f1 = DenseVector.mul(wv2, a);
        float[] f2 = DenseVector.mul(wv1, a*(lambda-1));
        return DenseVector.add(f1, f2);
    }
    
    /**
     * Compose vectors wv1 and wv2 by a combination of addition and 
     * multiplication:
     * <blockquote>p = a*wv1 + b*wv2 + c*wv1*wv2</blockquote>
     * The contribution of multiplication and addition, as well
     * as the contribution of each of the two vectors can be controlled by the
     * three parameters a, b and c.<br>
     * For instance, in Mitchell and Lapata 2008 where wv1 is a verb and wv2 is
     * a noun, the parameters a, b and c are set as follows:
     * <blockquote>a = 0.95<br>
     * b = 0<br>
     * c = 0.05.</blockquote>
     * If one of a, b, c is null, then these default values are used.
     * @param wv1 first word vector
     * @param wv2 second word vector
     * @param a weight of additive contribution of first word vector
     * @param b weight of additive contribution of second word vector
     * @param c weight of multiplicative contribution of both word vectors
     * @return 
     */
    public static Map<String,Float> composeVectorsByCombinedMultAdd(
            Map<String,Float> wv1, Map<String,Float> wv2, Float a, 
            Float b, Float c){
        
        if( a == null || b == null || c == null ){
            a = 0.95F;
            b = 0.0F;
            c = 0.05F;
        }
        
        // Formula: result = a*wv1 + b*wv2 + c*wv1*wv2
        // m = wv1 * wv2
        Map<String,Float> m = SparseVector.mul(wv1, wv2);
        // m = c * m
        m = SparseVector.mul(m, c);
        // k = a * wv1
        Map<String,Float> k = SparseVector.mul(wv1, a);
        // l = b * wv2
        Map<String,Float> l = SparseVector.mul(wv2, b);
        // result = k + l + m
        return SparseVector.add(SparseVector.add(k,l),m);
    }
    
    /**
     * Compose vectors wv1 and wv2 by a combination of addition and 
     * multiplication:
     * <blockquote>p = a*wv1 + b*wv2 + c*wv1*wv2</blockquote>
     * The contribution of multiplication and addition, as well
     * as the contribution of each of the two vectors can be controlled by the
     * three parameters a, b and c.<br>
     * For instance, in Mitchell and Lapata 2008 where wv1 is a verb and wv2 is
     * a noun, the parameters a, b and c are set as follows:
     * <blockquote>a = 0.95<br>
     * b = 0<br>
     * c = 0.05.</blockquote>
     * If one of a, b, c is null, then these default values are used.
     * @param wv1 first word vector
     * @param wv2 second word vector
     * @param a weight of additive contribution of first word vector
     * @param b weight of additive contribution of second word vector
     * @param c weight of multiplicative contribution of both word vectors
     * @return 
     */
    public static float[] composeVectorsByCombinedMultAdd(
            float[] wv1, float[] wv2, Float a, Float b, Float c){
        
        if( a == null || b == null || c == null ){
            a = 0.95F;
            b = 0.0F;
            c = 0.05F;
        }
        
        // Formula: result = a*wv1 + b*wv2 + c*wv1*wv2
        // m = wv1 * wv2
        float[] m = DenseVector.mul(wv1, wv2);
        // m = c * m
        m = DenseVector.mul(m, c);
        // k = a * wv1
        float[] k = DenseVector.mul(wv1, a);
        // l = b * wv2
        float[] l = DenseVector.mul(wv2, b);
        // result = k + l + m
        return DenseVector.add(DenseVector.add(k,l),m);
    }
    
    /**
     * Computes vector rejection of a on b. See https://en.wikipedia.org/wiki/Vector_projection
     * and http://bookworm.benschmidt.org/posts/2015-10-25-Word-Embeddings.html.<br>
     * Example: to get the "river" meaning for the word "bank" use vector rejection
     * in the following way:<br>
     * <code>bank_without_finance = vectorRejection(bank, averageVector(deposit,
     * account, cashier))</code>
     * @param a 
     * @param b
     * @return vector a without the dimensions from b.
     * @since 3.0
     */
    public static Map<String,Float> vectorRejection(Map<String,Float> a,
            Map<String,Float> b){
        
        return SparseVector.sub(a, SparseVector.mul(b, 
                SparseVector.dotProduct(a, b) / SparseVector.dotProduct(b, b)));
    }
    
    /**
     * Computes vector rejection of a on b. See https://en.wikipedia.org/wiki/Vector_projection
     * and http://bookworm.benschmidt.org/posts/2015-10-25-Word-Embeddings.html.<br>
     * Example: to get the "river" meaning for the word "bank" use vector rejection
     * in the following way:<br>
     * <code>bank_without_finance = vectorRejection(bank, averageVector(deposit,
     * account, cashier))</code>
     * @param a 
     * @param b
     * @return vector a without the dimensions from b.
     * @since 3.0
     */
    public static float[] vectorRejection(float[] a, float[] b){
        
        return DenseVector.sub(a, DenseVector.mul(b, 
                DenseVector.dotProduct(a, b) / DenseVector.dotProduct(b, b)));
    }
    
    /**
     * Compose two word vectors by the composition method given in 
     * <code>compositionMethod</code>.
     * @param wv1 word vector #1
     * @param wv2 word vector #2
     * @param compositionMethod One of the methods in <code>VectorCompositionMethod</code>.
     * @param a only needed for composition method COMBINED.
     * @param b only needed for composition method COMBINED.
     * @param c only needed for composition method COMBINED.
     * @param lambda only needed for composition method DILATION.
     * @return the resulting word vector or <code>null</code>.
     */
    public static Map<String,Float> composeWordVectors(Map<String,Float> wv1,
            Map<String,Float> wv2, VectorCompositionMethod compositionMethod,
            Float a, Float b, Float c, Float lambda){
    
        if( wv1 == null || wv2 == null ){
            return null;
        }
        
        if( compositionMethod == VectorCompositionMethod.ADDITION ){
            return SparseVector.add(wv1, wv2);
        }else if( compositionMethod == VectorCompositionMethod.SUBTRACTION ){
            return SparseVector.sub(wv1, wv2);
        }else if( compositionMethod == VectorCompositionMethod.MULTIPLICATION ){
            return SparseVector.mul(wv1, wv2);
        }else if( compositionMethod == VectorCompositionMethod.COMBINED ){
            return composeVectorsByCombinedMultAdd(wv1, wv2, a, b, c); 
        }else if( compositionMethod == VectorCompositionMethod.DILATION ){
            return composeVectorsByDilation(wv1, wv2, lambda);   
        }else if( compositionMethod == VectorCompositionMethod.EXTREMA ){
            return SparseVector.vectorExtrema(wv1, wv2);     
        }else{
            return null;
        }
    }
    
    /**
     * Compose two word vectors by the composition method given in 
     * <code>compositionMethod</code>.
     * @param wv1 word vector #1
     * @param wv2 word vector #2
     * @param compositionMethod One of the methods in <code>VectorCompositionMethod</code>.
     * @param a only needed for composition method COMBINED.
     * @param b only needed for composition method COMBINED.
     * @param c only needed for composition method COMBINED.
     * @param lambda only needed for composition method DILATION.
     * @return the resulting word vector or <code>null</code>.
     */
    public static float[] composeWordVectors(float[] wv1, float[] wv2,
            VectorCompositionMethod compositionMethod,
            Float a, Float b, Float c, Float lambda){
    
        if( wv1 == null || wv2 == null ){
            return null;
        }
        
        if( compositionMethod == VectorCompositionMethod.ADDITION ){
            return DenseVector.add(wv1, wv2);
        }else if( compositionMethod == VectorCompositionMethod.SUBTRACTION ){
            return DenseVector.sub(wv1, wv2);
        }else if( compositionMethod == VectorCompositionMethod.MULTIPLICATION ){
            return DenseVector.mul(wv1, wv2);
        }else if( compositionMethod == VectorCompositionMethod.COMBINED ){
            return composeVectorsByCombinedMultAdd(wv1, wv2, a, b, c); 
        }else if( compositionMethod == VectorCompositionMethod.DILATION ){
            return composeVectorsByDilation(wv1, wv2, lambda);   
        }else if( compositionMethod == VectorCompositionMethod.EXTREMA ){
            return DenseVector.vectorExtrema(wv1, wv2);     
        }else{
            return null;
        }
    }
    
    /**
     * Compose two or more word vectors by the composition method given in 
     * <code>compositionMethod</code>.
     * @param wordvectorList a list of word vectors to be combined. The list has
     * to have at least two elements. The ordering of the list has no influence
     * on the result.
     * @param compositionMethod One of the methods in <code>VectorCompositionMethod</code>.
     * @param a only needed for composition method COMBINED.
     * @param b only needed for composition method COMBINED.
     * @param c only needed for composition method COMBINED.
     * @param lambda only needed for composition method DILATION.
     * 
     * @return the resulting word vector or <code>null</code>.
     */
    public static Map<String,Float> composeWordVectors(ArrayList<Map<String,Float>>
            wordvectorList, VectorCompositionMethod compositionMethod, Float a, 
            Float b, Float c, Float lambda){
        
        if( wordvectorList.size() < 2 ){
            return null;
        }
        if( wordvectorList.get(0) == null || wordvectorList.get(1) == null ){
            return null;
        }
        
        // combine the first two vectors in the list
        Map<String,Float> wv = composeWordVectors(wordvectorList.get(0),
                wordvectorList.get(1), compositionMethod, a, b, c, lambda);
        
        for(int i = 2; i < wordvectorList.size(); i++){
            if( wordvectorList.get(i) == null ){
                continue;
            }
            wv = composeWordVectors(wv, wordvectorList.get(i), compositionMethod,
                    a, b, c, lambda);
        }
        return wv;
    }
    
    /**
     * Compose two or more word vectors by the composition method given in 
     * <code>compositionMethod</code>.
     * @param wordvectorList a list of word vectors to be combined. The list has
     * to have at least two elements. The ordering of the list has no influence
     * on the result.
     * @param compositionMethod One of the methods in <code>VectorCompositionMethod</code>.
     * @param a only needed for composition method COMBINED.
     * @param b only needed for composition method COMBINED.
     * @param c only needed for composition method COMBINED.
     * @param lambda only needed for composition method DILATION.
     * 
     * @return the resulting word vector or <code>null</code>.
     */
    public static float[] composeWordVectors(List<float[]> wordvectorList, 
            VectorCompositionMethod compositionMethod, Float a, 
            Float b, Float c, Float lambda){
        
        if( wordvectorList.size() < 2 ){
            return null;
        }
        if( wordvectorList.get(0) == null || wordvectorList.get(1) == null ){
            return null;
        }
        
        // combine the first two vectors in the list
        float[] wv = composeWordVectors(wordvectorList.get(0),
                wordvectorList.get(1), compositionMethod, a, b, c, lambda);
        
        for(int i = 2; i < wordvectorList.size(); i++){
            if( wordvectorList.get(i) == null ){
                continue;
            }
            wv = composeWordVectors(wv, wordvectorList.get(i), compositionMethod,
                    a, b, c, lambda);
        }
        return wv;
    }
    
    /**
     * Utility function. Prints the word vector to standard output.
     * @param wordvector 
     */
    public static void printWordVector(Map<String,Float> wordvector){
        
        for (String w : wordvector.keySet()) {
            System.out.println(w+"\t"+wordvector.get(w));
        }
    }
    
    /**
     * This method computes the semantic similarity between two multi-word terms,
     * phrases, sentences or paragraphs. This is done by composition of the word
     * vectors of the constituent words.<br>
     * Each of the two input strings is split at whitespace, and the wordvectors
     * of the individual tokens (constituent words) are retrieved. Then the
     * word vectors are combined using the method <code>composeWordVectors()</code>.
     * The two resulting vectors are then compared using
     * <code>Compositionality.semanticSimilarity()</code>.<br>
     * <b>Note</b>: the methods in class <code>TextSimilarity</code> might give
     * more accurate results for short text similarity because they weight the
     * words in the input strings by their frequency and try to align words in 
     * the input strings.
     * @param multiWords1 a tokenized string containing a multi-word term, phrase,
     * sentence or paragraph.
     * @param multiWords2 a tokenized string containing a multi-word term, phrase,
     * sentence or paragraph.
     * @param compositionMethod a vector composition method.
     * @param simMeasure a similarity measure. 
     * @param disco a DISCOLuceneIndex word space.
     * @param a only needed for composition method COMBINED.
     * @param b only needed for composition method COMBINED.
     * @param c only needed for composition method COMBINED.
     * @param lambda only needed for composition method DILATION.
     * @return the distributional similarity between <code>multiWord1</code> and
     * <code>multiWord2</code>.
     * @throws java.io.IOException
     * @see de.linguatools.disco.TextSimilarity
     */
    public static float compositionalSemanticSimilarity(String multiWords1, 
            String multiWords2, VectorCompositionMethod compositionMethod, 
            SimilarityMeasure simMeasure, DISCO disco, Float a, 
            Float b, Float c, Float lambda) throws IOException{
        
        multiWords1 = multiWords1.trim();
        multiWords2 = multiWords2.trim();
        String[] multi1 = multiWords1.split("\\s+");
        String[] multi2 = multiWords2.split("\\s+");
        
        if( disco instanceof DISCOLuceneIndex ){
            Map<String,Float> wv1 = computeWordVector(multi1, compositionMethod, disco,
                    a, b, c, lambda);
            Map<String,Float> wv2 = computeWordVector(multi2, compositionMethod, disco,
                    a, b, c, lambda);
            return (float) DISCO.getVectorSimilarity(simMeasure).computeSimilarity(
                    wv1, wv2);
        }else{
            float[] wv1 = computeWordVector(multi1, (DenseMatrix)disco, compositionMethod,
                    a, b, c, lambda);
            float[] wv2 = computeWordVector(multi2, (DenseMatrix)disco, compositionMethod,
                    a, b, c, lambda);
            return (float) DISCO.getVectorSimilarity(simMeasure).computeSimilarity(
                    wv1, wv2);
        }
    }
    
    /**
     * Construct a word vector that represents the <code>multi</code>-word
     * phrase. 
     * @param multi a multi-token term, phrase or sentence (one token per array element).
     * @param compositionMethod
     * @param disco
     * @param a
     * @param b
     * @param c
     * @param lambda
     * @return
     * @throws IOException 
     */
    public static Map<String,Float> computeWordVector(String[] multi, 
            VectorCompositionMethod compositionMethod, DISCO disco, Float a, 
            Float b, Float c, Float lambda) throws IOException{
        
        Map<String,Float> wv;
        if( multi.length == 1 ){
            wv = disco.getWordvector(multi[0]);
        }else if( multi.length == 2 ){
            wv = composeWordVectors(disco.getWordvector(multi[0]),
                disco.getWordvector(multi[1]), compositionMethod, a, b, c, lambda);
        }else{
            wv = composeWordVectors(disco.getWordvector(multi[0]),
                disco.getWordvector(multi[1]), compositionMethod, a, b, c, lambda);
            for(int i = 2; i < multi.length; i++){
                wv = composeWordVectors(wv, disco.getWordvector(multi[i]),
                        compositionMethod, a, b, c, lambda);
            }
        }
        return wv;
    }
    
    /**
     * Construct a word embedding that represents the <code>multi</code> word
     * phrase. 
     * @param multi a multi-token term, phrase or sentence (one token per array element).
     * @param disco
     * @param compositionMethod
     * @param a
     * @param b
     * @param c
     * @param lambda
     * @return
     * @throws IOException 
     */
    public static float[] computeWordVector(String[] multi, DenseMatrix disco,
            VectorCompositionMethod compositionMethod, Float a, 
            Float b, Float c, Float lambda) throws IOException{
        
        float[] wv;
        if( multi.length == 1 ){
            wv = disco.getWordEmbedding(multi[0]);
        }else if( multi.length == 2 ){
            wv = composeWordVectors(disco.getWordEmbedding(multi[0]),
                disco.getWordEmbedding(multi[1]), compositionMethod, a, b, c, lambda);
        }else{
            wv = composeWordVectors(disco.getWordEmbedding(multi[0]),
                disco.getWordEmbedding(multi[1]), compositionMethod, a, b, c, lambda);
            for(int i = 2; i < multi.length; i++){
                wv = composeWordVectors(wv, disco.getWordEmbedding(multi[i]),
                        compositionMethod, a, b, c, lambda);
            }
        }
        return wv;
    }
    
    /**
     * Find the most similar words in the DISCO word space for an input word 
     * vector. While the word vector can represent a multi-token word (if it was
     * produced by one of the methods 
     * <code>Compositionality.composeWordVectors()</code>) the most
     * similar words will only be single-token words from the index.<br>
     * <b>Warning</b>: This method is very time consuming and should only be
     * used with a word space that has been loaded into memory!
     * @param wordvector input word vector
     * @param disco DISCO word space
     * @param simMeasure
     * @param maxN return only the <code>maxN</code> most similar words. If 
     * <code>maxN &lt; 1</code> all words are returned. 
     * @return List of all words (with their similarity values) whose similarity
     * with the <code>wordvector</code> is greater than zero, ordered by 
     * similarity value (highest value first).
     * @throws IOException 
     */
    public static List<ReturnDataCol> similarWords(Map<String,Float> wordvector,
            DISCO disco, SimilarityMeasure simMeasure, int maxN)
            throws IOException{
        
        List<ReturnDataCol> result = new ArrayList();
        if( wordvector == null ){
            return result;
        }
        // durchlaufe alle Dokumente
        Iterator<String> iterator = disco.getVocabularyIterator();
        while( iterator.hasNext() ){
            String word = iterator.next();
            Map<String,Float> wv = disco.getWordvector(word);
            if( wv == null ){
                continue;
            }
            // Ähnlichkeit zwischen Wortvektoren berechnen
            float sim = (float) DISCO.getVectorSimilarity(simMeasure).computeSimilarity(
                    wordvector, wv);
            if( sim > 0.0F){
                ReturnDataCol r = new ReturnDataCol(word, sim);
                result.add(r);
            }
        }
        
        // nach höchstem Ähnlichkeitswert sortieren
        Collections.sort(result);
        if( maxN > 0 ){
            if( maxN > result.size() ){
                maxN = result.size();
            }
            result = result.subList(0, maxN);
        }
        return result;
    }
    
    /**
     * Find the most similar words in the DISCO word space for an input word 
     * vector. While the word vector can represent a multi-token word (if it was
     * produced by one of the methods 
     * <code>Compositionality.composeWordVectors()</code>) the most
     * similar words will only be single-token words from the index.<br>
     * <b>Warning</b>: This method is very time consuming and should only be
     * used with a word space that has been loaded into memory!
     * @param wordEmbedding input word vector
     * @param disco DISCO word space
     * @param simMeasure
     * @param maxN return only the <code>maxN</code> most similar words. If 
     * <code>maxN &lt; 1</code> all words are returned. 
     * @return List of all words (with their similarity values) whose similarity
     * with the <code>wordvector</code> is greater than zero, ordered by 
     * similarity value (highest value first).
     * @throws IOException 
     */
    public static List<ReturnDataCol> similarWords(float[] wordEmbedding,
            DenseMatrix disco, SimilarityMeasure simMeasure, int maxN)
            throws IOException{
        
        List<ReturnDataCol> result = new ArrayList();
        if( wordEmbedding == null ){
            return result;
        }
        // durchlaufe alle Dokumente
        Iterator<String> iterator = disco.getVocabularyIterator();
        while( iterator.hasNext() ){
            String word = iterator.next();
            float[] wv = disco.getWordEmbedding(word);
            if( wv == null ){
                continue;
            }
            // Ähnlichkeit zwischen Wortvektoren berechnen
            float sim = (float) DISCO.getVectorSimilarity(simMeasure).computeSimilarity(
                    wordEmbedding, wv);
            if( sim > 0.0F){
                ReturnDataCol r = new ReturnDataCol(word, sim);
                result.add(r);
            }
        }
        
        // nach höchstem Ähnlichkeitswert sortieren
        Collections.sort(result);
        if( maxN > 0 ){
            if( maxN > result.size() ){
                maxN = result.size();
            }
            result = result.subList(0, maxN);
        }
        return result;
    }
    
    /**
     * Approximate nearest neighbor search to find the most similar word in the 
     * vocabulary for an input <code>wordvector</code>. This is about 20 times
     * faster than brute-force search with <code>Compositionality.similarWords</code>.
     * The true nearest neighbor is found in 80% of cases (depending on the number
     * of similar words stored for each word).<br>
     * <b>Important</b>: This only works with word spaces of type SIM.<br>
     * This method is an implementation of
     * <blockquote>Kohei Sugawara, Hayato Kobayashi, Masajiro Iwasaki.
     * <a href="https://aclweb.org/anthology/P/P16/P16-1214.pdf">On Approximately
     * Searching for Similar Word Embeddings</a>. Proceedings of the 54th Annual
     * Meeting of the Association for Computational Linguistics, pages 2265–2275,
     * Berlin, Germany, August 7-12, 2016.</blockquote>
     * The pre-computed most similar words that are stored for each word (in 
     * DISCO word spaces of type SIM) constitute a neighborhood graph. The basic
     * idea is to use this graph as a search index. Instead of comparing the input
     * word vector with the word vectors of all words in the vocabulary (brute-force
     * search) we perform a best-first search. First, we pick a random word w and
     * compute the similarity of all neighbors of w with the input word vector.
     * The closest neighbor is then set as new word w, and the process is repeated
     * until no new w can be found that is closer to the input vector.
     * <br> 
     * This works because of the small world properties of word spaces: the
     * shortest path length between two arbitrary words in a word space is less
     * than 7 on average, as described by 
     * <blockquote>Mark Steyvers, Joshua B. Tenenbaum. 
     * <a href="http://web.mit.edu/cocosci/Papers/03nSteyvers.pdf">The Large-Scale
     * Structure of Semantic Networks: Statistical Analyses and a Model of Semantic
     * Growth</a>. Cognitive Science 29 (2005) 41–78.</blockquote>
     * 
     * @param wordvector
     * @param disco
     * @param simMeasure
     * @param nMax return at most <code>nMax</code> words.
     * @return approximate nearest neighbor words for input wordvector sorted by
     * similarity to wordvector (most similar first).
     * @throws IOException
     * @throws WrongWordspaceTypeException 
     */
    public static List<ReturnDataCol> similarWordsGraphSearch(Map<String,Float> wordvector,
            DISCO disco, SimilarityMeasure simMeasure, int nMax)
            throws IOException, WrongWordspaceTypeException{
        
        // check word space type
        if( disco.getWordspaceType() != DISCO.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + "to word spaces of type "+disco.getWordspaceType());
        }
        
        // good values (default)
        int initSetSize = 100;
        int maxN = disco.numberOfSimilarWords(); // 300 (the higher the better)
        
        VectorSimilarity vectorSimilarity = DISCO.getVectorSimilarity(simMeasure);
        
        // generate a set of initSetSize random words and pick the best one as
        // start word
        String startWord;
        String maxWord = null;
        float sim = 0;
        float maxSim = 0;
        Map<String,Float> wvStart = null;
        for( int n = 0; n < initSetSize; n++ ){
            int w = ThreadLocalRandom.current().nextInt(0, disco.numberOfWords() );
            startWord = disco.getWord(w);
            if( startWord == null ){
                continue;
            }
            wvStart = disco.getWordvector(startWord);
            if( wvStart == null ){
                continue;
            }
            sim = (float) vectorSimilarity.computeSimilarity(wvStart, wordvector);
            if( sim > maxSim ){
                maxSim = sim;
                maxWord = startWord;
            }
        }
        startWord = maxWord;
        List<ReturnDataCol> returnData = new ArrayList<>();
        boolean better = true;
        do{
            // get similar words for start word
            ReturnDataBN similarWords = disco.similarWords(startWord);
            if( similarWords == null ){
                continue;
            }
            // find the most similar word to the input word vector. Only look at
            // the first nMax words
            maxWord = startWord;
            maxSim = sim;
            Map<String,Float> maxWordvector = wvStart;
            for( int i = 0; i < similarWords.words.length; i++ ){
                if( i == maxN ){
                    break;
                }
                Map<String,Float> wv = disco.getWordvector(similarWords.words[i]);
                float s = (float) vectorSimilarity.computeSimilarity(wordvector, wv);
                if( s > maxSim ){
                    maxSim = s;
                    maxWord = similarWords.words[i];
                    maxWordvector = wv;
                }
            }
            if( maxSim > sim ){
                sim = maxSim;
                startWord = maxWord;
                wvStart = maxWordvector;
                better = true;
                returnData.add(new ReturnDataCol(maxWord, maxSim));
            }else{
                better = false;
            }
        }while( better );
        Collections.sort(returnData);
        if( returnData.size() > nMax ){
            return returnData.subList(0, nMax);
        }else{
            return returnData;
        }
    }
    
    /**
     * Approximate nearest neighbor search to find the most similar word in the 
     * vocabulary for an input <code>wordvector</code>. This is about 20 times
     * faster than brute-force search with <code>Compositionality.similarWords</code>.
     * The true nearest neighbor is found in 80% of cases (depending on the number
     * of similar words stored for each word).<br>
     * <b>Important</b>: This only works with word spaces of type SIM.<br>
     * This method is an implementation of
     * <blockquote>Kohei Sugawara, Hayato Kobayashi, Masajiro Iwasaki.
     * <a href="https://aclweb.org/anthology/P/P16/P16-1214.pdf">On Approximately
     * Searching for Similar Word Embeddings</a>. Proceedings of the 54th Annual
     * Meeting of the Association for Computational Linguistics, pages 2265–2275,
     * Berlin, Germany, August 7-12, 2016.</blockquote>
     * The pre-computed most similar words that are stored for each word (in 
     * DISCO word spaces of type SIM) constitute a neighborhood graph. The basic
     * idea is to use this graph as a search index. Instead of comparing the input
     * word vector with the word vectors of all words in the vocabulary (brute-force
     * search) we perform a best-first search. First, we pick a random word w and
     * compute the similarity of all neighbors of w with the input word vector.
     * The closest neighbor is then set as new word w, and the process is repeated
     * until no new w can be found that is closer to the input vector.
     * <br> 
     * This works because of the small world properties of word spaces: the
     * shortest path length between two arbitrary words in a word space is less
     * than 7 on average, as described by 
     * <blockquote>Mark Steyvers, Joshua B. Tenenbaum. 
     * <a href="http://web.mit.edu/cocosci/Papers/03nSteyvers.pdf">The Large-Scale
     * Structure of Semantic Networks: Statistical Analyses and a Model of Semantic
     * Growth</a>. Cognitive Science 29 (2005) 41–78.</blockquote>
     * 
     * @param wordEmbedding
     * @param disco
     * @param simMeasure
     * @param nMax return max. nMax words
     * @return approximate nearest neighbor words for input <code>wordEmbedding</code>
     * sorted by similarity to <code>wordEmbedding</code> (most similar first).
     * @throws IOException
     * @throws WrongWordspaceTypeException 
     */
    public static List<ReturnDataCol> similarWordsGraphSearch(float[] wordEmbedding,
            DenseMatrix disco, SimilarityMeasure simMeasure, int nMax)
            throws IOException, WrongWordspaceTypeException{
        
        // check word space type
        if( disco.getWordspaceType() != DISCO.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + "to word spaces of type "+disco.getWordspaceType());
        }
        
        // good values (default)
        int initSetSize = 100;
        int maxN = disco.numberOfSimilarWords(); // 300 (the higher the better)
        
        VectorSimilarity vectorSimilarity = DISCO.getVectorSimilarity(simMeasure);
        
        // generate a set of initSetSize random words and pick the best one as
        // start word
        String startWord;
        String maxWord = null;
        float sim = 0;
        float maxSim = 0;
        float[] wvStart = null;
        for( int n = 0; n < initSetSize; n++ ){
            int w = ThreadLocalRandom.current().nextInt(0, disco.numberOfWords() );
            startWord = disco.getWord(w);
            if( startWord == null ){
                continue;
            }
            wvStart = disco.getWordEmbedding(startWord);
            if( wvStart == null ){
                continue;
            }
            sim = (float) vectorSimilarity.computeSimilarity(wvStart, wordEmbedding);
            if( sim > maxSim ){
                maxSim = sim;
                maxWord = startWord;
            }
        }
        startWord = maxWord;
        List<ReturnDataCol> returnData = new ArrayList<>();
        boolean better = true;
        do{
            // get similar words for start word
            ReturnDataBN similarWords = disco.similarWords(startWord);
            if( similarWords == null ){
                continue;
            }
            // find the most similar word to the input word vector. Only look at
            // the first nMax words
            maxWord = startWord;
            maxSim = sim;
            float[] maxWordvector = wvStart;
            for( int i = 0; i < similarWords.words.length; i++ ){
                if( i == maxN ){
                    break;
                }
                float[] wv = disco.getWordEmbedding(similarWords.words[i]);
                if( wv == null ){
                    continue;
                }
                float s = (float) vectorSimilarity.computeSimilarity(wordEmbedding, wv);
                if( s > maxSim ){
                    maxSim = s;
                    maxWord = similarWords.words[i];
                    maxWordvector = wv;
                }
            }
            if( maxSim > sim ){
                sim = maxSim;
                startWord = maxWord;
                wvStart = maxWordvector;
                better = true;
                returnData.add(new ReturnDataCol(maxWord, maxSim));
            }else{
                better = false;
            }
        }while( better );
        Collections.sort(returnData);
        if( returnData.size() > nMax ){
            return returnData.subList(0, nMax);
        }else{
            return returnData;
        }
    }
    
    /**
     * Exhaustive breath-first search to find the shortest path between two input
     * words in the neighborhood graph. Interestingly, this always finds a path
     * (at least if <code>numberOfSimilarWords &gt;= 50</code>), showing that the
     * neighborhood graph of word spaces is fully connected. For more information
     * on the neighborhood graph see <code>similarWordsGraphSearch</code>.<br>
     * <b>Important</b>: This method only works with word spaces of type SIM.<br>
     * @param i1 ID of input word #1
     * @param i2 ID of input word #2
     * @param denseMatrix
     * @return shortest path between <code>i1</code> and <code>i2</code>. The 
     * resulting list contains the path in reverse order, i.e. the first list 
     * element is <code>i2</code>, the last element is <code>i1</code>.
     * @throws de.linguatools.disco.WrongWordspaceTypeException 
     */
    public static List<Integer> findShortestPath(int i1, int i2, DenseMatrix denseMatrix) 
            throws WrongWordspaceTypeException{
        
        if( denseMatrix.getWordspaceType() != WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + " to word spaces of type "+denseMatrix.getWordspaceType());
        }
        
        // Breitensuche
        int val[] = new int[denseMatrix.numberOfWords()+1];
        int back[] = new int[denseMatrix.numberOfWords()+1];
        int nr = 0;
        List<Integer> queue = new LinkedList<>();
        queue.add(i1);
        
        while( !queue.isEmpty() ){
            int k = queue.remove(0);
            // gefunden?
            if( k == i2 ){
                break;
            }
            val[k] = ++nr;
            // besuche Nachbarknoten von k = ähnliche Wörter
            for( int n = 0; n < denseMatrix.numberOfSimilarWords(); n++ ){
                if( val[denseMatrix.getSecondOrderWordvector(k)[n]] == 0 ){
                    queue.add( denseMatrix.getSecondOrderWordvector(k)[n] );
                    back[denseMatrix.getSecondOrderWordvector(k)[n]] = k;
                    val[ denseMatrix.getSecondOrderWordvector(k)[n] ] = -1;
                }
            }
        }
        
        List<Integer> shortestPath = new LinkedList<>();
        shortestPath.add(i2);
        int p = i2;
        do{
            p = back[p];
            shortestPath.add(p);
        }while( p != i1 );
        
        return shortestPath;
    }
    
    /**
     * Wrapper method.
     * @param w1
     * @param w2
     * @param denseMatrix
     * @return the words forming the shortest path.
     * @throws WrongWordspaceTypeException 
     * @throws java.io.IOException 
     */
    public static List<String> findShortestPath(String w1, String w2, 
            DenseMatrix denseMatrix) 
            throws WrongWordspaceTypeException, IOException{
        
        if( denseMatrix.getWordspaceType() != WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + " to word spaces of type "+denseMatrix.getWordspaceType());
        }
        
        // lookup word IDs
        int id1 = denseMatrix.getWordId(w1);
        if( id1 == -1 ){
            return null;
        }
        int id2 = denseMatrix.getWordId(w2);
        if( id2 == -1 ){
            return null;
        }
        
        List<Integer> idsPath = findShortestPath(id1, id2, denseMatrix);
        
        List<String> path = new ArrayList<>();
        for( Integer id : idsPath ){
            path.add( denseMatrix.getWord(id) );
        }
        return path;
    }
    
    /**
     * This method solves the analogy "a1 is to b1 like a2 is to b2", i.e. it 
     * returns the missing word a1. Example: "a1 is to woman like king is to man"
     * with a1 = queen. This is done by the formula v(a1) = v(b1) + v(a2) - v(b2),
     * where v(a1) is the word vector for a word a1.<br>
     * Example 2: <i>childless</i> : <i>child</i> = <i>armless</i> : <i>arm</i><br>
     * with <i>a1 = childless</i> (the unknown word we want to find), <i>b1 = child</i>,
     * <i>a2 = armless</i>, <i>b2 = arm</i>.<br>
     * First step is to compute an offset vector that corresponds to the suffix 
     * "-less". If we add this "-less" vector to the vector for "arm" the resulting
     * vector should be equal to the vector for "armless": <i>arm + less = armless</i>.
     * Or: <i>less = armless - arm</i>, or with our variables above: <i>less = a2 - b2</i>.
     * Now, if we add this offset vector to our <i>b1 = child</i> then we should get
     * the vector for our missing <i>a1</i>: <i>a1 = child + less = b1 + a2 - b2</i>.<br> 
     * <b>Warning:</b> This method is very time consuming because after computing
     * v(a1), the most similar word vector to v(a1) has to be found in the word 
     * space. This is done by comparing <b>all</b> vectors in the word space with
     * v(a1). For a faster approximation use <code>solveAnalogyApprox</code> instead.
     * @param b1 must be single token, e.g. "woman"
     * @param a2 must be single token, e.g. "king"
     * @param b2 must be single token, e.g. "man"
     * @param disco
     * @return ordered list with the nearest words to v(a1) or <code>null</code>
     * if one of words b1, a2, or b2 was not found in the DISCO index. You may
     * want to filter out b1, a2, and b2 from the resulting list.
     * @throws IOException 
     * @throws de.linguatools.disco.WrongWordspaceTypeException 
     */
    public static List<ReturnDataCol> solveAnalogy(String b1, String a2, String b2,
            DISCO disco) throws IOException, WrongWordspaceTypeException{
         
        // DISCOLuceneIndex with sparse vectors (word vectors)
        if( disco instanceof DISCOLuceneIndex ){
            Map<String,Float> vb1 = disco.getWordvector(b1);
            if( vb1 == null ){
                return null;
            }
            Map<String,Float> va2 = disco.getWordvector(a2);
            if( va2 == null ){
                return null;
            }
            Map<String,Float> vb2 = disco.getWordvector(b2);
            if( vb2 == null ){
                return null;
            }
            // compute va1 = vb1 + va2 - vb2
            Map<String,Float> offset = SparseVector.sub(va2, vb2);
            Map<String,Float> va1 = SparseVector.add(vb1, offset);
            // find nearest words for va1
            return similarWords(va1, disco, SimilarityMeasure.COSINE, 12);
        }
        // DenseMatrix with dense vectors (word embeddings)
        else{
            DenseMatrix dm = (DenseMatrix) disco;
            float[] vb1 = dm.getWordEmbedding(b1);
            if( vb1 == null ){
                return null;
            }
            float[] va2 = dm.getWordEmbedding(a2);
            if( va2 == null ){
                return null;
            }
            float[] vb2 = dm.getWordEmbedding(b2);
            if( vb2 == null ){
                return null;
            }
            // compute va1 = vb1 + va2 - vb2
            float[] offset = DenseVector.sub(va2, vb2);
            float[] va1 = DenseVector.add(vb1, offset);
            // find nearest words for va1
            return similarWords(va1, dm, SimilarityMeasure.COSINE, 12);
        }
    }
    
    /**
     * Fast approximation of <code>solveAnalogy</code>. This uses <code>similarWordsGraphSearch</code>
     * instead of <code>similarWords</code> to find the nearest words for the 
     * composed word vector <code>v(a1)</code>.
     * @param b1
     * @param a2
     * @param b2
     * @param disco
     * @return ordered list with the nearest words to v(a1) or <code>null</code>
     * if one of words b1, a2, or b2 was not found in the DISCO index. You may
     * want to filter out b1, a2, and b2 from the resulting list.
     * @throws IOException
     * @throws WrongWordspaceTypeException 
     */
    public static List<ReturnDataCol> solveAnalogyApprox(String b1, String a2, 
            String b2, DISCO disco) throws IOException, WrongWordspaceTypeException{
        
        // DISCOLuceneIndex with sparse vectors (word vectors)
        if( disco instanceof DISCOLuceneIndex ){
            Map<String,Float> vb1 = disco.getWordvector(b1);
            if( vb1 == null ){
                return null;
            }
            Map<String,Float> va2 = disco.getWordvector(a2);
            if( va2 == null ){
                return null;
            }
            Map<String,Float> vb2 = disco.getWordvector(b2);
            if( vb2 == null ){
                return null;
            }
            // compute va1 = vb1 + va2 - vb2
            Map<String,Float> offset = SparseVector.sub(va2, vb2);
            Map<String,Float> va1 = SparseVector.add(vb1, offset);
            // find nearest words for va1
            return similarWordsGraphSearch(va1, disco, SimilarityMeasure.COSINE, 12);
        }
        // DenseMatrix with dense vectors (word embeddings)
        else{
            DenseMatrix dm = (DenseMatrix) disco;
            float[] vb1 = dm.getWordEmbedding(b1);
            if( vb1 == null ){
                return null;
            }
            float[] va2 = dm.getWordEmbedding(a2);
            if( va2 == null ){
                return null;
            }
            float[] vb2 = dm.getWordEmbedding(b2);
            if( vb2 == null ){
                return null;
            }
            // compute va1 = vb1 + va2 - vb2
            float[] offset = DenseVector.sub(va2, vb2);
            float[] va1 = DenseVector.add(vb1, offset);
            // find nearest words for va1
            return similarWordsGraphSearch(va1, dm, SimilarityMeasure.COSINE, 12);
        }
    }
    
    /**
     * Computes the average vector over all offset vectors in the <code>wordPairs</code>
     * list. An offset vector is computed for each entry in <code>wordPairs</code>
     * as <code>v(a2) - v(b2) = v(wordPairs.get(i)[0]) - v(wordPairs.get(i)[1])</code>
     * with <code>v(x)</code> being the word vector for the word <code>x</code>.
     * @param wordPairs each String array in the list must store word pairs 
     * <code>[a2, b2]</code> (see <code>solveAnalogy</code>).
     * @param disco
     * @return average vector or <code>null</code> if no word pair was found in
     * <code>disco</code>.
     */
    public static float[] computeAvgDenseOffsetVector(List<String[]> wordPairs,
            DISCO disco){
    
        DenseMatrix dm = (DenseMatrix) disco;
        List<float[]> vectorList = new ArrayList<>();
        for( String[] pair : wordPairs ){
            float[] a2 = dm.getWordEmbedding(pair[0]);
            if( a2 == null ){
                continue;
            }
            float[] b2 = dm.getWordEmbedding(pair[1]);
            if( b2 == null ){
                continue;
            }
            float[] offset = DenseVector.sub(a2, b2);
            vectorList.add(offset);
        }
        return DenseVector.average(vectorList);
    }
    
    /**
     * Solves the analogy <code>a1 : b1 = a2 : b2</code> by returning the missing
     * word <code>a1</code>. In contrast to the method <code>solveAnalogy</code>
     * where you have to supply only a single pair <code>a2, b2</code>, this method 
     * computes the average offset vector over all pairs in <code>wordPairs</code>
     * and uses this as offset vector to get more robust results.
     * @param b1 
     * @param wordPairs list of pairs <code>[a2, b2]</code>
     * @param disco
     * @return word in <code>disco</code> that is most similar to the vector 
     * computed by <code>v(b1) + computeAvgDenseOffsetVector(wordPairs)</code>
     * or <code>null</code> if <code>b1</code> or none of <code>wordPairs</code>
     * was found in <code>disco</code>.
     * @throws IOException 
     */
    public static List<ReturnDataCol> solveAnalogyAverageOffset(String b1, List<String[]> wordPairs,
            DISCO disco) throws IOException{
        
        DenseMatrix dm = (DenseMatrix) disco;
        float[] vb1 = dm.getWordEmbedding(b1);
        if( vb1 == null ){
            return null;
        }
        float[] avgOffset = computeAvgDenseOffsetVector(wordPairs, disco);
        if( avgOffset == null ){
            return null;
        }
        float[] va1 = DenseVector.add(vb1, avgOffset);
        return similarWords(va1, dm, SimilarityMeasure.COSINE, 12);
    }
    
}
