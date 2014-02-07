#!/bin/sh
set -e

# core
mvn ebr:create-recipe -DgroupId=org.glassfish.jersey.core -DartifactId=jersey-common -DbundleSymbolicName=org.glassfish.jersey.core.common
mvn ebr:create-recipe -DgroupId=org.glassfish.jersey.core -DartifactId=jersey-client -DbundleSymbolicName=org.glassfish.jersey.core.client
mvn ebr:create-recipe -DgroupId=org.glassfish.jersey.core -DartifactId=jersey-server -DbundleSymbolicName=org.glassfish.jersey.core.server

# containers
mvn ebr:create-recipe -DgroupId=org.glassfish.jersey.containers -DartifactId=jersey-container-servlet-core -DbundleSymbolicName=org.glassfish.jersey.containers.servlet.core
mvn ebr:create-recipe -DgroupId=org.glassfish.jersey.containers -DartifactId=jersey-container-servlet -DbundleSymbolicName=org.glassfish.jersey.containers.servlet

# client connectors
mvn ebr:create-recipe -DgroupId=org.glassfish.jersey.connectors -DartifactId=jersey-jetty-connector -DbundleSymbolicName=org.glassfish.jersey.connectors.jetty
mvn ebr:create-recipe -DgroupId=org.glassfish.jersey.connectors -DartifactId=jersey-apache-connector -DbundleSymbolicName=org.glassfish.jersey.connectors.apache

# extensions
mvn ebr:create-recipe -DgroupId=org.glassfish.jersey.ext -DartifactId=jersey-entity-filtering -DbundleSymbolicName=org.glassfish.jersey.ext.entityfiltering

# media support
mvn ebr:create-recipe -DgroupId=org.glassfish.jersey.media -DartifactId=jersey-media-multipart -DbundleSymbolicName=org.glassfish.jersey.media.multipart
mvn ebr:create-recipe -DgroupId=org.glassfish.jersey.media -DartifactId=jersey-media-moxy -DbundleSymbolicName=org.glassfish.jersey.media.moxy
mvn ebr:create-recipe -DgroupId=org.glassfish.jersey.media -DartifactId=jersey-media-sse -DbundleSymbolicName=org.glassfish.jersey.media.sse

# other
mvn ebr:create-recipe -DgroupId=org.glassfish.jersey.bundles.repackaged -DartifactId=jersey-guava -DbundleSymbolicName=org.glassfish.jersey.repackaged.guava

