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

/**
 * This exception is thrown when a method that only works with word spaces of
 * type <code>WordspaceType.SIM</code> is called with a word space of type
 * <code>WordspaceType.COL</code>.
 * @author peter
 * @version 2.0
 */
public class WrongWordspaceTypeException extends Exception{
    
    public WrongWordspaceTypeException(){
        
    }
    
    public WrongWordspaceTypeException(String s){
        super(s);
    }
}
