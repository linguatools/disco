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

import de.linguatools.disco.DISCO.SimilarityMeasure;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

/*******************************************************************************
 * This class contains methods for creating, reading and accessing the
 * configuration file "disco.config".
 * @author peter
 * @version 3.0
 ******************************************************************************/
public class ConfigFile implements Serializable {
    
    private static final long serialVersionUID = 20160717002L;
    
    /**
     * Known file formats. 
     * TOKENIZED: tokenized text.
     * LEMMATIZED: three tab-separated columns per line: wordform posTag lemma.
     * CONNL: CoNNL-U format for dependency parsed text.
     */
    public enum FileFormat {
        TOKENIZED,
        LEMMATIZED,
        CONLL,
        UNDEFINED
    }
    
    /**
     * Get FileFormat type from string.
     * @param fileFormat
     * @return one of the three known file formats or null.
     */
    public static FileFormat parseFileFormat(String fileFormat){
        
        if( fileFormat.equalsIgnoreCase("tokenized") || 
                fileFormat.equalsIgnoreCase("tokenised")){
            return FileFormat.TOKENIZED;
        }
        if( fileFormat.equalsIgnoreCase("lemmatized") || 
                fileFormat.equalsIgnoreCase("lemmatised")){
            return FileFormat.LEMMATIZED;
        }
        if( fileFormat.equalsIgnoreCase("connl") || fileFormat.equalsIgnoreCase("conll") ||
                fileFormat.equalsIgnoreCase("connl-u") || fileFormat.equalsIgnoreCase("conll-u") ){
            return FileFormat.CONLL;
        }
        return null;
    }
    
    public FileFormat inputFileFormat = FileFormat.UNDEFINED;
    // lemmata instead of word forms for both index words AND features
    public boolean lemma = false;
    // lemmata only as features. Default is set to true if inputFileFormat is
    // LEMMATIZED!
    public boolean lemmaFeatures = false;
    // comma separated list
    public String boundaryMarks = "";
    // file name
    public String stopwordFile = "";
    // actual list with all the stopwords separated by space
    public String stopwords = "";
    public int minFreq = 100;
    public int maxFreq = -1;
    // corpus size (N)
    public long tokencount = -1; 
    public int vocabularySize = -1;
    public String inputDir = "";
    public String outputDir = "";
    public int leftContext = 3;     // overridden by openingTag, closingTag
    public int rightContext = 3;    // overridden by openingTag, closingTag
    public boolean position = true; // overridden by openingTag, closingTag
    public String openingTag = "";
    public String closingTag = "";
    public boolean wordByDocument = false;
    // with DPT only
    public boolean addInverseRelations = true;
    // bz
    public int numberFeatureWords = 30000;
    // number of most similar words to store for each word. Given by DISCOBuilder
    // parameter -nBest (default 300) or Import parameter -nBest (overridden by
    // dimension of input vector file).
    // 0 for word spaces of type COL.
    public int numberOfSimilarWords = 0;
    // does the word space (DenseMatrix only) contain ngrams (subwords)?
    public int numberOfNgrams = 0;
    public String weightingMethod = "lin";
    public float minWeight = 0.1F;
    public SimilarityMeasure similarityMeasure = SimilarityMeasure.KOLB;
    public boolean dontCompute2ndOrder = false;
    public String existingCoocFile = "";
    public String existingWeightFile = "";
    public int discoVersion = 2;
    // token filter for words
    public int minimumWordLength = 2;   
    public int maximumWordLength = 31;
    public String allowedCharactersWord = "\\.\\-'_";
    // token filter for features
    public int minimumFeatureLength = 2;   
    public int maximumFeatureLength = 31;
    public String allowedCharactersFeature = "\\.\\-'_";
    public boolean findMultiTokenWords = false;
    public String multiTokenWordsDictionary = "";
    public String tokenAnnotatorMap = "";

    /***************************************************************************
     * Constructor 1: create class with empty (default) fields.
     **************************************************************************/
    public ConfigFile() {
    }

