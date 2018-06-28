# DISCO API
Java API for word embeddings

This is the source code repository for the linguatools DISCO API. For more information on DISCO visit [http://www.linguatools.de/disco/disco\_en.html](http://www.linguatools.de/disco/disco\_en.html).

## Quickstart
### Install DISCO API
Download the source code by cloning this repository:
```
git clone git@github.com:linguatools/disco.git
```
Go into the repository folder and build the executable jar with dependencies:
```
cd disco/
./gradlew shadowJar
```
For instructions on command line usage call DISCO API without any parameters:
```
java -jar build/libs/disco-3.0.0-all.jar
```
or consult the [web page](http://www.linguatools.de/disco/disco_en.html#aufruf).

### Import a vector file from fastText
Download a [fastText vector file](https://github.com/facebookresearch/fastText/blob/master/docs/crawl-vectors.md) in **text format** and unpack it:
```
wget https://s3-us-west-1.amazonaws.com/fasttext-vectors/word-vectors-v2/cc.de.300.vec.gz
gunzip cc.de.300.vec.gz
```
Download [DISCO Builder](http://www.linguatools.de/disco/disco-builder.html):
```
wget http://www.linguatools.de/disco/DISCOBuilder-1.1.tar.gz
tar zxf DISCOBuilder-1.1.tar.gz
```
Convert the vector file into a DISCO DenseMatrix:
```
java -Xmx8g -cp DISCOBuilder-1.1/DISCOBuilder-1.1.0-all.jar de.linguatools.disco.builder.Import -in cc.de.300.vec -out cc.de.300.col.denseMatrix -wsType COL 
```
Query the new DISCO word space from the command line with the DISCO API:
```
java -Xmx4g -jar ~/repos-linguatools/disco/build/libs/disco-3.0.0-all.jar cc.de.300.col.denseMatrix/cc.de.300-COL.denseMatrix -s Haus Wohnung COSINE
0.64413786
```

## Java API
To include DISCO in your Maven or Gradle project see below or visit the [DISCO page on JitPack](https://jitpack.io/#linguatools/disco).
### Gradle
Add this to your `build.gradle` file:
```
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    compile 'com.github.linguatools:disco:v3.0.0'
}
```
### Maven
Add this to your `pom.xml` file:
```
<repositories>
	<repository>
	    <id>jitpack.io</id>
	    <url>https://jitpack.io</url>
	</repository>
</repositories>

<dependency>
    <groupId>com.github.linguatools</groupId>
    <artifactId>disco</artifactId>
    <version>v3.0.0</version>
</dependency>
```
### Example Java code
```java
DISCO disco = DISCO.load("cc.de.300-COL.denseMatrix");
float sim = disco.semanticSimilarity("Haus", "Häuschen", 
      	    	DISCO.getVectorSimilarity(SimilarityMeasure.COSINE));
System.out.println("similarity between 'Haus' and 'Häuschen': "+sim);
// get word vector for "Haus" as map
Map<String,Float> wordVectorHaus = disco.getWordvector("Haus");
// get word embedding for "Haus" as float array
float[] wordEmbeddingHaus = ((DenseMatrix) disco).getWordEmbedding("Haus");
// solve analogy x is to "Frau" as "König" is to "Mann"
List<ReturnDataCol> result = Compositionality.solveAnalogy("Frau", "König", "Mann", disco); 
```

### Documentation
* [javadoc](http://www.linguatools.de/disco/disco-api-3.0/index.html)
* [DISCO home page](http://www.linguatools.de/disco/disco_en.html)

## How to get word spaces for DISCO?
* import vector files (text format) from [word2vec](http://code.google.com/p/word2vec/), [GloVe](http://nlp.stanford.edu/projects/glove/) or fastText using [DISCO Builder](http://www.linguatools.de/disco/disco-builder.html). There are pre-computed vector files from [fastText for 157 languages](https://fasttext.cc/docs/en/crawl-vectors.html). 
* you can download ready-to-use native DISCO word spaces (high-dimensional distributional count vectors) and DISCO word embeddings (low-dimensional predict vectors) imported from word2vec for several languages at [http://www.linguatools.de/disco/disco-download_en.html](http://www.linguatools.de/disco/disco-download_en.html).
* you can create your own high-dimensional distributional count vectors from a text corpus using [DISCO Builder](http://www.linguatools.de/disco/disco-builder.html).

## Features
- native Java API
- the API provides many useful methods for computing text similarity, solving analogies, clustering of similar words, compositional semantics, etc. 
- efficient storage of high-dimensional sparse matrices (distributional count vectors) as well as low-dimensional dense matrices (word embeddings)
- higher-order word similarities can be stored and retrieved efficiently
- API is open source with Apache license.
