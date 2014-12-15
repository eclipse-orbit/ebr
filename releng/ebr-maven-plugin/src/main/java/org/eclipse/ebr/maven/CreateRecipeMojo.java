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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.apache.commons.lang3.CharEncoding.UTF_8;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.eclipse.ebr.maven.OsgiLocalizationUtil.I18N_KEY_BUNDLE_NAME;
import static org.eclipse.ebr.maven.OsgiLocalizationUtil.I18N_KEY_BUNDLE_VENDOR;
import static org.osgi.framework.Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.aether.repository.RepositoryPolicy;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.felix.utils.properties.Properties;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.repository.RepositorySystem;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;

/**
 * A Maven plug-in for creating recipes.
 */
@Mojo(name = "create-recipe", requiresProject = false)
public class CreateRecipeMojo extends AbstractMojo {

	public final class MavenModelResolver implements ModelResolver {

		private final Map<String, ArtifactRepository> repositoryById = new HashMap<>();

		public MavenModelResolver(final Collection<ArtifactRepository> repositories) {
			for (final ArtifactRepository repository : repositories) {
				putRepository(repository, false);
			}
		}

		@Override
		public void addRepository(final Repository repository) throws org.apache.maven.model.resolution.InvalidRepositoryException {
			addRepository(repository, false);
		}

		@Override
		public void addRepository(final Repository repository, final boolean replace) throws org.apache.maven.model.resolution.InvalidRepositoryException {
			try {
				putRepository(repositorySystem.buildArtifactRepository(repository), replace);
			} catch (final InvalidRepositoryException e) {
				throw new org.apache.maven.model.resolution.InvalidRepositoryException(e.getMessage(), repository, e);
			}
		}

		@Override
		public ModelResolver newCopy() {
			return new MavenModelResolver(repositoryById.values());
		}

		private void putRepository(final ArtifactRepository repository, final boolean replace) {
			if (replace || !repositoryById.containsKey(repository.getId())) {
				repositoryById.put(repository.getId(), repository);
			}
		}

		public void resetRepositories() {
			getLog().debug("Clearing repositories.");
			repositoryById.clear();
		}

		public Artifact resolveArtifactPom(final String groupId, final String artifactId, final String version) throws UnresolvableModelException {
			getLog().debug(format("Resolving POM for artifact %s:%s:%s.", groupId, artifactId, version));

			final Artifact requestedArtifact = repositorySystem.createArtifact(groupId, artifactId, version, "pom");
			final ArtifactResolutionRequest request = new ArtifactResolutionRequest();
			request.setArtifact(requestedArtifact);
			request.setRemoteRepositories(new ArrayList<>(repositoryById.values()));
			if (getLog().isDebugEnabled()) {
				getLog().debug("Request: " + request);
			}

			final ArtifactResolutionResult result = repositorySystem.resolve(request);
			if (getLog().isDebugEnabled()) {
				getLog().debug("Result: " + result);
			}
			if (result.getArtifacts().isEmpty())
				throw new UnresolvableModelException(format("No POM found for artifact %s:%s:%s.", groupId, artifactId, version), groupId, artifactId, version);
			if (result.getArtifacts().size() > 1)
				throw new UnresolvableModelException(format("More than one POM found for artifact %s:%s:%s. This is unexpected. Please re-try with a more specific version or report this error.", groupId, artifactId, version), groupId, artifactId, version);
			return result.getArtifacts().iterator().next();
		}

		@Override
		public ModelSource resolveModel(final Parent parent) throws UnresolvableModelException {
			return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
		}

		@Override
		public ModelSource resolveModel(final String groupId, final String artifactId, final String version) throws UnresolvableModelException {
			getLog().debug(format("Resolving model for artifact %s:%s:%s.", groupId, artifactId, version));
			return new FileModelSource(resolveArtifactPom(groupId, artifactId, version).getFile());
		}

	}

	private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

	private static final String DOT_PROJECT = ".project";

