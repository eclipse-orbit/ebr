/*******************************************************************************
 * Copyright (c) 2014 Gunnar Wagenknecht and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Gunnar Wagenknecht - initial API and implementation
 *    Sonatype Inc. - methods for reading OSGi I10N properties from Tycho
 *    Brian de Alwis - handle artifacts that lack source
 *******************************************************************************/
package org.eclipse.ebr.maven;

import static aQute.bnd.osgi.Constants.CREATED_BY;
import static aQute.bnd.osgi.Constants.SNAPSHOT;
import static java.lang.String.format;
import static org.eclipse.ebr.maven.OsgiLocalizationUtil.I18N_KEY_BUNDLE_NAME;
import static org.eclipse.ebr.maven.OsgiLocalizationUtil.I18N_KEY_BUNDLE_VENDOR;
import static org.eclipse.ebr.maven.OsgiLocalizationUtil.I18N_KEY_PREFIX;
import static org.osgi.framework.Constants.BUNDLE_LOCALIZATION;
import static org.osgi.framework.Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
import static org.osgi.framework.Constants.BUNDLE_MANIFESTVERSION;
import static org.osgi.framework.Constants.BUNDLE_NAME;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VENDOR;
import static org.osgi.framework.Constants.BUNDLE_VERSION;
import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.felix.bundleplugin.ManifestPlugin;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.util.AbstractScanner;
import org.codehaus.plexus.util.IOUtil;

import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

/**
 * A Maven plug-on for downloading dependencies and re-packaging them as a
 * single OSGi bundle.
 */