    /***************************************************************************
     * Constructor 2: read data from file into class fields.
     * @param dirName Name of the directory where "disco.config" resides OR name
     *                of the configFile itself.
     * @throws java.io.FileNotFoundException
     * @throws de.linguatools.disco.CorruptConfigFileException
     **************************************************************************/
    public ConfigFile(String dirName) throws FileNotFoundException, IOException, 
            CorruptConfigFileException {

        // is dirName a directory or the configFile itself?
        String filename;
        File f = new File(dirName);
        if (f.isDirectory()) {
            filename = dirName + File.separator + "disco.config";
        } else {
            filename = dirName;
        }
        
        Properties props = new Properties();
        props.load(new FileInputStream(new File(filename)));
        
        if( props.getProperty("inputFileFormat") != null && !props.getProperty("inputFileFormat").isEmpty()){
            inputFileFormat = parseFileFormat(props.getProperty("inputFileFormat"));
            if( inputFileFormat == FileFormat.LEMMATIZED ){
                lemmaFeatures = true; // set default to true
            }
        }
        
        if( props.getProperty("lemma") != null && !props.getProperty("lemma").isEmpty()){
            lemma = Boolean.parseBoolean(props.getProperty("lemma"));
        }
        
        if( props.getProperty("lemmaFeatures") != null && !props.getProperty("lemmaFeatures").isEmpty()){
            lemmaFeatures = Boolean.parseBoolean(props.getProperty("lemmaFeatures"));
        }
        
        if( props.getProperty("boundaryMarks") != null && !props.getProperty("boundaryMarks").isEmpty()){
            boundaryMarks = props.getProperty("boundaryMarks");
        }
        
        if( props.getProperty("stopwordFile") != null && !props.getProperty("stopwordFile").isEmpty()){
            stopwordFile = props.getProperty("stopwordFile");
        }
        
        if( props.getProperty("stopwords") != null && !props.getProperty("stopwords").isEmpty()){
            stopwords = props.getProperty("stopwords");
        }
        
        if( props.getProperty("minFreq") != null && !props.getProperty("minFreq").isEmpty()){
            minFreq = Integer.parseInt(props.getProperty("minFreq"));
        }
        
        if( props.getProperty("maxFreq") != null && !props.getProperty("maxFreq").isEmpty()){
            maxFreq = Integer.parseInt(props.getProperty("maxFreq"));
        }
        
        if( props.getProperty("tokencount") != null && !props.getProperty("tokencount").isEmpty() ){
            tokencount = Long.parseLong(props.getProperty("tokencount"));
        }
        
        if( props.getProperty("vocabularySize") != null && !props.getProperty("vocabularySize").isEmpty() ){
            vocabularySize = Integer.parseInt(props.getProperty("vocabularySize"));
        }
        
        if( props.getProperty("inputDir") != null && !props.getProperty("inputDir").isEmpty() ){
            inputDir = props.getProperty("inputDir");
        }
        
        if( props.getProperty("outputDir") != null && !props.getProperty("outputDir").isEmpty() ){
            outputDir = props.getProperty("outputDir");
        }
        
        if( props.getProperty("leftContext") != null && !props.getProperty("leftContext").isEmpty() ){
            leftContext = Integer.parseInt(props.getProperty("leftContext"));
        }
        
        if( props.getProperty("rightContext") != null && !props.getProperty("rightContext").isEmpty() ){
            rightContext = Integer.parseInt(props.getProperty("rightContext"));
        }
           
        if( props.getProperty("position") != null && !props.getProperty("position").isEmpty() ){
            position = Boolean.parseBoolean(props.getProperty("position"));
        }
            
        if( props.getProperty("openingTag") != null && !props.getProperty("openingTag").isEmpty() ){
            openingTag = props.getProperty("openingTag");
        }
       
        if( props.getProperty("closingTag") != null && !props.getProperty("closingTag").isEmpty() ){
            closingTag = props.getProperty("closingTag");
        }
        
        if( props.getProperty("wordByDocument") != null && !props.getProperty("wordByDocument").isEmpty() ){
            wordByDocument = Boolean.parseBoolean(props.getProperty("wordByDocument"));
        }
        
        if( props.getProperty("addInverseRelations") != null && !props.getProperty("addInverseRelations").isEmpty() ){
            addInverseRelations = Boolean.parseBoolean(props.getProperty("addInverseRelations"));
        }
        
        if( props.getProperty("numberFeatureWords") != null && !props.getProperty("numberFeatureWords").isEmpty() ){
            numberFeatureWords = Integer.parseInt(props.getProperty("numberFeatureWords"));
        }
        
        if( props.getProperty("numberOfSimilarWords") != null && !props.getProperty("numberOfSimilarWords").isEmpty() ){
            numberOfSimilarWords = Integer.parseInt(props.getProperty("numberOfSimilarWords"));
        }
        
        if( props.getProperty("numberOfNgrams") != null && !props.getProperty("numberOfNgrams").isEmpty() ){
            numberOfNgrams = Integer.parseInt(props.getProperty("numberOfNgrams"));
        }
        
        if( props.getProperty("weightingMethod") != null && !props.getProperty("weightingMethod").isEmpty() ){
            weightingMethod = props.getProperty("weightingMethod");
        }
        
        if( props.getProperty("minWeight") != null && !props.getProperty("minWeight").isEmpty() ){
            minWeight = Float.parseFloat(props.getProperty("minWeight"));
        }
        
        if( props.getProperty("similarityMeasure") != null && !props.getProperty("similarityMeasure").isEmpty() ){
            similarityMeasure = DISCOLuceneIndex.getSimilarityMeasure(props.getProperty("similarityMeasure"));
        }
            
        if( props.getProperty("dontCompute2ndOrder") != null && !props.getProperty("dontCompute2ndOrder").isEmpty() ){
            dontCompute2ndOrder = Boolean.parseBoolean(props.getProperty("dontCompute2ndOrder"));
        }
        
        if( props.getProperty("existingCoocFile") != null && !props.getProperty("existingCoocFile").isEmpty() ){
            existingCoocFile = props.getProperty("existingCoocFile");
        }
        
        if( props.getProperty("existingWeightFile") != null && !props.getProperty("existingWeightFile").isEmpty() ){
            existingWeightFile = props.getProperty("existingWeightFile");
        }
        
        if( props.getProperty("discoVersion") != null && !props.getProperty("discoVersion").isEmpty() ){
            discoVersion = Integer.parseInt(props.getProperty("discoVersion"));
        }
        
        if( props.getProperty("minimumWordLength") != null && !props.getProperty("minimumWordLength").isEmpty() ){
            minimumWordLength = Integer.parseInt(props.getProperty("minimumWordLength"));
        }
        
        if( props.getProperty("maximumWordLength") != null && !props.getProperty("maximumWordLength").isEmpty() ){
            maximumWordLength = Integer.parseInt(props.getProperty("maximumWordLength"));
        }
        
        if( props.getProperty("allowedCharactersWord") != null && !props.getProperty("allowedCharactersWord").isEmpty() ){
            allowedCharactersWord = props.getProperty("allowedCharactersWord");
        }
        
        if( props.getProperty("minimumFeatureLength") != null && !props.getProperty("minimumFeatureLength").isEmpty() ){
            minimumFeatureLength = Integer.parseInt(props.getProperty("minimumFeatureLength"));
        }
        
        if( props.getProperty("maximumFeatureLength") != null && !props.getProperty("maximumFeatureLength").isEmpty() ){
            maximumFeatureLength = Integer.parseInt(props.getProperty("maximumFeatureLength"));
        }
        
        if( props.getProperty("allowedCharactersFeature") != null && !props.getProperty("allowedCharactersFeature").isEmpty() ){
            allowedCharactersFeature = props.getProperty("allowedCharactersFeature");
        }
        
        if( props.getProperty("findMultiTokenWords") != null && !props.getProperty("findMultiTokenWords").isEmpty() ){
            findMultiTokenWords = Boolean.parseBoolean(props.getProperty("findMultiTokenWords"));
        }
        
        if( props.getProperty("multiTokenWordsDictionary") != null && !props.getProperty("multiTokenWordsDictionary").isEmpty() ){
            multiTokenWordsDictionary = props.getProperty("multiTokenWordsDictionary");
        }
        
        if( props.getProperty("tokenAnnotatorMap") != null && !props.getProperty("tokenAnnotatorMap").isEmpty() ){
            tokenAnnotatorMap = props.getProperty("tokenAnnotatorMap");
        }
        
        // openingTag, closingTag overrides leftContext, rightContext, position
        if( !openingTag.isEmpty() && !closingTag.isEmpty() ){
            position = false;
            leftContext = -1;
            rightContext = -1;
        }
    }

