#!/bin/sh
set -euv

mvn -U -e -V ebr:create-recipe -DgroupId=org.glassfish.hk2 -DartifactId=hk2-api -DbundleSymbolicName=org.glassfish.hk2.api.ebr
mvn -U -e -V ebr:create-recipe -DgroupId=org.glassfish.hk2 -DartifactId=hk2-locator -DbundleSymbolicName=org.glassfish.hk2.locator.ebr
mvn -U -e -V ebr:create-recipe -DgroupId=org.glassfish.hk2 -DartifactId=hk2-utils -DbundleSymbolicName=org.glassfish.hk2.utils.ebr

mvn -U -e -V ebr:create-recipe -DgroupId=org.glassfish.hk2 -DartifactId=osgi-resource-locator -DbundleSymbolicName=org.glassfish.hk2.osgiresourcelocator.ebr

mvn -U -e -V ebr:create-recipe -DgroupId=org.glassfish.hk2.external -DartifactId=asm-all-repackaged -DbundleSymbolicName=org.glassfish.hk2.external.asm.ebr