	private static final String OSGI_BND = "osgi.bnd";

	private static final String ABOUT_HTML = "about.html";

	private static final String POM_XML = "pom.xml";

	private static final String HTTP_PREFIX = "http://";

	private static final String HTTPS_PREFIX = "https://";

	private static final String REQUIRES_FORCE_TO_OVERRIDE_MESSAGE = "Please set the force property to true in order to override it (eg. '-Dforce=true' via command line).";

	@Component
	MavenSession mavenSession;

	@Component
	RepositorySystem repositorySystem;

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

	protected MavenXpp3Reader modelReader = new MavenXpp3Reader();

	protected MavenXpp3Writer modelWriter = new MavenXpp3Writer();

	private void appendAndDownloadLicenseInfo(final StrBuilder text, final File downloadDir, final Artifact artifact, final Model artifactPom) {
		boolean first = true;
		for (final Iterator<License> stream = artifactPom.getLicenses().iterator(); stream.hasNext();) {
			final License license = stream.next();
			if (!first && !stream.hasNext()) {
				text.append(" and ");
			} else if (!first && stream.hasNext()) {
				text.append(", ");
			} else {
				first = false;
			}
			final String url = license.getUrl();
			boolean wroteUrl = false;
			String licenseFileName = null;
			if (isPotentialWebUrl(url)) {
				try {
					final URL licenseUrl = toUrl(url); // parse as url to avoid surprises
					text.append("<a href=\"").append(licenseUrl.toExternalForm()).append("\" target=\"_blank\">");
					wroteUrl = true;
					try {
						getLog().info(format("Downloading license '%s' (%s).", license.getName(), licenseUrl.toExternalForm()));
						licenseFileName = downloadLicenseFile(downloadDir, license, licenseUrl);
						getLog().info(format("  -> %s.", licenseFileName));
					} catch (final IOException e) {
						licenseFileName = null;
						getLog().debug(e);
						getLog().warn(format("Unable to download license file from '%s'. Please add manually to recipe project. %s", licenseUrl.toExternalForm(), e.getMessage()));
					}

				} catch (final MalformedURLException e) {
					getLog().debug(e);
					getLog().warn(format("Invalid license url '%s' in artifact pom '%s'.", url, artifact.getFile()));
				}
			}
			text.append(escapeHtml4(license.getName()));
			if (wroteUrl) {
				text.append("</a>");
			}
			if (licenseFileName != null) {
				text.append(" (<a href=\"").append(licenseFileName).append("\" target=\"_blank\">").append(FilenameUtils.getName(licenseFileName)).append("</a>)");
			}
		}
	}

	private void appendDeveloperInfo(final StrBuilder text, final Model artifactPom) {
		boolean first = true;
		for (final Iterator<Developer> stream = artifactPom.getDevelopers().iterator(); stream.hasNext();) {
			final Developer developer = stream.next();
			if (!first && !stream.hasNext()) {
				text.append(" and ");
			} else if (!first && stream.hasNext()) {
				text.append(", ");
			} else {
				first = false;
			}
			text.append(escapeHtml4(developer.getName())).append(" &lt;").append(escapeHtml4(developer.getEmail())).append("&gt;");
			if (StringUtils.isNotBlank(developer.getOrganization())) {
				text.append(" (").append(escapeHtml4(developer.getOrganization())).append(")");
			}
		}
	}

	private void appendIssueTrackingInfo(final StrBuilder text, final Artifact artifact, final Model artifactPom) {
		final String url = artifactPom.getIssueManagement().getUrl();
		if (isPotentialWebUrl(url)) {
			try {
				final URL issueTrackingUrl = toUrl(url); // parse as URL to avoid surprises
				text.append("Bugs or feature requests can be made in the project issue tracking system at ");
				text.append("<a href=\"").append(issueTrackingUrl.toExternalForm()).append("\" target=\"_blank\">");
				text.append(escapeHtml4(removeWebProtocols(url)));
				text.append("</a>.");
			} catch (final MalformedURLException e) {
				getLog().debug(e);
				getLog().warn(format("Invalide project issue tracking url '%s' in artifact pom '%s'.", url, artifact.getFile()));
			}
		} else if (StringUtils.isNotBlank(url)) {
			text.append("Bugs or feature requests can be made in the project issue tracking system at ");
			text.append(escapeHtml4(url)).append('.');
		}
	}

