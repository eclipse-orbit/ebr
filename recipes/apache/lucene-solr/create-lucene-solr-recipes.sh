#!/bin/sh
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-core -DbundleSymbolicName=org.apache.lucene.core.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-queries -DbundleSymbolicName=org.apache.lucene.queries.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-common -DbundleSymbolicName=org.apache.lucene.analyzers.common.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-highlighter -DbundleSymbolicName=org.apache.lucene.highlighter.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-queryparser -DbundleSymbolicName=org.apache.lucene.queryparser.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-memory -DbundleSymbolicName=org.apache.lucene.memory.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-spatial -DbundleSymbolicName=org.apache.lucene.spatial.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-misc -DbundleSymbolicName=org.apache.lucene.misc.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-codecs -DbundleSymbolicName=org.apache.lucene.codecs.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-grouping -DbundleSymbolicName=org.apache.lucene.grouping.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-suggest -DbundleSymbolicName=org.apache.lucene.suggest.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-facet -DbundleSymbolicName=org.apache.lucene.facet.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-join -DbundleSymbolicName=org.apache.lucene.join.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-smartcn -DbundleSymbolicName=org.apache.lucene.analyzers.smartcn.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-stempel -DbundleSymbolicName=org.apache.lucene.analyzers.stempel.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-icu -DbundleSymbolicName=org.apache.lucene.analyzers.icu.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-phonetic -DbundleSymbolicName=org.apache.lucene.analyzers.phonetic.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-kuromoji -DbundleSymbolicName=org.apache.lucene.analyzers.kuromoji.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-uima -DbundleSymbolicName=org.apache.lucene.analyzers.uima.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-analyzers-morfologik -DbundleSymbolicName=org.apache.lucene.analyzers.morfologik.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-expressions -DbundleSymbolicName=org.apache.lucene.expressions.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-classification -DbundleSymbolicName=org.apache.lucene.classification.ebr
mvn ebr:create-recipe -DgroupId=org.apache.lucene -DartifactId=lucene-replicator -DbundleSymbolicName=org.apache.lucene.replicator.ebr




mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-core -DbundleSymbolicName=org.apache.solr.core.ebr
mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-solrj -DbundleSymbolicName=org.apache.solr.solrj.ebr
mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-analysis-extras -DbundleSymbolicName=org.apache.solr.analysis.extras.ebr
#mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-dataimporthandler -DbundleSymbolicName=org.apache.solr.dataimporthandler.ebr
#mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-dataimporthandler-extras -DbundleSymbolicName=org.apache.solr.dataimporthandler.extras.ebr
mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-cell -DbundleSymbolicName=org.apache.solr.cell.ebr
mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-clustering -DbundleSymbolicName=org.apache.solr.clustering.ebr
#mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-velocity -DbundleSymbolicName=org.apache.solr.velocity
mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-uima -DbundleSymbolicName=org.apache.solr.uima.ebr
mvn ebr:create-recipe -DgroupId=org.apache.solr -DartifactId=solr-langid -DbundleSymbolicName=org.apache.solr.langid.ebr
