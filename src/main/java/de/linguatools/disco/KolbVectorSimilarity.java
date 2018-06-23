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

import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.document.Document;

/**
 *
 * @author peter
 */
public class KolbVectorSimilarity implements VectorSimilarity{
    
    @Override
    public double computeSimilarity(float[] denseVector1, float[] denseVector2){
        
        if( denseVector1.length != denseVector2.length ){
            throw new RuntimeException("[KolbVectorSimilarity.computeSimilarity] input"
                    + " vectors have different lengths!");
        }
        
        float nenner = 0;
        float zaehler = 0;
        for( int k = 0; k < denseVector1.length; k++ ){
            nenner += denseVector1[k] + denseVector2[k];
            if( denseVector1[k] > 0 && denseVector2[k] > 0 ){
                zaehler += denseVector1[k] + denseVector2[k];
            }
        }
        return 2 * zaehler / nenner;  // DICE-KOEFFIZIENT !
    }
    
    /**
     * This method compares two word vectors from a DISCOLuceneIndex using the
     * similarity measure <code>SimilarityMeasures.KOLB</code> that is described
     * in the paper
     * <blockquote>Peter Kolb. <a href="http://hdl.handle.net/10062/9731">Experiments
     * on the difference between semantic similarity and relatedness</a>. In 
     * <i>Proceedings of the <a href="http://beta.visl.sdu.dk/nodalida2009/">17th
     * Nordic Conference on Computational Linguistics - NODALIDA '09</a></i>, 
     * Odense, Denmark, May 2009.</blockquote>
     * @param doc1 A document from the Lucene index containing the word vector
     * for word #1
     * @param doc2 A document from the Lucene index containing the word vector
     * for word #2
     * @return the similarity between the two word vectors; a value between 0.0F
     * and 1.0F.
     */
    @Override
    public double computeSimilarity(Document doc1, Document doc2){
        
        // Kollokationen von Wort #1 durchlaufen (über alle Relationen), in Hash 
        // speichern (nach Relationen unterschieden) und alle Werte addieren.
        HashMap colloHash = new HashMap();
        String[] wordsBuffer;
        String[] valuesBuffer;
        int i;
        float nenner = 0, v;
        wordsBuffer = doc1.get("kol").split(" ");
        valuesBuffer = doc1.get("kolSig").split(" ");
        for( i = 0; i < wordsBuffer.length; i++ ){
            v = Float.parseFloat(valuesBuffer[i]);
            colloHash.put(wordsBuffer[i], v );
            nenner += v;
        }
        // Kollokationen von Wort #2 durchlaufen (über alle Relationen), mit den
        // Kollokationen von Wort #1 im Hash vergleichen und ggf. die Werte zum
        // Zähler addieren und alle Werte zum Nenner addieren.
        float zaehler = 0;
        wordsBuffer = doc2.get("kol").split(" ");
        valuesBuffer = doc2.get("kolSig").split(" ");
        for( i = 0; i < wordsBuffer.length; i++ ){
            v = Float.parseFloat(valuesBuffer[i]);
            if ( colloHash.containsKey(wordsBuffer[i]) ){
                zaehler += v + (Float) colloHash.get(wordsBuffer[i]);
            }
            nenner += v;
        }
        return (double) 2 * zaehler / nenner;  // DICE-KOEFFIZIENT !
    }
    
    @Override
    public double computeSimilarity(Map<String,Float> mapVector1, 
            Map<String,Float> mapVector2){
     
        float nenner = 0;
        float zaehler = 0;
        for( String feature : mapVector1.keySet() ){
            nenner += mapVector1.get(feature);
            if( mapVector2.containsKey(feature) ){
                zaehler += mapVector1.get(feature) + mapVector2.get(feature);
            }
        }
        for( String feature : mapVector2.keySet() ){
            nenner += mapVector2.get(feature);
        }
        
        return (double) 2 * zaehler / nenner;
    }
}