	private void appendMailingListInfo(final StrBuilder text, final Artifact artifact, final Model artifactPom) {
		boolean first = true;
		for (final Iterator<MailingList> stream = artifactPom.getMailingLists().iterator(); stream.hasNext();) {
			final MailingList mailingList = stream.next();
			if (!first && !stream.hasNext()) {
				text.append(" or ");
			} else if (!first && stream.hasNext()) {
				text.append(", ");
			} else {
				first = false;
			}
			text.append(escapeHtml4(mailingList.getName()));
			if (StringUtils.isNotBlank(mailingList.getPost())) {
				text.append(" &lt;").append(escapeHtml4(mailingList.getPost())).append("&gt;");
			}
			final String url = mailingList.getArchive();
			if (isPotentialWebUrl(url)) {
				try {
					final URL archiveUrl = toUrl(url); // parse as URL to avoid surprises
					text.append(" (<a href=\"").append(archiveUrl.toExternalForm()).append("\" target=\"_blank\">archive</a>)");
				} catch (final MalformedURLException e) {
					getLog().debug(e);
					getLog().warn(format("Invalide mailing list archive url '%s' in artifact pom '%s'.", url, artifact.getFile()));
				}
			}
		}
	}

	private Model buildEffectiveModel(final File pomFile) throws MojoExecutionException {
		getLog().debug(format("Building effective model for pom '%s'.", pomFile));

		final DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
		request.setModelResolver(getResolver());
		request.setPomFile(pomFile);
		request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
		request.setProcessPlugins(false);
		request.setTwoPhaseBuilding(false);
		request.setUserProperties(mavenSession.getUserProperties());
		request.setSystemProperties(mavenSession.getSystemProperties());
		if (getLog().isDebugEnabled()) {
			getLog().debug("Request: " + request);
		}

		ModelBuildingResult result;
		try {
			result = modelBuilder.build(request);
		} catch (final ModelBuildingException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to build model for pom '%s'. %s", pomFile, e.getMessage()));
		}

		if (getLog().isDebugEnabled()) {
			getLog().debug("Result: " + result);
		}

		return result.getEffectiveModel();
	}

	void configureRepositoryRequest(final RepositoryRequest request) throws MojoExecutionException {
		request.setLocalRepository(mavenSession.getLocalRepository());
		if (!mavenSession.isOffline()) {
			try {
				request.setRemoteRepositories(Arrays.asList(repositorySystem.createDefaultRemoteRepository()));
			} catch (final InvalidRepositoryException e) {
				getLog().debug(e);
				throw new MojoExecutionException(format("Unable to create the default remote repository. Please verify the Maven configuration. %s", e.getMessage()));
			}
		}
		request.setOffline(mavenSession.isOffline());
		request.setForceUpdate(RepositoryPolicy.UPDATE_POLICY_ALWAYS.equals(mavenSession.getRepositorySession().getUpdatePolicy()));
	}

