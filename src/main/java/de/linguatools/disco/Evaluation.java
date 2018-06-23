/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.linguatools.disco;

import de.linguatools.disco.Compositionality.VectorCompositionMethod;
import de.linguatools.disco.DISCO.SimilarityMeasure;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.lucene.index.CorruptIndexException;

/**
 *
 * @author peterkolb
 */
public class Evaluation {
    
    public static void evaluateSimilarWordsGraphSearch(String discoFile) throws IOException,
            CorruptIndexException, FileNotFoundException, CorruptConfigFileException, WrongWordspaceTypeException{
        
        DenseMatrix dm = (DenseMatrix) DISCO.load(discoFile);
        System.out.println("DISCO word space loaded.");
        System.out.println("number of similar words = "+dm.numberOfSimilarWords());
        if( dm.simMatrix == null ){
            System.out.println("sim-Matrix ist null.");
        }else{
            System.out.println("sim-Matrix dim = "+dm.simMatrix[0].length);
        }
        System.out.flush();
        
        String nochmal = "";
        do{
            // erzeuge kompositionelles word embedding
            List<String> inputWords = new ArrayList<>();
            System.out.println("Enter a number of words (empty to finish):");
            Scanner scanner = new Scanner(System.in);
            String inputWord = "_go_";
            while( !inputWord.isEmpty() ){
                System.out.print("word: ");
                inputWord = scanner.nextLine();
                if( !inputWord.isEmpty() ){
                    inputWords.add(inputWord);
                }
            }
            System.out.println(inputWords.size()+" input words.");
            String[] multi = new String[inputWords.size()];
            multi = inputWords.toArray(multi);
            float[] multiWordEmbedding = Compositionality.computeWordVector(multi, dm, 
                    Compositionality.VectorCompositionMethod.ADDITION, null, null, null, null);
            int nMax = 10;
            // suche ähnlichste Wörter mit similarWords
            List<ReturnDataCol> simWordsExact = Compositionality.similarWords(multiWordEmbedding,
                    dm, DISCO.SimilarityMeasure.COSINE, nMax);

            // suche ähnlichste Wörter mit similarWordsGraphSearch
            List<ReturnDataCol> simWordsApprox = Compositionality.similarWordsGraphSearch(
                    multiWordEmbedding, dm, DISCO.SimilarityMeasure.COSINE, nMax);

            // was gefunden?
            if( simWordsExact.isEmpty() ){
                System.out.println("keine ähnlichen Wörter gefunden mit exact.");
            }else{
                System.out.println("ähnlichstes mit exact: "+simWordsExact.get(0).word);
            }
            if( simWordsApprox.isEmpty() ){
                System.out.println("keine ähnlichen Wörter gefunden mit approx.");
            }else{
                System.out.println("ähnlichstes mit approx: "+simWordsApprox.get(0).word);
            }
            if( simWordsExact.isEmpty() || simWordsApprox.isEmpty() ){
                continue;
            }
            
            // vergleiche beide: 
            // - an welcher Stelle in der approx. Liste ist das ähnlichste Wort?
            int i;
            for( i = 0; i < simWordsApprox.size(); i++ ){
                if( simWordsExact.get(0).word.equals(simWordsApprox.get(i).word) ){
                    break;
                }
            }
            i++;
            if( i > nMax ){
                System.out.println("echtes ähnlichstes Wort nicht gefunden in top "+nMax);
            }else{
                System.out.println("echtes ähnlichstes Wort an Platz "+i);
            }
            // - wie hoch ist die Überschneidung der top 10 Wörter beider Listen?
            Map<String,Byte> mapExact = new HashMap<>();
            for( ReturnDataCol c : simWordsExact ){
                mapExact.put(c.word, Byte.MIN_VALUE);
            }
            int treffer = 0;
            for( ReturnDataCol c : simWordsApprox ){
                if( mapExact.containsKey(c.word) ){
                    treffer++;
                }
            }
            System.out.println(treffer+" Wörter aus approx sind unter top "+nMax+" von exact");
            // - wie groß ist die Ähnlichkeit der beiden top-Wörter?
            double sim = DISCO.getVectorSimilarity(SimilarityMeasure.COSINE).computeSimilarity(
                    dm.getWordEmbedding(simWordsApprox.get(0).word), 
                    dm.getWordEmbedding(simWordsExact.get(0).word));
            System.out.println("sim approx - exact (top word) = "+sim);
            // Listen ausgeben
            System.out.println("exact: ");
            for(ReturnDataCol c : simWordsExact){
                System.out.println(" "+c.word);
            }
            System.out.println("\napprox: ");
            for(ReturnDataCol c : simWordsApprox){
                System.out.println(" "+c.word);
            }
            // nochmal?
            System.out.println("Nochmal? (j/n) ");
            nochmal = scanner.nextLine();
        }while( !nochmal.toLowerCase().equals("n") );
    }
    
