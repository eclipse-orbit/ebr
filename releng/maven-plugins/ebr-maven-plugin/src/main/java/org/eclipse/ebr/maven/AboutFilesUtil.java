/**
 * Copyright (c) 2014 Tasktop Technologies and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.ebr.maven;

import static java.lang.String.format;
import static org.apache.commons.lang3.CharEncoding.UTF_8;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.eclipse.ebr.maven.TemplateHelper.getTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.eclipse.ebr.maven.eclipseip.KnownLicense;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * A utility for working with licenses.
 */
public class AboutFilesUtil extends LicenseProcessingUtility {

	private static final String REQUIRES_FORCE_DOWNLOAD_MESSAGE = "Please set the forceDownload property to true in order to download it again (eg. '-DforceDownload=true' via command line).";
	private static final String ABOUT_HTML = "about.html";

	private final boolean forceDownload;

	public AboutFilesUtil(final Log log, final MavenSession mavenSession, final boolean force, final boolean forceDownload) {
		super(log, mavenSession, force);
		this.forceDownload = forceDownload;
	}

	private void appendAndDownloadLicenseInfo(final StrBuilder text, final File downloadDir, final Artifact artifact, final List<License> licenses) throws MojoExecutionException {
		boolean first = true;
		for (final Iterator<License> stream = licenses.iterator(); stream.hasNext();) {
			final License license = stream.next();
			if (!first && !stream.hasNext()) {
				text.append(" and ");
			} else if (!first && stream.hasNext()) {
				text.append(", ");
			} else {
				first = false;
			}
			final String localLicenseFile = getLocalLicenseFile(downloadDir, license);
			final String url = license.getUrl();
			boolean wroteUrl = false;
			String licenseFileName = null;
			if (null != localLicenseFile) {
				// prefer configured local if available
				getLog().info(format("Using local license file (%s) for '%s'.", localLicenseFile, license.getName()));
				licenseFileName = localLicenseFile;
			} else if (isPotentialWebUrl(url)) {
				// try download if we have a web url
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

	private String downloadLicenseFile(final File licenseOutputDir, final License license, final URL licenseUrl) throws IOException {
		String licenseFileName = "about_files/" + sanitizeFileName(license.getName()).toUpperCase();
		final String existingLicense = findExistingLicenseFile(licenseOutputDir, licenseFileName);
		if (existingLicense != null) {
			if (!forceDownload) {
				getLog().info(format("Found existing license file at '%s'. %s", existingLicense, REQUIRES_FORCE_DOWNLOAD_MESSAGE));
				return existingLicense;
			} else if (getMavenSession().isOffline()) {
				getLog().warn(format("Re-using existing license file at '%s'. Maven is offline.", existingLicense));
				return existingLicense;
			}
		} else if (getMavenSession().isOffline())
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

	private String findExistingLicenseFile(final File licenseOutputDir, final String licenseFileName) {
		for (final String extension : Arrays.asList(".txt", ".html")) {
			if (new File(licenseOutputDir, licenseFileName + extension).isFile())
				return licenseFileName + extension;
		}
		return null;
	}

	public void generateAboutHtmlFile(final SortedMap<Artifact, Model> dependencies, final File outputDirectory) throws MojoExecutionException {
		final File aboutHtmlFile = new File(outputDirectory, ABOUT_HTML);
		if (aboutHtmlFile.isFile() && !isForce()) {
			getLog().warn(format("Found existing about.html file at '%s'. %s", aboutHtmlFile, REQUIRES_FORCE_TO_OVERRIDE_MESSAGE));
			return;
		}

		String aboutHtmlText = readAboutHtmlTemplate();
		aboutHtmlText = StringUtils.replaceEach(aboutHtmlText, new String[] { // @formatter:off
				"@DATE@",
				"@THIRD_PARTY_INFO@"
			}, new String[] {
				DateFormat.getDateInstance(DateFormat.LONG, Locale.US).format(new Date()),
				getThirdPartyInfo(dependencies, outputDirectory) }
		);
		// @formatter:on

		try {
			FileUtils.writeStringToFile(aboutHtmlFile, aboutHtmlText, UTF_8);
		} catch (final IOException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to write about.html file '%s'. %s", aboutHtmlFile, e.getMessage()));
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

	private String getLicenseInfo(final Artifact resolvedPomArtifact, final Model artifactPom, final File resourcesDir) throws MojoExecutionException {
		final StrBuilder licenseInfo = new StrBuilder();
		final KnownLicense knownLicense = getLicense(resolvedPomArtifact);
		List<License> licenses = artifactPom.getLicenses();

		if (getLog().isDebugEnabled()) {
			getLog().debug("Collecting license information...");
			getLog().debug("  known license: " + knownLicense);
			if ((licenses != null) && !licenses.isEmpty()) {
				for (final License license : licenses) {
					getLog().debug(format("    pom license: %s (%s)", license.getName(), license.getUrl()));
				}
			} else {
				getLog().debug("    pom license: none");
			}
		}

		if ((knownLicense != null) && isDualOrMoreLicensed(licenses)) {
			getLog().debug("Detected dual license ... electing to use package under: " + knownLicense);
			licenseInfo.append("Though this package is dually licensed, the Eclipse Foundation elects to use the package under the ");
			appendAndDownloadLicenseInfo(licenseInfo, resourcesDir, resolvedPomArtifact, Arrays.asList(knownLicense.toMavenLicense()));
			licenseInfo.append(" license.");
		} else if ((knownLicense != null) || !licenses.isEmpty()) {
			if (knownLicense != null) {
				getLog().debug("Overruling pom license with known license " + knownLicense);
				licenses = Arrays.asList(knownLicense.toMavenLicense());
			}
			licenseInfo.append(escapeHtml4(artifactPom.getName())).append(" is provided to you under the terms and conditions of the ");
			appendAndDownloadLicenseInfo(licenseInfo, resourcesDir, resolvedPomArtifact, licenses);
			if (licenses.size() == 1) {
				licenseInfo.append(" license.");
			} else {
				licenseInfo.append(" licenses.");
			}
		} else {
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
		}
		return licenseInfo.toString();
	}

	private String getLocalLicenseFile(final File licenseOutputDir, final License license) throws MojoExecutionException {
		final String localLicenseFile = getLicenseFile(license.getName());
		if (localLicenseFile == null)
			return null; // no local license configures

		final String licenseFileName = "about_files/" + localLicenseFile;
		final File licenseFile = new File(licenseOutputDir, licenseFileName);
		getLog().debug(format("Searching for existing local license file '%s' at '%s'.", licenseFileName, licenseFile));
		if (!licenseFile.isFile())
			throw new MojoExecutionException(format("Local license file '%s' configured for license '%s' not found at '%s'.", localLicenseFile, license.getName(), licenseFileName));

		return licenseFileName;
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

	private String getThirdPartyInfo(final SortedMap<Artifact, Model> dependencies, final File outputDirectory) throws MojoExecutionException {
		final StrBuilder thirdPartyInfoText = new StrBuilder();

		for (final Entry<Artifact, Model> entry : dependencies.entrySet()) {
			String thirdPartyInfo = readThirdPartyHtmlTemplate();
			final Artifact artifact = entry.getKey();
			final Model artifactPom = entry.getValue();
			thirdPartyInfo = StringUtils.replaceEach(thirdPartyInfo, new String[] { // @formatter:off
					"@DEPENDENCY_HEADLINE@",
					"@DEPENDENCY_BY@",
					"@DEPENDENCY_NAME@",
					"@DEPENDENCY_LICENSING@",
					"@DEPENDENCY_ORIGIN@"
				}, new String[] {
					escapeHtml4(artifactPom.getName()),
					getDevelopedByInfo(artifact, artifactPom),
					escapeHtml4(artifactPom.getName()),
					getLicenseInfo(artifact, artifactPom, outputDirectory),
					getOriginInfo(artifact, artifactPom)
			});
			// @formatter:on

			thirdPartyInfoText.append(thirdPartyInfo);
		}

		return thirdPartyInfoText.toString();
	}

	private String readAboutHtmlTemplate() throws MojoExecutionException {
		try {
			return IOUtils.toString(getTemplate("recipe-about.html"), UTF_8);
		} catch (final Exception e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Error reading about.html template: %s", e.getMessage()));
		}
	}

	private String readThirdPartyHtmlTemplate() throws MojoExecutionException {
		try {
			return IOUtils.toString(getTemplate("recipe-about-3rdparty.html"), UTF_8);
		} catch (final Exception e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Error reading 3rd party info template: %s", e.getMessage()));
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
}
