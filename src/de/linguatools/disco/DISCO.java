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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;

/*******************************************************************************
 * DISCO (Extracting DIStributionally Similar Words Using CO-occurrences) 
 * provides a number of methods for computing the distributional (i.e. semantic)
 * similarity between arbitrary words and text passages, for retrieving a word's
 * collocations or its corpus frequency. It also provides a method to retrieve
 * the semantically most similar words for a given word.<br/>
 * The methods in this class work with word spaces (a.k.a. language data 
 * packets) stored in the form of <a href="http://lucene.apache.org">Lucene</a>
 * indexes. Word spaces for several languages are available on the DISCO
 * <a href="http://www.linguatools.de/disco/disco-download_en.html">download
 * page</a>.<br/>
 * It is important to keep in mind that there are two different types of word
 * spaces:
 * <ul>
 * <li><code>DISCO.WordspaceType.COL</code>: this type stores a <i>word vector</i>
 * for each word. A word vector is the list of the significant co-occurrences of
 * the word together with the type of co-occurrence (if any) and a significance
 * value. The significant co-occurring words of a word are also called its
 * <i>collocations</i>. The type of co-occurrence can be a relative position in
 * a context window, or a syntactic relation</li>
 * <li><code>DISCO.WordspaceType.SIM</code>: this type stores the above word
 * vectors, but also contains pre-computed lists of the most similar words for
 * each word. These words can be queried using the method DISCO.similarWords().
 * There are several methods in the DISCO API that only work with word spaces of
 * type <code>DISCO.WordspaceType.SIM</code>.</li>
 * </ul>
 * DISCO is described in the following conference papers:
 * <ul>
 * <li>Peter Kolb: <a href="http://hdl.handle.net/10062/9731">Experiments on the
 * difference between semantic similarity and relatedness</a>. In <i>Proceedings
 * of the 17th Nordic Conference on Computational Linguistics - NODALIDA '09</i>,
 * Odense, Denmark, May 2009.</li>
 * <li>Peter Kolb: <a href="http://www.ling.uni-potsdam.de/~kolb/KONVENS2008-Kolb.pdf">
 * DISCO: A Multilingual Database of Distributionally Similar Words</a>. In 
 * <i>Tagungsband der 9. KONVENS</i>, Berlin, 2008.</li>
 * </ul>
 * @author peter
 * @version 2.1
 *******************************************************************************/
public class DISCO {

    /**
     * Name of the word space directory.
     */
    public String indexDir = null;
    /**
     * The word space loaded into RAM.
     */
    public RAMDirectory indexRAM = null;
    private IndexSearcher is = null;
    private Analyzer analyzer = null;
    private QueryParser parser = null;
    /**
     * Available word space types (SIM = word space contains lists of
     * pre-computed similar words for each word, COL = word space contains only
     * word vectors).
     */
    public enum WordspaceType {
        /**
         * Word spaces of this type only store word vectors.
         */
        COL,
        /**
         * Word spaces of this type store word vectors and a pre-computed list
         * of the most similar words for each word. The similarity measure that 
         * was used in generating the pre-computed list of similar words is given
         * in the file <code>disco.config</code> in the word space directory. If it
         * is not given, the default value <code>SimilarityMeasure.KOLB</code>
         * was used.
         */
        SIM
    }
    /**
     * Type of this word space.
     */
    public WordspaceType wordspaceType;
    
    /**
     * Available measures for vector comparison. 
     */
    public enum SimilarityMeasure { 
        /**
         * The well-known cosine vector similarity measure. This measure should
         * always be used with word spaces imported from word2vec.
         */
        COSINE, 
        /**
         * The vector similarity measure described in the paper <a 
         * href="http://hdl.handle.net/10062/9731">Experiments on the difference
         * between semantic similarity and relatedness</a>. Note that this measure
         * does not give usable results with word spaces imported from word2vec.
         */
        KOLB 
    }
    /**
     * This string is used as separator between a feature word and its relation.
     * It is a character from the Unicode private use area.
     */
    public final static String relationSeparator = "\uF8FF";
    
    /**
     * Get SimilarityMeasure from string.
     * @param simMeasure
     * @return SimilarityMeasure or null.
     */
    public static SimilarityMeasure getSimilarityMeasure(String simMeasure){
        
        if( simMeasure.equalsIgnoreCase("cosine") ){
            return SimilarityMeasure.COSINE;
        }else if( simMeasure.equalsIgnoreCase("Kolb") ){
            return SimilarityMeasure.KOLB;
        }else return null;
    }
    
