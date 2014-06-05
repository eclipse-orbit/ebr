#!/bin/bash
set -eux

#
# This script can be used to replace the Eclipse group id with soomething else.
# 

echoerr() { echo "$@" 1>&2; }

if [ -z ${1+x} ] || [ -z "$1" ]; then
  echoerr "ERROR: missing group id"
  echo "$(basename "$0") <new.group.id>"
else
  MYPATH="$( cd "$(dirname "$0")" ; pwd -P )"
  find "${MYPATH}/../../recipes" -name "pom.xml" -print0 | xargs -0 sed -i '' -e "s/MY_EBR_BUNDLES_GROUP/$1/g"
fi

