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

package de.linguatools.disco;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;

/*******************************************************************************
 * This class provides methods that operate on sets of semantically similar 
 * words or collocations.
 * @author peter
 * @version 2.0
 */
public class Cluster {

    /***************************************************************************
     * This method takes the list of the n most similar words of the
     * input word and filters out all words that do not appear in the
     * similarity list of at least one of the other similar
     * words of the input word.<br/>
     * The resulting list of similar words will have size &lt;= n.<br/>
     * <b>Important note:</b> This method only works with word spaces
     * of type <code>DISCO.WordspaceType.SIM</code>.
     * @param disco DISCO word space of type <code>DISCO.WordspaceType.SIM</code>.
     * @param word input word (must be a single token).
     * @param n look in the list of the n most similar words of the input word
     * @return return data structure or null
     * @throws java.io.IOException
     * @throws WrongWordspaceTypeException if the <code>disco</code> word space
     * is not of type <code>DISCO.WordspaceType.SIM</code>.
     */
    public static ReturnDataBN filterOutliers(DISCO disco, String word, int n)
            throws IOException, WrongWordspaceTypeException{

        // check word space type
        if( disco.wordspaceType != DISCO.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + "to word spaces of type "+disco.wordspaceType);
        }
        
        // retrieve the n most similar words of the input word ...
        ReturnDataBN simW0;
        simW0 = disco.similarWords(word);
        if( simW0 == null ){
            System.out.println("Word \""+word+"\" not found in index.");
            return null;
        }
        // ... and save them in a Hash
        HashMap hash = new HashMap();
        for(int i = 0; i < simW0.words.length; i++ ){
            hash.put(simW0.words[i], 1);
            if( i >= (n-1) ) break;
        }
        // for each similar word w of the input word:
        //     retrieve its n most similar words and compare them to the words
        //     in the hash. If a word is found in the hash, set its value to 2.
        ReturnDataBN sim;
        for(int i = 0; i < simW0.words.length; i++ ){
            sim = disco.similarWords(simW0.words[i]);
            if( sim == null ) continue;
            for (String w : sim.words) {
                if (hash.containsKey(w)) {
                    hash.put(w, 2);
                }
            }
            if( i >= (n-1) ) break;
        }
        // how many words in the hash have a value of 2? Remove elements from the
        // set that have a smaller value than 2 in the hash.
        Set keys = hash.keySet();
        int a = 0;
        for(Iterator it = keys.iterator(); it.hasNext(); ){
            String w = (String) it.next();
            if( (Integer)hash.get(w) > 1 ){
                a++;
            }else{
                it.remove();
            }
        }
        // allocate String arrays with correct dimensionality
        String[] wordsResult = new String[a];
        String[] valuesResult = new String[a];
        // save all words from simW0 that have a value of 2 in the hash in the
        // output data structure
        a = 0;
        for(int i = 0; i < simW0.words.length; i++ ){
            if( (Integer)hash.get(simW0.words[i]) > 1 ){
                wordsResult[a] = simW0.words[i];
                valuesResult[a] = simW0.values[i];
                a++;
            }
            if( i >= (n-1) ) break;
        }
        ReturnDataBN res = new ReturnDataBN();
        res.words = wordsResult;
        res.values = valuesResult;
        return res;
    }

    /***************************************************************************
     * Retrieves the similar words for all the words in the input set
     * and extends the input set by all words that appear in the
     * similarity lists of all the input words. I.e. adds the
     * intersection of the similarity lists of the input words to
     * the input set.<br/>
     * This method is comparable to <a 
     * href="http://googlesystem.blogspot.de/2011/08/google-sets-will-be-shut-down.html">Google
     * Sets</a>.<br/>
     * <b>Important note:</b> This method only works with word spaces
     * of type <code>DISCO.WordspaceType.SIM</code>!
     * @param disco DISCO word space of type <code>DISCO.WordspaceType.SIM</code>.
     * @param inputSet set of input words (must be single tokens).
     * @return input set plus the similar words that were found (if any)
     * @throws java.io.IOException
     * @throws WrongWordspaceTypeException if the <code>disco</code> word space
     * is not of type <code>DISCO.WordspaceType.SIM</code>.
     */
    public static String[] growSet(DISCO disco, String[] inputSet) 
            throws IOException, WrongWordspaceTypeException{

        // check word space type
        if( disco.wordspaceType != DISCO.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + "to word spaces of type "+disco.wordspaceType);
        }
        
        // store the input set in a hash
        HashMap inputHash = new HashMap();
        for (String set : inputSet) {
            inputHash.put(set, 1);
        }
        // retrieve the similar words for all words in the input set and save
        // them in a hash.
        ReturnDataBN sim;
        HashMap hash = new HashMap();
        for (String set : inputSet) {
            sim = disco.similarWords(set);
            if( sim == null ) continue;
            for (String word : sim.words) {
                if (hash.containsKey(word)) {
                    int v = (Integer) hash.get(word);
                    hash.put(word, v + 1);
                } else {
                    hash.put(word, 1);
                }
            }
        }
        // for all keys in the hash: save to vector buffer if the hash value is
        // equal to the length of the input set, i.e. the word appeared in the
        // similarity lists of all the input words. Do not save if the word is in
        // the input set.
        ArrayList<String> buffer = new ArrayList<>();
        Set keys = hash.keySet();
        for(Iterator it = keys.iterator(); it.hasNext(); ){
            String w = (String) it.next();
            if( (Integer)hash.get(w) >= inputSet.length ){
                if( ! inputHash.containsKey(w) ){
                    buffer.add(w);
                }
            }
        }
        // return the input set plus the words from the buffer
        String[] res = new String[inputSet.length + buffer.size()];
        int i;
        for(i = 0; i < inputSet.length; i++ ){
            res[i] = inputSet[i];
        }
        for (String buffer1 : buffer) {
            res[i++] = (String) buffer1;
        }
        return res;
    }

    /***************************************************************************
     * Creates a sparse graph file that can be clustered with <a 
     * href="http://glaros.dtc.umn.edu/gkhome/cluto/cluto/overview">CLUTO</a>'s
     * <code>scluster</code> program.<br/>
     * <b>Important note:</b> This method only works with word spaces of type
     * <code>DISCO.WordspaceType.SIM</code>!
     * @param disco DISCO word space loaded into RAM. The word space has to be
     *              of type <code>DISCO.WordspaceType.SIM</code>.
     * @param n cluster the first n words in the word space index.
     * @param minSim create an edge between words that have a similarity value
     * of at least <code>minSim</code>.
     * @param outputDir output directory. Two files are created in the output
     * directory <code>outputDir</code>: <code>sparseGraph.dat</code> and
     * <code>rowLabels.dat</code>. Existing files with these names are 
     * overwritten.
     * @throws CorruptIndexException
     * @throws IOException
     * @throws WrongWordspaceTypeException if the <code>disco</code> word space
     * is not of type <code>DISCO.WordspaceType.SIM</code>.
     */
    public void clutoClusterSimilarityGraph(DISCO disco, int n, float minSim,
            String outputDir)
            throws CorruptIndexException, IOException, WrongWordspaceTypeException{

        // check word space type
        if( disco.wordspaceType != DISCO.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + "to word spaces of type "+disco.wordspaceType);
        }
        
        // erzeuge einen IndexReader fuer das indexDir
        IndexReader ir = DirectoryReader.open(disco.indexRAM);

        // Hole Anzahl Dokumente im Index
        int N = ir.numDocs();

        if( n > N ){
            System.out.println("Error: there are only "+N+" words in the index.");
            return;
        }

        // erzeuge Mapping word --> ID
        // for every word (Lucene document) from id 0 to n
        System.out.println("create word-ID mapping for first "+n+" words in index...");
        HashMap<String, Integer> wordIDHash = new HashMap<>();
        for(int i = 0; i < n; i++){
            Document doc = ir.document(i);
            wordIDHash.put(doc.get("word"), i+1);
            if( i % 10 == 0 ){
                System.out.print("\r"+i);
            }
        }
        System.out.println("   OK.");
        System.out.flush();

        // create output file (CLUTO's sparse graph format)
        FileWriter sparseMatrixFileWriter = new FileWriter(outputDir +
                File.separator + "sparseGraph.dat");
        // öffne Ausgabedatei (row labels)
        FileWriter rowLabelsFileWriter = new FileWriter(outputDir +
                File.separator + "rowLabels.dat");

        // for every word (Lucene document) from id 0 to n
        System.out.println("create similarity graph for first "+n+" words...");
        int emptyRows = 0;
        int numberOfEntries = 0;
        for(int i = 0; i < n; i++){
            // retrieve similar words with their similarity values
            Document doc = ir.document(i);
            String[] dsb = doc.get("dsb").split(" ");
            String[] dsbSim = doc.get("dsbSim").split(" ");
            // for every similar word dsb[s]
            boolean first = true;
            for(int s = 0; s < dsb.length; s++){
                // if the word has a similarity lower than minSim, process the
                // next word (the words in dsb are ordered by similarity)
                if( Float.parseFloat("0."+dsbSim[s]) < minSim) break;
                // only use words that are among the first n
                if( !wordIDHash.containsKey(dsb[s])) break;
                // write pair <wordID, sim>
                if( first ){
                    sparseMatrixFileWriter.write(wordIDHash.get(dsb[s])+" 0."+dsbSim[s]);
                    first = false;
                }else{
                    sparseMatrixFileWriter.write(" "+wordIDHash.get(dsb[s])+" 0."+dsbSim[s]);
                }
                numberOfEntries++;
            }
            sparseMatrixFileWriter.write("\n");
            rowLabelsFileWriter.write(doc.get("word")+"\n");
            if( first ) emptyRows++;
            // Info
            if( i % 10 == 0 ){
                System.out.print("\r"+i);
            }
        }
        System.out.println("   OK.\nempty rows = "+emptyRows);
        System.out.println("numberOfVertices = "+n);
        System.out.println("numberOfEntries = "+numberOfEntries);
        sparseMatrixFileWriter.close();
        rowLabelsFileWriter.close();
    }

    /***************************************************************************
     * Creates sparse matrix file for use with <a 
     * href="http://glaros.dtc.umn.edu/gkhome/cluto/cluto/overview">CLUTO</a>'s
     * <code>vcluster</code> program. For every word in the word list its word
     * vector is retrieved from the DISCO index and written to the sparse matrix
     * file. A row label file is also created that maps the row numbers to the
     * words.
     * @param disco DISCO word space loaded into RAM. The word space may be of
     *              any type.
     * @param wordList list of words to be clustered.
     * @param outputDir output directory. Two files are created in the output
     * directory <code>outputDir</code>: <code>sparseMatrix.dat</code> and
     * <code>rowLabels.dat</code>. Existing files with these names are 
     * overwritten.
     * @throws IOException
     */
    public void clutoClusterVectors(DISCO disco, ArrayList<String> wordList,
            String outputDir) throws IOException{

        // öffne Ausgabedatei (sparse matrix)
        FileWriter sparseMatrixFileWriter = new FileWriter(outputDir +
                File.separator + "sparseMatrix.dat");
        // öffne Ausgabedatei (row labels)
        FileWriter rowLabelsFileWriter = new FileWriter(outputDir +
                File.separator + "rowLabels.dat");

        HashMap<String, Integer> featureHash = new HashMap<>();
        int fNr = 1;
        int rowNumber = 0;
        int numberOfEntries = 0;
        int emptyRows = 0;

        // für jedes Wort in der Wort-Liste den Wortvektor holen
        System.out.println("Creating word vectors for "+wordList.size()+" words");
        for (String wordList1 : wordList) {
            HashMap<String, Float> wv = disco.getWordvector(wordList1);
            if (wv == null) {
                System.out.println("word " + wordList1 + " not found in index" + " -- word ignored");
                continue;
            }
            if( wv.isEmpty()) emptyRows++;
            // for every entry (feature) in the word vector
            boolean first = true;
            for (String feature : wv.keySet()) {
                // speichere feature (=word + relation) in Hash
                int m;
                if( featureHash.containsKey(feature)){
                    m = featureHash.get(feature);
                }else{
                    featureHash.put(feature, fNr);
                    m = fNr;
                    fNr++;
                }
                // Paar <fNr, value> ausgeben
                if( first == true){
                    sparseMatrixFileWriter.write(m+" "+wv.get(feature));
                    first = false;
                }else{
                    sparseMatrixFileWriter.write(" "+m+" "+wv.get(feature));
                }
                numberOfEntries++;
            }
            // Zeile (Dokument) beenden
            sparseMatrixFileWriter.write("\n");
            // Wort als Label in row labels Datei schreiben
            rowLabelsFileWriter.write(wordList1 + "\n");
            rowNumber++;
            // Info ausgeben
            if( rowNumber % 10 == 0 ){
                System.out.print("\r"+rowNumber);
            }
        }
        sparseMatrixFileWriter.close();
        rowLabelsFileWriter.close();
        System.out.println("\nSparse matrix and labels written (emptyRows = " +
                ""+emptyRows+")");
        System.out.println("Please verify if the first line of the output file "
                + "\"sparseMatrix.dat\" contains the following values:");
        System.out.println("NumberOfRows = "+(rowNumber-1));
        System.out.println("NumberOfColumns = "+featureHash.size());
        System.out.println("NumberOfNonZeroEntries = "+numberOfEntries);
    }
}