    /***************************************************************************
     * DISCO version 2.0 allows to load a complete word space into RAM to
     * speed up similarity computations. Make sure that you have enough free
     * memory since word spaces can be very large. Also, remember that loading a
     * huge word space into RAM will take some time.<br/>
     * This constructor reads the word space type from the file 
     * <code>disco.config</code> in the word space directory. If the file is not
     * found in the word space directory a <code>FileNotFoundException</code> is
     * thrown. If the word space type can not be determined (due to a corrupted
     * config file), a <code>CorruptConfigFileException</code> is thrown.
     * @param idxName the name of the word space directory
     * @param loadIntoRAM if true the word space is loaded into RAM
     * @throws IOException
     * @throws FileNotFoundException if the file "disco.config" can not be found
     * in the word space directory <code>idxName</code>.
     * @throws CorruptIndexException
     * @throws CorruptConfigFileException if the file "disco.config" is corrupt.
     */
    public DISCO(String idxName, boolean loadIntoRAM) throws FileNotFoundException, 
            CorruptIndexException, IOException, CorruptConfigFileException{
        
        indexDir = idxName;
        Path indexDirPath = Paths.get(indexDir);
        analyzer = new WhitespaceAnalyzer();
        parser = new QueryParser("word", analyzer);
        if(loadIntoRAM == true){
            indexRAM = new RAMDirectory(FSDirectory.open(indexDirPath), new IOContext());
            is = new IndexSearcher(DirectoryReader.open(indexRAM));
        }else{
            is = new IndexSearcher(DirectoryReader.open(
                    FSDirectory.open(indexDirPath)));
        }
        
        // get the word space type from the line "dontCompute2ndOrder" in the file
        // "disco.config"
        String configFileName = indexDir + File.separator + "disco.config";
        try (BufferedReader br = new BufferedReader(new FileReader(configFileName))) {
            String line;
            while( (line = br.readLine()) != null ){
                line = line.trim();
                if( line.startsWith("dontCompute2ndOrder=") ){
                    if( line.endsWith("true")){
                        wordspaceType = WordspaceType.COL;
                    }
                    else if( line.endsWith("false")){
                        wordspaceType = WordspaceType.SIM;
                    }
                }
            }
        }
        
        // throw CorruptConfigFileException
        if( wordspaceType == null ){
            throw new CorruptConfigFileException("ERROR: the word space type "
                    + "could not be determined from the file "+configFileName);
        }
    }
    
    /**
     * Returns the type of the word space instance.
     * @return word space type
     */
    public WordspaceType getWordspaceType(){
        
        return wordspaceType;
    }
    
