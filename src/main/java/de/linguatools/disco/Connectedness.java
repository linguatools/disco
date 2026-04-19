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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Methods related to connectivity in the neighborhood graph of word spaces.
 * @author peter
 */
public class Connectedness {

    /**
     * Exhaustive breath-first search to find the shortest path between two input
     * words in the neighborhood graph. Interestingly, this always finds a path
     * (at least if <code>numberOfSimilarWords &gt;= 50</code>), showing that the
     * neighborhood graph of word spaces is fully connected. For more information
     * on the neighborhood graph see <code>similarWordsGraphSearch</code>.<br>
     * <b>Important</b>: This method only works with word spaces of type SIM.<br>
     * @param i1 ID of input word #1
     * @param i2 ID of input word #2
     * @param denseMatrix
     * @return shortest path between <code>i1</code> and <code>i2</code>. The 
     * resulting list contains the path in reverse order, i.e. the first list 
     * element is <code>i2</code>, the last element is <code>i1</code>.
     * @throws de.linguatools.disco.WrongWordspaceTypeException 
     */
    public static List<Integer> findShortestPath(int i1, int i2, DenseMatrix denseMatrix) 
            throws WrongWordspaceTypeException{
        
        if( denseMatrix.getWordspaceType() != WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + " to word spaces of type "+denseMatrix.getWordspaceType());
        }
        
        // Breitensuche
        int val[] = new int[denseMatrix.numberOfWords()+1];
        int back[] = new int[denseMatrix.numberOfWords()+1];
        int nr = 0;
        List<Integer> queue = new LinkedList<>();
        queue.add(i1);
        boolean reached = false;
        
        while( !queue.isEmpty() ){
            int k = queue.remove(0);
            // gefunden?
            if( k == i2 ){
                reached = true;
                break;
            }
            val[k] = ++nr;
            // besuche Nachbarknoten von k = ähnliche Wörter
            for( int n = 0; n < denseMatrix.numberOfSimilarWords(); n++ ){
                if( val[denseMatrix.getSecondOrderWordvector(k)[n]] == 0 ){
                    queue.add( denseMatrix.getSecondOrderWordvector(k)[n] );
                    back[denseMatrix.getSecondOrderWordvector(k)[n]] = k;
                    val[ denseMatrix.getSecondOrderWordvector(k)[n] ] = -1;
                }
            }
        }
        
        if( !reached ){
            return new ArrayList<>();
        }
        
        List<Integer> shortestPath = new LinkedList<>();
        shortestPath.add(i2);
        int p = i2;
        do{
            p = back[p];
            shortestPath.add(p);
        }while( p != i1 );
        
        return shortestPath;
    }
    
    /**
     * Wrapper method.
     * @param w1
     * @param w2
     * @param denseMatrix
     * @return the words forming the shortest path.
     * @throws WrongWordspaceTypeException 
     * @throws java.io.IOException 
     */
    public static List<String> findShortestPath(String w1, String w2, 
            DenseMatrix denseMatrix) 
            throws WrongWordspaceTypeException, IOException{
        
        if( denseMatrix.getWordspaceType() != WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + " to word spaces of type "+denseMatrix.getWordspaceType());
        }
        
        // lookup word IDs
        int id1 = denseMatrix.getWordId(w1);
        if( id1 == -1 ){
            return null;
        }
        int id2 = denseMatrix.getWordId(w2);
        if( id2 == -1 ){
            return null;
        }
        
        List<Integer> idsPath = findShortestPath(id1, id2, denseMatrix);
        
        List<String> path = new ArrayList<>();
        for( Integer id : idsPath ){
            path.add( denseMatrix.getWord(id) );
        }
        return path;
    }
    
    /**
     * For a {@link DenseMatrix} of type {@link WordspaceType#SIM}, checks whether
     * each unordered vocabulary pair is connected in the neighborhood graph
     * (edges point from a word to its stored similar words). Uses
     * {@link DISCO#getVocabularyIterator()} and {@link #findShortestPath(int, int, DenseMatrix)}.
     * <p>
     * Word pairs without a path are written to {@code disconnectedPairsLogFile}
     * (one tab-separated pair per line). Progress is reported on standard output.
     * When every pair is connected, prints {@code Graph is fully connected!} In all
     * cases with at least one connected pair, prints the longest shortest-path length
     * (in edges), and the average and median of those lengths.
     * </p>
     * @param denseMatrix SIM-type word space
     * @param disconnectedPairsLogFile path to the file receiving disconnected word pairs
     * @throws WrongWordspaceTypeException if {@code denseMatrix} is not SIM
     * @throws IOException from {@link DISCO#getVocabularyIterator()}, word lookup,
     * or file output
     */
    public static void logWordspacePairConnectivity(DenseMatrix denseMatrix,
            String disconnectedPairsLogFile)
            throws WrongWordspaceTypeException, IOException{
        
        if( denseMatrix.getWordspaceType() != WordspaceType.SIM ){
            throw new WrongWordspaceTypeException("This method can not be applied"
                    + " to word spaces of type "+denseMatrix.getWordspaceType());
        }
        
        List<String> words = new ArrayList<>();
        Iterator<String> vit = denseMatrix.getVocabularyIterator();
        while( vit.hasNext() ){
            words.add( vit.next() );
        }
        
        int n = words.size();
        if( n < 2 ){
            return;
        }
        
        List<Integer> edgeLengths = new ArrayList<>();
        
        long totalPairs = (long) n * (n - 1) / 2;
        long progressStep = Math.max( 1L, totalPairs / 50 );
        int lastPct = -1;
        long processed = 0;
        
        try( PrintWriter pairLog = new PrintWriter( new OutputStreamWriter(
                new FileOutputStream( disconnectedPairsLogFile ), StandardCharsets.UTF_8 ) ) ){
            
            for( int i = 0; i < n; i++ ){
                int id1 = denseMatrix.getWordId( words.get(i) );
                for( int j = i + 1; j < n; j++ ){
                    int id2 = denseMatrix.getWordId( words.get(j) );
                    List<Integer> path = findShortestPath( id1, id2, denseMatrix );
                    if( path.isEmpty() ){
                        pairLog.println( words.get(i)+"\t"+words.get(j) );
                    }else{
                        edgeLengths.add( path.size() - 1 );
                    }
                    
                    processed++;
                    int pct = totalPairs > 0 ? (int) (100 * processed / totalPairs) : 100;
                    if( processed % progressStep == 0 || processed == totalPairs || pct != lastPct ){
                        lastPct = pct;
                        System.out.print( "\rProgress: "+processed+"/"+totalPairs+" pairs ("+pct+"%)" );
                        System.out.flush();
                    }
                }
            }
        }
        
        System.out.println();
        
        if( edgeLengths.size() == n * (n - 1) / 2 ){
            System.out.println( "Graph is fully connected!" );
        }
        
        if( !edgeLengths.isEmpty() ){
            int longest = Collections.max( edgeLengths );
            double sum = 0.0;
            for( int len : edgeLengths ){
                sum += len;
            }
            double average = sum / edgeLengths.size();
            
            Collections.sort( edgeLengths );
            int m = edgeLengths.size();
            double median;
            if( m % 2 == 1 ){
                median = edgeLengths.get( m / 2 );
            }else{
                median = ( edgeLengths.get( m / 2 - 1 ) + edgeLengths.get( m / 2 ) ) / 2.0;
            }
            
            System.out.println( "Longest shortest path (edges): "+longest );
            System.out.println( "Average shortest path length (edges): "+average );
            System.out.println( "Median shortest path length (edges): "+median );
        }
    }
}