    /**
     * Write the current values to new disco.config properties file in the 
     * directory dirName.
     * @param dirName
     * @throws IOException 
     */
    public void write(String dirName) throws IOException{
        
        // is dirName a directory or the configFile itself?
        String filename;
        File f = new File(dirName);
        if (f.isDirectory()) {
            filename = dirName + File.separator + "disco.config";
        } else {
            filename = dirName;
        }
        
        Properties props = new Properties();
        props.setProperty("inputFileFormat", String.valueOf(inputFileFormat));
        props.setProperty("lemma", String.valueOf(lemma));
        props.setProperty("lemmaFeatures", String.valueOf(lemmaFeatures));
        props.setProperty("boundaryMarks", boundaryMarks);
        props.setProperty("stopwordFile", stopwordFile);
        props.setProperty("stopwords", stopwords);
        props.setProperty("minFreq", String.valueOf(minFreq));
         props.setProperty("maxFreq", String.valueOf(maxFreq));
         props.setProperty("tokencount", String.valueOf(tokencount));
         props.setProperty("vocabularySize", String.valueOf(vocabularySize));
         props.setProperty("inputDir", inputDir);
         props.setProperty("outputDir", outputDir);
         props.setProperty("leftContext", String.valueOf(leftContext));
         props.setProperty("rightContext", String.valueOf(rightContext));
         props.setProperty("position", String.valueOf(position));
         props.setProperty("openingTag", openingTag);
         props.setProperty("closingTag", closingTag);
         props.setProperty("wordByDocument", String.valueOf(wordByDocument));
         props.setProperty("addInverseRelations", String.valueOf(addInverseRelations));
         props.setProperty("numberFeatureWords", String.valueOf(numberFeatureWords));
         props.setProperty("numberOfSimilarWords", String.valueOf(numberOfSimilarWords));
        props.setProperty("numberOfNgrams", String.valueOf(numberOfNgrams));
         props.setProperty("weightingMethod", weightingMethod);
         props.setProperty("minWeight", String.valueOf(minWeight));
         props.setProperty("similarityMeasure", String.valueOf(similarityMeasure));
         props.setProperty("dontCompute2ndOrder", String.valueOf(dontCompute2ndOrder));
         props.setProperty("existingCoocFile", existingCoocFile);
         props.setProperty("existingWeightFile", existingWeightFile);
         props.setProperty("discoVersion", String.valueOf(discoVersion));
         props.setProperty("minimumWordLength", String.valueOf(minimumWordLength));
         props.setProperty("maximumWordLength", String.valueOf(maximumWordLength));
         props.setProperty("allowedCharactersWord", allowedCharactersWord);
         props.setProperty("minimumFeatureLength", String.valueOf(minimumFeatureLength));
         props.setProperty("maximumFeatureLength", String.valueOf(maximumFeatureLength));
         props.setProperty("allowedCharactersFeature", allowedCharactersFeature);
         props.setProperty("findMultiTokenWords", String.valueOf(findMultiTokenWords));
         props.setProperty("multiTokenWordsDictionary", multiTokenWordsDictionary);
         props.setProperty("tokenAnnotatorMap", tokenAnnotatorMap);
         props.store(new FileOutputStream(new File(filename)), null);
    }
}