    /***************************************************************************
     * Searches for a input word in index field <code>word</code> and returns
     * the first hit <code>Document</code> or <code>null</code>.<br/>
     * DISCO uses the <a href="http://lucene.apache.org">Lucene</a> index. A 
     * word's data are stored in the index in an object of type 
     * <code>Document</code>. A <code>Document</code> has the following 6 
     * fields:
     * <ul>
     * <li><code>word</code>: contains a word, tokenized with 
     * <code>WhitespaceAnalyzer</code>. This is the only searchable field.
     * </li>
     * <li><code>freq</code>: the corpus frequency of the word. This field is 
     * only stored, but not indexed.
     * </li>
     * <li><code>dsb</code>: the distributionally similar words for the input 
     * word. They are stored in a single string, in which the words are
     * separated by spaces. This field is not indexed, and therefore not
     * searchable. The words are sorted by their similarity value, highest value
     * first.<br/>
     * For word spaces of type <code>WordspaceType.COL</code>, this field is 
     * empty!
     * </li>
     * <li><code>dsbSim</code>: contains a single string with the similarity
     * values for the words in the field <code>dsb</code>, separated by spaces.
     * The string in this field is parallel to the string in the field 
     * <code>dsb</code>, i.e., the n-th token of the string in <code>dsbSim</code>
     * corresponds to the n-th token in <code>dsb</code>.<br/> 
     * Example: field <code>dsb</code> contains the string "apple banana cherry",
     * field <code>dsbSim</code> contains the string "0.3241 0.1233 0.0788". This
     * means that the similarity between the word in the field <code>word</code>
     * and "cherry" is 0.0788.<br/>
     * For word spaces of type <code>WordspaceType.COL</code>, this field is 
     * empty!
     * </li>
     * <li><code>kol</code>: contains the features from the input word's sparse 
     * word vector. "Sparse" means that only those features are stored that have
     * a value greater than or equal to the threshold that was set in 
     * <code>minWeight</code> in the <code>disco.config</code> file.<br/>
     * There are three forms features can have:
     * <ul>
     * <li><code>featureWord</code>: the feature is a plain word.</li>
     * <li><code>featureWord&lt;SEP&gt;relation</code>: the feature is composed of a word and
     * a specific relation between the inputWord and the featureWord. The relation
     * can be a window position or a syntactic dependency relation. featureWord
     * and relation are separated by the character <code>DISCO.relationSeparator</code>.</li>
     * <li><code>ID</code>: the feature is a number. This is the case for word spaces that have
     * been imported from other tools like word2vec. Word spaces of type word x 
     * document also have IDs as features.</li>
     * </ul>
     * The features in the field <code>kol</code> are separated by a space. 
     * </li>
     * <li><code>kolSig</code>: contains the significance values for 
     * <code>kol</code>, in a string parallel to the string in <code>kol</code>.
     * </li>
     * </ul>
     * @param word input word to be looked up in index (must be a single token).
     * @return index entry of input word or <code>null</code> if the input word
     * can not be found in the index.
     * @throws java.io.IOException
     */
    public Document searchIndex(String word) throws IOException{
        
        try {
            // Anfrage tokenisieren und parsen
            Query query;
            query = parser.parse(word); // can throw ParseException !
            // nach Anfrage im Index suchen
            TopDocs hits = is.search(query, 1);
            if( hits.totalHits == 0 ) return null;
            // Nur den ersten Treffer verwenden (es sollte nur einen geben)
            Document doc = is.doc(hits.scoreDocs[0].doc);
            return doc;
        } catch (ParseException ex) {
            return null;
        }
    }

