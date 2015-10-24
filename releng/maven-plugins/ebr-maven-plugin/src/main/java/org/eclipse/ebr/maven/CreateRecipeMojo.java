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
 *    Brian de Alwis - avoid NPEs on logging and resolving version metadata unless required
 *******************************************************************************/
package org.eclipse.ebr.maven;

import static java.lang.String.format;
import static org.apache.commons.lang3.CharEncoding.UTF_8;
import static org.eclipse.ebr.maven.BaseUtility.REQUIRES_FORCE_TO_OVERRIDE_MESSAGE;
import static org.eclipse.ebr.maven.BaseUtility.getTemplate;
import static org.eclipse.ebr.maven.OsgiLocalizationUtil.I18N_KEY_BUNDLE_NAME;
import static org.eclipse.ebr.maven.OsgiLocalizationUtil.I18N_KEY_BUNDLE_VENDOR;
import static org.osgi.framework.Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.felix.utils.properties.Properties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.repository.RepositorySystem;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * A Maven plug-in for creating recipes.
 */
@Mojo(name = "create-recipe", requiresProject = false)
public class CreateRecipeMojo extends AbstractMojo {

	private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

	private static final String DOT_PROJECT = ".project";

	private static final String POM_XML = "pom.xml";

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession mavenSession;

	@Component
	private RepositorySystem repositorySystem;

	@Component
	private RepositoryMetadataManager repositoryMetadataManager;

	@Component
	private ModelBuilder modelBuilder;

	@Parameter(property = "groupId", required = true)
	private String groupId;

	@Parameter(property = "artifactId", required = true)
	private String artifactId;

	@Parameter(property = "version", defaultValue = "RELEASE")
	private String artifactVersion;

	private ArtifactVersion version;

	@Parameter(property = "bundleSymbolicName", required = true)
	private String bundleSymbolicName;

	@Parameter(property = "bundleVendor", defaultValue = "Eclipse EBR Maven Plug-In")
	private String bundleVendor;

	@Parameter(defaultValue = ".", property = "baseDir")
	private File baseDir;

	@Parameter(defaultValue = "false", property = "force")
	private boolean force;

	@Parameter(property = "license")
	private String license;

	protected MavenXpp3Reader modelReader = new MavenXpp3Reader();

	protected MavenXpp3Writer modelWriter = new MavenXpp3Writer();

	private Model buildEffectiveModel(final File parentPomFile) throws MojoExecutionException {
		return getModelUtil().buildEffectiveModel(parentPomFile);
	}

	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Searching for parent pom.xml.");
		final File parentPomFile = findParentPom();
		if (!parentPomFile.isFile())
			throw new MojoExecutionException(format("No parent pom.xml found at '%s'.", parentPomFile.getAbsolutePath()));
		final Model parentPom = buildEffectiveModel(parentPomFile);

		final Artifact resolvedPomArtifact = resolveArtifactPom();
		final Model artifactPom = buildEffectiveModel(resolvedPomArtifact.getFile());

		logDependencies(artifactPom, resolvedPomArtifact);

		final Model recipePom = getRecipePom(parentPom, resolvedPomArtifact, artifactPom);
		final File projectDir = getProjectDir(recipePom);

		getLog().info("Generating Eclipse .project file.");
		generateEclipseProjectFile(projectDir);

		getLog().info("Generating recipe POM.");
		writeRecipePom(parentPomFile, recipePom, projectDir);

		getLog().info("Generating recipe bundle.properties.");
		final File resourcesDir = new File(projectDir, "src/main/resources");
		generateBundleL10nFile(recipePom, resourcesDir);

		getLog().info("Generating recipe osgi.bnd.");
		generateOsgiBndFile(recipePom, projectDir, getCompileTimeDependencies(artifactPom));

