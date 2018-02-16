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

import de.linguatools.disco.DISCO.SimilarityMeasure;
import de.linguatools.disco.Rank.WordAndRank;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.lucene.index.CorruptIndexException;

/*******************************************************************************
 * This class provides the command line interface for DISCO.
 * @author peter
 * @version 2.1
 *******************************************************************************/
public class Main{
    
    
    /*******************************************************************
     * Print usage information.
     *******************************************************************/
    private static void printUsage(){
        System.out.println("disco V2.1 -- www.linguatools.de/disco");
        System.out.println("Usage: java -jar disco-2.1.jar <indexDir> <option>");
        System.out.println("Options:   NOTE THAT <w>, <w1>, <w2> have to be single tokens!");
        System.out.println("\t\t-f <w>\t\treturn corpus frequency of word <w>");
        System.out.println("\t\t-s <w1> <w2> <simMeasure>\treturn semantic similarity between words <w1> and <w2>");
        System.out.println("\t\t             simMeasure = {COSINE, KOLB}, default is COSINE.");
        System.out.println("\t\t-s2 <w1> <w2>\treturn second order similarity between words <w1> and <w2>");
        System.out.println("\t\t\t\tDoes not work with word spaces of type \"COL\"!");
        System.out.println("\t\t-cv <w1> <w2>\treturn collocational value between words <w1> and <w2>");
        System.out.println("\t\t-bn <w> <n>\treturn the <n> most similar words for word <w>");
        System.out.println("\t\t\t\tDoes not work with word spaces of type \"COL\"!");
        System.out.println("\t\t-bs <w> <s>\treturn all words that are at least <s> similar to word <w>");
        System.out.println("\t\t\t\tDoes not work with word spaces of type \"COL\"!");
        System.out.println("\t\t-bc <w> <n>\treturn the <n> best collocations for word <w>");
        System.out.println("\t\t-cc <w1> <w2>\treturn the common context for <w1> and <w2>");
        System.out.println("\t\t-n\t\treturn the number of words in the index");
        System.out.println("\t\t-wl <file>\t\twrite word frequency list to file");
        // compositional
        System.out.println("\t\t-cs \"<p1>\" \"<p2>\"\tcompute semantic similarity between multi-word terms or phrases"
                + "\n\t\t\t<p1> and <p2> using vector composition");
        // file input
        System.out.println("\t\t-ds <inputFile>\toutput semantic similarity for all word pairs in input file");
        System.out.println("\t\t-dr <inputFile>\toutput rank of w2 in similarity list of w1 for all word pairs in input file");
        System.out.println("\t\t-dbn <inputFile>\toutput all words with similarity >= 0.01 for every word in input file");
        System.out.println("\t\t-dbc <inputFile>\toutput collocations for every word in input file");
        // text similarity
        System.out.println("\t\t-ts \"<text1>\" \"<text2>\" <simMeasure>\tcompute semantic relatedness between <text1> and <text2>");
        System.out.println("\t\t-tsd \"<text>\" \"<hypothesis>\" <simMeasure>\tcompute the directed " +
                "semantic relatedness between the <text>\n\t\t\tand the <hypothesis>");
        // clusters
        System.out.println("\t\t-fo <w> <n>\tfilter outliers from the similar words of word <w>");
        System.out.println("\t\t\t\tDoes not work with word spaces of type \"COL\"!");
        System.out.println("\t\t-gs <w1> <w2> ... <wN>\tgrow set of input words");
        System.out.println("\t\t\t\tDoes not work with word spaces of type \"COL\"!");
        // ranks
        System.out.println("\t\t-rs <w1> <w2> ... <wN>\tfind words for which the input words rank highest (sim)");
        System.out.println("\t\t\t\tDoes not work with word spaces of type \"COL\"!");
        System.out.println("\t\t-rc <w1> <w2> ... <wN>\tfind words for which the input words rank highest (col)");
        // CLUTO
        System.out.println("\t\t-cg <n> <minSim> <outputDir>\tcreates sparse graph file (for the first n words) that\n\t\t\t"
                + " can be clustered with CLUTO's scluster program.");
        System.out.println("\t\t\t\tDoes not work with word spaces of type \"COL\"!");
        System.out.println("\t\t-cv <wordlist> <outputDir>\tcreates sparse matrix file (with word vector for every\n\t\t\t"
                + " word in wordlist) that can be clustered with CLUTO's vcluster program.");
    }
  
    
    /*******************************************************************************
     * Process all word pairs in the input file. For the options -ds and
     * -dr the input file format is one word pair per line, with the two words
     * separated by a white space. For the options -dbn and -dbc the input file
     * format is a single word per line.<br/>
     * Options:<br/>
     * -ds: compute semantic similarity of word pairs<br/>
     * -dbn: output all words with similarity >= 0.01 for every word in input file<br/>
     * -dbc: output collocations for every word in input file<br/>
     * -dr: compute rank of w2 in similarity list of w1<br/>
     * The output is written into the file "INPUT-FILE.SUFFIX" where SUFFIX is
     * "sim", "rank", "bn" or "bc".<br/>
     * If a word can not be found in the index, the word pair receives the
     * similarity 0, and a warning is printed to the standard error stream.
     * @param indexName path to the index directory (DISCO word space)
     * @param inputFile input file
     * @param s
     *******************************************************************************/
    private static void readFile(String indexName, File inputFile, int s) 
            throws IOException, FileNotFoundException, CorruptIndexException, 
            CorruptConfigFileException{
        
        // open input file
        BufferedReader br = new BufferedReader(new FileReader(inputFile.getCanonicalPath()));
        // output file suffix
        String suffix = "";
        if ( s == 1 ) suffix = ".sim";
        else if ( s == 3 ) suffix = ".bn";
        else if ( s == 4 ) suffix = ".bc";
        else if ( s == 5 ) suffix = ".rank";
        String outputName = inputFile.getAbsolutePath() + suffix;
        File outputFile = new File(outputName);
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile.getCanonicalPath()));
        // create class DISCO and load word space into RAM
        DISCO d = new DISCO(indexName, true);
        // Rank?
        Rank rank = null;
        if( s == 5 || s == 6 ){
            rank = new Rank();
        }
        String line;
        String[] segs;
        int i = 0;
        while( (line = br.readLine()) != null ){
            i++;
            line = line.trim();
            segs = line.split("[ \\s\\t]+");
            if( segs[0].equals("") || segs[1].equals("") ){
                System.err.println("Warning: wrong format in line "+i+" -- ignored.");
                continue;
            }
            try {
                System.out.println("segs[0]: "+segs[0]+", segs[1]: "+segs[1]);
                    float sim = 0;
                    if( s == 1 ) sim = d.semanticSimilarity(segs[0], segs[1]);
                    else if ( s == 3 ){
                        ReturnDataBN res = d.similarWords(segs[0]);
                        if ( res == null ){ 
                            System.out.println("The word \""+segs[0]+"\" was not found."); 
                            continue;
                        }
                        float schwellwert = (float) 0.01;
                        bw.write(segs[0]);
                        for(int k = 1; k < res.words.length; k++){
                            if( Float.parseFloat(res.values[k]) < schwellwert ) break;
                            bw.write(" "+res.words[k]+res.values[k]);
                        }
                        bw.newLine();
                        continue;
                    }
                    else if ( s == 4 ){
                        ReturnDataCol[] res;
                        res = d.collocations(segs[0]);
                        if ( res == null ){ 
                            System.out.println("The word \""+segs[0]+"\" was not found."); 
                            continue; 
                        }
                        bw.write(segs[0]);
                    for (ReturnDataCol re : res) {
                        bw.write(" " + re.word + " " + re.value);
                    }
                        bw.newLine();
                        continue;
                    }
                    // rank for semantic similarity
                    else if ( s == 5 ){
                        int r = rank.rankSim(d, segs[0], segs[1]);
                        bw.write(segs[0]+" "+segs[1]+" "+r);
                        bw.newLine();
                        continue;
                    }
                    if ( sim == -1 ){
                        System.err.println("Line "+i+": Word not found in index.");
                        bw.write(segs[0]+" "+segs[1]+" 0");
                        bw.newLine();
                    }else{
                        System.out.println("Result written to file.");
                        bw.write(segs[0]+" "+segs[1]+" "+sim);
                        bw.newLine();
                    }
            } catch (IOException ex) {
                System.out.println("Error: IOException: "+ex);
            } catch (WrongWordspaceTypeException ex) {
                System.out.println("Error: Wrong wordspace type: only works "
                            + "with wordspaces of type SIM!");    
            }
        }
        bw.close();
    }
    
    /***************************************************************************
     * Main method. Invoke from command line. For command line options type
     * "java -jar disco-2.0.jar".
     * For more information consult the documentation or visit the website at
     * www.linguatools.de/disco/.
     * @param args
     * @throws java.io.IOException
     * @throws java.io.FileNotFoundException
     * @throws org.apache.lucene.index.CorruptIndexException
     * @throws de.linguatools.disco.CorruptConfigFileException
     **************************************************************************/
    public static void main(String[] args) throws IOException, 
            FileNotFoundException, CorruptIndexException, 
            CorruptConfigFileException{
        
        if (args.length < 2) {
            printUsage();
        }else{
            // first argument must be index directory
            File indexDir = new File(args[0]);
            if( ! indexDir.isDirectory() ){
                System.out.println("Error: can't open directory "+args[0]);
                printUsage();
                return;
            }       
            //////////////////////////////////////////            
            // -f <w>: return frequency of word <w> //
            //////////////////////////////////////////
            if( args[1].equals("-f") ){
                if ( args[2] == null ){
                    printUsage();
                    return;
                }
                try {
                    DISCO d = new DISCO(args[0], false);
                    int freq = d.frequency(args[2]);
                    System.out.println(freq);
                } catch (IOException ex) {
                    System.out.println("Error: IOException: "+ex);
                }
            }
            ///////////////////////////////////////////////////////////////////////////////////////
            // -s <w1> <w2> <similarityMeasure>: return similarity between words <w1> and <w2> //
            ///////////////////////////////////////////////////////////////////////////////////////
            else if( args[1].equals("-s") ){
                if ( args.length < 5 ){
                    printUsage();
                    return;
                }
                if ( args[2] == null || args[3] == null || args[4] == null ){
                    printUsage();
                    return;
                }
                SimilarityMeasure measure = DISCO.getSimilarityMeasure(args[4]);
                if( measure == null ){
                    System.out.println("Error: unknown similarity measure: "+args[4]);
                    return;
                }
                try {
                    DISCO d = new DISCO(args[0], false);
                    float sim = d.semanticSimilarity(args[2], args[3], measure);
                    if ( sim == -2.0F ){
                        System.out.println("Error: Word not found in index.");
                    }else if( sim == -3.0F ){
                        System.out.println("Error: unknown similarity measure: "+args[4]);
                    }else{
                        System.out.println(sim);
                    }
                } catch (IOException ex) {
                    System.out.println("Error: IOException: "+ex);
                }  
            }
            ///////////////////////////////////////////////////////////////////////////////            
            // -s2 <w1> <w2>: return second order similarity between words <w1> and <w2> //
            ///////////////////////////////////////////////////////////////////////////////
            else if( args[1].equals("-s2") ){
                if ( args.length < 4 ){
                    printUsage();
                    return;
                }
                if ( args[2] == null || args[3] == null ){
                    printUsage();
                    return;
                }
                try {
                    DISCO d = new DISCO(args[0], false);
                    float sim = d.secondOrderSimilarity(args[2], args[3]);
                    if ( sim == -1 ){
                        System.out.println("Error: Word not found in index.");
                    }else{
                        System.out.println(sim);
                    }
                } catch (IOException ex) {
                    System.out.println("Error: IOException: "+ex);
                } catch (WrongWordspaceTypeException ex) {
                    System.out.println("Error: Wrong wordspace type: only works "
                            + "with wordspaces of type SIM!");
                }
            }
            ///////////////////////////////////////////////////////////////////////////            
            // -cv <w1> <w2>: return collocational value between words <w1> and <w2> //
            ///////////////////////////////////////////////////////////////////////////
            else if( args[1].equals("-cv") ){
                if ( args.length < 4 ){
                    printUsage();
                    return;
                }
                if ( args[2] == null || args[3] == null ){
                    printUsage();
                    return;
                }
                try {
                    DISCO d = new DISCO(args[0], false);
                    float sig = d.collocationalValue(args[2], args[3]);
                    System.out.println(sig);
                } catch (IOException ex) {
                    System.out.println("Error: IOException: "+ex);
                }
            }
            /////////////////////////////////////////////////////////////////            
            // -bn <w> <n>: return the <n> most similar words for word <w> //
            /////////////////////////////////////////////////////////////////
            else if( args[1].equals("-bn") ){
                if ( args.length < 4 ){
                    printUsage();
                    return;
                }
                if ( args[2] == null || args[3] == null ){
                    printUsage();
                    return;
                }
                try {
                    DISCO d = new DISCO(args[0], false);
                    ReturnDataBN res = d.similarWords(args[2]);
                    if ( res == null ){ 
                        System.out.println("The word \""+args[2]+"\" was not found."); 
                        return; 
                    }
                    int n = Integer.parseInt(args[3]) - 1;
                    for(int k = 0; k < res.words.length; k++){
                        System.out.println(res.words[k]+"\t"+res.values[k]);
                        if( k >= n ) break;
                    }
                } catch (IOException ex) {
                    System.out.println("Error: IOException: "+ex);
                } catch (WrongWordspaceTypeException ex) {
                    System.out.println("Error: Wrong wordspace type: only works "
                            + "with wordspaces of type SIM!");
                }
            }
            ////////////////////////////////////////////////////////////////////////            
            // -bs <w> <s>: return all words that are at least <s> similar to <w> //
            ////////////////////////////////////////////////////////////////////////
            else if( args[1].equals("-bs") ){
                if ( args.length < 4 ){
                    printUsage();
                    return;
                }
                if ( args[2] == null || args[3] == null ){
                    printUsage();
                    return;
                }
                try {
                    DISCO d = new DISCO(args[0], false);
                    ReturnDataBN res = d.similarWords(args[2]);
                    if ( res == null ){ 
                        System.out.println("The word \""+args[2]+"\" was not found."); 
                        return; 
                    }
                    float s = Float.parseFloat(args[3]);
                    for(int k = 0; k < res.words.length; k++){
                        if( Float.parseFloat(res.values[k]) < s ) break;
                        System.out.println(res.words[k]+"\t"+res.values[k]);
                    }
                } catch (IOException ex) {
                    System.out.println("Error: IOException: "+ex);
                } catch (WrongWordspaceTypeException ex) {
                    System.out.println("Error: Wrong wordspace type: only works "
                            + "with wordspaces of type SIM!");
                }
            }
            ////////////////////////////////////////////////////////////////            
            // -bc <w> <n>: return the <n> best collocations for word <w> //
            ////////////////////////////////////////////////////////////////
            else if( args[1].equals("-bc") ){
                if ( args.length < 4 ){
                    printUsage();
                    return;
                }
                if ( args[2] == null || args[3] == null ){
                    printUsage();
                    return;
                }
                ReturnDataCol[] res;
                try {
                    DISCO d = new DISCO(args[0], false);
                    res = d.collocations(args[2]);
                    if ( res == null ){ 
                        System.out.println("The word \""+args[2]+"\" was not found."); 
                        return; 
                    }
                    int n = Integer.parseInt(args[3]) - 1;
                    for(int k = 0; k < res.length; k++){
                        System.out.println(res[k].word+"\t"+res[k].value);
                        if( k >= n ) break;
                    }
                } catch (IOException ex) {
                    System.out.println("Error: IOException: "+ex);
                }
            }
            ////////////////////////////////////////////////////////////////            
            // -cc <w1> <w2>: return the common context for <w1> and <w2> //
            ////////////////////////////////////////////////////////////////
            else if( args[1].equals("-cc") ){
                if ( args.length < 4 ){
                    printUsage();
                    return;
                }
                if ( args[2] == null || args[3] == null ){
                    printUsage();
                    return;
                }
                // fetch the collocations (= context) for the first word and save
                // them with their values in a hash
                ReturnDataCol[] res;
                HashMap colloHash = new HashMap();
                try {
                    DISCO d = new DISCO(args[0], false);
                    res = d.collocations(args[2]);
                    if ( res == null ){ 
                        System.out.println("The word \""+args[2]+"\" was not found."); 
                        return; 
                    }
                    for (ReturnDataCol re : res) {
                        colloHash.put(re.word, re.value);
                    }
                } catch (IOException ex) {
                    System.out.println("Error: IOException: "+ex);
                }
                // fetch the collocations for the second word and compare them to
                // the ones already in the hash. Save equal words in a new hash.
                ReturnDataCol[] res2;
                HashMap resHash = new HashMap();
                try {
                    DISCO d = new DISCO(args[0], false);
                    res2 = d.collocations(args[3]);
                    if ( res2 == null ){ 
                        System.out.println("The word \""+args[3]+"\" was not found."); 
                        return; 
                    }
                    for (ReturnDataCol res21 : res2) {
                        if (colloHash.containsKey(res21.word)) {
                            resHash.put(res21.word, res21.value);
                        }
                    }
                } catch (IOException ex) {
                    System.out.println("Error: IOException: "+ex);
                }
                // save the words from the resulting hash in an array and sort it
                ReturnDataCol[] fin = new ReturnDataCol[resHash.size()];            
                int i = 0;
                for(Iterator it = resHash.keySet().iterator(); it.hasNext(); ){
                    String w = (String) it.next();
                    fin[i++] = new ReturnDataCol(w, (Float)resHash.get(w));
                }
                // sort Array ReturnDataCol[] according to highest significance
                Arrays.sort(fin);
                for (ReturnDataCol fin1 : fin) {
                    System.out.println(fin1.word + "\t" + fin1.value);
                }
            }
            /////////////////////////////////////////////////
            // -n: return the number of words in the index //
            /////////////////////////////////////////////////
            else if( args[1].equals("-n") ){
                try {
                    DISCO d = new DISCO(args[0], false);
                    int n = d.numberOfWords();
                    System.out.println(n);
                } catch (IOException ex) {
                    System.out.println("Error: IOException: "+ex);
                }
            }
            ////////////////////////////////////////////
            // -wl: write word frequency list to file //
            ////////////////////////////////////////////
            else if( args[1].equals("-wl") ){
                try {
                    DISCO d = new DISCO(args[0], false);
                    int i = d.wordFrequencyList(args[2]);
                    System.out.println(i+" of "+d.numberOfWords()+" words were written.");
                } catch (IOException ex) {
                    System.out.println("Error: IOException: "+ex);
                }
            }
            ///////////////////////////////////////////////////////////////////////
            // -ds, -dbn, -dbc, -dr: Process word pairs in input file
            ///////////////////////////////////////////////////////////////////////
            else if( args[1].equals("-ds") || 
                    args[1].equals("-dbn") || args[1].equals("-dbc") ||
                    args[1].equals("-dr") ){
                // next argument has to be a file name
                File inputFile = new File(args[2]);
                if( ! inputFile.canRead() ){
                    System.out.println("Error: can't open file "+args[2]);
                    printUsage();
                    return;
                }
                if ( args[1].equals("-ds") ) readFile(args[0], inputFile, 1);
                else if( args[1].equals("-dbn") ) readFile(args[0], inputFile, 3);
                else if( args[1].equals("-dbc") ) readFile(args[0], inputFile, 4);
                else if( args[1].equals("-dr") ) readFile(args[0], inputFile, 5);
            }
            ////////////////////////////////////////////////////////////////////
            // -ts: text similarity
            ////////////////////////////////////////////////////////////////////
            else if( args[1].equals("-ts") ){
                // correct number of arguments?
                if( args.length < 5 ){
                    System.out.println("Error: Too few arguments.");
                    printUsage();
                    return;
                }
                if( args.length > 5 ){
                    System.out.println("Too many arguments (enclose each of TEXT1 and TEXT2 in double quotes)!");
                    printUsage();
                    return;
                }
                SimilarityMeasure measure = DISCO.getSimilarityMeasure(args[4]);
                if( measure == null ){
                    System.out.println("Error: unknown similarity measure: "+args[4]);
                    return;
                }
                // call method
                DISCO disco = new DISCO(args[0], false);
                TextSimilarity ts = new TextSimilarity();
                float sim = ts.textSimilarity(args[2], args[3], disco, measure);
                System.out.println(sim);
            }
            ////////////////////////////////////////////////////////////////////
            // -tsd: directed text similarity between text and hypothesis
            ////////////////////////////////////////////////////////////////////
            else if( args[1].equals("-tsd") ){
                // correct number of arguments?
                if( args.length < 5 ){
                    System.out.println("Error: Too few arguments.");
                    printUsage();
                    return;
                }
                if( args.length > 5 ){
                    System.out.println("Too many arguments (enclose TEXT1 and TEXT2 in double quotes)!");
                    printUsage();
                    return;
                }
                SimilarityMeasure measure = DISCO.getSimilarityMeasure(args[4]);
                if( measure == null ){
                    System.out.println("Error: unknown similarity measure: "+args[4]);
                    return;
                }
                // call method
                TextSimilarity ts = new TextSimilarity();
                DISCO disco = new DISCO(args[0], false);
                float sim = ts.directedTextSimilarity(args[2], args[3], disco, measure);
                System.out.println(sim);
            }
            ////////////////////////////////////////////////////////////////////
            // -fo: filter outliers
            ////////////////////////////////////////////////////////////////////
            else if( args[1].equals("-fo") ){
                try {
                    // correct number of arguments?
                    if( args.length < 4 ){
                        System.out.println("Error: Too few arguments.");
                        printUsage();
                        return;
                    }
                    // call method
                    DISCO disco = new DISCO(args[0], false);
                    ReturnDataBN res = Cluster.filterOutliers(disco, args[2], Integer.parseInt(args[3]));
                    if( res == null ){
                        System.out.println("The word \""+args[2]+"\" was not found in the index.");
                        return;
                    }
                    for(int i = 0; i < res.words.length; i++){
                        System.out.println(res.words[i]+"\t"+res.values[i]);
                    }
                } catch (WrongWordspaceTypeException ex) {
                    System.out.println("Error: Wrong wordspace type: only works "
                            + "with wordspaces of type SIM!");
                }
            }
            ////////////////////////////////////////////////////////////////////
            // -gs: grow input set
            ////////////////////////////////////////////////////////////////////
            else if( args[1].equals("-gs") ){
                try {
                    // correct number of arguments?
                    if( args.length < 3 ){
                        System.out.println("Error: Too few arguments.");
                        printUsage();
                        return;
                    }
                    // create inputSet
                    String[] inputSet = new String[args.length - 2];
                    int k = 0;
                    for(int i = 2; i < args.length; i++){
                        inputSet[k++] = args[i];
                    }
                    // call method
                    DISCO disco = new DISCO(args[0], false);
                    String[] res = Cluster.growSet(disco, inputSet);
                    for (String re : res) {
                        System.out.println(re);
                    }
                } catch (WrongWordspaceTypeException ex) {
                    System.out.println("Error: Wrong wordspace type: only works "
                            + "with wordspaces of type SIM!");
                }
            }
            ////////////////////////////////////////////////////////////////////
            // -rs / -rc: show similar words according to ranks
            ////////////////////////////////////////////////////////////////////
            else if( args[1].equals("-rs") || args[1].equals("-rc") ){
                // correct number of arguments?
                if( args.length < 3 ){
                    System.out.println("Error: Too few arguments.");
                    printUsage();
                    return;
                }
                // create inputSet
                Set<String> inputSet = new HashSet();
                for(int i = 2; i < args.length; i++){
                    inputSet.add(args[i]);
                }
                // load word space into RAM
                System.out.print("Loading word space...");
                System.out.flush();
                DISCO disco = new DISCO(args[0], true);
                System.out.println("OK");
                System.out.flush();
                // call method
                try {
                    Rank rank = new Rank();
                    ArrayList<WordAndRank> res;
                    if(args[1].equals("-rs")){
                        res = rank.highestRanking(disco, inputSet, DISCO.WordspaceType.SIM);
                    }else{
                        res = rank.highestRanking(disco, inputSet, DISCO.WordspaceType.COL);
                    }
                    int max = 100;
                    for(int k = 0; k < res.size(); k++){
                        System.out.println(res.get(k).word + "\t" + res.get(k).rank);
                        if( k >= max ) break;
                    }
                } catch (WrongWordspaceTypeException ex) {
                    System.out.println("Error: Wrong wordspace type: only works "
                            + "with wordspaces of type SIM!");
                }
            }
            ////////////////////////////////////////////////////////////////////
            // CLUTO: indexDir -cg n minSim outputDir
            ////////////////////////////////////////////////////////////////////
            else if( args[1].equals("-cg") ){
                try {
                    // load word space into RAM
                    System.out.print("Loading word space...");
                    System.out.flush();
                    DISCO disco = new DISCO(args[0], true);
                    System.out.println("OK");
                    System.out.flush();
                    // call method
                    Cluster cluster = new Cluster();
                    cluster.clutoClusterSimilarityGraph(disco, Integer.parseInt(args[2]),
                            Float.parseFloat(args[3]), args[4]);
                } catch (WrongWordspaceTypeException ex) {
                    System.out.println("Error: Wrong wordspace type: only works "
                            + "with wordspaces of type SIM!");
                }
            }
            ////////////////////////////////////////////////////////////////////
            // CLUTO: indexDir -cv wordListFile outputDir
            ////////////////////////////////////////////////////////////////////
            else if( args[1].equals("-cv") ){
                // load word space into RAM
                System.out.print("Loading word space...");
                System.out.flush();
                DISCO disco = new DISCO(args[0], true);
                System.out.println("OK");
                System.out.flush();
                // read word list from file
                System.out.print("Reading word list... ");
                ArrayList<String> wordList = new ArrayList<>();
                BufferedReader br = new BufferedReader(new FileReader(args[2]));
                String line;
                while( (line = br.readLine()) != null){
                    line = line.trim();
                    wordList.add(line);
                }
                System.out.println(wordList.size()+" words read.");
                System.out.flush();
                // call method
                Cluster cluster = new Cluster();
                cluster.clutoClusterVectors(disco, wordList, args[3]);
            }
            ////////////////////////////////////////////////////////////////////
            // Compositionality: -cs <p1> <p2> compMethod simMeasure a b c lambda
            //                        2    3       4          5      6 7 8   9
            //                                  add       kolb
            //                                  mult      cos
            //                                  combi
            //                                  dilat
            ////////////////////////////////////////////////////////////////////
            else if( args[1].equals("-cs") ){
                if( args.length < 4 ){
                    System.out.println("Error: Too few arguments.");
                    printUsage();
                    return;
                }
                // get composition method
                Compositionality.VectorCompositionMethod compMethod;
                if( args[4].equalsIgnoreCase("add")){
                    compMethod = Compositionality.VectorCompositionMethod.ADDITION;
                }else if( args[4].equalsIgnoreCase("mult")){
                    compMethod = Compositionality.VectorCompositionMethod.MULTIPLICATION;
                }else if( args[4].equalsIgnoreCase("combi")){
                    compMethod = Compositionality.VectorCompositionMethod.COMBINED;
                }else if( args[4].equalsIgnoreCase("dilat")){
                    compMethod = Compositionality.VectorCompositionMethod.DILATION;
                }else{
                    System.out.println("Error: Unknown composition method \""+
                            args[4]+"\".");
                    printUsage();
                    return;
                }
                // get similarity measure
                DISCO.SimilarityMeasure simMeasure;
                if( args[5].equalsIgnoreCase("kolb")){
                    simMeasure = DISCO.SimilarityMeasure.KOLB;
                }else if( args[5].equalsIgnoreCase("cos")){
                    simMeasure = DISCO.SimilarityMeasure.COSINE;
                }else{
                    System.out.println("Error: Unknown similarity measure \""+
                            args[5]+"\".");
                    printUsage();
                    return;
                }
                // open DISCO index
                DISCO disco = new DISCO(args[0], false);
                
                Compositionality comp = new Compositionality();
                float sim = comp.compositionalSemanticSimilarity(args[2], args[3],
                        compMethod, simMeasure, disco, Float.parseFloat(args[6]),
                        Float.parseFloat(args[7]), Float.parseFloat(args[8]), 
                        Float.parseFloat(args[9]));
                System.out.println(sim);
            }
            else if( args[1].equals("-wl") ){
                if( args.length < 3 ){
                    System.out.println("Error: output file name required for option -wl");
                    printUsage();
                    return;
                }
                DISCO disco = new DISCO(args[0], false);
                disco.wordFrequencyList(args[2]);
            }
            ////////////////////////////////////////////////////////////////////
            // unknown option
            ////////////////////////////////////////////////////////////////////
            else{
                System.out.println("Error: unknown command line option: "+args[1]);
                printUsage();
                return;
            }
        }
    }

}// end of class