@Mojo(name = "bundle", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BundleMojo extends ManifestPlugin {

	private static final String CLASSIFIER_SOURCES = "sources";

	static boolean isRecipeProject(final MavenProject project) {
		return "eclipse-bundle-recipe".equals(project.getPackaging());
	}

	/**
	 * The project output directory where all classes and resources will be
	 * collected for generating the final bundle jar later.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
	protected File outputDirectory;

	/**
	 * The directory for the generated JAR.
	 */
	@Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
	protected String buildDirectory;

	/** The directory for gathering and extracting dependencies. */
	@Parameter(defaultValue = "${project.build.directory}/dependency-bin", readonly = true, required = true)
	protected String dependenciesDirectory;

	/**
	 * The directory for gathering and extracting sources of all dependencies
	 */
	@Parameter(defaultValue = "${project.build.directory}/dependency-src", readonly = true, required = true)
	protected String dependenciesSourcesDirectory;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	protected MavenSession session;

	@Component
	private BuildPluginManager pluginManager;

	@Component
	private MavenProjectHelper projectHelper;

	/**
	 * The instructions passed to BND for the bundle.
	 */
	@Parameter
	protected Map<String, String> bndInstructions = new LinkedHashMap<String, String>();

	/**
	 * A comma separated list of file patterns to include when unpacking the
	 * artifacts. i.e. <code>**\/*.xml,**\/*.properties</code> NOTE: Excludes
	 * patterns override the includes. (component code =
	 * <code>return isIncluded( name ) AND !isExcluded( name );</code>)
	 */
	@Parameter
	protected String includes;

	/**
	 * A comma separated list of file patterns to exclude when unpacking the
	 * artifacts. i.e. <code>**\/*.xml,**\/*.properties</code> NOTE: Excludes
	 * patterns override the includes. (component code =
	 * <code>return isIncluded( name ) AND !isExcluded( name );</code>)
	 */
	@Parameter
	protected String excludes;

	/**
	 * A comma separated list of artifactIds which should be exclude completely
	 * from the processing. TEST dependencies will always be excluded.
	 */
	@Parameter(defaultValue = "${excludeDependencies}")
	protected String excludeDependencies;

	/**
	 * Name of the generated JAR.
	 */
	@Parameter(defaultValue = "${project.build.finalName}", alias = "jarName", required = true)
	protected String finalName;

	/**
	 * The maven archiver to use.
	 */
	@Parameter
	private final MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	/**
	 * Configuration of the source archive
	 */
	@Parameter
	private final MavenArchiveConfiguration sourceArchive = new MavenArchiveConfiguration();

	/**
	 * Build qualifier. Recommended way to set this parameter is using
	 * build-qualifier goal.
	 */
	@Parameter(defaultValue = "${buildQualifier}")
	protected String qualifier;

	@Parameter(defaultValue = "0.22", property = "tycho-plugin.version", required = true)
	protected String tychoPluginVersionFallback;

	@Parameter(defaultValue = "2.7", property = "maven-resource-plugin.version", required = true)
	protected String mavenResourcesPluginVersionFallback;

	@Parameter(defaultValue = "2.10", property = "maven-dependency-plugin.version", required = true)
	protected String mavenDependencyPluginVersionFallback;

	private File assembleJar(final String jarName, final File manifest, final File directory, final MavenArchiveConfiguration archiveConfiguration) throws MojoExecutionException {
		try {
			final MavenArchiver archiver = new MavenArchiver();
			archiver.setArchiver(new JarArchiver());

			final File jarFile = new File(buildDirectory, jarName);
			if (jarFile.exists()) {
				FileUtils.forceDelete(jarFile);
			}

			// 1. include all collected files
			archiver.getArchiver().addFileSet(getFileSet(directory));

			// 2. update the manifest
			if (manifest.exists()) {
				archiveConfiguration.setManifestFile(manifest);
			}

			archiver.setOutputFile(jarFile);
			if (!archiveConfiguration.isForced()) {
				// optimized archive creation not supported for now because of build qualifier mismatch issues
				// see TYCHO-502
				getLog().warn("ignoring unsupported archive forced = false parameter.");
				archiveConfiguration.setForced(true);
			}
			archiver.createArchive(session, project, archiveConfiguration);
			return jarFile;
		} catch (final Exception e) {
			throw new MojoExecutionException("Error assembling JAR " + jarName + ": " + e.getMessage(), e);
		}
	}

	private void buildBundle(final Set<Artifact> dependencies) throws MojoExecutionException {
		// unpack dependencies
		getLog().info("Gathering and extracting dependencies");
		// @formatter:off
		final List<Element> unpackConfiguration = getUnpackConfiguration(dependenciesDirectory, dependencies, null);
		executeMojo(
				plugin(
						groupId("org.apache.maven.plugins"),
						artifactId("maven-dependency-plugin"),
						version(detectPluginVersion("org.apache.maven.plugins", "maven-dependency-plugin", mavenDependencyPluginVersionFallback))
						),
						goal("unpack"),
						configuration(
								unpackConfiguration.toArray(new Element[unpackConfiguration.size()])
								),
								executionEnvironment(
										project,
										session,
										pluginManager
										)
				);
		// @formatter:on

		// copy into output directory
		getLog().info("Merging collected dependencies");
		// @formatter:off
		executeMojo(
				plugin(
						groupId("org.apache.maven.plugins"),
						artifactId("maven-resources-plugin"),
						version(detectPluginVersion("org.apache.maven.plugins", "maven-resources-plugin", mavenResourcesPluginVersionFallback))
						),
						goal("copy-resources"),
						configuration(
								element("outputDirectory", "${project.build.outputDirectory}"),
								element("resources",
										element("resource", element("directory", dependenciesDirectory))
										)
								),
								executionEnvironment(
										project,
										session,
										pluginManager
										)
				);
		// @formatter:on

		// generate manifest based on output only
		getLog().info("Generating OSGi MANIFEST.MF");
		try {
			setOutputDirectory(outputDirectory);
			setBuildDirectory(buildDirectory);
			manifestLocation = new File(outputDirectory, "META-INF");
			super.excludeDependencies = excludeDependencies;

			// sanity check
			if (bndInstructions.containsKey(BUNDLE_SYMBOLICNAME) && !StringUtils.equals(project.getArtifactId(), bndInstructions.get(BUNDLE_SYMBOLICNAME)))
				// something is wrong, fail and report to the use instead of overriding quietly
				throw new MojoExecutionException("Plug-in configuration is wrong! The Bundle-SymbolicName must match the project's artifact id but it doesn't. Please correct the value in bndInstructions.");

			initializeBndInstruction(BUNDLE_SYMBOLICNAME, project.getArtifactId());
			initializeBndInstruction(BUNDLE_VERSION, getExpandedVersion());
			initializeBndInstruction(BUNDLE_NAME, project.getName());
			initializeBndInstruction(SNAPSHOT, qualifier);
			execute(project, bndInstructions, new Properties(), getClasspath(project)); // BND also needs transitive dependencies
		} catch (final Exception e) {
			throw new MojoExecutionException("Error generating Bundle manifest: " + e.getMessage(), e);
		}

		// create JAR
		getLog().debug("Generating OSGi bundle jar...");
		final File pluginFile = createPluginJar();
		project.getArtifact().setFile(pluginFile);
	}

	private void buildSourceBundle(final Set<Artifact> dependencies) throws MojoExecutionException {
		// unpack sources
		getLog().info("Gathering sources");
		// @formatter:off
		final List<Element> unpackConfigurationSource = getUnpackConfiguration(dependenciesSourcesDirectory, dependencies, CLASSIFIER_SOURCES);
		try {
			executeMojo(
					plugin(
							groupId("org.apache.maven.plugins"),
							artifactId("maven-dependency-plugin"),
							version(detectPluginVersion("org.apache.maven.plugins", "maven-dependency-plugin", mavenDependencyPluginVersionFallback))
							),
							goal("unpack"),
							configuration(
									unpackConfigurationSource.toArray(new Element[unpackConfigurationSource.size()])
									),
									executionEnvironment(
											project,
											session,
											pluginManager
											)
					);
		} catch(final MojoExecutionException e) {
			getLog().warn("Unable to resolve source jar; skipping source bundle");
			getLog().debug(e);
			return;
		}
		// @formatter:on

		// create sources JAR
		getLog().debug("Generating OSGi bundle jar...");
		final File sourceBundleFile = createSourcesJar();
		projectHelper.attachArtifact(project, "java-source", CLASSIFIER_SOURCES, sourceBundleFile);
	}

	private File createPluginJar() throws MojoExecutionException {
		return assembleJar(finalName + ".jar", generateFinalBundleManifest(), outputDirectory, archive);
	}

	private File createSourcesJar() throws MojoExecutionException {
		sourceArchive.setAddMavenDescriptor(false); // no maven descriptors in source bundle
		return assembleJar(finalName + "-sources.jar", generateSourceBundleManifest(), new File(dependenciesSourcesDirectory), sourceArchive);
	}

	private String detectPluginVersion(final String groupId, final String artifactId, final String fallbackVersion) {
		final List<Plugin> plugins = project.getPluginManagement().getPlugins();
		for (final Plugin plugin : plugins) {
			if (groupId.equals(plugin.getGroupId()) && artifactId.equals(plugin.getArtifactId())) {
				getLog().debug("Using managed version " + plugin.getVersion() + " for plugin " + groupId + ":" + artifactId + ".");
				return plugin.getVersion();
			}
		}
		getLog().warn(format("No version defined in the efective model for plugin %s:%s. Please consider defining one in the pluginManagement section. Falling back to version \"%s\"", groupId, artifactId, fallbackVersion));
		return fallbackVersion;
	}

	@Override
	public void execute() throws MojoExecutionException {
		if (!isRecipeProject(project)) {
			getLog().debug(format("Skipping execution for project with packaging type \"%s\"", project.getPackaging()));
			return;
		}

		final Set<Artifact> dependencies = getDependenciesToInclude();
		buildBundle(dependencies);
		buildSourceBundle(dependencies);
		publishP2Metedata();
	}

	private File generateFinalBundleManifest() throws MojoExecutionException {
		try {
			File mfile = new File(outputDirectory, "META-INF/MANIFEST.MF");
			final InputStream is = new FileInputStream(mfile);
			Manifest mf;
			try {
				mf = new Manifest(is);
			} finally {
				is.close();
			}
			final Attributes attributes = mf.getMainAttributes();

			if (attributes.getValue(Name.MANIFEST_VERSION) == null) {
				attributes.put(Name.MANIFEST_VERSION, "1.0");
			}

			// shameless self-promotion
			attributes.putValue(CREATED_BY, "Eclipse Bundle Recipe Maven Plug-in");

			final String expandedVersion = getExpandedVersion();
			attributes.putValue(BUNDLE_VERSION, expandedVersion);

			mfile = getFinalBundleManifestFile();
			mfile.getParentFile().mkdirs();
			final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(mfile));
			try {
				mf.write(os);
			} finally {
				os.close();
			}

			return mfile;
		} catch (final Exception e) {
			throw new MojoExecutionException("Error generating bundle manifest: " + e.getMessage(), e);
		}
	}

	private void generateSourceBundleL10nFile() throws IOException {
		// read generates manifest for resolving bundle name
		final InputStream is = new FileInputStream(getFinalBundleManifestFile());
		Manifest bundleManifest;
		try {
			bundleManifest = new Manifest(is);
		} finally {
			is.close();
		}
		final Properties l10nProps = readL10nProps(bundleManifest);
		String bundleName = getL10nResolvedValue(bundleManifest, BUNDLE_NAME, l10nProps);
		if (bundleName == null) {
			getLog().warn("Bundle-Name header not found in " + getFinalBundleManifestFile() + ", fallback to Bundle-SymbolicName for source bundle");
			bundleName = getSourceBundleSymbolicName();
		}
		final String sourceBundleName = bundleName + " Source";
		String bundleVendor = getL10nResolvedValue(bundleManifest, BUNDLE_VENDOR, l10nProps);
		if (bundleVendor == null) {
			getLog().warn("Bundle-Vendor header not found in " + getFinalBundleManifestFile() + ", fallback to 'unknown' for source bundle");
			bundleVendor = "unknown";
		}
		final File l10nOutputDir = new File(dependenciesSourcesDirectory);
		final Properties sourceL10nProps = new Properties();
		sourceL10nProps.setProperty(I18N_KEY_BUNDLE_NAME, sourceBundleName);
		sourceL10nProps.setProperty(I18N_KEY_BUNDLE_VENDOR, bundleVendor);
		final File l10nPropsFile = new File(l10nOutputDir, BUNDLE_LOCALIZATION_DEFAULT_BASENAME + ".properties");
		l10nPropsFile.getParentFile().mkdirs();
		OutputStream out = null;
		try {
			out = new FileOutputStream(l10nPropsFile);
			sourceL10nProps.store(out, "Source Bundle Localization");
		} finally {
			IOUtil.close(out);
		}
	}

	private File generateSourceBundleManifest() throws MojoExecutionException {
		try {
			generateSourceBundleL10nFile();

			final Manifest mf = new Manifest();
			final Attributes attributes = mf.getMainAttributes();

			if (attributes.getValue(Name.MANIFEST_VERSION) == null) {
				attributes.put(Name.MANIFEST_VERSION, "1.0");
			}

			final String expandedVersion = getExpandedVersion();
			attributes.putValue(BUNDLE_VERSION, expandedVersion);
			attributes.putValue(BUNDLE_MANIFESTVERSION, "2");
			attributes.putValue(BUNDLE_SYMBOLICNAME, getSourceBundleSymbolicName());
			attributes.putValue(BUNDLE_NAME, I18N_KEY_PREFIX + I18N_KEY_BUNDLE_NAME);
			attributes.putValue(BUNDLE_VENDOR, I18N_KEY_PREFIX + I18N_KEY_BUNDLE_VENDOR);
			//attributes.putValue(BUNDLE_LOCALIZATION, BUNDLE_LOCALIZATION_DEFAULT_BASENAME);
			attributes.putValue("Eclipse-SourceBundle", project.getArtifactId() + ";version=\"" + expandedVersion + "\";roots:=\".\"");
			attributes.putValue(CREATED_BY, "Eclipse Bundle Recipe Maven Plug-in");

			final File mfile = getSourceBundleManifestFile();
			mfile.getParentFile().mkdirs();
			final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(mfile));
			try {
				mf.write(os);
			} finally {
				os.close();
			}

			return mfile;
		} catch (final Exception e) {
			throw new MojoExecutionException("Error generating source bundle manifest: " + e.getMessage(), e);
		}
	}

	private String getBundleVersion() {
		return BundleUtil.getBundleVersion(project.getVersion());
	}

	private Set<Artifact> getDependenciesToInclude() {
		final DependencyUtil dependencyUtil = new DependencyUtil(getLog(), session);
		dependencyUtil.initializeExcludeDependencies(excludeDependencies);
		return dependencyUtil.getDependenciesToInclude(project);
	}

	private String getExpandedVersion() {
		return BundleUtil.getExpandedVersion(getBundleVersion(), qualifier);
	}

	private FileSet getFileSet(final File basedir) {
		final DefaultFileSet fileSet = new DefaultFileSet();
		fileSet.setDirectory(basedir);
		fileSet.setExcludes(AbstractScanner.DEFAULTEXCLUDES);
		return fileSet;
	}

	private File getFinalBundleManifestFile() {
		return new File(buildDirectory, "MANIFEST.MF");
	}

	private String getL10nResolvedValue(final Manifest manifest, final String manifestHeaderKey, final Properties l10nProps) {
		final String value = manifest.getMainAttributes().getValue(manifestHeaderKey);
		if ((value == null) || !value.startsWith(I18N_KEY_PREFIX))
			return value;
		if (l10nProps == null)
			return null;
		final String key = value.substring(1).trim();
		return l10nProps.getProperty(key);
	}

	@Override
	protected File getOutputDirectory() {
		return outputDirectory;
	}

	private File getSourceBundleManifestFile() {
		return new File(buildDirectory, "MANIFEST-SRC.MF");
	}

	private String getSourceBundleSymbolicName() {
		return project.getArtifactId() + ".source";
	}

	private List<Element> getUnpackConfiguration(final String outputDirectory, final Set<Artifact> dependencies, final String classifier) throws MojoExecutionException {
		final List<Element> unpackConfiguration = new ArrayList<Element>();
		unpackConfiguration.add(element(name("outputDirectory"), outputDirectory));
		if (null != excludes) {
			unpackConfiguration.add(element(name("excludes"), excludes));
		}
		if (null != includes) {
			unpackConfiguration.add(element(name("includes"), includes));
		}
		final List<Element> artifactItems = new ArrayList<Element>();
		for (final Artifact artifact : dependencies) {
			// @formatter:off
			if(classifier != null) {
				artifactItems.add(
						element("artifactItem",
								element("groupId", artifact.getGroupId()),
								element("artifactId", artifact.getArtifactId()),
								element("version", artifact.getVersion()),
								element("classifier", classifier)
								)
						);
			}
			else {
				artifactItems.add(
						element("artifactItem",
								element("groupId", artifact.getGroupId()),
								element("artifactId", artifact.getArtifactId()),
								element("version", artifact.getVersion())
								)
						);
				// @formatter:on
			}
		}
		unpackConfiguration.add(element("artifactItems", artifactItems.toArray(new Element[artifactItems.size()])));
		return unpackConfiguration;
	}

	private void initializeBndInstruction(final String key, final String value) {
		if (StringUtils.isBlank(bndInstructions.get(key))) {
			bndInstructions.put(key, value);
		}
	}

	private void publishP2Metedata() throws MojoExecutionException {
		// copy into output directory
		getLog().debug("Publishing p2 metadata...");
		try {
			// @formatter:off
			executeMojo(
					plugin(
							groupId("org.eclipse.tycho"),
							artifactId("tycho-p2-plugin"),
							version(detectPluginVersion("org.eclipse.tycho", "tycho-p2-plugin", tychoPluginVersionFallback))
							),
							goal("p2-metadata"),
							configuration(
									element("supportedProjectTypes",
											element("supportedProjectType", "eclipse-bundle-recipe"))
									),
									executionEnvironment(
											project,
											session,
											pluginManager
											)
					);
			// @formatter:on
		} catch (final MojoExecutionException e) {
			// check for http://eclip.se/428950
			final Throwable rootCause = ExceptionUtils.getRootCause(e);
			if ((rootCause instanceof IllegalArgumentException) && StringUtils.isBlank(rootCause.getMessage())) {
				final String[] trace = ExceptionUtils.getRootCauseStackTrace(e);
				if ((trace.length > 1) && (trace[1].indexOf("P2GeneratorImpl.getCanonicalArtifact") > 0)) {
					getLog().debug(e);
					throw new MojoExecutionException("The generated bundle manifest is broken. Unfortunately, the error is hard to discover (see http://eclip.se/428950). Try running Maven with '-Dosgi.logfile=/tmp/tycho-eclipse.log' to get a log file of the embedded Equinox OSGi framework.");
				}
			}
			throw new MojoExecutionException(format("Unable to generate p2 metadata. Please check the generated bundle manifest and any bnd instructions. Try running Maven with '-Dosgi.logfile=/tmp/tycho-eclipse.log' to get a log file of the embedded Equinox OSGi framework. %s", e.getMessage()), e);
		}

	}

	private Properties readL10nProps(final Manifest manifest) throws IOException {
		String bundleL10nBase = manifest.getMainAttributes().getValue(BUNDLE_LOCALIZATION);
		if (bundleL10nBase == null) {
			bundleL10nBase = BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
		}
		final File l10nPropsFile = new File(outputDirectory, bundleL10nBase + ".properties");
		if (!l10nPropsFile.isFile()) {
			getLog().warn("Bundle localization file " + l10nPropsFile + " not found.");
			return null;
		}
		final Properties l10nProps = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(l10nPropsFile);
			l10nProps.load(in);
		} finally {
			IOUtil.close(in);
		}
		return l10nProps;
	}
}