	private String downloadLicenseFile(final File licenseOutputDir, final License license, final URL licenseUrl) throws IOException {
		String licenseFileName = "about_files/" + sanitizeFileName(license.getName()).toUpperCase();
		final String existingLicense = findExistingLicenseFile(licenseOutputDir, licenseFileName);
		if (existingLicense != null) {
			if (!force) {
				getLog().warn(format("Found existing license file at '%s'. %s", existingLicense, REQUIRES_FORCE_TO_OVERRIDE_MESSAGE));
				return existingLicense;
			} else if (mavenSession.isOffline()) {
				getLog().warn(format("Re-using existing license file at '%s'. Maven is offline.", existingLicense));
				return existingLicense;
			}
		} else if (mavenSession.isOffline())
			throw new IOException("Maven is offline.");
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			final HttpGet get = new HttpGet(licenseUrl.toExternalForm());
			get.setHeader("Accept", "text/plain,text/html");
			try (final CloseableHttpResponse response = client.execute(get)) {
				if (response.getStatusLine().getStatusCode() != 200)
					throw new IOException(format("Download failed: %s", response.getStatusLine().toString()));
				final HttpEntity entity = response.getEntity();
				if (entity == null)
					throw new IOException("Download faild. Empty respose.");

				try (final InputStream is = entity.getContent()) {
					final ContentType contentType = ContentType.getOrDefault(entity);
					if (StringUtils.equalsIgnoreCase(contentType.getMimeType(), "text/plain")) {
						licenseFileName = licenseFileName + ".txt";
					} else if (StringUtils.equalsIgnoreCase(contentType.getMimeType(), "text/html")) {
						licenseFileName = licenseFileName + ".html";
					} else {
						getLog().warn(format("Unexpected content type (%s) returned by remote server. Falling back to text/plain.", contentType));
						licenseFileName = licenseFileName + ".txt";
					}

					final FileOutputStream os = FileUtils.openOutputStream(new File(licenseOutputDir, licenseFileName));
					try {
						IOUtils.copy(is, os);
					} finally {
						IOUtils.closeQuietly(os);
					}
				}
			}
		}
		return licenseFileName;
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
		generateOsgiBndFile(recipePom, projectDir);

