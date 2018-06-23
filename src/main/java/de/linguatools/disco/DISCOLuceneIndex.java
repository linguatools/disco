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

import de.linguatools.disco.DISCO.WordspaceType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
 * The methods in this class work with word spaces stored in the form of 
 * <a href="http://lucene.apache.org">Lucene</a> indexes. Word spaces for 
 * several languages are available on the DISCO
 * <a href="http://www.linguatools.de/disco/disco-download_en.html">download
 * page</a>.<br>
 * 
 * @author peter
 * @version 3.0
 *******************************************************************************/
public class DISCOLuceneIndex extends DISCO {

    /**
     * Name of the word space directory.
     */
    public String indexDir = null;
    /**
     * disco.config
     */
    private final ConfigFile config;
    /**
     * The word space loaded into RAM.
     */
    public RAMDirectory indexRAM = null;
    private IndexSearcher is = null;
    private Analyzer analyzer = null;
    private QueryParser parser = null;
    /**
     * Type of this word space.
     */
    private final WordspaceType wordspaceType;
    
    /***************************************************************************
     * DISCO version 2.0 allows to load a complete word space into RAM to
     * speed up similarity computations. Make sure that you have enough free
     * memory since word spaces can be very large. Also, remember that loading a
     * huge word space into RAM will take some time.<br>
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
    public DISCOLuceneIndex(String idxName, boolean loadIntoRAM) throws FileNotFoundException, 
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
        config = new ConfigFile(indexDir);
        if( config.dontCompute2ndOrder == true ){
            wordspaceType = WordspaceType.COL;
        }else{
            wordspaceType = WordspaceType.SIM;
        }
    }
    
    /**
     * Returns the type of the word space instance.
     * @return word space type
     */
    @Override
    public WordspaceType getWordspaceType(){
        
        return wordspaceType;
    }

    /***************************************************************************
     * Returns the number of <code>Documents</code> (i.e. words) in the word
     * space.
     * @return number of words in index
     */
    @Override
    public int numberOfWords(){
        return config.vocabularySize;
    }
    
    @Override
    public int numberOfFeatureWords(){
        return config.numberFeatureWords;
    }
    
    @Override
    public int numberOfSimilarWords(){
        if( wordspaceType == WordspaceType.COL ){
            return 0;
        }else{
            return config.numberOfSimilarWords;
        }
    }
    
    /***************************************************************************
     * Looks up the input word in the word space and returns its frequency.
     * If the word is not found the return value is zero.
     * @param word word to be looked up (must be a single token).
     * @return frequency of the input word in the text corpus from which the 
     * word space index was built
     * @throws java.io.IOException
     */
    @Override
    public int frequency(String word) throws IOException{
        
        Document doc = searchIndex(word);
        if ( doc == null ) return 0;
        return Integer.parseInt( doc.get("freq") );
    }

    /***************************************************************************
     * Looks up the input word in the index and returns its semantically
     * similar words ordered by decreasing similarity together
     * with their similarity values.<br>
     * If the search word isn't found in the word space, the return value is 
     * <code>null</code>.<br>
     * The similarity values in the result can differ from the values you get
     * with <code>DISCOLuceneIndex.semanticSimilarity</code> for the same word pair. This is
     * the case when another similarity measure was used in generating the word
     * space. Consult the file <code>disco.config</code> in the word space 
     * directory to get the similarity measure that was used. If no measure is
     * given there the default measure <code>KOLB</code> was used.<br>
     * <b>Important note</b>: This method only works with word spaces of type
     * <code>DISCOLuceneIndex.WordspaceType.SIM</code>.
     * @param word word to be looked up (must be a single token).
     * @return result data structure or <code>null</code>
     * @throws IOException
     * @throws WrongWordspaceTypeException if the word space does not have the
     * type <code>DISCOLuceneIndex.WordspaceType.SIM</code>.
     */
    @Override
    public ReturnDataBN similarWords(String word) throws IOException, 
            WrongWordspaceTypeException{
        
        // check word space type
        if( wordspaceType != DISCOLuceneIndex.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + " to word spaces of type "+wordspaceType);
        }
        
