A set of Maven plugins that simplifies and automates working
with Eclipse Bundle Recipies via Maven.

Part of the [Eclipse Orbit](https://projects.eclipse.org/projects/tools.orbit) project.

Using the plugins
=================

Add the EBR repository to your Maven &lt;pluginRepositories&gt; section

    https://repo.eclipse.org/content/repositories/ebr-releases/

Release Process
===============

Internat note: we are not using the release plug-in.
Instead we just set version manually and use Jenkins to build and deploy.

    mvn versions:set -DnewVersion=....
