/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.linguatools.disco;

import java.util.ArrayList;
import java.util.List;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author peterkolb
 */
public class NgramsTest {
    
    public NgramsTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void testAddNGramsSizeN() {
        
        String word = "Häuserchen";
        int n = 3;
        List<String> allNGrams = new ArrayList<>();
        List<String> exp = new ArrayList<>();
        exp.add("Häu");
        exp.add("äus");
        exp.add("use");
        exp.add("ser");
        exp.add("erc");
        exp.add("rch");
        exp.add("che");
        exp.add("hen");
        Ngrams.addNGramsSizeN(word, n, allNGrams);
        Assert.assertThat(allNGrams, IsIterableContainingInOrder.contains(
                exp.toArray()));
    }
    
    @Test
    public void testAddAllNGramsFromWord() {
        
        String word = "Häuserchen";
        int minN = 2;
        int maxN = 3;
        List<String> exp = new ArrayList<>();
        // n=2
        exp.add("<H");
        exp.add("Hä");
        exp.add("äu");
        exp.add("us");
        exp.add("se");
        exp.add("er");
        exp.add("rc");
        exp.add("ch");
        exp.add("he");
        exp.add("en");
        exp.add("n>");
        // n=3
        exp.add("<Hä");
        exp.add("Häu");
        exp.add("äus");
        exp.add("use");
        exp.add("ser");
        exp.add("erc");
        exp.add("rch");
        exp.add("che");
        exp.add("hen");
        exp.add("en>");
        
        List<String> allNGrams = Ngrams.extractAllNGramsFromWord(word, minN, maxN);
        Assert.assertThat(allNGrams, IsIterableContainingInOrder.contains(
                exp.toArray()));
    }
}