    public static void testVectorRejection(String discoFile) throws IOException, 
            CorruptIndexException, FileNotFoundException, CorruptConfigFileException{
        
        DenseMatrix dm = (DenseMatrix) DISCO.load(discoFile);
        System.out.println("DISCO word space loaded.");
        System.out.flush();
        
        // bank_without_finance = vectorRejection(bank, 
        //                             averageVector(deposit, account, cashier))
        float[] bank = dm.getWordEmbedding("bank");
        List<float[]> financeVectors = new ArrayList<>();
        financeVectors.add(dm.getWordEmbedding("deposit"));
        financeVectors.add(dm.getWordEmbedding("account"));
        financeVectors.add(dm.getWordEmbedding("cashier"));
        float[] financeAvg = DenseVector.average(financeVectors); 
        
        float[] riverBank = Compositionality.vectorRejection(bank, financeAvg);
        List<ReturnDataCol> simList = Compositionality.similarWords(riverBank, dm,
                DISCO.SimilarityMeasure.COSINE, 10);
        for( ReturnDataCol c : simList ){
            System.out.println(c.word+"\t"+c.value);
        }
    }
    
    private static void vectorAlgebra(String discoFile) throws IOException, 
            CorruptIndexException, FileNotFoundException, CorruptConfigFileException{
        
        DenseMatrix dm = (DenseMatrix) DISCO.load(discoFile);
        System.out.println("DISCO word space loaded.");
        System.out.flush();
        
        String nochmal;
        do{
            Scanner scanner = new Scanner(System.in);
            VectorCompositionMethod cm;
            System.out.println("VectorCompositionMethod: ");
            String input = scanner.nextLine();
            if( input.startsWith("a") ){
                cm = VectorCompositionMethod.ADDITION;
            }else if( input.startsWith("s") ){
                cm = VectorCompositionMethod.SUBTRACTION;
            }else if( input.startsWith("m") ){
                cm = VectorCompositionMethod.MULTIPLICATION;
            }else{
                cm = VectorCompositionMethod.ADDITION;
            }
            // w1
            System.out.println("word 1: ");
            String w1 = scanner.nextLine();
            // w2
            System.out.println("word 2: ");
            String w2 = scanner.nextLine();
            // compose
            float[] comp = Compositionality.composeWordVectors(
                    dm.getWordEmbedding(w1), dm.getWordEmbedding(w2), cm, null,
                    null, null, null);
            // most similar
            List<ReturnDataCol> sims = Compositionality.similarWords(comp, dm, 
                    SimilarityMeasure.COSINE, 5);
            for( ReturnDataCol c : sims ){
                System.out.println(c.word+"\t"+c.value);
            }
            // nochmal?
            System.out.println("Nochmal? (j/n) ");
            nochmal = scanner.nextLine();
        }while( !nochmal.toLowerCase().equals("n") );
    }
    
    /**
     * CLI.
     * @param args 
     */
    public static void main(String[] args) throws IOException, CorruptIndexException,
            FileNotFoundException, CorruptConfigFileException, WrongWordspaceTypeException{
        
        if( args.length < 2 ){
            System.out.println("-a discoFile      test approx graph search");
            System.out.println("-r discoFile      test vector rejection");
            System.out.println("-v discoFile      vector algebra");
            return;
        }
        
        if( args[0].equals("-a") ){
            evaluateSimilarWordsGraphSearch(args[1]);
        }else if( args[0].equals("-r") ){
            testVectorRejection(args[1]);
        }else if( args[0].equals("-v") ){
            vectorAlgebra(args[1]);
        }
    }
}
