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
 * Computes the cosine similarity between two vectors.
 * @author peter
 */
public class CosineVectorSimilarity implements VectorSimilarity{
    
    @Override
    public double computeSimilarity(float[] denseVector1, float[] denseVector2){
        
        if( denseVector1.length != denseVector2.length ){
            throw new RuntimeException("[CosineVectorSimilarity.computeSimilarity] input"
                    + " vectors have different lengths!");
        }
        
        float nenner1 = 0;
        float nenner2 = 0;
        float zaehler = 0;
        for( int k = 0; k < denseVector1.length; k++ ){
            nenner1 += denseVector1[k] * denseVector1[k];
            nenner2 += denseVector2[k] * denseVector2[k];
            zaehler += denseVector1[k] * denseVector2[k];
        }
        return (double) (zaehler / Math.sqrt(nenner1 * nenner2));
    }
    
    @Override
    public double computeSimilarity(Map<String,Float> mapVector1, 
            Map<String,Float> mapVector2){
     
        float nenner1 = 0;
        float zaehler = 0;
        for( String feature : mapVector1.keySet() ){
            nenner1 += mapVector1.get(feature) * mapVector1.get(feature);
            if( mapVector2.containsKey(feature) ){
                zaehler += mapVector1.get(feature) * mapVector2.get(feature);
            }
        }
        float nenner2 = 0;
        for( String feature : mapVector2.keySet() ){
            nenner2 += mapVector2.get(feature) * mapVector2.get(feature);
        }
        
        return (double) (zaehler / Math.sqrt(nenner1 * nenner2));
    }
    
    @Override
    public double computeSimilarity(Document doc1, Document doc2){
        
        // Kollokationen von Wort #1 durchlaufen (über alle Relationen), in Hash 
        // speichern (nach Relationen unterschieden) und alle Werte addieren.
        HashMap<String,Float> colloHash = new HashMap();
        String[] wordsBuffer;
        String[] valuesBuffer;
        int i;
        float nenner1 = 0, v;
        wordsBuffer = doc1.get("kol").split(" ");
        valuesBuffer = doc1.get("kolSig").split(" ");
        for( i = 0; i < wordsBuffer.length; i++ ){
            v = Float.parseFloat(valuesBuffer[i]);
            colloHash.put(wordsBuffer[i], v );
            nenner1 += v * v;
        }
        // Kollokationen von Wort #2 durchlaufen (über alle Relationen), mit den
        // Kollokationen von Wort #1 im Hash vergleichen und ggf. die Werte zum
        // Zähler addieren und alle Werte zum Nenner addieren.
        float nenner2 = 0;
        float zaehler = 0;
        wordsBuffer = doc2.get("kol").split(" ");
        valuesBuffer = doc2.get("kolSig").split(" ");
        for( i = 0; i < wordsBuffer.length; i++ ){
            v = Float.parseFloat(valuesBuffer[i]);
            if ( colloHash.containsKey(wordsBuffer[i]) ){
                zaehler += v * colloHash.get(wordsBuffer[i]);
            }
            nenner2 += v * v;
        }
        return (double) (zaehler / Math.sqrt(nenner1 * nenner2));
    }
}
