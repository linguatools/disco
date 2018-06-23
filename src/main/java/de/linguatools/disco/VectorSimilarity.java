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

import java.util.Map;
import org.apache.lucene.document.Document;

/**
 * Interface for implementing vector similarity measures.
 * @author peter
 */
public interface VectorSimilarity {
    
    /**
     * Compute similarity between two dense vectors.
     * @param denseVector1
     * @param denseVector2
     * @return 
     */
    public double computeSimilarity(float[] denseVector1, float[] denseVector2);
    
    /**
     * Compute similarity between two sparse vectors.
     * @param mapVector1
     * @param mapVector2
     * @return 
     */
    public double computeSimilarity(Map<String,Float> mapVector1, 
            Map<String,Float> mapVector2);
    
    /**
     * Compute similarity between two vectors stored in Lucene Documents.
     * @param doc1
     * @param doc2
     * @return 
     */
    public double computeSimilarity(Document doc1, Document doc2);
    
}
