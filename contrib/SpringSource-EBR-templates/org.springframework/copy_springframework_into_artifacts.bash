#!/bin/bash

SPRING_BASE=$1
OLD_TAG=$2
TAG=$3

mkdir org.springframework.aop/${TAG}/
mkdir org.springframework.aspects/${TAG}/
mkdir org.springframework.beans/${TAG}/
mkdir org.springframework.context.support/${TAG}/
mkdir org.springframework.context/${TAG}/
mkdir org.springframework.core/${TAG}/
mkdir org.springframework.expression/${TAG}/
mkdir org.springframework.instrument.tomcat/${TAG}/
mkdir org.springframework.instrument/${TAG}/
mkdir org.springframework.jdbc/${TAG}/
mkdir org.springframework.jms/${TAG}/
mkdir org.springframework.orm/${TAG}/
mkdir org.springframework.oxm/${TAG}/
mkdir org.springframework.web.struts/${TAG}/
mkdir org.springframework.test/${TAG}/
mkdir org.springframework.transaction/${TAG}/
mkdir org.springframework.web/${TAG}/
mkdir org.springframework.web.portlet/${TAG}/
mkdir org.springframework.web.servlet/${TAG}/

cp org.springframework.aop/${OLD_TAG}/* org.springframework.aop/${TAG}/
cp org.springframework.aspects/${OLD_TAG}/* org.springframework.aspects/${TAG}/
cp org.springframework.beans/${OLD_TAG}/* org.springframework.beans/${TAG}/
cp org.springframework.context.support/${OLD_TAG}/* org.springframework.context.support/${TAG}/
cp org.springframework.context/${OLD_TAG}/* org.springframework.context/${TAG}/
cp org.springframework.core/${OLD_TAG}/* org.springframework.core/${TAG}/
cp org.springframework.expression/${OLD_TAG}/* org.springframework.expression/${TAG}/
cp org.springframework.instrument.tomcat/${OLD_TAG}/* org.springframework.instrument.tomcat/${TAG}/
cp org.springframework.instrument/${OLD_TAG}/* org.springframework.instrument/${TAG}/
cp org.springframework.jdbc/${OLD_TAG}/* org.springframework.jdbc/${TAG}/
cp org.springframework.jms/${OLD_TAG}/* org.springframework.jms/${TAG}/
cp org.springframework.orm/${OLD_TAG}/* org.springframework.orm/${TAG}/
cp org.springframework.oxm/${OLD_TAG}/* org.springframework.oxm/${TAG}/
cp org.springframework.web.struts/${OLD_TAG}/* org.springframework.web.struts/${TAG}/
cp org.springframework.test/${OLD_TAG}/* org.springframework.test/${TAG}/
cp org.springframework.transaction/${OLD_TAG}/* org.springframework.transaction/${TAG}/
cp org.springframework.web/${OLD_TAG}/* org.springframework.web/${TAG}/
cp org.springframework.web.portlet/${OLD_TAG}/* org.springframework.web.portlet/${TAG}/
cp org.springframework.web.servlet/${OLD_TAG}/* org.springframework.web.servlet/${TAG}/

rm org.springframework.aop/${TAG}/org.springframework*.jar
rm org.springframework.aspects/${TAG}/org.springframework*.jar
rm org.springframework.beans/${TAG}/org.springframework*.jar
rm org.springframework.context.support/${TAG}/org.springframework*.jar
rm org.springframework.context/${TAG}/org.springframework*.jar
rm org.springframework.core/${TAG}/org.springframework*.jar
rm org.springframework.expression/${TAG}/org.springframework*.jar
rm org.springframework.instrument.tomcat/${TAG}/org.springframework*.jar
rm org.springframework.instrument/${TAG}/org.springframework*.jar
rm org.springframework.jdbc/${TAG}/org.springframework*.jar
rm org.springframework.jms/${TAG}/org.springframework*.jar
rm org.springframework.orm/${TAG}/org.springframework*.jar
rm org.springframework.oxm/${TAG}/org.springframework*.jar
rm org.springframework.web.struts/${TAG}/org.springframework*.jar
rm org.springframework.test/${TAG}/org.springframework*.jar
rm org.springframework.transaction/${TAG}/org.springframework*.jar
rm org.springframework.web/${TAG}/org.springframework*.jar
rm org.springframework.web.portlet/${TAG}/org.springframework*.jar
rm org.springframework.web.servlet/${TAG}/org.springframework*.jar

cp ${SPRING_BASE}/spring-aop/build/libs/spring-aop* org.springframework.aop/${TAG}/
cp ${SPRING_BASE}/spring-aspects/build/libs/spring-aspects* org.springframework.aspects/${TAG}/
cp ${SPRING_BASE}/spring-beans/build/libs/spring-beans* org.springframework.beans/${TAG}/
cp ${SPRING_BASE}/spring-context-support/build/libs/spring-context* org.springframework.context.support/${TAG}/
cp ${SPRING_BASE}/spring-context/build/libs/spring-context* org.springframework.context/${TAG}/
cp ${SPRING_BASE}/spring-core/build/libs/spring-core* org.springframework.core/${TAG}/
cp ${SPRING_BASE}/spring-expression/build/libs/spring-expression* org.springframework.expression/${TAG}/
cp ${SPRING_BASE}/spring-instrument-tomcat/build/libs/spring-instrument* org.springframework.instrument.tomcat/${TAG}/
cp ${SPRING_BASE}/spring-instrument/build/libs/spring-instrument* org.springframework.instrument/${TAG}/
cp ${SPRING_BASE}/spring-jdbc/build/libs/spring-jdbc* org.springframework.jdbc/${TAG}/
cp ${SPRING_BASE}/spring-jms/build/libs/spring-jms* org.springframework.jms/${TAG}/
cp ${SPRING_BASE}/spring-orm/build/libs/spring-orm* org.springframework.orm/${TAG}/
cp ${SPRING_BASE}/spring-oxm/build/libs/spring-oxm* org.springframework.oxm/${TAG}/
cp ${SPRING_BASE}/spring-struts/build/libs/spring-struts* org.springframework.web.struts/${TAG}/
cp ${SPRING_BASE}/spring-test/build/libs/spring-test* org.springframework.test/${TAG}/
cp ${SPRING_BASE}/spring-tx/build/libs/spring-tx* org.springframework.transaction/${TAG}/
cp ${SPRING_BASE}/spring-web/build/libs/spring-web* org.springframework.web/${TAG}/
cp ${SPRING_BASE}/spring-webmvc-portlet/build/libs/spring-web* org.springframework.web.portlet/${TAG}/
cp ${SPRING_BASE}/spring-webmvc/build/libs/spring-web* org.springframework.web.servlet/${TAG}/

rm -f */${TAG}/*-javadoc.jar

mv org.springframework.aop/${TAG}/spring-*-sources.jar org.springframework.aop/${TAG}/org.springframework.aop-sources.jar
mv org.springframework.aspects/${TAG}/spring-*-sources.jar org.springframework.aspects/${TAG}/org.springframework.aspects-sources.jar
mv org.springframework.beans/${TAG}/spring-*-sources.jar org.springframework.beans/${TAG}/org.springframework.beans-sources.jar
mv org.springframework.context.support/${TAG}/spring-*-sources.jar org.springframework.context.support/${TAG}/org.springframework.context.support-sources.jar
mv org.springframework.context/${TAG}/spring-*-sources.jar org.springframework.context/${TAG}/org.springframework.context-sources.jar
mv org.springframework.core/${TAG}/spring-*-sources.jar org.springframework.core/${TAG}/org.springframework.core-sources.jar
mv org.springframework.expression/${TAG}/spring-*-sources.jar org.springframework.expression/${TAG}/org.springframework.expression-sources.jar
mv org.springframework.instrument.tomcat/${TAG}/spring-*-sources.jar org.springframework.instrument.tomcat/${TAG}/org.springframework.instrument.tomcat-sources.jar
mv org.springframework.instrument/${TAG}/spring-*-sources.jar org.springframework.instrument/${TAG}/org.springframework.instrument-sources.jar
mv org.springframework.jdbc/${TAG}/spring-*-sources.jar org.springframework.jdbc/${TAG}/org.springframework.jdbc-sources.jar
mv org.springframework.jms/${TAG}/spring-*-sources.jar org.springframework.jms/${TAG}/org.springframework.jms-sources.jar
mv org.springframework.orm/${TAG}/spring-*-sources.jar org.springframework.orm/${TAG}/org.springframework.orm-sources.jar
mv org.springframework.oxm/${TAG}/spring-*-sources.jar org.springframework.oxm/${TAG}/org.springframework.oxm-sources.jar
mv org.springframework.web.struts/${TAG}/spring-*-sources.jar org.springframework.web.struts/${TAG}/org.springframework.web.struts-sources.jar
mv org.springframework.test/${TAG}/spring-*-sources.jar org.springframework.test/${TAG}/org.springframework.test-sources.jar
mv org.springframework.transaction/${TAG}/spring-*-sources.jar org.springframework.transaction/${TAG}/org.springframework.transaction-sources.jar
mv org.springframework.web/${TAG}/spring-*-sources.jar org.springframework.web/${TAG}/org.springframework.web-sources.jar
mv org.springframework.web.portlet/${TAG}/spring-*-sources.jar org.springframework.web.portlet/${TAG}/org.springframework.web.portlet-sources.jar
mv org.springframework.web.servlet/${TAG}/spring-*-sources.jar org.springframework.web.servlet/${TAG}/org.springframework.web.servlet-sources.jar

mv org.springframework.aop/${TAG}/spring-*.jar org.springframework.aop/${TAG}/org.springframework.aop.jar
mv org.springframework.aspects/${TAG}/spring-*.jar org.springframework.aspects/${TAG}/org.springframework.aspects.jar
mv org.springframework.beans/${TAG}/spring-*.jar org.springframework.beans/${TAG}/org.springframework.beans.jar
mv org.springframework.context.support/${TAG}/spring-*.jar org.springframework.context.support/${TAG}/org.springframework.context.support.jar
mv org.springframework.context/${TAG}/spring-*.jar org.springframework.context/${TAG}/org.springframework.context.jar
mv org.springframework.core/${TAG}/spring-*.jar org.springframework.core/${TAG}/org.springframework.core.jar
mv org.springframework.expression/${TAG}/spring-*.jar org.springframework.expression/${TAG}/org.springframework.expression.jar
mv org.springframework.instrument.tomcat/${TAG}/spring-*.jar org.springframework.instrument.tomcat/${TAG}/org.springframework.instrument.tomcat.jar
mv org.springframework.instrument/${TAG}/spring-*.jar org.springframework.instrument/${TAG}/org.springframework.instrument.jar
mv org.springframework.jdbc/${TAG}/spring-*.jar org.springframework.jdbc/${TAG}/org.springframework.jdbc.jar
mv org.springframework.jms/${TAG}/spring-*.jar org.springframework.jms/${TAG}/org.springframework.jms.jar
mv org.springframework.orm/${TAG}/spring-*.jar org.springframework.orm/${TAG}/org.springframework.orm.jar
mv org.springframework.oxm/${TAG}/spring-*.jar org.springframework.oxm/${TAG}/org.springframework.oxm.jar
mv org.springframework.web.struts/${TAG}/spring-*.jar org.springframework.web.struts/${TAG}/org.springframework.web.struts.jar
mv org.springframework.test/${TAG}/spring-*.jar org.springframework.test/${TAG}/org.springframework.test.jar
mv org.springframework.transaction/${TAG}/spring-*.jar org.springframework.transaction/${TAG}/org.springframework.transaction.jar
mv org.springframework.web/${TAG}/spring-*.jar org.springframework.web/${TAG}/org.springframework.web.jar
mv org.springframework.web.portlet/${TAG}/spring-*.jar org.springframework.web.portlet/${TAG}/org.springframework.web.portlet.jar
mv org.springframework.web.servlet/${TAG}/spring-*.jar org.springframework.web.servlet/${TAG}/org.springframework.web.servlet.jar

./replace.py ${OLD_TAG} ${TAG}

echo "Your Spring Framework artifacts have been staged."
echo "* If the release.type has changed between ${OLD_TAG} and ${TAG}, you must edit each build.xml file."
echo "* Now you must manually go through each ivy.xml and manifest template file and update the versions based on build.gradle"
echo "When that is complete, you can start staging artifacts!"
echo "I recommend you run..."
echo
echo "$  git diff v${OLD_TAG}..v${TAG} build.gradle"
echo
echo "...to pinpoint the changes in versions and other things."
echo
echo "Now, go to each project, in the following order, and execute..."
echo
echo "$ ant clean publish-test && ant clean publish -propertyfile <path to s3 credential propertyfile> && git add ."
echo
echo "...Using '&&' causes the entire chain of commands to stop if one of the steps fails."
echo
echo "+ org.springframework.core"
echo "+ org.springframework.beans"
echo "+ org.springframework.aop"
echo "+ org.springframework.instrument"
echo "+ org.springframework.context"
echo "+ org.springframework.transaction"
echo "+ org.springframework.jdbc"
echo "+ org.springframework.context.support"
echo "+ org.springframework.oxm"
echo "+ org.springframework.web"
echo "+ org.springframework.orm"
echo "+ org.springframework.aspects"
echo "+ org.springframework.expression"
echo "+ org.springframework.instrument.tomcat"
echo "+ org.springframework.jms"
echo "+ org.springframework.web.servlet"
echo "+ org.springframework.web.struts"
echo "+ org.springframework.web.portlet"
echo "+ org.springframework.test"
echo
echo "After the bundles are published, don't forget to create a library entry at artifacts/libraries"
echo "Finally, commit everything to the artifacts repository, and push to origin."
