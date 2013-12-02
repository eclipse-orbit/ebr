#!/bin/sh
#
# Grab a distribution, unpack it and deploy to the repository tree

#
# Utility functions
#

copy_dist_jar()
{ # $1=CNAME, $2=BSN, $3=VERSION, $4=LVERSION
[ ! -f $2/$4/$2.jar ] && cp dist/$DISTDIR/dist/$1-$3.jar $2/$4/$2.jar
}

copy_source_jar()
{ # $1=CNAME, $2=BSN, $3=VERSION, $4=LVERSION
[ ! -f $2/$4/$2-sources.jar ] && \
	cp dist/$DISTDIR/dist/$1-$3-sources.jar $2/$4/$2-sources.jar
}


copy_license_file()
{ # $1=CNAME, $2=BSN, $3=VERSION, $4=LVERSION
[ ! -f $2/$4/license.txt ] && cp ./dist/$DISTDIR/$1 $2/$4/license.txt
}

copy_files()
{ # $1=CNAME, $2=BSN 
copy_dist_jar $1 $2 $VERSION $LVERSION
copy_source_jar $1 $2 $VERSION $LVERSION
copy_license_file license.txt $2 $VERSION $LVERSION
}

#----------------------------------------------------------------------
#
# main
#

VERSION=2.0.4
LVERSION=$VERSION.A
DIST=spring-security
DISTURL=http://downloads.sourceforge.net/springframework/$DIST-$VERSION.zip
DISTFILE=`basename $DISTURL`
DISTDIR=`basename $DISTFILE .zip`

[ -f dist/$DISTFILE ] || wget -Pdist $DISTURL

[ ! -d dist/$DISTDIR ] && unzip dist/$DISTFILE -d dist

BUNDLES="org.springframework.security \
	org.springframework.security.acls \
	org.springframework.security.adapters.catalina \
	org.springframework.security.adapters.jboss \
	org.springframework.security.adapters.jetty \
	org.springframework.security.adapters.resin \
	org.springframework.security.annotation \
	org.springframework.security.providers.cas \
	org.springframework.security.providers.openid \
	org.springframework.security.taglibs \
	org.springframework.security.ui.ntlm \
	org.springframework.security.ui.portlet"

for bundle in $BUNDLES
do
	[ ! -d $bundle/$LVERSION ] && svn mkdir $bundle/$LVERSION
	( cd $bundle/$LVERSION && $HOME/bin/genbuild.sh )
done

copy_files $DIST-core org.springframework.security
copy_files $DIST-acl org.springframework.security.acls
copy_files $DIST-catalina org.springframework.security.adapters.catalina
copy_files $DIST-jboss org.springframework.security.adapters.jboss
copy_files $DIST-jetty org.springframework.security.adapters.jetty
copy_files $DIST-resin org.springframework.security.adapters.resin
copy_files $DIST-core-tiger org.springframework.security.annotation
copy_files $DIST-cas-client org.springframework.security.providers.cas
copy_files $DIST-openid org.springframework.security.providers.openid
copy_files $DIST-taglibs org.springframework.security.taglibs
copy_files $DIST-ntlm org.springframework.security.ui.ntlm
copy_files $DIST-portlet org.springframework.security.ui.portlet
