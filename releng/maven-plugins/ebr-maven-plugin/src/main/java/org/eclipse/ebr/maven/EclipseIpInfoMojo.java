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

import static java.lang.String.format;
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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

/**
 * A Maven plug-on for collecting Eclipse IP information.
 *
 * <pre>
 *   &lt;build&gt;
 *     &lt;plugins&gt;
 *       &lt;plugin&gt;
 *         &lt;groupId&gt;org.eclipse.ebr&lt;/groupId&gt;
 *         &lt;artifactId&gt;ebr-maven-plugin&lt;/artifactId&gt;
 *         &lt;executions&gt;
 *           &lt;execution&gt;
 *             &lt;goals&gt;
 *               &lt;goal&gt;eclipse-ip-info&lt;/goal&gt;
 *             &lt;/goals&gt;
 *             &lt;configuration&gt;
 *               &lt;force&gt;true&lt;/force&gt;
 *               &lt;licenseMappings&gt;
 *                 &lt;logback-core&gt;Eclipse Public License&lt;/logback-core&gt;
 *               &lt;/licenseMappings&gt;
 *               &lt;localLicenseFiles&gt;
 *                 &lt;SLF4J-LICENSE.txt&gt;MIT License&lt;/SLF4J-LICENSE.txt&gt;
 *               &lt;/localLicenseFiles&gt;
 *             &lt;/configuration&gt;
 *           &lt;/execution&gt;
 *         &lt;/executions&gt;
 *       &lt;/plugin&gt;
 *     &lt;/plugins&gt;
 *   &lt;/build&gt;
 * </pre>
 */
