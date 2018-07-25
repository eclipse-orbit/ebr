#!/bin/sh
mvn ebr:create-recipe -U -DgroupId=org.slf4j -DartifactId=slf4j-api -DbundleSymbolicName=org.slf4j.api.ebr "$@"
mvn ebr:create-recipe -U -DgroupId=org.slf4j -DartifactId=jcl-over-slf4j -DbundleSymbolicName=org.slf4j.jcl.ebr "$@"
mvn ebr:create-recipe -U -DgroupId=org.slf4j -DartifactId=jul-to-slf4j -DbundleSymbolicName=org.slf4j.jul.ebr "$@"
mvn ebr:create-recipe -U -DgroupId=org.slf4j -DartifactId=log4j-over-slf4j -DbundleSymbolicName=org.slf4j.log4j.ebr "$@"

mvn ebr:create-recipe -U -DgroupId=org.slf4j -DartifactId=slf4j-log4j12 -DbundleSymbolicName=org.slf4j.impl.log4j12.ebr "$@"
mvn ebr:create-recipe -U -DgroupId=org.slf4j -DartifactId=slf4j-nop -DbundleSymbolicName=org.slf4j.impl.nop.ebr "$@"
mvn ebr:create-recipe -U -DgroupId=org.slf4j -DartifactId=slf4j-simple -DbundleSymbolicName=org.slf4j.impl.simple.ebr "$@"
