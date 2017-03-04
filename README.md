# disco
compute semantic similarity between arbitrary words and phrases in many languages

This is the source code repository for DISCO API version 3.0. For more information on DISCO visit [http://www.linguatools.de/disco/disco_en.html](http://www.linguatools.de/disco/disco_en.html)

DISCO needs a pre-computed database of word similarities. This database is also called a *word space*.
DISCO consists of 
- the DISCO API (this repository) to query an existing word space: [API description](http://www.linguatools.de/disco/disco_en.html#api) (javadoc)
- [DISCO Builder](http://www.linguatools.de/disco/disco-builder.html) which allows to create a word space from a text corpus, or to import it from [word2vec](http://code.google.com/p/word2vec/) or [GloVe](http://nlp.stanford.edu/projects/glove/) vector files.

You can download ready-to-use DISCO word spaces and word2vec word embeddings for many languages at [http://www.linguatools.de/disco/disco-download_en.html](http://www.linguatools.de/disco/disco-download_en.html).

## Usage


## Features
- native Java API
- the API provides many useful methods for computing text similarity, solving analogies, clustering of similar words, compositional semantics, etc. 
- efficient storage of high-dimensional sparse matrices (classic distributional count vectors) as well as low-dimensional dense matrices (word embeddings)
- higher-order word similarities can be stored and retrieved efficiently
- open source with Apache license.
