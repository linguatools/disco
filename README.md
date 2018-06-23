# DISCO API
compute semantic similarity between arbitrary words and phrases in many languages

This is the official source code repository for linguatools' DISCO API. For more information on DISCO visit [http://www.linguatools.de/disco/disco_en.html](http://www.linguatools.de/disco/disco_en.html)

DISCO needs a pre-computed database of word similarities. This database is also called a *word space*.
DISCO consists of 
- the DISCO API (this repository) to query an existing word space: [API description](http://www.linguatools.de/disco/disco-api-3.0/index.html) (javadoc)
- [DISCO Builder](http://www.linguatools.de/disco/disco-builder.html) which allows to create a word space from a text corpus, or to import it from [word2vec](http://code.google.com/p/word2vec/) or [GloVe](http://nlp.stanford.edu/projects/glove/) vector files.

You can download ready-to-use native DISCO word spaces (high-dimensional distributional count vectors) and DISCO word embeddings (low-dimensional predict vectors) imported from word2vec for many languages at [http://www.linguatools.de/disco/disco-download_en.html](http://www.linguatools.de/disco/disco-download_en.html).

## Usage
### Java API
To include DISCO in your Maven or Gradle project visit the [DISCO page on JitPack](https://jitpack.io/#linguatools/disco).
See the [javadoc](http://www.linguatools.de/disco/disco-api-3.0/index.html) and the [example Java code](https://github.com/linguatools/disco/blob/master/UseDISCO.java).

### Command line
1. Download the source code by cloning this repository:
```
git clone git@github.com:linguatools/disco.git
```
2. Go into the repository folder and build the executable jar with dependencies:
```
cd disco/
./gradlew fatJar
```
3. For instructions on command line usage call DISCO API without any parameters:
```
java -jar build/libs/disco-all-3.0.jar
```
or consult the [web page](http://www.linguatools.de/disco/disco_en.html#aufruf).

## Features
- native Java API
- the API provides many useful methods for computing text similarity, solving analogies, clustering of similar words, compositional semantics, etc. 
- efficient storage of high-dimensional sparse matrices (classic distributional count vectors) as well as low-dimensional dense matrices (word embeddings)
- higher-order word similarities can be stored and retrieved efficiently
- API is open source with Apache license.
