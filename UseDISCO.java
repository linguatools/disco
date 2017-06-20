
import de.linguatools.disco.CorruptConfigFileException;
import de.linguatools.disco.DISCO;
import de.linguatools.disco.ReturnDataBN;
import de.linguatools.disco.ReturnDataCol;
import de.linguatools.disco.WrongWordspaceTypeException;
import java.io.FileNotFoundException;
import java.io.IOException;

/*******************************************************************************
 *   Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2015 Peter Kolb
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

/**
 * sample code to show the use of the DISCO Java API version 2.0.
 * @author peter
 * @version 2.0
 */
public class UseDISCO {
    
    /************************************************************************
     * Call with: java UseDISCO WORDSPACE-DIR WORD
     * Make sure that disco-2.0.jar is in the classpath.
     * The word space must be of type DISCO.WordspaceType.SIM.
     * Set the JVM heap size to be larger than the word space that
     * will be loaded into RAM, otherwise an OutOfMemoryError will be thrown!
     * @param args
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException{

        // first command line argument is path to the DISCO word space directory
        String discoDir = args[0];
        // second argument is the input word
        String word = args[1];

	/****************************************
	 * create instance of class DISCO.      *
	 * Do NOT load the word space into RAM. *
	 ****************************************/
        DISCO disco;
        try {
            disco = new DISCO(discoDir, false);
        } catch (FileNotFoundException | CorruptConfigFileException ex) {
            System.out.println("Error creating DISCO instance: "+ex);
            return;
        }

        // is the word space of type "sim"?
        if( disco.wordspaceType != DISCO.WordspaceType.SIM ){
           System.out.println("The word space "+discoDir+" is not of type SIM!");
           return; 
        }
        
        // retrieve the frequency of the input word
        int freq = disco.frequency(word);
        // and print it to stdout
        System.out.println("Frequency of "+word+" is "+freq);

        // end if the word wasn't found in the index
        if(freq == 0) return;

        // retrieve the collocations for the input word
        ReturnDataCol[] collocationResult = disco.collocations(word);
        // and print the first 20 to stdout
        System.out.println("Collocations:");
        for(int i = 1; i < collocationResult.length; i++){
            System.out.println("\t"+collocationResult[i].word+"\t"+
                    collocationResult[i].value);
	    if( i >= 20 ) break;
        }

        // retrieve the most similar words for the input word
        ReturnDataBN simResult;
        try {
            simResult = disco.similarWords(word);
        } catch (WrongWordspaceTypeException ex) {
            System.out.println("Error retrieving most similar words: "+ex);
            return;
        }
        // and print the first 20 of them to stdout
        System.out.println("Most similar words:");
        for(int i = 1; i < simResult.words.length; i++){
            System.out.println("\t"+simResult.words[i]+"\t"+simResult.values[i]);
	    if( i >= 20 ) break;
        }

        // compute second order similarity between the input word and its most
        // similar words
	System.out.println("Computing second order similarity between "+word+
			   " and all of its similar words...");
	long startTime = System.currentTimeMillis();
        for(int i = 1; i < simResult.words.length; i++){
            try {
                float s2 = disco.secondOrderSimilarity(word, simResult.words[i]);
            } catch (WrongWordspaceTypeException ex) {
                System.out.println("Error computing second order similarity: "+ex);
                return;
            }
	}
	long endTime = System.currentTimeMillis();
	long elapsedTime = endTime - startTime;
	System.out.println("OK. Computation took "+elapsedTime+" ms.");

	/**********************************************
	 * Create another DISCO instance,             *
	 * this time loading the word space into RAM. *
	 **********************************************/
	System.out.println("Trying to load word space into RAM...\n"+
			 "(in case of OutOfMemoryError: increase JVM "+
			 "heap space to size of word space directory!)");
	startTime = System.currentTimeMillis();
        DISCO discoRAM;
        try {
            discoRAM = new DISCO(discoDir, true);
        } catch (FileNotFoundException | CorruptConfigFileException ex) {
            System.out.println("Error creating DISCO instance: "+ex);
            return;
        }
	endTime = System.currentTimeMillis();
	long elapsedTimeLoad = endTime - startTime;
	System.out.println("OK (loading to RAM took "+elapsedTimeLoad+" ms)");

        // compute second order similarity between the input word and its most
        // similar words in RAM
	System.out.println("Computing second order similarity between "+word+
			   " and all of its similar words in RAM...");
	startTime = System.currentTimeMillis();
        for(int i = 1; i < simResult.words.length; i++){
            try {
                float s2 = discoRAM.secondOrderSimilarity(word, simResult.words[i]);
            } catch (WrongWordspaceTypeException ex) {
                System.out.println("Error computing second order similarity: "+ex);
                return;
            }
	}
	endTime = System.currentTimeMillis();
	long elapsedTimeRAM = endTime - startTime;
	System.out.println("OK. Computation took "+elapsedTimeRAM+" ms in RAM.");
	if( elapsedTimeRAM >= elapsedTime ){
	    System.out.println("Maybe your system had to swap to disk?");
        }
    }
}
