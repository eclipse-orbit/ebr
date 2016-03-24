Eclipse Bundle Recipes
======================

This repositories hosts recipes and tools for building OSGi bundles from Java Maven artifacts.
To learn more about Eclipse Bunde Recipes, please have a look at [this presentation](http://de.slideshare.net/guw/tasty-recipes-for-osgi-bundles).

**Got a recipe?**

Please open pull requests with any recipe you would like to add. The Eclipse Bundle Recipes 
project is open to recipes for any library. Eclipse IP rules only apply to the recipes itself 
and not to any of the libraries being OSGi-ified. Recipes are a community effort.


Prerequisites
-------------

This project uses Maven for assembling of OSGi bundles based on artifacts in Maven Central or
any other accessible Maven repository. A Maven plug-in is provided which must be installed
in your local Maven repository for any of the builds to be successful.

1. Install Java (at least Java 7) and Maven
2. Clone this repository and go into the repository root folder
3. `mvn -f releng/maven-plugins/pom.xml install`


How to build all bundles yourself
---------------------------------

1. Clone this repository and go into the repository root folder.
2. `cd recipes`
3. `mvn clean install`

This will publish all OSGi bundles produced by the recipes into your local Maven repository. You can consume
the bundles directly from Maven in any Tycho build. By convention, the bundles will be published using the
Maven group id `MY_EBR_BUNDLES_GROUP`.


### Generate p2 repository

1. Go into the repository root folder.
2. `cd releng/p2`
3. `mvn clean package`

The repository will be made available as archive in `releng/p2/repository/target`.

Note, you **must** build the recipes first and *install* the result into your local Maven repository.
Otherwise the p2 build won't find any bundles and the build fails.


### How to build just a single recipe?

This is not difficult at all. Just change into the directory of the recipe to build and execute Maven from there.

1. `cd recipes/\<path/to/recipe\>`
2. `mvn clean package`

The resulting bundle will be available in the recipes `target` folder.


### Pack200 and signing bundles

It is possible to pack200 and sign bundles as part of the bundle build. In order to do this, simply set the
property `signingServiceType` to `eclipse`. This can be done at the Maven command line or within a Maven profile.
The `eclipse` signing service using Tycho Extras for pack200 and the Eclipse CBI Maven Jarsigner plug-in to sign bundles.


Adding a recipe
---------------

Create new recipes with something like the following:

    $ cd recipes/unsorted
    $ mvn org.eclipse.ebr:ebr-maven-plugin::create-recipe \
      -DbundleSymbolicName=org.joda.time.ebr \
      -DgroupId=joda-time \
      -DartifactId=joda-time \
      -Dversion=1.6

This command will create an EBR project in a directory named
`org.joda.time.ebr_1.6.0` within the current directory.  `groupId`,
`artifactId`, and `version` are the artifact coordinates in Maven
Central.  `version` is optional, and can also be `LATEST` or
`RELEASE`.

Note, all recipes producing bundles for EBR should define `Bundle-SymbolicName`
that ends with `.ebr`.

You can create new categories as desired. See /Adding a category/.

When your recipe is done and builds fine, add an entry to the aggregation feature
`releng/p2/aggregationfeature/feature.xml`.


Adding a category
-----------------

Recipies are organized by categories. The recommendation is to go with
a category per origin.

 1. Create folder within `recipes/`
 2. Add category entry and IU query to `releng/p2/repository/category.xml`
 3. Add `pom.xml` for category (use an existing category as template)


How the Maven plug-in works
---------------------------
 1. Direct dependencies are collected an unzipped.
 2. Direct dependencies are then merged with the output produced
    already by the Maven module, i.e. any content in src/main/resources
    or even source code compiled before from src/main/java will take
    precedence over stuff coming from dependencies.
 3. OSGi manifest is generated (via Apache Felix Maven Bundle plug-in).
      - Bundle-SymbolicName is assumed to be the artifact id
        (This is an important assumption made throughout the plug-in
        which can't be changed!)
      - Instructions for BND can be specified via module POM and
        overwrite any default behavior described below
      - The bundle version is inherited from the project version. In
        case the project version ends with "-SNAPSHOT" it will be
        replaced with ".qualifier". Tycho's qualifier expansion is
        supported. Thus, if Tycho computed a proper build qualifier
        then this one will be used as the qualifier replacement.
      - The Bundle-Name is inherited from the Maven project name.
      - The Bundle-Description is inherited from the Maven project
        description.
 4. Final bundle jar is produced.
 5. Source code for dependencies is collected (via sources classifier).
 6. OSGi manifest for source bundle is generated together with
    OSGI-INF/l10n/bundle.properties for localizable-ready content
    (Keys are bundleName and bundleVendor; inherited from main OSGi
    manifest generated before.)
 7. Final source bundle jar is produced.
 8. p2 metadata is generated (via Tycho p2 plug-in).