    /***************************************************************************
     * Returns the number of <code>Documents</code> (i.e. words) in the word
     * space.
     * @return number of words in index
     * @throws java.io.IOException
     */
    public int numberOfWords() throws IOException{
        
        // erzeuge einen IndexReader fuer das indexDir
        IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get(indexDir)));
        // Hole Anzahl Dokumente im Index
        return ( ir.numDocs() );
    }
    
    /***************************************************************************
     * Looks up the input word in the word space and returns its frequency.
     * If the word is not found the return value is zero.
     * @param word word to be looked up (must be a single token).
     * @return frequency of the input word in the text corpus from which the 
     * word space index was built
     * @throws java.io.IOException
     */
    public int frequency(String word) throws IOException{
        
        Document doc = searchIndex(word);
        if ( doc == null ) return 0;
        return Integer.parseInt( doc.get("freq") );
    }

    /***************************************************************************
     * Looks up the input word in the index and returns its semantically
     * similar words ordered by decreasing similarity together
     * with their similarity values.<br/>
     * If the search word isn't found in the word space, the return value is 
     * <code>null</code>.<br/>
     * The similarity values in the result can differ from the values you get
     * with <code>DISCO.semanticSimilarity</code> for the same word pair. This is
     * the case when another similarity measure was used in generating the word
     * space. Consult the file <code>disco.config</code> in the word space 
     * directory to get the similarity measure that was used. If no measure is
     * given there the default measure <code>KOLB</code> was used.<br/>
     * <b>Important note</b>: This method only works with word spaces of type
     * <code>DISCO.WordspaceType.SIM</code>.
     * @param word word to be looked up (must be a single token).
     * @return result data structure or <code>null</code>
     * @throws IOException
     * @throws WrongWordspaceTypeException if the word space does not have the
     * type <code>DISCO.WordspaceType.SIM</code>.
     */
    public ReturnDataBN similarWords(String word) throws IOException, 
            WrongWordspaceTypeException{
        
        // check word space type
        if( wordspaceType != DISCO.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + " to word spaces of type "+wordspaceType);
        }
        
        Document doc = searchIndex(word);
        if ( doc == null ) return null;
        // die gespeicherten Inhalte der Felder "dsb" und "dsbSim" holen 
        ReturnDataBN res = new ReturnDataBN();
        res.words = doc.get("dsb").split(" ");
        res.values = doc.get("dsbSim").split(" ");
        return res;
    }
    
    /***************************************************************************
     * Returns the collocations for the input word together with their 
     * significance values, ordered by significance value (highest significance
     * first). If the search word is not found in the index, the return value is 
     * <code>null</code>.<br/>
     * The collocations are derived from the word's features. As features can be
     * not only plain words, but also words plus their relation to the input 
     * word, the relation is cut off of the word, and the significance values of
     * identical words are summed up. (If you want to receive the full features
     * instead of only the words use the method <code>getWordvector()</code>.)
     * <br/>
     * Features can also be IDs (for word spaces with document features or 
     * imported from other tools). In this case, the "collocations" will be a list
     * of IDs.<br/>
     * The significance measure that was used in word space construction by 
     * <a href="http://www.linguatools.de/disco/disco-builder.html">DISCOBuilder</a>
     * is stored in the file <code>disco.config</code> in the word space
     * directory (look at the line <code>weightingMethod</code>). For more
     * information on available significance measures consult DISCOBuilder's
     * documentation.<br/> 
     * @param word the input word (must be a single token).
     * @return the list of collocations with their significance values or 
     * <code>null</code>. The <code>relation</code> fields of the array elements
     * are not set.
     * @throws java.io.IOException
     */
    public ReturnDataCol[] collocations(String word) throws IOException{
        
        Document doc = searchIndex(word);
        if ( doc == null ) return null;
        
        // die komprimiert gespeicherten Inhalte der Felder "Kol" und "KolSig"
        // holen und in ein Hash speichern. Die einzelnen Relationen werden
        // zusammengefasst und die jeweiligen Signifikanzwerte addiert.
        HashMap<String,Float> featuresHash = new HashMap<>();
        String w;
        String[] featuresBuffer = doc.get("kol").split(" ");
        String[] valuesBuffer = doc.get("kolSig").split(" ");
        for(int i = 0; i < featuresBuffer.length; i++ ){
            int p = featuresBuffer[i].lastIndexOf(relationSeparator);
            if( p == -1 )
                w = featuresBuffer[i]; // feature is "word" or "ID"
            else
                w = featuresBuffer[i].substring(0, p); // feature is "word<SEP>relation"
            if( !featuresHash.containsKey(w) ){
                featuresHash.put(w, Float.parseFloat(valuesBuffer[i]) );
            }else{
                float sig = (float) (Float.parseFloat(valuesBuffer[i])) +
                        featuresHash.get(w);
                featuresHash.put(w, sig);
            }
        }
        // jetzt das Hash in ein Array speichern und nach Signifikanz sortieren
        ReturnDataCol[] res = new ReturnDataCol[featuresHash.size()];            
        int i = 0;
        for (String f : featuresHash.keySet()) {
            res[i++] = new ReturnDataCol(f, (featuresHash.get(f)));
        }
        // sortiere Array ReturnDataCol[] nach hoechstem Signifikanzwert
        Arrays.sort(res);
        return res;
    }
    
    /**
     * This method compares two word vectors using the similarity measure
     * SimilarityMeasures.KOLB that is described in the paper
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
    private float computeSimilarityKolb(Document doc1, Document doc2){
        
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
        return 2 * zaehler / nenner;  // DICE-KOEFFIZIENT !
    }
    
    /**
     * This method compares two word vectors using the similarity measure
     * SimilarityMeasures.COSINE.
     * @param doc1 A document from the Lucene index containing the word vector
     * for word #1
     * @param doc2 A document from the Lucene index containing the word vector
     * for word #2
     * @return the similarity between the two word vectors; a value between -1.0F
     * and 1.0F. A return value of -2.0F indicates an error.
     */
    private float computeSimilarityCosine(Document doc1, Document doc2){
        
        // Kollokationen von Wort #1 durchlaufen (über alle Relationen), in Hash
        // speichern (nach Relationen unterschieden) und alle Werte addieren.
        HashMap colloHash = new HashMap();
        String[] wordsBuffer;
        String[] valuesBuffer;
        int i;
        float nenner1 = 0, v;
        wordsBuffer = doc1.get("kol").split(" ");
        valuesBuffer = doc1.get("kolSig").split(" ");
        if( wordsBuffer.length != valuesBuffer.length){
            return -2.0F;
        }
        for( i = 0; i < wordsBuffer.length; i++ ){
            if(valuesBuffer[i].equals("")) return -2;
            v = Float.parseFloat(valuesBuffer[i]);
            colloHash.put(wordsBuffer[i], v );
            nenner1 += v * v;
        }
        // Kollokationen von Wort #2 durchlaufen (über alle Relationen), mit den
        // Kollokationen von Wort #1 im Hash vergleichen und ggf. die Werte zum
        // Zähler addieren und alle Werte zum Nenner addieren.
        float nenner2 = 0, zaehler = 0;
        wordsBuffer = doc2.get("kol").split(" ");
        valuesBuffer = doc2.get("kolSig").split(" ");
        if( wordsBuffer.length != valuesBuffer.length){
            return -2.0F;
        }
        for( i = 0; i < wordsBuffer.length; i++ ){
            if(valuesBuffer[i].equals("")) return -2;
            v = Float.parseFloat(valuesBuffer[i]);
            if ( colloHash.containsKey(wordsBuffer[i]) ){
                zaehler += v * (Float) colloHash.get(wordsBuffer[i]);
            }
            nenner2 += v * v;
        }
        return (float) (zaehler / Math.sqrt(nenner1 * nenner2));
    }
    
    /***************************************************************************
     * Computes the semantic similarity (according to the vector similarity 
     * measure <code>SimilarityMeasures.KOLB</code> which is described in 
     * <a href="http://hdl.handle.net/10062/9731">Kolb 2009</a>) between the 
     * input words based on their collocation sets (i.e. word vectors). If any 
     * of the two words isn't found in the index, the return value is -2.<br/>
     * <b>Note</b>: To compute the similarity between multi-word expressions
     * (e.g. "New York" or "nuclear power plant") use the methods in the class
     * <code>Compositionality</code>.
     * @param w1 input word #1 (must be a single token).
     * @param w2 input word #2 (must be a single token).
     * @return similarity value between 0.0F and 1.0F or -2.0F.
     * @throws java.io.IOException
     * @deprecated 
     */
    public float semanticSimilarity(String w1, String w2) throws IOException{
        
        // die beiden zu vergleichenden Wörter im Index nachschlagen
        Document doc1 = searchIndex(w1);
        Document doc2 = searchIndex(w2);
        // wenn ein Wort nicht gefunden wurde, -2 zurückgeben
        if ( doc1 == null || doc2 == null ) return -2.0F;
        // Vektorähnlichkeitsmaß "Kolb" gibt Wert zwischen 0 und 1 zurück
        return computeSimilarityKolb(doc1, doc2);
    }

    /**
     * Computes the semantic similarity (according to the vector similarity 
     * measure <code>similarityMeasure</code>) between the two input words based
     * on their collocation sets (i.e. word vectors).<br/>
     * <b>Important</b>: The measure <code>SimilarityMeasure.KOLB</code> should
     * <i>not</i> be used with word spaces imported from word2vec!<br/>
     * <b>Note</b>: To compute the similarity between multi-word expressions
     * (e.g. "New York" or "nuclear power plant") use the methods in the class
     * <code>Compositionality</code>.
     * @param w1 input word #1 (must be a single token).
     * @param w2 input word #2 (must be a single token).
     * @param similarityMeasure One of the similarity measures enumerated in
     * <code>SimilarityMeasures</code>.
     * @return The similarity between the two input words; depending on the 
     * chosen similarity measure a value between 0.0F and 1.0F 
     * (SimilarityMeasure.KOLB), or -1.0F and 1.0F (SimilarityMeasure.COSINE).
     * If any of the two words isn't found in the index, the return value
     * is -2.0F. In case the <code>similarityMeasure</code> is unknown the
     * return value is -3.0F.
     * @throws IOException 
     */
    public float semanticSimilarity(String w1, String w2, SimilarityMeasure 
            similarityMeasure) throws IOException{
        
        // die beiden zu vergleichenden Wörter im Index nachschlagen
        Document doc1 = searchIndex(w1);
        Document doc2 = searchIndex(w2);
        // wenn ein Wort nicht gefunden wurde, -2 zurückgeben
        if ( doc1 == null || doc2 == null ) return -2.0F;
        
        if( similarityMeasure == SimilarityMeasure.KOLB ){
            // Vektorähnlichkeitsmaß "Kolb" gibt Wert zwischen 0 und 1 zurück
            return computeSimilarityKolb(doc1, doc2);
        }else if( similarityMeasure == SimilarityMeasure.COSINE ){
            // Vektorähnlichkeitsmaß "Kosinus" gibt Wert zwischen -1 und 1 zurück
            return computeSimilarityCosine(doc1, doc2);
        }else{
            // unknown similarity measure
            return -3.0F;
        }
    }

    /**
     * Computes the second order semantic similarity between the input words
     * based on the sets of their distributionally similar words.<br/>
     * <b>Important note</b>: This method only works with word spaces of type
     * <code>DISCO.WordspaceType.SIM</code>.
     * @param w1 input word #1 (must be a single token).
     * @param w2 input word #2 (must be a single token).
     * @return similarity value between 0.0F and 1.0F. If any of the two words 
     * isn't found in the index, the return value is -2.0F.
     * @throws java.io.IOException
     * @throws WrongWordspaceTypeException
     */
    public float secondOrderSimilarity(String w1, String w2)
            throws IOException, WrongWordspaceTypeException{
        
        // check word space type
        if( wordspaceType != DISCO.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + "to word spaces of type "+wordspaceType);
        }
        
        // die beiden zu vergleichenden Wörter im Index nachschlagen
        Document doc1 = searchIndex(w1);
        Document doc2 = searchIndex(w2);
        if ( doc1 == null || doc2 == null ) return -2.0F;
        
        // ähnliche Wörter von Wort #1 durchlaufen, in Hash speichern und alle
        // Werte addieren.
        HashMap simHash = new HashMap();
        String[] wordsBuffer;
        String[] valuesBuffer;
        int i;
        float nenner = 0, v;
        wordsBuffer = doc1.get("dsb").split(" ");
        valuesBuffer = doc1.get("dsbSim").split(" ");
        for( i = 1; i < wordsBuffer.length; i++ ){
            v = Float.parseFloat(valuesBuffer[i]);
            simHash.put(wordsBuffer[i], v );
            nenner += v;
        }
        // ähnliche Wörter von Wort #2 durchlaufen, mit den ähnlichen Wörtern von
        // Wort #1 im Hash vergleichen und ggf. die Werte zum Zähler addieren und
        // alle Werte zum Nenner addieren.
        float zaehler = 0;
        wordsBuffer = doc2.get("dsb").split(" ");
        valuesBuffer = doc2.get("dsbSim").split(" ");
        for( i = 1; i < wordsBuffer.length; i++ ){
            v = Float.parseFloat(valuesBuffer[i]);
            if ( simHash.containsKey(wordsBuffer[i]) ){
                zaehler += v + (Float) simHash.get(wordsBuffer[i]);
            }
            nenner += v;
        }
        return 2 * zaehler / nenner; // x 2 ???
    }
    
    /***************************************************************************
     * Returns the collocational strength between words <code>w1</code> and 
     * <code>w2</code>, summed up over all relations. 
     * @param w1 input word #1 (must be a single token).
     * @param w2 input word #2 (must be a single token).
     * @return the sum of the significance values between word w1 and all its
     * features that have w2 as their word part while ignoring the relation (if
     * any). If w1 is not found the return value is 0.
     * @throws java.io.IOException
     */
    public float collocationalValue(String w1, String w2) throws IOException{

        // get the cooccurrences of w1 summed up over all relations
        ReturnDataCol[] cols = collocations(w1);
        if( cols == null ) return 0.0F;
        
        float v = 0.0F;
        for(ReturnDataCol col : cols){
            if( col.word.equals(w2) ){
                v = col.value;
            }
        }
        return v;
    }

    /***************************************************************************
     * Returns the word vector representing the distribution of the input word
     * in the corpus.<br/>
     * The word vector can be used with the methods in the class 
     * <code>Compositionality</code>. 
     * @param word input word (must be a single token).
     * @return HashMap containing the word vector or <code>null</code> if 
     * <code>word</code> is not found. The features of the word vector are
     * the keys of the resulting HashMap, the values are the significance values
     * of the word vector. For more information on the values consult the
     * documentation of the method <code>searchIndex()</code> (field 
     * <code>kol</code>).
     * @throws IOException
     */
    public HashMap<String,Float> getWordvector(String word)
            throws IOException{

        Document doc = searchIndex(word);
        if ( doc == null ) return null;

        // die Inhalte der Felder "Kol" und "KolSig" holen
        HashMap<String,Float> wv = new HashMap<>();
        String[] wordsBuffer;
        String[] valuesBuffer;
        wordsBuffer = doc.get("kol").split(" ");
        valuesBuffer = doc.get("kolSig").split(" ");
        for( int i = 0; i < wordsBuffer.length; i++ ){
            wv.put(wordsBuffer[i], Float.parseFloat(valuesBuffer[i]));
        }
        
        return wv;
    }
    
    /***************************************************************************
     * Run trough all documents (i.e. queryable words) in the index, and 
     * retrieve the word and its frequency. Write both informations to the text
     * file named <code>outputFileName</code>. Note that the output is not 
     * sorted.<br/>
     * This method can be used to check index integrity. If an error occurs
     * while querying a word, a warning is written to standard output.
     * @param outputFileName name of the output file.
     * @return number of words written to the output file. In case of success
     * the value is equal to the number of words in the index.
     */
    public int wordFrequencyList(String outputFileName){
                
       // erzeuge einen IndexReader fuer das indexDir
        IndexReader ir;
        try {
            if( indexRAM != null ){
                ir = DirectoryReader.open(indexRAM);
            }else{
                ir = DirectoryReader.open(FSDirectory.open(Paths.get(indexDir)));
            }
        } catch (CorruptIndexException ex) {
            System.out.println(DISCO.class.getName()+": "+ex);
            return -1;
        } catch (IOException ex) {
            System.out.println(DISCO.class.getName()+": "+ex);
            return -1;
        }

        // Hole Anzahl Dokumente im Index
        int N = ir.numDocs();
        
        // öffne Ausgabedatei
        FileWriter fw;
        try {
            fw = new FileWriter(outputFileName);
        } catch (IOException ex) {
            System.out.println(DISCO.class.getName()+": "+ex);
            return -1;
        }
        
        // durchlaufe alle Dokumente
        int corrupt = 0;
        int ioerror = 0;
        int i;
        for(i = 0; i < N; i++){
            Document doc;
            try {
                doc = ir.document(i);
            } catch (CorruptIndexException ex) {
                corrupt++;
                continue;
            } catch (IOException ex) {
                ioerror++;
                continue;
            }
            // Wort Nr. i holen
            String word = doc.get("word");
            // Frequenz von Wort i holen
            int f = Integer.parseInt(doc.get("freq"));
            String dsb = doc.get("dsbSim");
            try {
                // Wort und Frequenz in Ausgabe schreiben
                fw.write(word+"\t"+f+"\t"+dsb+"\n");
            } catch (IOException ex) {
                System.out.println(DISCO.class.getName()+": word "+i+": "+ex);
                return i;
            }
            // Info ausgeben
            if( i % 100 == 0 ){
                System.out.print("\r"+i);
            }
        }
        System.out.println();
        if( corrupt > 0 || ioerror > 0 ){
            int e = corrupt + ioerror;
            System.out.println("*** WARNING! ***");
            System.out.println("The language data packet \""+indexDir+"\" "
                    + "has "+e+" defect entries ("+corrupt+" corrupt, "+ioerror+
                    " IO errors)");
            System.out.println("All functioning words have been written to "+
                    outputFileName);
        }
        
        // aufräumen
        try {
            fw.close();
            ir.close();
        } catch (IOException ex) {
            System.out.println(DISCO.class.getName()+": "+ex);
            return -1;
        }
        
        return (i - corrupt - ioerror);
    }
    
    /**
     * Get the stopwords for this word space instance. 
     * @return Array with stopwords
     * @throws FileNotFoundException
     * @throws IOException
     * @throws CorruptConfigFileException 
     */
    public String[] getStopwords() throws FileNotFoundException, IOException,
            CorruptConfigFileException{
        
        // get the stopwords from the line "stopwords" in the file
        // "disco.config"
        String configFileName = indexDir + File.separator + "disco.config";
        try (BufferedReader br = new BufferedReader(new FileReader(configFileName))) {
            String line;
            while( (line = br.readLine()) != null ){
                line = line.trim();
                if( line.startsWith("stopwords=") ){
                    String[] stopwords = line.substring(10).trim().split("\\s+");
                    return stopwords;
                }
            }
        }
        
        // throw CorruptConfigFileException
        throw new CorruptConfigFileException("ERROR: the stopwords "
                    + "could not be determined from the file "+configFileName);
    }
    
}
