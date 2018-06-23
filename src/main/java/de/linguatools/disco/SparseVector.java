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
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Document;

/**
 * Methods for sparse vectors. A sparse vector is stored in an object of type 
 * <code>Map</code>.
 * @author peter
 */
public class SparseVector {
    
    /**
     * Convert a Document from a DISCOLuceneIndex (which stores a word's data) 
     * to a map vector.
     * @param doc
     * @return map vector representation of word vector from <code>doc</code>.
     */
    public static Map<String,Float> getMapVector(Document doc){
        
        Map<String,Float> mapVector = new HashMap<>();
        String[] wordsBuffer = doc.get("kol").split(" ");
        String[] valuesBuffer = doc.get("kolSig").split(" ");
        for( int i = 0; i < wordsBuffer.length; i++ ){
                mapVector.put(wordsBuffer[i], Float.parseFloat(valuesBuffer[i]));
        }
        return mapVector;
    }
    
    /**
     * Get the second order word vector from <code>doc</code> as a sparse vector.<br>
     * <b>Important:</b> this only works with documents from a <code>DISCOLuceneIndex</code>
     * of type <code>WordspaceType.SIM</code>. Documents from a word space of type
     * <code>COL</code> always return <code>null</code>.
     * @param doc
     * @return map vector representation of the second order word vector from
     * <code>doc</code>. The second order word vector are the distributionally 
     * most similar words for a word with their similarity scores. If <code>doc</code>
     * is from a word space of type <code>COL</code> then the return value is
     * <code>null</code>.
     */
    public static Map<String,Float> getSecondOrderMapVector(Document doc){
        
        Map<String,Float> mapVector = new HashMap<>();
        String[] wordsBuffer = doc.get("dsb").split(" ");
        String[] valuesBuffer = doc.get("dsbSim").split(" ");
        if( wordsBuffer == null || valuesBuffer == null ){
            return null;
        }
        for( int i = 0; i < wordsBuffer.length; i++ ){
                mapVector.put(wordsBuffer[i], Float.parseFloat(valuesBuffer[i]));
        }
        return mapVector;
    }
    
    /**
     * 
     * @param mapVector
     * @return the L2-norm of <code>mapVector</code>.
     */
    public static float norm(Map<String,Float> mapVector){
        
        float n = 0;
        for( String feature : mapVector.keySet() ){
            n += mapVector.get(feature) * mapVector.get(feature);
        }
        return (float)Math.sqrt(n);
    }
    
    /**
     * 
     * @param mapVector
     * @return <code>mapVector</code> converted to unit length. 
     */
    public static Map<String,Float> getNormalizedVector(Map<String,Float> mapVector){
        
        float norm = norm(mapVector);
        for( String feature : mapVector.keySet() ){
            mapVector.put(feature, (float)mapVector.get(feature) / norm);
        }
        return mapVector;
    }
    
    /**
     * Adds the two argument map vectors.
     * @param v1
     * @param v2
     * @return <code>v1 + v2</code>
     */
    public static Map<String,Float> add(Map<String,Float> v1, Map<String,Float> v2){
        
        Map<String,Float> result = new HashMap();
        for (String w : v1.keySet()) {
            if( !v2.containsKey(w) ){
                result.put(w, v1.get(w));
            }
        }
        for (String w : v2.keySet()) {
            if( v1.containsKey(w) ){
                result.put(w, v1.get(w) + v2.get(w));
            }else{
                result.put(w, v2.get(w));
            }
        }
        
        return result;
    }
    
    /**
     * Subtract the second word vector from the first.
     * @param v1
     * @param v2
     * @return <code>v1 - v2</code>
     */
    public static Map<String,Float> sub(Map<String,Float> v1, Map<String,Float> v2){
        
        Map<String,Float> result = new HashMap();
        for (String w : v1.keySet()) {
            if( !v2.containsKey(w) ){
                result.put(w, v1.get(w));
            }else{
                result.put(w, v1.get(w) - v2.get(w));
            }
        }
        for (String w : v2.keySet()) {
            if( !v1.containsKey(w) ){
                result.put(w, -v2.get(w));
            }
        }
        
        return result;
    }
    
    /**
     * Element-wise vector multiplication.
     * @param v1 
     * @param v2 
     * @return
     */
    public static Map<String,Float> mul(Map<String,Float> v1, Map<String,Float> v2){
        
        Map<String,Float> result = new HashMap();
        for (String feature : v1.keySet()) {
            if( v2.containsKey(feature) ){
                result.put(feature, v1.get(feature) * v2.get(feature));
            }
        }
        return result;
    }
    
    /**
     * Multiply vector with scalar. 
     * @param v
     * @param scalarValue
     * @return 
     */
    public static Map<String,Float> mul(Map<String,Float> v, float scalarValue){
        
        for (String w : v.keySet()) {
            v.put(w, v.get(w) * scalarValue);
        }
        return v;
    }
    
    /**
     * Compute the dot product (inner product, scalar product) of v1 and v2.
     * @param v1 
     * @param v2 
     * @return result (a scalar, not a vector)
     */
    public static float dotProduct(Map<String,Float> v1, Map<String,Float> v2){
        
        float sp = 0.0F;
        for (String w : v1.keySet()) {
            if( v2.containsKey(w) ){
                sp = sp + v1.get(w) * v2.get(w);
            }
        }
        return sp;
    }
    
    /**
     * Compute the average vector of all vectors in the list.
     * @param vectorList
     * @return average vector
     * @since 3.0
     */
    public static Map<String,Float> average(List<Map<String,Float>> vectorList){
        
        Map<String,Float> result = new HashMap();
        
        // sum up for all dimensions
        for( Map<String,Float> v : vectorList ){
            for( String w : v.keySet() ){
                if( result.containsKey(w) ){
                    result.put(w, v.get(w) + result.get(w));
                }else{
                    result.put(w, v.get(w));
                }
            }
        }
        // divide each dimension's value by number of vectors
        for( String w : result.keySet() ){
            result.put(w, (float) result.get(w) / (float)vectorList.size());
        }
        return result;
    }
    
    /**
     * Choose for each dimension the highest absolute value.
     * @param v1
     * @param v2
     * @return extrema vector.
     */
    public static Map<String,Float> vectorExtrema(Map<String,Float> v1, 
            Map<String,Float> v2){
        
        Map<String,Float> result = new HashMap();
        for (String w : v1.keySet()) {
            if( !v2.containsKey(w) ){
                result.put(w, v1.get(w));
            }else{
                if( Math.abs(v1.get(w)) >= Math.abs(v2.get(w)) ){
                    result.put(w, v1.get(w));
                }else{
                    result.put(w, v2.get(w));
                }
            }
        }
        for (String w : v2.keySet()) {
            if( !v1.containsKey(w) ){
                result.put(w, v2.get(w));
            }
        }
        
        return result;
    }
}
