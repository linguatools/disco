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

import java.util.List;

/**
 * This deals with ngrams stored in a DenseMatrix.
 * 
 * =============================================================================
 * Use fastText fork by GitHub user englhardt, as long as pull request 
 * https://github.com/facebookresearch/fastText/pull/289 not merged!
 * =============================================================================
 * 
 * @author peterkolb
 * @version 3.0
 */
public class Subword {
    
    /*
    
    print-ngrams
    ------------
    ./fasttext print-ngrams   gibt zuerst eine Zeile mit dem normalen Vektor für
    das Eingabewort aus:
    
        dann 1.6039 -0.49412 0.79227 0.85873 -0.77665 -1.2215 1.0841 -0.5211 0.18144
    
    Falls es OOV ist, besteht die Zeile aus lauter Nullen:
    
        Nase 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
    
    Danach kommen die Vektoren für die n-Gramme:
    
        <da -0.66894 0.14653 -1.2953 -0.66303 1.1439 0.72226 -0.35796 -0.35721
        <dan -0.037997 -0.60624 -0.19129 0.0050245 -1.1552 -0.51058 -1.09 -0.59276
        dann -1.0621 -2.3095 2.1543 -1.1765 1.7491 -0.39576 -0.033601 2.3951 0.20716
    
    Wie man sieht, unterscheidet sich der n-Gramm-Vektor für "dann" vom Wortvektor
    von "dann".
    
    Bestimmung der n-Gramme für ein Wort
    ------------------------------------
    first pad input word with <...>, then extract all n-grams from minN to maxN:
        <da     0..2  size 3
        <dan    0..3       4
        <dann   0..4       5
        <dann>  0..5       6
        dan     1..3       3
        dann    1..4       4
        dann>   1..5       5
        ann     2..4       3
        ann>    2..5       4
        nn>     3..5       3
    
    How to generate ngramVectorsFile from a binary fasttext model:
    --------------------------------------------------------------
    1. Print all n-gram vectors for each vocabulary word:
     cat wiki.de.vec | cut -d' ' -f 1 | sed 1d | ./fasttext print-ngrams 
           wiki.de.bin >all-words-and-ngrams.vec
       (this uses the fasttext fork from github.com:englhardt/fastText.git that
        was adapted to print a line containing "###_end_of_word_linguatools"
        after each word's ngrams.)
    ACHTUNG: die Ausgabedatei all-words-and-ngrams.vec wird extrem groß! Z.B.
      wiki.de.vec (2.275.234 Wörter): die ngramme für ein Viertel davon (522.492)
      benötigen 41G! Insgesamt also ~160G.
    2. parse-printed-ngrams.pl <all-words-and-ngrams.vec >nur-grams.vec
    3. sort -unique <nur-grams.vec >ngramVectorsFile
    
    */
    
    /**
     * Compute word embedding for out of vocabulary word.
     * @param oov out of vocabulary word
     * @param denseMatrix has to contain subword n-grams.
     * @return word embedding for out of vocabulary word <code>oov</code> computed
     * as the sum of all n-gram embeddings.
     * In case no n-gram from the <code>oov</code> word can be found in the 
     * <code>denseMatrix</code> the return array is all zeroes.
     */
    public static float[] getEmbeddingForOov(String oov, DenseMatrix denseMatrix){
        
        // generiere alle n-Gramme für oov
        List<String> ngrams = Ngrams.extractAllNGramsFromWord(oov, denseMatrix.getMinN(), 
                denseMatrix.getMaxN());
        // hole Vektoren für alle n-Gramme und berechne Summe
        float[] sumVector = new float[denseMatrix.numberOfFeatureWords()];
        for( String ngram : ngrams ){
            float[] ngramVector = denseMatrix.getNgramVector(ngram);
            if( ngramVector != null ){
                for( int i = 0; i < sumVector.length; i++){
                    sumVector[i] += ngramVector[i]; 
                }
            }
        }
        
        return sumVector;
    }
   
}
