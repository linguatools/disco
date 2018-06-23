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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/*******************************************************************************
 * This class provides methods to compute the semantic similarity between two
 * short texts, i.e. phrases, sentences or paragraphs.
 * @author peter
 * @version 2.1
 ******************************************************************************/
public class TextSimilarity {

    /***************************************************************************
     * Computes the weight of a word according to the formula given in Jijkoun
     * & De Rijke 2005.
     * @param word a word
     * @param disco
     *  @return weight of word (between 0 and 1)
     **************************************************************************/
    private static float weight(String word, DISCO disco){

        // read the frequency of the word from the DISCO word space
        int freq = 0;
        try {
            freq = disco.frequency(word);
        } catch (IOException ex) {
            Logger.getLogger(TextSimilarity.class.getName()).log(Level.SEVERE, null, ex);
        }
        // ICF(word) = Freq(word) / N
        // ICFmin = minFreq / N
        // ICFmax = maxFreq / N
        // weight(word) = 1 - ((ICF(word)-ICFmin)/(ICFmax-ICFmin))
        float icf = (float) freq / disco.getTokenCount();
        float icfMin = (float) disco.getMinFreq() / disco.getTokenCount();
        float icfMax = (float) disco.getMaxFreq() / disco.getTokenCount();
        float w = 1 - (icf - icfMin) / (icfMax - icfMin);
        return w;
    }

    /***************************************************************************
     * Compute the semantic similarity of words w1 and w2 according to the
     * DISCO word space.
     * @param w1 a word
     * @param w2 another word
     * @param disco
     * @param similarityMeasure
     * @return similarity value between 0 and 1
     **************************************************************************/
    private static float wordSim(String w1, String w2, DISCO disco, 
            SimilarityMeasure similarityMeasure){
        
        // convert both words to lower case and compare them
        if ( w1.equalsIgnoreCase(w2) ){
            return (float) 1.0;
        }
        // if they are not equal then compute their semantic similarity
        float sim = (float) 0.0;
        try {
            sim = disco.semanticSimilarity(w1, w2, DISCO.getVectorSimilarity(
                    similarityMeasure));
            if( similarityMeasure == SimilarityMeasure.COSINE ){
                // map cosine range -1..1 to 0..1
                sim = sim/2.0F + 0.5F;
            }
        } catch (IOException ex) {
            System.out.println(ex);
        }
        return sim;
    }

    /***************************************************************************
     * Compute the semantic similarity of text and hypothesis according to the
     * algorithm given in Jijkoun &amp; De Rijke (2005): "Recognizing Textual Entailment
     * Using Lexical Similarity".<br>
     * The method tests if the hypothesis is licensed by the text.<br>
     * @param text a short piece of text
     * @param hypothesis another short text to be checked against the first one
     * @param disco
     * @param similarityMeasure use <code>SimilarityMeasure.COSINE</code> with
     * word spaces imported from word2vec.
     * @return similarity value between 0.0 and 1.0.
     * @throws CorruptConfigFileException
     * @throws java.io.IOException
     **************************************************************************/
    public static float directedTextSimilarity(String text, String hypothesis,
            DISCO disco, SimilarityMeasure similarityMeasure) throws 
            CorruptConfigFileException, IOException{

        float totalSim = 0.0F;
        float totalWeight = 0.0F;

        // store the stopword list from the DISCO word space in a Map
        HashMap stopHash = new HashMap();
        for( String s : disco.getStopwords() ) {
            stopHash.put(s, 1);
        }

        // store text in a vector, filter stop words
        ArrayList t = new ArrayList();
        String[] buf = text.split("[\\s]+");
        for (String b : buf) {
            if (!stopHash.containsKey(b)) {
                t.add(b);
            }
        }

        // store hypothesis in a vector, filter stop words
        ArrayList h = new ArrayList();
        String[] h_buf = hypothesis.split("[\\s]+");
        for (String hb : h_buf) {
            if (!stopHash.containsKey(hb)) {
                h.add(hb);
            }
        }

        // maybe all words in the text or the hypothesis were stop words
        // --> text or hypothesis empty --> return 0
        if( t.isEmpty() || h.isEmpty() ){
            return (float) 0.0;
        }

        // run through the words in the hypothesis
        float maxsim;
        float sim;
        float w;
        int max_k;
        int k;
        for(int i = 0; i < h.size(); i++){
            // bestimme maximale Ähnlichkeit von h[i] mit Wörtern aus T
            maxsim = (float) 0.0;
            max_k = -1;
            for(k = 0; k < t.size(); k++){
                sim = wordSim((String) h.get(i), (String) t.get(k), disco, 
                        similarityMeasure);
                if ( sim > maxsim ){
                    maxsim = sim;
                    max_k = k;
                }
            }
            if( max_k == -1 ){
                maxsim = (float) -1.0;
            }else{
                // entferne t[max_k] aus text1
                t.remove(max_k);
            }
            w = weight( (String) h.get(i), disco);
            totalSim = totalSim + maxsim * w;
            totalWeight = totalWeight + w;
        }
        return (float) totalSim / totalWeight;
    }

    /***************************************************************************
     * Computes the semantic similarity between the two texts as an average of
     * both directed text similarities.<br>
     * @param text1 a short piece of text
     * @param text2 another short piece of text
     * @param disco a DISCO word space
     * @param similarityMeasure use <code>SimilarityMeasure.COSINE</code> with
     * word spaces imported from word2vec.
     * @return similarity value between 0 and 1
     * @throws CorruptConfigFileException
     * @throws java.io.IOException
     **************************************************************************/
    public static float textSimilarity(String text1, String text2, DISCO disco,
            SimilarityMeasure similarityMeasure) 
            throws CorruptConfigFileException, IOException{

        return (directedTextSimilarity(text1, text2, disco, similarityMeasure) +
                directedTextSimilarity(text2, text1, disco, similarityMeasure)) / 2;
    }

}
