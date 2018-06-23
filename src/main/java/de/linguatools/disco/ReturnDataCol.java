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

/***************************************************************************
 * This class provides a data structure that is used as return value by several
 * methods in the DISCO package. 
 * @author peter
 * @version 2.0
 ***************************************************************************/
public class ReturnDataCol implements Comparable<ReturnDataCol>{
    
    public String word;
    public float value;
    public int relation;

    /**
     * Constructor 1/3.
     */
    ReturnDataCol() {
        
        word = "";
        value = (float) 0.0;
        relation = 0;
    }

    /**
     * Constructor 2/3.
     * @param w word
     * @param floatValue significance value 
     */
    public ReturnDataCol(String w, float floatValue) {
        
        word = w;
        value = floatValue;
    }

    /**
     * Constructor 3/3.
     * @param w word
     * @param floatValue significance value at position/relation <code>rel</code>
     * @param rel  position/relation
     */
    public ReturnDataCol(String w, float floatValue, int rel) {
        
        word = w;
        value = floatValue;
        relation = rel;
    }
    
    /*********************************************************************
     * Sortiert von groß nach klein. (descending)
     * @param other
     * @return
     */
    @Override
    public int compareTo(ReturnDataCol other){
    
        if( value < other.value ) return 1;
        if( value > other.value ) return -1;
        return 0;
    }
    
}
