#!/bin/bash
#
# Usage: ./copy_springsecurity_into_artifacts.bash <path of spring security built artifacts> <tag name to pull support files> <new tag name>
#
#

SPRING_BASE=$1
OLD_TAG=$2
NEW_TAG=$3

mkdir org.springframework.security.acls/${NEW_TAG}/
mkdir org.springframework.security.aspects/${NEW_TAG}/
mkdir org.springframework.security.cas/${NEW_TAG}/
mkdir org.springframework.security.config/${NEW_TAG}/
mkdir org.springframework.security.core/${NEW_TAG}/
mkdir org.springframework.security.crypto/${NEW_TAG}/
mkdir org.springframework.security.ldap/${NEW_TAG}/
mkdir org.springframework.security.openid/${NEW_TAG}/
mkdir org.springframework.security.remoting/${NEW_TAG}/
mkdir org.springframework.security.taglibs/${NEW_TAG}/
mkdir org.springframework.security.web/${NEW_TAG}/

cp org.springframework.security.acls/${OLD_TAG}/* org.springframework.security.acls/${NEW_TAG}/
cp org.springframework.security.aspects/${OLD_TAG}/* org.springframework.security.aspects/${NEW_TAG}/
cp org.springframework.security.cas/${OLD_TAG}/* org.springframework.security.cas/${NEW_TAG}/
cp org.springframework.security.config/${OLD_TAG}/* org.springframework.security.config/${NEW_TAG}/
cp org.springframework.security.core/${OLD_TAG}/* org.springframework.security.core/${NEW_TAG}/
cp org.springframework.security.crypto/${OLD_TAG}/* org.springframework.security.crypto/${NEW_TAG}/
cp org.springframework.security.ldap/${OLD_TAG}/* org.springframework.security.ldap/${NEW_TAG}/
cp org.springframework.security.openid/${OLD_TAG}/* org.springframework.security.openid/${NEW_TAG}/
cp org.springframework.security.remoting/${OLD_TAG}/* org.springframework.security.remoting/${NEW_TAG}/
cp org.springframework.security.taglibs/${OLD_TAG}/* org.springframework.security.taglibs/${NEW_TAG}/
cp org.springframework.security.web/${OLD_TAG}/* org.springframework.security.web/${NEW_TAG}/

rm org.springframework.security.acls/${NEW_TAG}/org.springframework*.jar
rm org.springframework.security.aspects/${NEW_TAG}/org.springframework*.jar
rm org.springframework.security.cas/${NEW_TAG}/org.springframework*.jar
rm org.springframework.security.config/${NEW_TAG}/org.springframework*.jar
rm org.springframework.security.core/${NEW_TAG}/org.springframework*.jar
rm org.springframework.security.crypto/${NEW_TAG}/org.springframework*.jar
rm org.springframework.security.ldap/${NEW_TAG}/org.springframework*.jar
rm org.springframework.security.openid/${NEW_TAG}/org.springframework*.jar
rm org.springframework.security.remoting/${NEW_TAG}/org.springframework*.jar
rm org.springframework.security.taglibs/${NEW_TAG}/org.springframework*.jar
rm org.springframework.security.web/${NEW_TAG}/org.springframework*.jar

cp ${SPRING_BASE}/acl/build/libs/spring-security-acl* org.springframework.security.acls/${NEW_TAG}/
cp ${SPRING_BASE}/aspects/build/libs/spring-security-aspects* org.springframework.security.aspects/${NEW_TAG}/
cp ${SPRING_BASE}/cas/build/libs/spring-security-cas* org.springframework.security.cas/${NEW_TAG}/
cp ${SPRING_BASE}/config/build/libs/spring-security-config* org.springframework.security.config/${NEW_TAG}/
cp ${SPRING_BASE}/core/build/libs/spring-security-core* org.springframework.security.core/${NEW_TAG}/
cp ${SPRING_BASE}/crypto/build/libs/spring-security-crypto* org.springframework.security.crypto/${NEW_TAG}/
cp ${SPRING_BASE}/ldap/build/libs/spring-security-ldap* org.springframework.security.ldap/${NEW_TAG}/
cp ${SPRING_BASE}/openid/build/libs/spring-security-openid* org.springframework.security.openid/${NEW_TAG}/
cp ${SPRING_BASE}/remoting/build/libs/spring-security-remoting* org.springframework.security.remoting/${NEW_TAG}/
cp ${SPRING_BASE}/taglibs/build/libs/spring-security-taglibs* org.springframework.security.taglibs/${NEW_TAG}/
cp ${SPRING_BASE}/web/build/libs/spring-security-web* org.springframework.security.web/${NEW_TAG}/

rm -f */${NEW_TAG}/*-javadoc.jar

mv org.springframework.security.acls/${NEW_TAG}/spring-security-*-sources.jar org.springframework.security.acls/${NEW_TAG}/org.springframework.security.acls-sources.jar
mv org.springframework.security.aspects/${NEW_TAG}/spring-security-*-sources.jar org.springframework.security.aspects/${NEW_TAG}/org.springframework.security.aspects-sources.jar
mv org.springframework.security.cas/${NEW_TAG}/spring-security-*-sources.jar org.springframework.security.cas/${NEW_TAG}/org.springframework.security.cas-sources.jar
mv org.springframework.security.config/${NEW_TAG}/spring-security-*-sources.jar org.springframework.security.config/${NEW_TAG}/org.springframework.security.config-sources.jar
mv org.springframework.security.core/${NEW_TAG}/spring-security-*-sources.jar org.springframework.security.core/${NEW_TAG}/org.springframework.security.core-sources.jar
mv org.springframework.security.crypto/${NEW_TAG}/spring-security-*-sources.jar org.springframework.security.crypto/${NEW_TAG}/org.springframework.security.crypto-sources.jar
mv org.springframework.security.ldap/${NEW_TAG}/spring-security-*-sources.jar org.springframework.security.ldap/${NEW_TAG}/org.springframework.security.ldap-sources.jar
mv org.springframework.security.openid/${NEW_TAG}/spring-security-*-sources.jar org.springframework.security.openid/${NEW_TAG}/org.springframework.security.openid-sources.jar
mv org.springframework.security.remoting/${NEW_TAG}/spring-security-*-sources.jar org.springframework.security.remoting/${NEW_TAG}/org.springframework.security.remoting-sources.jar
mv org.springframework.security.taglibs/${NEW_TAG}/spring-security-*-sources.jar org.springframework.security.taglibs/${NEW_TAG}/org.springframework.security.taglibs-sources.jar
mv org.springframework.security.web/${NEW_TAG}/spring-security-*-sources.jar org.springframework.security.web/${NEW_TAG}/org.springframework.security.web-sources.jar

mv org.springframework.security.acls/${NEW_TAG}/spring-security-*.jar org.springframework.security.acls/${NEW_TAG}/org.springframework.security.acls.jar
mv org.springframework.security.aspects/${NEW_TAG}/spring-security-*.jar org.springframework.security.aspects/${NEW_TAG}/org.springframework.security.aspects.jar
mv org.springframework.security.cas/${NEW_TAG}/spring-security-*.jar org.springframework.security.cas/${NEW_TAG}/org.springframework.security.cas.jar
mv org.springframework.security.config/${NEW_TAG}/spring-security-*.jar org.springframework.security.config/${NEW_TAG}/org.springframework.security.config.jar
mv org.springframework.security.core/${NEW_TAG}/spring-security-*.jar org.springframework.security.core/${NEW_TAG}/org.springframework.security.core.jar
mv org.springframework.security.crypto/${NEW_TAG}/spring-security-*.jar org.springframework.security.crypto/${NEW_TAG}/org.springframework.security.crypto.jar
mv org.springframework.security.ldap/${NEW_TAG}/spring-security-*.jar org.springframework.security.ldap/${NEW_TAG}/org.springframework.security.ldap.jar
mv org.springframework.security.openid/${NEW_TAG}/spring-security-*.jar org.springframework.security.openid/${NEW_TAG}/org.springframework.security.openid.jar
mv org.springframework.security.remoting/${NEW_TAG}/spring-security-*.jar org.springframework.security.remoting/${NEW_TAG}/org.springframework.security.remoting.jar
mv org.springframework.security.taglibs/${NEW_TAG}/spring-security-*.jar org.springframework.security.taglibs/${NEW_TAG}/org.springframework.security.taglibs.jar
mv org.springframework.security.web/${NEW_TAG}/spring-security-*.jar org.springframework.security.web/${NEW_TAG}/org.springframework.security.web.jar

./replace.py ${OLD_TAG} ${NEW_TAG}

echo "Your Spring Security artifacts have been staged."
echo "* If the release.type has changed between ${OLD_TAG} and ${NEW_TAG}, you must edit each build.xml file."
echo "* Now you must manually go through each ivy.xml and manifest template file and update the versions based on build.gradle"
echo "When that is complete, you can start staging artifacts!"
echo "I recommend you run..."
echo
echo "$ git diff ${OLD_TAG}..${NEW_TAG} */*.gradle"
echo
echo "...to pinpoint the changes in versions and other things."
echo
echo "After that, go through each package, executing 'ant clean publish-test' to spot any missing dependencies."
echo "When the dependencies are fixed, run 'ant clean publish -propertyfile <s3.properties.files>'."
echo "The order of the packages is..."
echo "+ org.springframework.security.core"
echo "+ org.springframework.security.acls"
echo "+ org.springframework.security.aspects"
echo "+ org.springframework.security.web"
echo "+ org.springframework.security.cas"
echo "+ org.springframework.security.config"
echo "+ org.springframework.security.crypto"
echo "+ org.springframework.security.ldap"
echo "+ org.springframework.security.openid"
echo "+ org.springframework.security.remoting"
echo "+ org.springframework.security.taglibs"