        Document doc = searchIndex(word);
        if ( doc == null ) return null;
        // die gespeicherten Inhalte der Felder "dsb" und "dsbSim" holen 
        ReturnDataBN res = new ReturnDataBN();
        String dsb = doc.get("dsb");
        if( dsb == null ){
            return res;
        }
        res.words = dsb.split(" ");
        res.values = new float[ res.words.length ];
        String[] valuesBuffer = doc.get("dsbSim").split(" ");
        int i = 0;
        for( String value : valuesBuffer ){
            if( i >= res.values.length ){
                break;
            }
            res.values[i] = Float.parseFloat( value );
            i++;
        }
        
        return res;
    }
    
    /**
     * Computes the semantic similarity (according to the vector similarity 
     * measure <code>similarityMeasure</code>) between the two input words based
     * on their collocation sets (i.e. word vectors).<br>
     * <b>Important</b>: The measure <code>SimilarityMeasure.KOLB</code> should
     * <i>not</i> be used with word spaces imported from word2vec!<br>
     * <b>Note</b>: To compute the similarity between multi-word expressions
     * (e.g. "New York" or "nuclear power plant") use the methods in the class
     * <code>Compositionality</code>.
     * @param w1 input word #1 (must be a single token).
     * @param w2 input word #2 (must be a single token).
     * @param vectorSimilarity
     * @return The similarity between the two input words; depending on the 
     * chosen similarity measure a value between 0.0F and 1.0F 
     * (SimilarityMeasure.KOLB), or -1.0F and 1.0F (SimilarityMeasure.COSINE).
     * If any of the two words isn't found in the index, the return value
     * is -2.0F. In case the <code>similarityMeasure</code> is unknown the
     * return value is -3.0F.
     * @throws IOException 
     */
    @Override
    public float semanticSimilarity(String w1, String w2, VectorSimilarity 
            vectorSimilarity) throws IOException{
        
        // die beiden zu vergleichenden Wörter im Index nachschlagen
        Document doc1 = searchIndex(w1);
        Document doc2 = searchIndex(w2);
        // wenn ein Wort nicht gefunden wurde, -2 zurückgeben
        if ( doc1 == null || doc2 == null ) return -2.0F;
        
        return (float)vectorSimilarity.computeSimilarity(doc1, doc2);
    }

    /**
     * Computes the second order semantic similarity between the input words
     * based on the sets of their distributionally similar words.<br>
     * <b>Important note</b>: This method only works with word spaces of type
     * <code>WordspaceType.SIM</code>.
     * @param w1 input word #1 (must be a single token).
     * @param w2 input word #2 (must be a single token).
     * @param vectorSimilarity
     * @return similarity value between 0.0F and 1.0F. If any of the two words 
     * isn't found in the index, the return value is -2.0F.
     * @throws java.io.IOException
     * @throws WrongWordspaceTypeException
     */
    @Override
    public float secondOrderSimilarity(String w1, String w2, 
            VectorSimilarity vectorSimilarity)
            throws IOException, WrongWordspaceTypeException{
        
        // check word space type
        if( wordspaceType != DISCOLuceneIndex.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + "to word spaces of type "+wordspaceType);
        }
        
        // look up words in index
        Document doc1 = searchIndex(w1);
        Document doc2 = searchIndex(w2);
        if ( doc1 == null || doc2 == null ){
            return -2.0F;
        }
        
        Map<String,Float> v1 = SparseVector.getSecondOrderMapVector(doc1);
        Map<String,Float> v2 = SparseVector.getSecondOrderMapVector(doc2);
        
        return (float)vectorSimilarity.computeSimilarity(v1, v2);
    }

    /***************************************************************************
     * Returns the word vector representing the distribution of the input word
     * in the corpus.<br>
     * The word vector can be used with the methods in the class 
     * <code>Compositionality</code>. 
     * @param word input word (must be a single token - to get a word vector for
     * a phrase use <code>Compositionality.composeWordVectors</code>).
     * @return HashMap containing the word vector or <code>null</code> if 
     * <code>word</code> is not found. The features of the word vector are
     * the keys of the resulting HashMap, the values are the significance values
     * of the word vector. For more information on the values consult the
     * documentation of the method <code>searchIndex()</code> (field 
     * <code>kol</code>).
     * @throws IOException
     * @see de.linguatools.disco.Compositionality
     */
    @Override
    public Map<String,Float> getWordvector(String word)
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
    
    @Override
    public Map<String,Float> getSecondOrderWordvector(String word) throws 
            WrongWordspaceTypeException, IOException{
        
        // check word space type
        if( wordspaceType != DISCOLuceneIndex.WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + "to word spaces of type "+wordspaceType);
        }
        // lookup word
        Document doc = searchIndex(word);
        if ( doc == null ){
            return null;
        }
        return SparseVector.getSecondOrderMapVector(doc);
    }
    
    /***************************************************************************
     * Run trough all documents (i.e. queryable words) in the index, and 
     * retrieve the word and its frequency. Write both informations to the text
     * file named <code>outputFileName</code>. Note that the output is not 
     * sorted.<br>
     * This method can be used to check index integrity. If an error occurs
     * while querying a word, a warning is written to standard output.
     * @param outputFileName name of the output file.
     * @return number of words written to the output file. In case of success
     * the value is equal to the number of words in the index.
     */
    @Override
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
            System.out.println(DISCOLuceneIndex.class.getName()+": "+ex);
            return -1;
        } catch (IOException ex) {
            System.out.println(DISCOLuceneIndex.class.getName()+": "+ex);
            return -1;
        }

        // Hole Anzahl Dokumente im Index
        int N = ir.numDocs();
        
        // öffne Ausgabedatei
        PrintWriter fw;
        try {
            fw = new PrintWriter(outputFileName, "UTF-8");
        } catch (IOException ex) {
            System.out.println(DISCOLuceneIndex.class.getName()+": "+ex);
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
            fw.write(word+"\t"+f+"\n");
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
            System.out.println(DISCOLuceneIndex.class.getName()+": "+ex);
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
    @Override
    public String[] getStopwords() throws FileNotFoundException, IOException,
            CorruptConfigFileException{
        
        // get the stopwords from the line "stopwords" in the file
        // "disco.config"
        String configFileName = indexDir + File.separator + "disco.config";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(new File(configFileName)), "UTF-8"))) {
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
    
    @Override
    public long getTokenCount(){
        
        return config.tokencount;
    }
    
    @Override
    public int getMinFreq(){
        
        return config.minFreq;
    }
    
    @Override
    public int getMaxFreq(){
        
        return config.maxFreq;
    }
    
    @Override
    public Iterator<String> getVocabularyIterator() throws IOException{
        
        return new VocabularyIterator();
    }
    
    class VocabularyIterator implements Iterator<String>{
        
        private int i;
        private final int N;
        private final IndexReader ir;
        
        public VocabularyIterator() throws IOException{
            
            // erzeuge einen IndexReader fuer das indexDir
            if( indexRAM != null ){
                ir = DirectoryReader.open(indexRAM);
            }else{
                ir = DirectoryReader.open(FSDirectory.open(Paths.get(indexDir)));
            }
            // Hole Anzahl Dokumente im Index
            N = ir.numDocs();
            i = 0;
        }
        
        @Override
        public boolean hasNext(){
            
            if( i < N ){
                return true;
            }else{
                return false;
            }
        }
        
        @Override
        public String next(){
            
            int buffer = i;
            i++;
            
            Document doc;
            try {
                doc = ir.document( buffer );
            } catch (IOException ex) {
                System.err.println(DISCOLuceneIndex.class.getName()+": word "+
                        buffer+": "+ex);
                return "";
            }
            // Wort Nr. i holen
            return doc.get("word");
        }
        
        @Override
        public void remove(){
            
        }
    }
    
    @Override
    public String getWord(int id) throws IOException{
        
        // erzeuge einen IndexReader fuer das indexDir
        IndexReader ir;
        if( indexRAM != null ){
            ir = DirectoryReader.open(indexRAM);
        }else{
            ir = DirectoryReader.open(FSDirectory.open(Paths.get(indexDir)));
        }
        // Hole Anzahl Dokumente im Index
        int N = ir.numDocs();
        if( id >= N ){
            return null;
        }
        Document doc;
        try {
            doc = ir.document( id );
        } catch (IOException ex) {
            System.err.println(DISCOLuceneIndex.class.getName()+": word "+
                    id+": "+ex);
            return null;
        }
        ir.close();
        // Wort Nr. i holen
        return doc.get("word");
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // additional methods specific to DISCOLuceneIndex
    ////////////////////////////////////////////////////////////////////////////
    
    /***************************************************************************
     * Searches for a input word in index field <code>word</code> and returns
     * the first hit <code>Document</code> or <code>null</code>.<br>
     * DISCOLuceneIndex uses the <a href="http://lucene.apache.org">Lucene</a> index. A 
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
     * first.<br>
     * For word spaces of type <code>WordspaceType.COL</code>, this field is 
     * empty!
     * </li>
     * <li><code>dsbSim</code>: contains a single string with the similarity
     * values for the words in the field <code>dsb</code>, separated by spaces.
     * The string in this field is parallel to the string in the field 
     * <code>dsb</code>, i.e., the n-th token of the string in <code>dsbSim</code>
     * corresponds to the n-th token in <code>dsb</code>.<br> 
     * Example: field <code>dsb</code> contains the string "apple banana cherry",
     * field <code>dsbSim</code> contains the string "0.3241 0.1233 0.0788". This
     * means that the similarity between the word in the field <code>word</code>
     * and "cherry" is 0.0788.<br>
     * For word spaces of type <code>WordspaceType.COL</code>, this field is 
     * empty!
     * </li>
     * <li><code>kol</code>: contains the features from the input word's sparse 
     * word vector. "Sparse" means that only those features are stored that have
     * a value greater than or equal to the threshold that was set in 
     * <code>minWeight</code> in the <code>disco.config</code> file.<br>
     * There are three forms features can have:
     * <ul>
     * <li><code>featureWord</code>: the feature is a plain word.</li>
     * <li><code>featureWord&lt;SEP&gt;relation</code>: the feature is composed of a word and
     * a specific relation between the inputWord and the featureWord. The relation
     * can be a window position or a syntactic dependency relation. featureWord
     * and relation are separated by the character <code>DISCOLuceneIndex.relationSeparator</code>.</li>
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
     * Returns the collocations for the input word together with their 
     * significance values, ordered by significance value (highest significance
     * first). If the search word is not found in the index, the return value is 
     * <code>null</code>.<br>
     * The collocations are derived from the word's features. As features can be
     * not only plain words, but also words plus their relation to the input 
     * word, the relation is cut off of the word, and the significance values of
     * identical words are summed up. (If you want to receive the full features
     * instead of only the words use the method <code>getWordvector()</code>.)
     * <br>
     * Features can also be IDs (for word spaces with document features or 
     * imported from other tools). In this case, the "collocations" will be a list
     * of IDs.<br>
     * The significance measure that was used in word space construction by 
     * <a href="http://www.linguatools.de/disco/disco-builder.html">DISCOBuilder</a>
     * is stored in the file <code>disco.config</code> in the word space
     * directory (look at the line <code>weightingMethod</code>). For more
     * information on available significance measures consult DISCOBuilder's
     * documentation.<br> 
     * @param word the input word (must be a single token).
     * @return the list of collocations with their significance values or 
     * <code>null</code>. The <code>relation</code> fields of the array elements
     * are not set.
     * @throws java.io.IOException
     */
    @Override
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
            int p = featuresBuffer[i].lastIndexOf(RELATION_SEPARATOR);
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
    
    /***************************************************************************
     * Returns the collocational strength between words <code>w1</code> and 
     * <code>w2</code>, summed up over all relations. 
     * @param w1 input word #1 (must be a single token).
     * @param w2 input word #2 (must be a single token).
     * @return the sum of the significance values between word w1 and all its
     * features that have w2 as their word part while ignoring the relation (if
     * any) and the same for w2 with w1 as feature. Returns whichever value is
     * greater. If w1 or w2 are not found the return value is 0.
     * @throws java.io.IOException
     */
    public float collocationalValue(String w1, String w2) throws IOException{

        // get the cooccurrences of w1 and w2 summed up over all relations
        ReturnDataCol[] cols1 = collocations(w1);
        ReturnDataCol[] cols2 = collocations(w2);
        
        float v1 = 0.0F;
        if( cols1 != null ){
            for(ReturnDataCol col : cols1){
                if( col.word.equals(w2) ){
                    v1 = col.value;
                    break;
                }
            }
        }
        
        float v2 = 0.0F;
        if( cols2 != null ){
            for(ReturnDataCol col : cols2){
                if( col.word.equals(w1) ){
                    v2 = col.value;
                    break;
                }
            }
        }
        
        return (v1 > v2) ? v1 : v2;
    }
}
