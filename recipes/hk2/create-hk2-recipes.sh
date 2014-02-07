#!/bin/sh
set -e

# external
mvn ebr:create-recipe -DgroupId=org.glassfish.hk2.external -DartifactId=asm-all-repackaged -DbundleSymbolicName=org.glassfish.hk2.external.asm

