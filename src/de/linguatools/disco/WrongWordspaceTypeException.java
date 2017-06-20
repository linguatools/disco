/*
 * Copyright (C) 2012, 2015 Peter Kolb & Prochazkova GbR
 * peter.kolb@linguatools.org
 */
package de.linguatools.disco;

/**
 * This exception is thrown when a method that only works with word spaces of
 * type <code>WordspaceType.SIM</code> is called with a word space of type
 * <code>WordspaceType.COL</code>.
 * @author peter
 * @version 2.0
 */
public class WrongWordspaceTypeException extends Exception{
    
    public WrongWordspaceTypeException(){
        
    }
    
    public WrongWordspaceTypeException(String s){
        super(s);
    }
}
