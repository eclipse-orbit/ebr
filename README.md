[![Build Status](https://github.com/eclipse/ebr/actions/workflows/plugins.yml/badge.svg)](https://github.com/eclipse/ebr/actions/workflows/plugins.yml)

Eclipse Orbit's EBR maven plug-ins
==================================

This repositories hosts maven plug-ins for building OSGi bundles from Java Maven artifacts.
Please see the main [Eclipse Orbit pages](https://projects.eclipse.org/projects/tools.orbit/developer) for additional information.

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

History
-------

Eclipse Bundle Recipes (EBR) used to be an independent project and included contibuted recipes as well as the Maven plug-ins.
In early 2023 the EBR project was restructured and the Maven plug-ins were moved to the Eclipse Orbit project and the rest of the project was archived.
See [this email thread](https://www.eclipse.org/lists/orbit-dev/msg05612.html) for more details that led to this change.