		getLog().info("Generating recipe about.html.");
		generateAboutHtmlFile(resolvedPomArtifact, artifactPom, recipePom, resourcesDir);
	}

	private File findParentPom() {
		getLog().debug("Using base dir: " + baseDir.getAbsolutePath());
		return new File(baseDir, POM_XML);
	}

	private void generateAboutHtmlFile(final Artifact resolvedPomArtifact, final Model artifactPom, final Model recipePom, final File resourcesDir) throws MojoExecutionException {
		final SortedMap<Artifact, Model> dependencies = new TreeMap<>();
		dependencies.put(resolvedPomArtifact, artifactPom);

		final AboutFilesUtil aboutFilesUtil = new AboutFilesUtil(getLog(), mavenSession, force, true);
		if (license != null) {
			aboutFilesUtil.setLicense(resolvedPomArtifact, license);
		}
		aboutFilesUtil.generateAboutHtmlFile(dependencies, resourcesDir);
	}

	private void generateBundleL10nFile(final Model recipePom, final File l10nOutputDir) throws MojoExecutionException {
		final File l10nPropsFile = new File(l10nOutputDir, BUNDLE_LOCALIZATION_DEFAULT_BASENAME + ".properties");
		if (l10nPropsFile.isFile() && !force) {
			getLog().warn(format("Found existing bundle localization file at '%s'. %s", l10nPropsFile, REQUIRES_FORCE_TO_OVERRIDE_MESSAGE));
			return;
		}
		final Properties l10nProps = new Properties();
		l10nProps.put(I18N_KEY_BUNDLE_NAME, recipePom.getName() != null ? recipePom.getName() : recipePom.getArtifactId());
		l10nProps.put(I18N_KEY_BUNDLE_VENDOR, bundleVendor);
		l10nProps.setHeader(Arrays.asList("# Bundle Localization"));
		try {
			FileUtils.forceMkdir(l10nPropsFile.getParentFile());
		} catch (final IOException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to create directory '%s'. %s", l10nPropsFile.getParentFile(), e.getMessage()));
		}
		OutputStream out = null;
		try {
			out = new FileOutputStream(l10nPropsFile);
			l10nProps.save(out);
		} catch (final IOException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to write bundle localization file '%s'. %s", l10nPropsFile, e.getMessage()));
		} finally {
			IOUtil.close(out);
		}
	}

	private void generateEclipseProjectFile(final File projectDir) throws MojoExecutionException {
		final File eclipseProjectFile = new File(projectDir, DOT_PROJECT);
		if (eclipseProjectFile.isFile() && !force) {
			getLog().warn(format("Found existing .project file at '%s'. %s", eclipseProjectFile, REQUIRES_FORCE_TO_OVERRIDE_MESSAGE));
			return;
		}
		String eclipseProjectFileText = readEclipseProjectFileTemplate();
		eclipseProjectFileText = StringUtils.replaceEach(eclipseProjectFileText, new String[] { // @formatter:off
				"@RECIPE_PROJECT_NAME@" }, new String[] { projectDir.getName() });
		// @formatter:on

		try {
			FileUtils.writeStringToFile(eclipseProjectFile, eclipseProjectFileText, UTF_8);
		} catch (final IOException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to write .project file '%s'. %s", eclipseProjectFile, e.getMessage()));
		}
	}

	private void generateOsgiBndFile(final Model recipePom, final File projectDir, final Collection<Dependency> compileTimeDependencies) throws MojoExecutionException {
		final OsgiBndUtil osgiBndUtil = new OsgiBndUtil(getLog(), mavenSession, force);
		osgiBndUtil.generateOsgiBndFile(projectDir, compileTimeDependencies);
	}

	private Collection<Dependency> getCompileTimeDependencies(final Model artifactPom) {
		return Collections2.filter(artifactPom.getDependencies(), new Predicate<Dependency>() {

			@Override
			public boolean apply(final Dependency input) {
				return (input != null) && (Objects.equals(input.getScope(), Artifact.SCOPE_COMPILE) || Objects.equals(input.getScope(), Artifact.SCOPE_PROVIDED));
			}
		});
	}

	private ModelUtil getModelUtil() {
		return new ModelUtil(getLog(), mavenSession, repositorySystem, repositoryMetadataManager, modelBuilder);
	}

	private File getProjectDir(final Model recipePom) throws MojoExecutionException {
		final File projectDir = new File(baseDir, recipePom.getArtifactId() + "_" + StringUtils.removeEnd(recipePom.getVersion(), SNAPSHOT_SUFFIX));
		getLog().debug("Using project directory: " + projectDir);
		try {
			FileUtils.forceMkdir(projectDir);
		} catch (final IOException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to create recipe project directory '%s'. %s", projectDir, e.getMessage()));
		}
		return projectDir;
	}

	private Model getRecipePom(final Model parentPom, final Artifact resolvedPomArtifact, final Model artifactPom) throws MojoExecutionException {
		final Model recipePom = readPomTemplate();
		recipePom.setParent(new Parent());
		recipePom.getParent().setGroupId(parentPom.getGroupId());
		recipePom.getParent().setArtifactId(parentPom.getArtifactId());
		recipePom.getParent().setVersion(parentPom.getVersion());

		recipePom.setArtifactId(bundleSymbolicName);

		final String version = getRecipePomVersion();
		recipePom.setVersion(version);

		recipePom.setName(artifactPom.getName());

		final Dependency dependency = new Dependency();
		dependency.setGroupId(artifactPom.getGroupId());
		dependency.setArtifactId(artifactPom.getArtifactId());
		dependency.setVersion(artifactPom.getVersion());
		recipePom.setDependencies(Arrays.asList(dependency));
		return recipePom;
	}

	private String getRecipePomVersion() {
		final String version = format("%d.%d.%d%s", this.version.getMajorVersion(), this.version.getMinorVersion(), this.version.getIncrementalVersion(), SNAPSHOT_SUFFIX);
		getLog().debug("Using recipe pom.xml version: " + version);
		return version;
	}

	private void logDependencies(final Model artifactPom, final Artifact resolvedPomArtifact) {
		final List<Dependency> artifactDependencies = artifactPom.getDependencies();
		if (!artifactDependencies.isEmpty()) {
			if (getLog().isDebugEnabled()) {
				getLog().debug("Dependency trail for " + resolvedPomArtifact);
				getLog().debug(StringUtils.join(resolvedPomArtifact.getDependencyTrail(), SystemUtils.LINE_SEPARATOR));
				getLog().debug("------------");
			}
			getLog().info(format("The following dependencies are defined for artifact %s:%s:%s. Please consider creating recipes for them as well.", artifactPom.getGroupId(), artifactPom.getArtifactId(), artifactPom.getVersion()));
			for (final Dependency artifactDependency : artifactPom.getDependencies()) {
				getLog().info(format("   %s:%s:%s (scope %s)", artifactDependency.getGroupId(), artifactDependency.getArtifactId(), artifactDependency.getVersion(), artifactDependency.getScope()));
			}
		}
	}

	private String readEclipseProjectFileTemplate() throws MojoExecutionException {
		try {
			return IOUtils.toString(getTemplate("recipe.project"), UTF_8);
		} catch (final Exception e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Error reading .project template: %s", e.getMessage()));
		}
	}

	private Model readPomTemplate() throws MojoExecutionException {
		XmlStreamReader reader = null;
		try {
			reader = ReaderFactory.newXmlReader(getTemplate("recipe-pom.xml"));
			return modelReader.read(reader);
		} catch (final Exception e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Error reading pom.xml template: %s", e.getMessage()));
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	private Artifact resolveArtifactPom() throws MojoExecutionException {
		getLog().info(format("Resolving POM for artifact %s:%s:%s.", groupId, artifactId, artifactVersion));

		// find latest version
		final ArtifactVersion resolvedVersion = getModelUtil().resolveArtifactVersion(groupId, artifactId, artifactVersion);
		if (StringUtils.equals("RELEASE", artifactVersion) || StringUtils.isBlank(artifactVersion) || StringUtils.equals("LATEST", artifactVersion)) {
			version = resolvedVersion;
			getLog().info(format("   Using verson %s.", version));
		} else {
			version = new DefaultArtifactVersion(artifactVersion);
			if (resolvedVersion.compareTo(version) > 0) {
				getLog().info(format("   Using verson %s. The latest available release is %s.", version, resolvedVersion));
			}
		}

		return getModelUtil().resolveArtifactPom(groupId, artifactId, version.toString());
	}

	private void writePom(final File file, final Model model) throws MojoExecutionException {
		Writer writer = null;
		try {
			writer = new OutputStreamWriter(new FileOutputStream(file), UTF_8);
			modelWriter.write(writer, model);
		} catch (final IOException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Error writing '%s': %s", file.getAbsolutePath(), e.getMessage()));
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	private void writeRecipePom(final File parentPomFile, final Model recipePom, final File projectDir) throws MojoExecutionException {
		final File recipePomFile = new File(projectDir, POM_XML);
		if (recipePomFile.isFile() && !force) {
			getLog().warn(format("Recipe pom.xml already exists at '%s'. %s", recipePomFile.getAbsolutePath(), REQUIRES_FORCE_TO_OVERRIDE_MESSAGE));
			return;
		}
		writePom(recipePomFile, recipePom);
	}
}
