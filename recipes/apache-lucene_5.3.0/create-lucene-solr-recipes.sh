#!/bin/sh
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-core -DbundleSymbolicName=org.apache.lucene.core
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-queries -DbundleSymbolicName=org.apache.lucene.queries
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-common -DbundleSymbolicName=org.apache.lucene.analyzers.common
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-highlighter -DbundleSymbolicName=org.apache.lucene.highlighter
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-queryparser -DbundleSymbolicName=org.apache.lucene.queryparser
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-memory -DbundleSymbolicName=org.apache.lucene.memory
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-spatial -DbundleSymbolicName=org.apache.lucene.spatial
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-misc -DbundleSymbolicName=org.apache.lucene.misc
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-codecs -DbundleSymbolicName=org.apache.lucene.codecs
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-grouping -DbundleSymbolicName=org.apache.lucene.grouping
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-suggest -DbundleSymbolicName=org.apache.lucene.suggest
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-facet -DbundleSymbolicName=org.apache.lucene.facet
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-join -DbundleSymbolicName=org.apache.lucene.join
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-smartcn -DbundleSymbolicName=org.apache.lucene.analyzers.smartcn
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-stempel -DbundleSymbolicName=org.apache.lucene.analyzers.stempel
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-icu -DbundleSymbolicName=org.apache.lucene.analyzers.icu
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-phonetic -DbundleSymbolicName=org.apache.lucene.analyzers.phonetic
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-kuromoji -DbundleSymbolicName=org.apache.lucene.analyzers.kuromoji
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-uima -DbundleSymbolicName=org.apache.lucene.analyzers.uima
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-morfologik -DbundleSymbolicName=org.apache.lucene.analyzers.morfologik
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-expressions -DbundleSymbolicName=org.apache.lucene.expressions
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-classification -DbundleSymbolicName=org.apache.lucene.classification
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-replicator -DbundleSymbolicName=org.apache.lucene.replicator




mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-core -DbundleSymbolicName=org.apache.solr.core
mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-solrj -DbundleSymbolicName=org.apache.solr.solrj
mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-analysis-extras -DbundleSymbolicName=org.apache.solr.analysis.extras
#mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-dataimporthandler -DbundleSymbolicName=org.apache.solr.dataimporthandler
#mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-dataimporthandler-extras -DbundleSymbolicName=org.apache.solr.dataimporthandler.extras
mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-cell -DbundleSymbolicName=org.apache.solr.cell
mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-clustering -DbundleSymbolicName=org.apache.solr.clustering
#mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-velocity -DbundleSymbolicName=org.apache.solr.velocity
mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-uima -DbundleSymbolicName=org.apache.solr.uima
mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-langid -DbundleSymbolicName=org.apache.solr.langid
mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr -DbundleSymbolicName=org.apache.solr.server
