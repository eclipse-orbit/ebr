SLF4J Recipes
=============

SLF4J comes with OSGi headers out of the box. But there are some flaws and drawbacks
with the approach implemented by default. With these recipes we hope to address
some of them by giving adopters more freedom in picking their approach.

Modifications & Enhancements
----------------------------

The original API bundle does not contain the SLF4J implementation and thus, expects
to import the package `org.slf4j.impl`. However, this creates circular dependencies.
The recipes therefore make the import optional in order to encourage contributions via
fragments instead of bundles. This has the benefit of fragments attaching to the API
bundle and delivering the implementation. It does not create a circular reference
between the actual logging implementation and the API bundle *if* the fragment is just
a small separate module. Therefore, logging implementations might need to change too.

Additional, the API bundle defines a `Require-Capability` constraint. Therefore,
logging implementations have to define a `Provide-Capability` header in there OSGi
manifest.

org.slf4j.api:

    Require-Capability: org.slf4j.impl; filter:="(version=1.7.5)"


Any logging implementation:

    Provide-Capability: org.slf4j.impl; org.slf4j.impl="logback"; version:Version="1.7.5"

or

    Provide-Capability: org.slf4j.impl; org.slf4j.impl="logback"; version:List<Version>="1.7.5,1.7.4,1.7.3"


Bundles
-------

### org.slf4j.api

This is the API bundle. It exports the API packages. The actual
implementation should be contributed as a fragment to this bundle.

Note, it may been possible to have an Import-Package dependency on
the implementation packages. However, this approach is not recommended
because it will introduced binary (or even worse - source) cycles in a
workspace which has both - the API and the implementation bundle.

An `Require-Capability` header is used in order to specify a dependency
on a SLF4J logger implementation. Note, the requirement  will try
to align with the SLF4J API compatibility list as found in LoggerFactory.


### org.slf4j.impl.<impl-name>

This defines a bundle for a SLF4J implementation library. Every
implementation bundle will be a fragment of the API bundle
(`Fragment-Host: org.slf4j.api`) _and_ provide a SLF4J logger implementation
(`Provide-Capability: org.slf4j.impl`) header.

Special care must be taken to the version dependency as SLF4J allows for
breaking changes within the implementation between minor releases. As for
the implementation they could also break within micro releases.