		getLog().info("Generating recipe about.html.");
		generateAboutHtmlFile(resolvedPomArtifact, artifactPom, recipePom, resourcesDir);
	}

	private String findExistingLicenseFile(final File licenseOutputDir, final String licenseFileName) {
		for (final String extension : Arrays.asList(".txt", ".html")) {
			if (new File(licenseOutputDir, licenseFileName + extension).isFile())
				return licenseFileName + extension;
		}
		return null;
	}

	private File findParentPom() {
		getLog().debug("Using base dir: " + baseDir.getAbsolutePath());
		return new File(baseDir, POM_XML);
	}

	private void generateAboutHtmlFile(final Artifact resolvedPomArtifact, final Model artifactPom, final Model recipePom, final File resourcesDir) throws MojoExecutionException {
		final File aboutHtmlFile = new File(resourcesDir, ABOUT_HTML);
		if (aboutHtmlFile.isFile() && !force) {
			getLog().warn(format("Found existing about.html file at '%s'. %s", aboutHtmlFile, REQUIRES_FORCE_TO_OVERRIDE_MESSAGE));
			return;
		}
		String aboutHtmlText = readAboutHtmlTemplate();
		aboutHtmlText = StringUtils.replaceEach(aboutHtmlText, new String[] {// @formatter:off
				"@DATE@",
				"@DEPENDENCY_HEADLINE@",
				"@DEPENDENCY_BY@",
				"@DEPENDENCY_NAME@",
				"@DEPENDENCY_LICENSING@",
				"@DEPENDENCY_ORIGIN@"
		}, new String[] {
				DateFormat.getDateInstance(DateFormat.LONG, Locale.US).format(new Date()),
				escapeHtml4(recipePom.getName()),
				getDevelopedByInfo(resolvedPomArtifact, artifactPom),
				escapeHtml4(recipePom.getName()),
				getLicenseInfo(resolvedPomArtifact, artifactPom, resourcesDir),
				getOriginInfo(resolvedPomArtifact, artifactPom)
		});
		// @formatter:on

		try {
			FileUtils.writeStringToFile(aboutHtmlFile, aboutHtmlText, UTF_8);
		} catch (final IOException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to write about.html file '%s'. %s", aboutHtmlFile, e.getMessage()));
		}
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
		eclipseProjectFileText = StringUtils.replaceEach(eclipseProjectFileText, new String[] {// @formatter:off
				"@RECIPE_PROJECT_NAME@"
		}, new String[] {
				projectDir.getName()
		});
		// @formatter:on

		try {
			FileUtils.writeStringToFile(eclipseProjectFile, eclipseProjectFileText, UTF_8);
		} catch (final IOException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to write .project file '%s'. %s", eclipseProjectFile, e.getMessage()));
		}
	}

	private void generateOsgiBndFile(final Model recipePom, final File projectDir) throws MojoExecutionException {
		final File osgiBndFile = new File(projectDir, OSGI_BND);
		if (osgiBndFile.isFile() && !force) {
			getLog().warn(format("Found existing osgi.bnd file at '%s'. %s", osgiBndFile, REQUIRES_FORCE_TO_OVERRIDE_MESSAGE));
			return;
		}
		final String osgiBndText = readOsgiBndTemplate();

		try {
			FileUtils.writeStringToFile(osgiBndFile, osgiBndText, UTF_8);
		} catch (final IOException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to write osgi.bnd file '%s'. %s", osgiBndFile, e.getMessage()));
		}
	}

	private String getDevelopedByInfo(final Artifact artifact, final Model artifactPom) {
		final StrBuilder developedByInfo = new StrBuilder();

		// prefer organization if available
		if (null != artifactPom.getOrganization()) {
			final String url = artifactPom.getOrganization().getUrl();
			boolean wroteUrl = false;
			if (isPotentialWebUrl(url)) {
				try {
					final URL organizationUrl = toUrl(url); // parse as URL to avoid surprises
					developedByInfo.append("<a href=\"").append(organizationUrl.toExternalForm()).append("\" target=\"_blank\">");
					wroteUrl = true;
				} catch (final MalformedURLException e) {
					getLog().debug(e);
					getLog().warn(format("Invalide organization url '%s' in artifact pom '%s'.", url, artifact.getFile()));
				}
			}
			if (StringUtils.isNotBlank(artifactPom.getOrganization().getName())) {
				developedByInfo.append(escapeHtml4(artifactPom.getOrganization().getName()));
			} else if (StringUtils.isNotBlank(url)) {
				developedByInfo.append(escapeHtml4(removeWebProtocols(url)));
			}
			if (wroteUrl) {
				developedByInfo.append("</a>");
			}
		}

		// use to developers if no organization is available
		if (developedByInfo.isEmpty()) {
			if (!artifactPom.getDevelopers().isEmpty()) {
				appendDeveloperInfo(developedByInfo, artifactPom);
			} else {
				getLog().warn(format("Neither organization nor developer information is available for artifact '%s:%s:%s'. Please fill in manually.", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
				developedByInfo.append("someone");
			}
		}
		return developedByInfo.toString();
	}

	private String getLicenseInfo(final Artifact resolvedPomArtifact, final Model artifactPom, final File resourcesDir) {
		final StrBuilder licenseInfo = new StrBuilder();
		if (artifactPom.getLicenses().isEmpty()) {
			getLog().warn(format("No licensing information found for artifact %s:%s:%s. Please fill in information in about.html manually!", artifactPom.getGroupId(), artifactPom.getArtifactId(), artifactPom.getVersion()));
			licenseInfo.append(escapeHtml4(artifactPom.getName())).append(" is distributed without licensing information.");
			if (!artifactPom.getDevelopers().isEmpty()) {
				licenseInfo.append("Please contact the ");
				if (artifactPom.getDevelopers().size() == 1) {
					licenseInfo.append("developer ");
				} else {
					licenseInfo.append("developers ");

				}
				appendDeveloperInfo(licenseInfo, artifactPom);
				licenseInfo.append(" for further information");
			}
		} else {
			licenseInfo.append(escapeHtml4(artifactPom.getName())).append(" is provided to you under the terms and conditions of the ");
			appendAndDownloadLicenseInfo(licenseInfo, resourcesDir, resolvedPomArtifact, artifactPom);
			if (artifactPom.getLicenses().size() == 1) {
				licenseInfo.append(" license.");
			} else {
				licenseInfo.append(" licenses.");
			}
		}
		return licenseInfo.toString();
	}

	private String getOriginInfo(final Artifact artifact, final Model artifactPom) {
		final StrBuilder originInfo = new StrBuilder();
		{
			final String url = artifactPom.getUrl();
			if (isPotentialWebUrl(url)) {
				try {
					final URL organizationUrl = toUrl(url); // parse as URL to avoid surprises
					originInfo.append(escapeHtml4(artifactPom.getName())).append(" including its source is available from ");
					originInfo.append("<a href=\"").append(organizationUrl.toExternalForm()).append("\" target=\"_blank\">");
					originInfo.append(escapeHtml4(removeWebProtocols(url)));
					originInfo.append("</a>.");
				} catch (final MalformedURLException e) {
					getLog().debug(e);
					getLog().warn(format("Invalide project url '%s' in artifact pom '%s'.", url, artifact.getFile()));
				}
			} else if (StringUtils.isNotBlank(url)) {
				originInfo.append(escapeHtml4(artifactPom.getName())).append(" including its source is available from ");
				originInfo.append(escapeHtml4(url)).append('.');
			}
		}
		// fall-back to Maven coordinates
		if (originInfo.isEmpty()) {
			getLog().warn(format("No project origin information is available for artifact '%s:%s:%s'. Please fill in manually.", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
			originInfo.append(escapeHtml4(artifactPom.getName())).append(" is available from Maven as ");
			originInfo.append(escapeHtml4(artifact.getGroupId())).append(':').append(escapeHtml4(artifact.getArtifactId())).append(':').append(escapeHtml4(artifact.getVersion())).append('.');
		}

		// include additional contact information if available
		if (null != artifactPom.getIssueManagement()) {
			originInfo.append(' ');
			appendIssueTrackingInfo(originInfo, artifact, artifactPom);
		}

		if (!artifactPom.getMailingLists().isEmpty()) {
			originInfo.append(" The following");
			if (artifactPom.getMailingLists().size() == 1) {
				originInfo.append(" mailing list");
			} else {
				originInfo.append(" mailing lists");
			}
			originInfo.append(" can be used to communicate with the project communities: ");
			appendMailingListInfo(originInfo, artifact, artifactPom);
			originInfo.append(".");
		}
		return originInfo.toString();
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

	private MavenModelResolver getResolver() throws MojoExecutionException {
		if (!mavenSession.isOffline()) {
			try {
				return new MavenModelResolver(Arrays.asList(repositorySystem.createDefaultRemoteRepository()));
			} catch (final InvalidRepositoryException e) {
				getLog().debug(e);
				throw new MojoExecutionException(format("Unable to create the default remote repository. Please verify the Maven configuration. %s", e.getMessage()));
			}
		} else
			return new MavenModelResolver(Collections.<ArtifactRepository> emptyList());
	}

	private InputStream getTemplate(final String name) throws FileNotFoundException {
		final ClassLoader cl = CreateRecipeMojo.class.getClassLoader();
		final InputStream is = cl.getResourceAsStream(name);
		if (is == null)
			throw new FileNotFoundException(format("Template '%s' cannot be found! Please check the plug-in packaging.", name));
		return is;
	}

	private boolean isPotentialWebUrl(final String url) {
		return (url != null) && (StringUtils.startsWithAny(url.toLowerCase(), HTTP_PREFIX, HTTPS_PREFIX) || (StringUtils.indexOf(url, ':') == -1));
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

	private String readAboutHtmlTemplate() throws MojoExecutionException {
		try {
			return IOUtils.toString(getTemplate("recipe-about.html"), UTF_8);
		} catch (final Exception e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Error reading about.html template: %s", e.getMessage()));
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

	private String readOsgiBndTemplate() throws MojoExecutionException {
		try {
			return IOUtils.toString(getTemplate("recipe-osgi.bnd"), UTF_8);
		} catch (final Exception e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Error reading osgi.bnd template: %s", e.getMessage()));
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

	private String removeWebProtocols(final String url) {
		return StringUtils.removeStart(StringUtils.removeStart(url, HTTPS_PREFIX), HTTP_PREFIX);
	}

	private Artifact resolveArtifactPom() throws MojoExecutionException {
		getLog().info(format("Resolving POM for artifact %s:%s:%s.", groupId, artifactId, artifactVersion));

		// find latest version
		final ArtifactVersion resolvedVersion = resolveArtifactVersion(artifactVersion);
		if (StringUtils.equals("RELEASE", artifactVersion) || StringUtils.isBlank(artifactVersion) || StringUtils.equals("LATEST", artifactVersion)) {
			version = resolvedVersion;
			getLog().info(format("   Using verson %s.", version));
		} else {
			version = new DefaultArtifactVersion(artifactVersion);
			if (resolvedVersion.compareTo(version) > 0) {
				getLog().info(format("   Using verson %s. The latest available release is %s.", version, resolvedVersion));
			}
		}

		// resolve POM
		try {
			return getResolver().resolveArtifactPom(groupId, artifactId, version.toString());
		} catch (final UnresolvableModelException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to resolve POM for artifact %s:%s:%s. %s", groupId, artifactId, version, e.getMessage()));
		}
	}

	private ArtifactVersion resolveArtifactVersion(final String version) throws MojoExecutionException {
		getLog().debug(format("Reading version metadata for artifact %s:%s.", groupId, artifactId));

		final RepositoryRequest request = new DefaultRepositoryRequest();
		configureRepositoryRequest(request);

		final Artifact artifact = repositorySystem.createArtifact(groupId, artifactId, "", null, "pom");
		try {
			final RepositoryMetadata metadata = new ArtifactRepositoryMetadata(artifact);
			repositoryMetadataManager.resolve(metadata, request);

			final Metadata repositoryMetadata = checkNotNull(metadata.getMetadata(), "No repository metadata loaded.");
			if (StringUtils.equals("LATEST", version)) {
				final Versioning metadataVersions = checkNotNull(repositoryMetadata.getVersioning(), "No versioning information available in repository metadata.");
				getLog().debug(format("Resolving '%s' to latest version.", version));
				return new DefaultArtifactVersion(metadataVersions.getLatest());
			} else if (StringUtils.equals("RELEASE", version)) {
				final Versioning metadataVersions = checkNotNull(repositoryMetadata.getVersioning(), "No versioning information available in repository metadata.");
				getLog().debug(format("Resolving '%s' to release version.", version));
				return new DefaultArtifactVersion(metadataVersions.getRelease());
			} else {
				getLog().debug(format("Resolving '%s' to version.", version));
				return new DefaultArtifactVersion(version);
			}
		} catch (final Exception e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to retrieve available versions for artifact %s:%s:%s. %s", groupId, artifactId, version, e.getMessage()));
		}
	}

	private String sanitizeFileName(final String name) {
		final StrBuilder result = new StrBuilder();
		for (final char c : name.toCharArray()) {
			if (Character.isLetterOrDigit(c) || (c == '+') || (c == '-') || (c == '.')) {
				result.append(c);
			} else {
				result.append('_');
			}
		}
		return result.toString();
	}

	private URL toUrl(final String url) throws MalformedURLException {
		try {
			return new URL(url);
		} catch (final MalformedURLException e) {
			// try with fall-back using http protocol
			try {
				return toUrl(HTTP_PREFIX + url);
			} catch (final MalformedURLException ignored) {
				// propagate original error
				throw e;
			}
		}
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
