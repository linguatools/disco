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
 * Basic vector algebra for dense vectors (float arrays).
 * @author peterkolb
 */
public class DenseVector {
    
    /**
     * Adds the two argument vectors.
     * @param v1 must have same length as <code>v2</code>
     * @param v2
     * @return <code>v1 + v2</code> or <code>null</code> if argument vectors have
     * different lengths.
     */
    public static float[] add(float[] v1, float[] v2){
        
        if( v1.length != v2.length ){
            return null;
        }
        
        float[] result = new float[v1.length];
        for( int i = 0; i < v1.length; i++ ){
            result[i] = v1[i] + v2[i];
        }
        return result;
    }
    
    /**
     * Subtracts the second vector from the first.
     * @param v1 must have same length as <code>v2</code>
     * @param v2
     * @return <code>v1 - v2</code> or <code>null</code> if argument vectors have
     * different lengths.
     */
    public static float[] sub(float[] v1, float[] v2){
        
        if( v1.length != v2.length ){
            return null;
        }
        
        float[] result = new float[v1.length];
        for( int i = 0; i < v1.length; i++ ){
            result[i] = v1[i] - v2[i];
        }
        return result;
    }
    
    /**
     * Element-wise vector multiplication.
     * @param v1 must have same length as <code>v2</code>
     * @param v2
     * @return resulting vector or <code>null</code> if argument vectors have
     * different lengths.
     */
    public static float[] mul(float[] v1, float[] v2){
        
        if( v1.length != v2.length ){
            return null;
        }
        
        float[] result = new float[v1.length];
        for( int i = 0; i < v1.length; i++ ){
            result[i] = v1[i] * v2[i];
        }
        return result;
    }
    
    /**
     * Multiply vector with scalar.
     * @param v1
     * @param scalarValue
     * @return 
     */
    public static float[] mul(float[] v1, float scalarValue){
        
        float[] result = new float[v1.length];
        for( int i = 0; i < v1.length; i++ ){
            result[i] = v1[i] * scalarValue;
        }
        return result;
    }
    
    /**
     * Compute the dot product between the argument vectors.
     * @param v1 must have same length as <code>v2</code>
     * @param v2
     * @return the dot product (scalar value). Throws a <code>RuntimeException</code>
     * if the argument vectors have different lengths!
     */
    public static float dotProduct(float[] v1, float[] v2){
        
        if( v1.length != v2.length ){
            throw new RuntimeException("[Vector.dotProduct] argument vectors have "
                    + "different length!");
        }
        
        float sp = 0.0F;
        for (int i = 0; i < v1.length; i++) {
            sp = sp + v1[i] * v2[i];
        }
        return sp;
    }
    
    /**
     * Choose for each dimension the highest absolute value.
     * @param v1 must have same length as <code>v2</code>
     * @param v2
     * @return extrema vector. Throws a <code>RuntimeException</code>
     * if the argument vectors have different lengths!
     */
    public static float[] vectorExtrema(float[] v1, float[] v2){
        
        if( v1.length != v2.length ){
            throw new RuntimeException("[Vector.vectorExtrema] argument vectors have "
                    + "different length!");
        }
        
        float[] result = new float[v1.length];
        for (int i = 0; i < v1.length; i++) {
            if( Math.abs(v1[i]) >= Math.abs(v2[i]) ){
                result[i] = v1[i];
            }else{
                result[i] = v2[i];
            }     
        }
        
        return result;
    }
    
    /**
     * Compute the average vector of all vectors in the list.
     * @param vectorList
     * @return average vector
     */
    public static float[] average(List<float[]> vectorList){
        
        if( vectorList == null || vectorList.isEmpty() ){
            return null;
        }
        if( vectorList.size() == 1 ){
            return vectorList.get(0);
        }
        
        float[] result = new float[vectorList.get(0).length];
        // sum up for all dimensions
        for( float[] v : vectorList ){
            if( v.length != result.length ){
                throw new RuntimeException("[Vector.average] argument vectors have "
                        + "different length!");
            }
            for (int i = 0; i < v.length; i++) {
                result[i] += v[i];   
            }
        }
        // divide each dimension's value by number of vectors
        for( int i = 0; i < result.length; i++ ){
            if( result[i] != 0 ){
                result[i] = (float) result[i] / vectorList.size();
            }
        }
        
        return result;
    }
    
    
}