@Mojo(name = "eclipse-ip-info", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class EclipseIpInfoMojo extends AbstractMojo {

	private static final String CLASSIFIER_SOURCES = "sources";

	/**
	 * The project output directory where all sources and licenses will be
	 * collected for generating the ip info.
	 */
	@Parameter(defaultValue = "${project.build.directory}/sources-for-eclipse-ipzilla", readonly = true, required = true)
	protected File outputDirectory;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	protected MavenSession mavenSession;

	@Parameter(defaultValue = "${settings}", readonly = true)
	protected Settings settings;

	@Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
	protected List<ArtifactRepository> remoteRepositories;

	@Component
	private RepositorySystem repositorySystem;

	@Component
	private RepositoryMetadataManager repositoryMetadataManager;

	@Component
	private ModelBuilder modelBuilder;

	@Component
	private BuildPluginManager pluginManager;

	@Component
	private MavenProjectHelper projectHelper;

	/**
	 * A comma separated list of artifactIds which should be exclude completely
	 * from the processing. TEST dependencies will always be excluded.
	 */
	@Parameter(defaultValue = "${excludeDependencies}")
	protected String excludeDependencies;

	@Parameter(defaultValue = "2.6", property = "maven-resource-plugin.version", required = true)
	protected String mavenResourcesPluginVersion = "2.6";

	@Parameter(defaultValue = "2.8", property = "maven-dependency-plugin.version", required = true)
	protected String mavenDependencyPluginVersion = "2.8";

	@Parameter(defaultValue = "3.1.1", property = "maven-dependency-plugin.version", required = true)
	protected String mavenDependencyPluginVersionFallback;

	@Parameter(defaultValue = "false", property = "force")
	private boolean force;

	@Parameter(defaultValue = "false", property = "forceDownload")
	private boolean forceDownload;

	@Parameter(defaultValue = "true", property = "failBuildIfIpLogIsIncomplete")
	private boolean failBuildIfIpLogIsIncomplete;

	@Parameter(property = "submitCqsToProject")
	protected String submitCqsToProject;

	@Parameter(property = "cqCryptography")
	protected String cqCryptography;

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

	@Component
	private SettingsDecrypter settingsDecrypter;

	/**
	 * Custom license mappings (key is artifactId and value is the license used
	 * at Eclipse.org)
	 */
	@Parameter
	protected Map<String, String> licenseMappings = new LinkedHashMap<String, String>();

	/**
	 * Custom about_files mappings (key is the local license file name within
	 * the about_files folder, value is the license name it represents)
	 */
	@Parameter
	protected Map<String, String> localLicenseFiles = new LinkedHashMap<String, String>();

	private void collectSources(final Set<Artifact> dependencies) throws MojoExecutionException {
		// collect sources
		getLog().info("Gathering sources archives");

		// collect source files (filtered with excludes and includes) into folders
		// @formatter:off
		final List<Element> unpackConfigurationSource = getDependenciesUnpackConfiguration(outputDirectory, dependencies, CLASSIFIER_SOURCES);
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
							mavenSession,
							pluginManager
							)
					);
		} catch(final MojoExecutionException e) {
			getLog().warn("Unable to resolve source jar; skipping Eclipse IP information");
			getLog().debug(e);
			return;
		}
		// @formatter:on

		// copy full source jars into output folder
		// @formatter:off
		final List<Element> copyConfigurationSource = getCopyConfiguration(outputDirectory.getAbsolutePath(), dependencies, CLASSIFIER_SOURCES);
		try {
			executeMojo(plugin(groupId("org.apache.maven.plugins"), artifactId("maven-dependency-plugin"), version(mavenDependencyPluginVersion)), goal("copy"), configuration(copyConfigurationSource.toArray(new Element[copyConfigurationSource.size()])), executionEnvironment(project, mavenSession, pluginManager));
		} catch (final MojoExecutionException e) {
			getLog().warn("Unable to resolve source jar; skipping Eclipse IP information");
			getLog().debug(e);
			return;
		}
		// @formatter:on

		// rename all jars to zip
		for (final File file : outputDirectory.listFiles((f) -> f.getAbsolutePath().endsWith(".jar"))) {
			final String absolutePath = file.getAbsolutePath();
			final File newFile = new File(StringUtils.removeEnd(absolutePath, ".jar") + ".zip");
			getLog().debug(format("Renaming '%s' to '%s'.", file.getName(), newFile.getName()));
			file.renameTo(newFile);
		}
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
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (!BundleMojo.isRecipeProject(project)) {
			getLog().debug(format("Skipping execution for project with packaging type \"%s\"", project.getPackaging()));
			return;
		}

		final Set<Artifact> dependencies = getDependenciesToInclude();
		collectSources(dependencies);
		refreshAboutFiles(dependencies);
	}

	private List<Element> getCopyConfiguration(final String outputDirectory, final Set<Artifact> dependencies, final String classifier) throws MojoExecutionException {
		final List<Element> copyConfiguration = new ArrayList<Element>();
		copyConfiguration.add(element(name("outputDirectory"), outputDirectory));

		final List<Element> artifactItems = new ArrayList<Element>();
		for (final Artifact artifact : dependencies) {
			// @formatter:off
			if (classifier != null) {
				artifactItems.add(element("artifactItem", element("groupId", artifact.getGroupId()), element("artifactId", artifact.getArtifactId()), element("version", artifact.getVersion()), element("classifier", classifier)));
			} else {
				artifactItems.add(element("artifactItem", element("groupId", artifact.getGroupId()), element("artifactId", artifact.getArtifactId()), element("version", artifact.getVersion())));
				// @formatter:on
			}
		}
		copyConfiguration.add(element("artifactItems", artifactItems.toArray(new Element[artifactItems.size()])));

		return copyConfiguration;
	}

	private Set<Artifact> getDependenciesToInclude() {
		final DependencyUtil dependencyUtil = new DependencyUtil(getLog(), mavenSession);
		dependencyUtil.initializeExcludeDependencies(excludeDependencies);
		return dependencyUtil.getDependenciesToInclude(project);
	}

	private Element getDependenciesUnpackArtifactItems(final Set<Artifact> dependencies, final String classifier, final File outputDirectory) {
		final List<Element> artifactItems = new ArrayList<Element>();
		for (final Artifact artifact : dependencies) {
			// @formatter:off
			artifactItems.add(
					element("artifactItem",
							element("groupId", artifact.getGroupId()),
							element("artifactId", artifact.getArtifactId()),
							element("version", artifact.getVersion()),
							element("classifier", classifier),
							element("outputDirectory", getOutputDirectoryForFilteredDependencySources(outputDirectory, artifact).getAbsolutePath())
							)
					);
		}
		return element("artifactItems", artifactItems.toArray(new Element[artifactItems.size()]));
	}

	private List<Element> getDependenciesUnpackConfiguration(final File outputDirectory, final Set<Artifact> dependencies, final String classifier) throws MojoExecutionException {
		final List<Element> unpackConfiguration = new ArrayList<Element>();
		unpackConfiguration.add(element(name("outputDirectory"), outputDirectory.getAbsolutePath()));
		if (null != excludes) {
			unpackConfiguration.add(element(name("excludes"), excludes));
		}
		if (null != includes) {
			unpackConfiguration.add(element(name("includes"), includes));
		}
		unpackConfiguration.add(getDependenciesUnpackArtifactItems(dependencies, classifier, outputDirectory));


		return unpackConfiguration;
	}

	private File getIpLogXmlDirectory() throws MojoExecutionException {
		return new File(getProjectDir(), "src/eclipse");
	}

	private ModelUtil getModelUtil() {
		return new ModelUtil(getLog(), mavenSession, repositorySystem, repositoryMetadataManager, modelBuilder, remoteRepositories);
	}

	private File getOutputDirectoryForFilteredDependencySources(final File outputDirectory, final Artifact dependency) {
		return new File(outputDirectory, format("%s-%s-sources-filtered", dependency.getArtifactId(), dependency.getVersion()));
	}

	private File getProjectDir() throws MojoExecutionException {
		final File projectDir = project.getBasedir();
		if (projectDir == null)
			throw new MojoExecutionException("Unable to determine project directory for project: " + project);
		return projectDir;
	}

	private void populateLicenseInformation(final LicenseProcessingUtility licenseProcessingUtility, final Set<Artifact> dependencies) throws MojoExecutionException {
		for (final Artifact artifact : dependencies) {
			final String license = licenseMappings.get(artifact.getArtifactId());
			getLog().debug(format("License mapping for artifact %s:%s:%s: %s", license, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), license));
			if (license != null) {
				licenseProcessingUtility.setLicense(artifact, license);
			}
		}
		for (final Entry<String, String> e : localLicenseFiles.entrySet()) {
			licenseProcessingUtility.setLicenseFile(e.getValue(), e.getKey());
		}
	}

	private void refreshAboutFiles(final Set<Artifact> dependencies) throws MojoExecutionException {
		getLog().info("Refreshing about files and about.html");

		// build models
		final SortedMap<Artifact, Model> effectiveModels = getModelUtil().buildEffectiveModels(dependencies);

		// populate license information
		final AboutFilesUtil aboutFilesUtil = new AboutFilesUtil(getLog(), mavenSession, force, forceDownload);
		populateLicenseInformation(aboutFilesUtil, dependencies);

		// generate about files
		final File resourcesDir = new File(getProjectDir(), "src/main/resources");
		aboutFilesUtil.generateAboutHtmlFile(effectiveModels, resourcesDir);
	}
}
