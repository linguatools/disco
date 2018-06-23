/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.linguatools.disco;

import java.util.ArrayList;
import java.util.List;
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
public class DenseVectorTest {
    
    public DenseVectorTest() {
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
    public void testAddScalarMul() {
        
        float[] v1 = new float[100];
        float[] v2 = new float[100];
        for( int i = 0; i < v1.length; i++ ){
            v1[i] = i;
            v2[i] = i;
        }
        float[] add = DenseVector.add(v1, v2);
        float[] mul = DenseVector.mul(v1, 2);
        Assert.assertArrayEquals(add, mul, 0.0001F);
    }
    
    @Test
    public void testAvg() {
        
        float[] v1 = new float[100];
        float[] v2 = new float[100];
        float[] exp = new float[100];
        for( int i = 0; i < v1.length; i++ ){
            v1[i] = 2.0F * i;
            exp[i] = i;
        }
        List<float[]> vectorList = new ArrayList<>();
        vectorList.add(v1);
        vectorList.add(v2);
        float[] avg = DenseVector.average(vectorList);
        Assert.assertArrayEquals(exp, avg, 0.0001F);
    }
}
