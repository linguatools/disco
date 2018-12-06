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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 *
 * @author peterkolb
 */
public class UnicodeTokenizer extends Analyzer{
    
    @Override
    protected TokenStreamComponents createComponents(String field) {
        Tokenizer tokenizer = new StandardTokenizer();
        return new TokenStreamComponents(tokenizer);
    }
    
    private static final Analyzer ANALYZER = new UnicodeTokenizer();
    
    public static List<String> tokenize(String text){
        
        List<String> tokens = new ArrayList<>();
        try {
            TokenStream ts  = ANALYZER.tokenStream("field", text);
            OffsetAttribute offsetAtt = ts.addAttribute(OffsetAttribute.class);
            ts.reset();
            int lastEnd = 0;
            while(ts.incrementToken()) {
                // non-term characters before current term
                if( offsetAtt.startOffset() > lastEnd ){
                    String t = text.substring(lastEnd, offsetAtt.startOffset());
                    if( !t.matches("\\s*") ){
                        tokens.add(t.trim());
                    }
                }
                // current term
                tokens.add(text.substring(offsetAtt.startOffset(), offsetAtt.endOffset()));
                lastEnd = offsetAtt.endOffset();
            }
            // non-term characters after final term
            if( lastEnd < text.length() ){
                String t = text.substring(lastEnd);
                if( !t.matches("\\s*") ){
                    tokens.add(t.trim());
                }
            }
            ts.end();
            ts.close();
        }
        catch(IOException e) {
            // never thrown b/c we're using a string reader
        }
        return tokens;
    }
    
}
