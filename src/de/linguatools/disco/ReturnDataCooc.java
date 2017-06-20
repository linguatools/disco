/*******************************************************************************
 *   Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012 Peter Kolb
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

/*******************************************************************************
 * This class provides a data structure that is used as return value for the
 * method DISCO.cooccurrenceValues().
 * @author peter
 * @version 2.0
 * @deprecated 
 */
public class ReturnDataCooc {

    /**
     * The significance value between the two input words for the 
     * position/relation <code>position</code>.
     */
    public float sig;
    /**
     * The position or relation number, which is some integer between 0 and n 
     * (with n >= 0). The number indicates the relative position of the two
     * input words in the co-occurrence window, or the dependency relation
     * (depending on the context type that was chosen during word space 
     * building). If there is only one possible relation between the two words,
     * the position is always 0 (e.g. for word spaces were the exact position in
     * the co-occurrence window was not recorded, or word spaces with a 
     * word-by-document context).<br/>
     * For word spaces with a co-occurrence window recording exact position (the
     * standard setting in DISCOBuilder for semantic similarity) of the size x
     * words to the left and y words to the right, the position 0 corresponds to
     * the leftmost window position, the position 1 corresponds to the window
     * position x-1 words to the left, and the position x+y-1 corresponds to the
     * rightmost window position.
     */
    public int position;

    /**
     * Constructor.
     * @param s significance value
     * @param p position
     */
    public ReturnDataCooc(float s, int p){
        
        sig = s;
        position = p;
    }

}
