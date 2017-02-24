#!/bin/sh
mvn ebr:create-recipe -U -DgroupId=ch.qos.logback -DartifactId=logback-core -DbundleSymbolicName=ch.qos.logback.core.ebr
mvn ebr:create-recipe -U -DgroupId=ch.qos.logback -DartifactId=logback-classic -DbundleSymbolicName=ch.qos.logback.classic.ebr
mvn ebr:create-recipe -U -DgroupId=ch.qos.logback -DartifactId=logback-classic -DbundleSymbolicName=ch.qos.logback.slf4j.ebr

echo "\n"
echo "\n"
echo "\n"
echo "\n"
echo 'Do not forget to set recipe.includes/recipe.excludes for slf4j/classic bundle!'