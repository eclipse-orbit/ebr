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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.eclipse.ebr.maven.eclipseip.KnownLicense;
import org.eclipse.ebr.maven.eclipseip.KnownLicenses;

import org.osgi.framework.Version;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.google.common.base.Strings;

/**
 * A utility for generating an Eclipse IP log.
 */
public class EclipseIpLogUtil extends LicenseProcessingUtility {

	private static final String PORTAL_PHP = "https://dev.eclipse.org/portal/myfoundation/portal/portal.php";
	private static final String DISPATCH_PHP = "https://dev.eclipse.org/portal/myfoundation/portal/dispatch.php";

	private static final String IP_LOG_XML = "ip_log.xml";
	private Server server;
	private String projectId;
	private String cqCryptography;
	private File outputDirectory;

	public EclipseIpLogUtil(final Log log, final MavenSession mavenSession, final Settings settings, final boolean force) {
		super(log, mavenSession, force);
	}

	private void appenLegalInfo(final Xpp3Dom project, final Artifact artifact, final Model artifactPom, final Map<String, String> existingCqs, final Map<String, Xpp3Dom> existingLicenses) {
		final Xpp3Dom legal = new Xpp3Dom("legal");
		project.addChild(legal);

		final Xpp3Dom ipzilla = new Xpp3Dom("ipzilla");
		final String artifactFileName = artifact.getFile().getName();
		ipzilla.setAttribute("bug_id", Strings.nullToEmpty(existingCqs.get(artifactFileName)));
		legal.addChild(ipzilla);

		final Xpp3Dom license = new Xpp3Dom("license");
		legal.addChild(license);

		final KnownLicense knownLicense = getLicense(artifact, artifactPom, existingLicenses);
		if (knownLicense != null) {
			createChild(license, "name", knownLicense.getName());
			createChild(license, "reference", Strings.nullToEmpty(knownLicense.getUrl()));
		} else {
			getLog().warn(format("No licensing information found for artifact %s:%s:%s. Please fill in information in ip_log.xml manually!", artifactPom.getGroupId(), artifactPom.getArtifactId(), artifactPom.getVersion()));
		}

		createChild(legal, "package", artifactFileName);
	}

	private void collectExistingCqsAndLicenses(final Xpp3Dom existingIpLog, final Map<String, String> existingCqs, final Map<String, Xpp3Dom> existingLicenses) {
		final Xpp3Dom[] projects = existingIpLog.getChildren("project");
		if (projects != null) {
			for (final Xpp3Dom project : projects) {
				final Xpp3Dom[] legals = project.getChildren("legal");
				if (legals != null) {
					for (final Xpp3Dom legal : legals) {
						final String cqId = getCqId(legal);
						final String artifactFileName = getPackageValue(legal);
						if ((artifactFileName != null) && (cqId != null)) {
							existingCqs.put(artifactFileName, cqId);
						}
						final Xpp3Dom license = legal.getChild("license");
						if ((artifactFileName != null) && (license != null)) {
							existingLicenses.put(artifactFileName, license);
						}
					}
				}
			}
		}
	}

	private void createChild(final Xpp3Dom parent, final String name, final String value) {
		if (value == null)
			return; // no element on null value

		final Xpp3Dom element = new Xpp3Dom(name);
		element.setValue(value);
		parent.addChild(element);
	}

	private String createCq(final CloseableHttpClient httpclient, final Artifact artifact, final Model artifactPom, final Map<String, Xpp3Dom> existingLicenses) throws URISyntaxException, MojoExecutionException, IOException {
		final URIBuilder postUri = new URIBuilder(DISPATCH_PHP);
		postUri.addParameter("id", "portal/contribution_questionnaire.contribution_questionnaire_reuse." + projectId + "!reuse");
		postUri.addParameter("action", "submit");

		final HttpPost httpPost = new HttpPost(postUri.build());
		httpPost.addHeader("Referer", PORTAL_PHP);
		final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("name", getCqName(artifact, artifactPom)));
		nvps.add(new BasicNameValuePair("version", artifact.getVersion()));
		nvps.add(new BasicNameValuePair("description", getCqDescription(artifact, artifactPom)));
		nvps.add(new BasicNameValuePair("cryptography", Strings.nullToEmpty(getCqCryptography(artifact, artifactPom))));
		nvps.add(new BasicNameValuePair("projecturl", Strings.nullToEmpty(getCqProjectUrl(artifact, artifactPom))));
		nvps.add(new BasicNameValuePair("sourceurl", Strings.nullToEmpty(getCqSourceUrl(artifact, artifactPom))));
		nvps.add(new BasicNameValuePair("license", getCqLicense(artifact, artifactPom, existingLicenses)));
		nvps.add(new BasicNameValuePair("otherlicense", ""));
		nvps.add(new BasicNameValuePair("sourcebinary", "sourceandbinary"));
		nvps.add(new BasicNameValuePair("modifiedcode", "unmodified"));
		nvps.add(new BasicNameValuePair("apachehosted", getCqApacheHosted(artifact, artifactPom)));
		nvps.add(new BasicNameValuePair("codeagreement", getCqCodeAgreement(artifact, artifactPom)));
		nvps.add(new BasicNameValuePair("stateid", "not_existing"));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));

		if (getLog().isDebugEnabled()) {
			for (final NameValuePair pair : nvps) {
				getLog().debug("   " + pair.getName() + "=" + pair.getValue());
			}
		}

		final String responseHtml = executeRequest(httpclient, httpPost, StringUtils.removeEnd(artifact.getFile().getName(), ".jar") + "-ipzilla-response.html");

		final String cqUrl = "https://dev.eclipse.org/ipzilla/show_bug.cgi?id=";
		final int cqUrlIndex = responseHtml.indexOf(cqUrl);
		final StrBuilder cqId = new StrBuilder();
		for (int i = cqUrlIndex + cqUrl.length(); i < responseHtml.length(); i++) {
			final char c = responseHtml.charAt(i);
			if (Character.isDigit(c)) {
				cqId.append(c);
			} else {
				break;
			}
		}

		try {
			final int cqNumber = Integer.parseInt(cqId.toString());
			if (cqNumber > 0)
				return String.valueOf(cqNumber);
		} catch (final NumberFormatException e) {
			getLog().error(format("Error parsing extracted CQ number '%s'. %s", cqId, e.getMessage()), e);
		}

		// we can only fail at this point
		throw new MojoExecutionException("Unable to extract CQ number from response. Please check response and IPzilla!");
	}

	public void enableSubmissionOfCqs(final String projectId, final String cqCryptography, final Settings settings, final SettingsDecrypter settingsDecrypter, final File outputDirectory) throws MojoExecutionException {
		getLog().debug("Enabling automatic submission of CQs to portal.eclipse.org");

		if (projectId.toLowerCase().equals("tools.orbit"))
			throw new MojoExecutionException("It's not allowed to submit 3rd party CQs directly to Orbit. Please submit it to any other project and create an ATO ('Add To Orbit') CQ using the portal.");

		if (Strings.isNullOrEmpty(cqCryptography))
			throw new MojoExecutionException("Please specify property 'cqCryptography' indicating whether this library implements/distributes cryptography and explain what.");

		server = getServer("portal.eclipse.org", settings, settingsDecrypter);
		if (server == null)
			throw new MojoExecutionException("Unable to enable automatic CQ submission. Please configure a server with id 'portal.eclipse.org' in the Maven settings and specify username and password.");

		this.projectId = projectId;
		this.cqCryptography = cqCryptography;
		this.outputDirectory = outputDirectory;
	}

	private String executeRequest(final CloseableHttpClient httpclient, final HttpPost httpPost, final String responseHtmlFileName) throws IOException, ClientProtocolException, MojoExecutionException {
		getLog().debug(" >" + httpPost.getURI());
		final CloseableHttpResponse response = httpclient.execute(httpPost);
		try {
			getLog().debug(" <" + response.getStatusLine());
			final HttpEntity entity = response.getEntity();
			final String responseHtml = EntityUtils.toString(entity);
			EntityUtils.consume(entity);

			try {
				final File responseHtmlFile = new File(outputDirectory, responseHtmlFileName);
				FileUtils.writeStringToFile(responseHtmlFile, responseHtml, UTF_8);
				getLog().debug(format("  (response written to '%s')", responseHtmlFile.getAbsolutePath()));
			} catch (final Exception e) {
				if (getLog().isDebugEnabled()) {
					getLog().debug("---------------------------------");
					getLog().debug(responseHtml);
					getLog().debug("---------------------------------");
				}
			}

			if (response.getStatusLine().getStatusCode() != 200)
				throw new MojoExecutionException("Eclipse Portal returned: " + response.getStatusLine());

			return responseHtml;
		} finally {
			response.close();
		}
	}

	public File generateIpLogXmlFile(final Model recipePom, final SortedMap<Artifact, Model> dependencies, final File outputDirectory) throws MojoExecutionException {
		final File iplogXmlFile = new File(outputDirectory, IP_LOG_XML);
		if (iplogXmlFile.isFile() && !isForce()) {
			getLog().warn(format("Found existing ip_log.xml file at '%s'. %s", iplogXmlFile, REQUIRES_FORCE_TO_OVERRIDE_MESSAGE));
			return iplogXmlFile;
		}

		// read existing ip log if available
		final Xpp3Dom existingIpLog = readExistingIpLog(iplogXmlFile);

		// collect existing CQs and licenses
		final Map<String, String> existingCqs = new HashMap<>();
		final Map<String, Xpp3Dom> existingLicenses = new HashMap<>();
		if (existingIpLog != null) {
			collectExistingCqsAndLicenses(existingIpLog, existingCqs, existingLicenses);
		}

		// log/file missing CQs
		logOrCreateMissingCqs(dependencies, existingCqs, existingLicenses);

		// generate new ip log
		final Xpp3Dom ipLogXmlDom = getIpLogXml(recipePom, dependencies, existingIpLog, existingCqs, existingLicenses);

		// save
		try {
			FileUtils.writeStringToFile(iplogXmlFile, ipLogXmlDom.toString(), UTF_8);
		} catch (final IOException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to write ip_log.xml file '%s'. %s", iplogXmlFile, e.getMessage()));
		}

		return iplogXmlFile;
	}

	private Xpp3Dom[] getContact(final Xpp3Dom existingIpLog) {
		if (existingIpLog == null)
			return null;
		final Xpp3Dom project = existingIpLog.getChild("project");
		if (project != null)
			return project.getChildren("contact");
		return null;
	}

	private String getCqApacheHosted(final Artifact artifact, final Model artifactPom) {
		final String url = artifactPom.getUrl();
		if ((url != null) && (url.toLowerCase().indexOf("apache.org") > -1))
			return url;
		return "No";
	}

	private String getCqCodeAgreement(final Artifact artifact, final Model artifactPom) {
		if (artifactPom.getOrganization() != null) {
			if (null != artifactPom.getOrganization().getName())
				return artifactPom.getOrganization().getName();
			if (null != artifactPom.getOrganization().getUrl())
				return artifactPom.getOrganization().getUrl();
		}
		return "Unknown";
	}

	private String getCqCryptography(final Artifact artifact, final Model artifactPom) {
		return cqCryptography;
	}

	private String getCqDescription(final Artifact artifact, final Model artifactPom) {
		final StrBuilder description = new StrBuilder();
		if (null != artifactPom.getDescription()) {
			description.append(artifactPom.getDescription()).appendNewLine().appendNewLine();
		}

		description.append("Maven Information").appendNewLine();
		description.append("-----------------").appendNewLine();
		description.append("     Group Id: ").append(artifact.getGroupId()).appendNewLine();
		description.append("  Artifact Id: ").append(artifact.getArtifactId()).appendNewLine();
		description.append("      Version: ").append(artifact.getVersion()).appendNewLine();
		description.append("         File: ").append(artifact.getFile().getName()).appendNewLine();

		if (artifactPom.getOrganization() != null) {
			description.append(" Organisation: ");
			if (null != artifactPom.getOrganization().getName()) {
				description.append(artifactPom.getOrganization().getName()).append(" ");
			}
			if (null != artifactPom.getOrganization().getUrl()) {
				description.append(artifactPom.getOrganization().getUrl());
			}
			description.appendNewLine();
		}

		final List<MailingList> mailingLists = artifactPom.getMailingLists();
		if (mailingLists != null) {
			for (final MailingList mailingList : mailingLists) {
				description.append(" Mailing List: ").append(mailingList.getName()).append(" ").append(mailingList.getPost()).appendNewLine();
			}
		}

		return description.toString();
	}

	private String getCqId(final Xpp3Dom legal) {
		final Xpp3Dom ipzilla = legal.getChild("ipzilla");
		if (ipzilla != null)
			return ipzilla.getAttribute("bug_id");
		return null;
	}

	private String getCqLicense(final Artifact artifact, final Model artifactPom, final Map<String, Xpp3Dom> existingLicenses) throws MojoExecutionException {
		final KnownLicense license = getLicense(artifact, artifactPom, existingLicenses);
		if (license == null) {
			getLog().error(format("No known license configured for artifact %s:%s:%s.", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
			logKnownLicenses();
			throw new MojoExecutionException(format("Please configure a known license for artifact %s.", artifact.getArtifactId()));
		}
		return license.getName();
	}

	private String getCqName(final Artifact artifact, final Model artifactPom) {
		return format("%s (%s)", artifactPom.getName(), artifact.getFile().getName());
	}

	private String getCqProjectUrl(final Artifact artifact, final Model artifactPom) {
		return artifactPom.getUrl();
	}

	private String getCqSourceUrl(final Artifact artifact, final Model artifactPom) {
		final Scm scm = artifactPom.getScm();
		if (scm != null)
			return scm.getUrl();
		return null;
	}

	private Xpp3Dom getIpLogXml(final Model recipePom, final SortedMap<Artifact, Model> dependencies, final Xpp3Dom existingIpLog, final Map<String, String> existingCqs, final Map<String, Xpp3Dom> existingLicenses) {
		final Xpp3Dom ipLog = new Xpp3Dom("ip_log");
		ipLog.setAttribute("version", "1.0");

		final Xpp3Dom project = new Xpp3Dom("project");
		project.setAttribute("id", recipePom.getArtifactId());
		project.setAttribute("version", getProjectVersion(recipePom, existingIpLog));
		project.setAttribute("status", "done");
		ipLog.addChild(project);

		final Xpp3Dom info = new Xpp3Dom("info");
		createChild(info, "name", getProjectName(recipePom, existingIpLog));
		createChild(info, "origin", getProjectOrigin(recipePom, existingIpLog));
		createChild(info, "reference", getProjectReference(recipePom, existingIpLog));
		createChild(info, "repository", Strings.nullToEmpty(getProjectRepository(recipePom, existingIpLog)));
		createChild(info, "location", Strings.nullToEmpty(getProjectLocation(recipePom, existingIpLog)));
		createChild(info, "tag", getProjectTag(recipePom, existingIpLog));
		project.addChild(info);

		final Xpp3Dom[] existingContacts = getContact(existingIpLog);
		if ((existingContacts != null) && (existingContacts.length > 0)) {
			for (final Xpp3Dom contact : existingContacts) {
				project.addChild(contact);
			}
		} else {
			final Xpp3Dom contact = new Xpp3Dom("contact");
			createChild(contact, "name", "");
			createChild(contact, "email", "");
			createChild(contact, "company", "");
			project.addChild(contact);
		}

		final Xpp3Dom existingNotes = getNotes(existingIpLog);
		if (existingNotes != null) {
			project.addChild(existingNotes);
		}

		for (final Entry<Artifact, Model> dependency : dependencies.entrySet()) {
			appenLegalInfo(project, dependency.getKey(), dependency.getValue(), existingCqs, existingLicenses);
		}

		return ipLog;
	}

	private KnownLicense getLicense(final Artifact artifact, final Model artifactPom, final Map<String, Xpp3Dom> existingLicenses) {
		KnownLicense knownLicense = getLicense(artifact);
		if (knownLicense != null) {
			getLog().debug(format("Found configures license '%s' for artifact %s.", knownLicense.getName(), artifact.getArtifactId()));
			return knownLicense;
		}

		final Xpp3Dom existingLicense = existingLicenses.get(artifact.getFile().getName());
		if (existingLicense != null) {
			if (null != existingLicense.getChild("name")) {
				knownLicense = KnownLicenses.getInstance().getByName(existingLicense.getChild("name").getValue());
				if (knownLicense != null) {
					getLog().debug(format("Found exact license '%s' match based on existing ip_log.xml for artifact %s.", knownLicense.getName(), artifact.getArtifactId()));
					return knownLicense;
				}
			}
		}

		final List<License> licenses = artifactPom.getLicenses();
		if ((licenses != null) && !licenses.isEmpty()) {
			for (final License license : licenses) {
				knownLicense = getSimilarLicense(license);
				if (knownLicense != null) {
					getLog().warn(format("Detected '%s' for artifact license '%s (%s)'. Please verify the license correctness and consider configuring a static mapping for reproducible results.", knownLicense.getName(), license.getName(), license.getUrl()));
					return knownLicense;
				}
				getLog().debug(format("Found no license similar to '%s (%s)' for artifact %s.", license.getName(), license.getUrl(), artifact.getArtifactId()));
			}
		}

		return null;
	}

	public String getLicenseNameFromIpLogXmlFile(final File outputDirectory) throws MojoExecutionException {
		final File iplogXmlFile = new File(outputDirectory, IP_LOG_XML);

		// read existing ip log
		final Xpp3Dom existingIpLog = readExistingIpLog(iplogXmlFile);
		if ((existingIpLog == null) || !iplogXmlFile.isFile()) {
			getLog().debug(format("Unable to read license info: No ip_log.xml file found at '%s'", iplogXmlFile));
			return null;
		}

		// ensure there is at most one project
		final Xpp3Dom[] projects = existingIpLog.getChildren("project");
		if ((projects == null) || (projects.length == 0)) {
			getLog().debug("Unable to read license info: Missing project information in ip_log.xml");
			return null;
		} else if (projects.length != 1) {
			getLog().debug("Unable to read license info: Too many 'project' elements. Only one 'project' element is expected in the ip_log.xml.");
			return null;
		}

		// walk through legal info to extract license name
		final Xpp3Dom[] legals = projects[0].getChildren("legal");
		if ((legals == null) || (legals.length == 0)) {
			getLog().debug("Unable to read license info: Missing legal information in ip_log.xml");
			return null;
		} else if (legals.length != 1) {
			getLog().debug("Unable to read license info: Too many 'legal' elements. Only one 'legal' element is expected in the ip_log.xml.");
			return null;
		}
		final Xpp3Dom[] licenses = legals[0].getChildren("license");
		if ((licenses == null) || (licenses.length == 0)) {
			getLog().debug("Unable to read license info: Incomplete legal information in ip_log.xml. Element 'license' with license information is required!");
			return null;
		} else if (licenses.length != 1) {
			getLog().debug("Unable to read license info: Too many 'license' elements. Only one 'license' element is expected in the ip_log.xml.");
			return null;
		}
		final Xpp3Dom name = licenses[0].getChild("name");
		if ((name == null) || Strings.isNullOrEmpty(name.getValue())) {
			getLog().debug("Unable to read license info: Incomplete license information in ip_log.xml. Element 'name' is required and must not be empty!");
			return null;
		}

		return name.getValue();
	}

	private Xpp3Dom getNotes(final Xpp3Dom existingIpLog) {
		if (existingIpLog == null)
			return null;
		final Xpp3Dom project = existingIpLog.getChild("project");
		if (project != null)
			return project.getChild("notes");
		return null;
	}

	private String getPackageValue(final Xpp3Dom legal) {
		final Xpp3Dom legalPackage = legal.getChild("package");
		if (legalPackage != null)
			return legalPackage.getValue();
		return null;
	}

	private String getProjectAttribute(final Xpp3Dom existingIpLog, final String name) {
		if (existingIpLog == null)
			return null;
		final Xpp3Dom project = existingIpLog.getChild("project");
		if (project != null)
			return project.getAttribute(name);

		return null;
	}

	private String getProjectInfo(final Xpp3Dom existingIpLog, final String name) {
		if (existingIpLog == null)
			return null;
		final Xpp3Dom project = existingIpLog.getChild("project");
		if (project != null) {

			final Xpp3Dom info = project.getChild("info");
			if (info != null) {
				final Xpp3Dom child = info.getChild(name);
				if (child != null)
					return child.getValue();
			}
		}
		return null;
	}

	private String getProjectLocation(final Model recipePom, final Xpp3Dom existingIpLog) {
		final String existingValue = getProjectInfo(existingIpLog, "location");
		if (existingValue != null)
			return existingValue;

		final Scm scm = recipePom.getScm();
		if (scm != null) {
			final String url = getScmUrl(scm);

			// by definition, we return everything after to the last ".git"
			if (url != null) {
				final int lastIndexOf = StringUtils.lastIndexOf(url, ".git");
				if (lastIndexOf >= 0)
					return StringUtils.removeStart(url.substring(lastIndexOf + 4), "/");
				return url;
			}
		}

		return null;
	}

	private String getProjectName(final Model recipePom, final Xpp3Dom existingIpLog) {
		final String existingValue = getProjectInfo(existingIpLog, "name");
		if (existingValue != null)
			return existingValue;

		return recipePom.getName();
	}

	private String getProjectOrigin(final Model recipePom, final Xpp3Dom existingIpLog) {
		final String existingValue = getProjectInfo(existingIpLog, "origin");
		if (existingValue != null)
			return existingValue;

		final StrBuilder developedByInfo = new StrBuilder();

		// prefer organization if available
		if (null != recipePom.getOrganization()) {
			if (StringUtils.isNotBlank(recipePom.getOrganization().getName())) {
				developedByInfo.append(escapeHtml4(recipePom.getOrganization().getName()));
			} else if (StringUtils.isNotBlank(recipePom.getOrganization().getUrl())) {
				developedByInfo.append(escapeHtml4(removeWebProtocols(recipePom.getOrganization().getUrl())));
			}
		}

		// use to developers if no organization is available
		if (developedByInfo.isEmpty()) {
			if (!recipePom.getDevelopers().isEmpty()) {
				appendDeveloperInfo(developedByInfo, recipePom);
			}
		}

		// don't return anything if nothing is available
		if (developedByInfo.isEmpty())
			return null;

		return developedByInfo.toString();
	}

	private String getProjectReference(final Model recipePom, final Xpp3Dom existingIpLog) {
		final String existingValue = getProjectInfo(existingIpLog, "reference");
		if (existingValue != null)
			return existingValue;

		return recipePom.getUrl();
	}

	private String getProjectRepository(final Model recipePom, final Xpp3Dom existingIpLog) {
		final String existingValue = getProjectInfo(existingIpLog, "repository");
		if (existingValue != null)
			return existingValue;

		final Scm scm = recipePom.getScm();
		if (scm != null) {
			final String url = getScmUrl(scm);

			// by definition, we return everything up to the last ".git"
			if (url != null) {
				final int lastIndexOf = StringUtils.lastIndexOf(url, ".git");
				if (lastIndexOf >= 0)
					return url.substring(0, lastIndexOf + 4);
				return url;
			}
		}

		return null;
	}

	private String getProjectTag(final Model recipePom, final Xpp3Dom existingIpLog) {
		return getProjectInfo(existingIpLog, "tag");
	}

	private String getProjectVersion(final Model recipePom, final Xpp3Dom existingIpLog) {
		final String existingValue = getProjectAttribute(existingIpLog, "version");
		if (existingValue != null)
			return existingValue;

		final Version version = Version.parseVersion(StringUtils.removeEnd(recipePom.getVersion(), "-SNAPSHOT"));
		return format("%d.%d.%d", version.getMajor(), version.getMinor(), version.getMicro());
	}

	private String getScmUrl(final Scm scm) {
		String url = scm.getDeveloperConnection();
		if (null == url) {
			url = scm.getConnection();
		}
		if (null == url) {
			url = scm.getUrl();
		}
		return url;
	}

	private Server getServer(final String serverId, final Settings settings, final SettingsDecrypter settingsDecrypter) {
		for (Server server : settings.getServers()) {
			if (StringUtils.equals(server.getId(), serverId)) {
				final SettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(server);
				final SettingsDecryptionResult result = settingsDecrypter.decrypt(request);
				server = result.getServer();

				// log any detected problems
				for (final SettingsProblem problem : result.getProblems()) {
					getLog().warn(problem.getMessage(), problem.getException());
				}

				return server;
			}
		}

		return null;
	}

	private void loginToPortal(final CloseableHttpClient httpclient, final Server server) throws IOException, MojoExecutionException, URISyntaxException {
		final URIBuilder postUri = new URIBuilder(PORTAL_PHP);

		final HttpPost httpPost = new HttpPost(postUri.build());
		final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("user", server.getUsername()));
		nvps.add(new BasicNameValuePair("password", server.getPassword()));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));

		executeRequest(httpclient, httpPost, "portal-login-response.html");
	}

	private void logOrCreateMissingCqs(final SortedMap<Artifact, Model> dependencies, final Map<String, String> existingCqs, final Map<String, Xpp3Dom> existingLicenses) throws MojoExecutionException {
		CloseableHttpClient httpclient = null;
		try {

			if ((server != null) && (projectId != null)) {
				httpclient = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
				loginToPortal(httpclient, server);
			}

			for (final Entry<Artifact, Model> dependency : dependencies.entrySet()) {
				final Artifact artifact = dependency.getKey();
				final String existingCq = existingCqs.get(artifact.getFile().getName());
				if ((null == existingCq) || existingCq.trim().isEmpty()) {
					if (httpclient != null) {
						getLog().info(format("Creating CQ for artifact %s:%s:%s.", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
						final String cqId = createCq(httpclient, artifact, dependency.getValue(), existingLicenses);
						existingCqs.put(artifact.getFile().getName(), cqId);
						getLog().info(format("Created CQ %s for artifact %s:%s:%s.", cqId, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
					} else {
						getLog().warn(format("Missing CQ for artifact %s:%s:%s. Please visit portal.eclipse.org and file a CQ with IPzilla!", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
					}
				}
			}
		} catch (final IOException | URISyntaxException e) {
			getLog().debug(e);
			throw new MojoExecutionException("An error occured communicating with the Eclipse Portal: " + e.getMessage());
		} finally {
			if (httpclient != null) {
				try {
					httpclient.close();
				} catch (final IOException e) {
					getLog().debug("Ignored exception during close.", e);
				}
			}
		}
	}

	private void logWarningOrFailBuild(final boolean failBuildIfIpLogIsIncomplete, final String messageFormat, final Object... arguments) throws MojoFailureException {
		if (failBuildIfIpLogIsIncomplete)
			throw new MojoFailureException(format(messageFormat, arguments));
		getLog().warn(format(messageFormat, arguments));
	}

	private Xpp3Dom readExistingIpLog(final File iplogXmlFile) throws MojoExecutionException {
		final Xpp3Dom existingIpLog;
		if (iplogXmlFile.isFile()) {
			try (InputStream is = FileUtils.openInputStream(iplogXmlFile)) {
				existingIpLog = Xpp3DomBuilder.build(is, UTF_8);
			} catch (IOException | XmlPullParserException e) {
				getLog().debug(e);
				throw new MojoExecutionException(format("Unable to read ip_log.xml file '%s'. %s", iplogXmlFile, e.getMessage()));
			}
		} else {
			existingIpLog = null;
		}
		return existingIpLog;
	}

	public void verifyIpLogXmlFile(final File outputDirectory, final boolean failIfIpLogIsIncomplete) throws MojoFailureException, MojoExecutionException {
		final File iplogXmlFile = new File(outputDirectory, IP_LOG_XML);

		// read existing ip log
		final Xpp3Dom existingIpLog = readExistingIpLog(iplogXmlFile);
		if ((existingIpLog == null) || !iplogXmlFile.isFile()) {
			logWarningOrFailBuild(failIfIpLogIsIncomplete, "Verification failed: No ip_log.xml file found at '%s'", iplogXmlFile);
		}

		// ensure there is at most one project
		final Xpp3Dom[] projects = existingIpLog.getChildren("project");
		if ((projects == null) || (projects.length == 0)) {
			logWarningOrFailBuild(failIfIpLogIsIncomplete, "Missing project information in ip_log.xml.");
		} else if (projects.length != 1) {
			logWarningOrFailBuild(failIfIpLogIsIncomplete, "Too many 'project' elements. Only one 'project' element is expected in the ip_log.xml.");
		}

		// expect contact information
		final Xpp3Dom[] contacts = getContact(existingIpLog);
		if ((contacts == null) || (contacts.length == 0)) {
			logWarningOrFailBuild(failIfIpLogIsIncomplete, "Missing contact information in ip_log.xml.");
		} else {
			for (final Xpp3Dom contact : contacts) {
				final Xpp3Dom name = contact.getChild("name");
				if ((name == null) || Strings.isNullOrEmpty(name.getValue())) {
					logWarningOrFailBuild(failIfIpLogIsIncomplete, "Incomplete contact information in ip_log.xml. Element 'name' is required and must not be empty!");
				}
				final Xpp3Dom email = contact.getChild("email");
				if ((email == null) || Strings.isNullOrEmpty(email.getValue())) {
					logWarningOrFailBuild(failIfIpLogIsIncomplete, "Incomplete contact information in ip_log.xml. Element 'email' is required and must not be empty!");
				}
			}
		}

		// verify legal info
		for (final Xpp3Dom project : projects) {
			final Xpp3Dom[] legals = project.getChildren("legal");
			if ((legals == null) || (legals.length == 0)) {
				logWarningOrFailBuild(failIfIpLogIsIncomplete, "Missing legal information in ip_log.xml.");
			} else {
				for (final Xpp3Dom legal : legals) {
					final String cqId = getCqId(legal);
					if (Strings.isNullOrEmpty(cqId)) {
						logWarningOrFailBuild(failIfIpLogIsIncomplete, "Incomplete legal information in ip_log.xml. Reference to IPzilla CQ is required!");
					}
					final Xpp3Dom[] licenses = legal.getChildren("license");
					if ((licenses == null) || (licenses.length == 0)) {
						logWarningOrFailBuild(failIfIpLogIsIncomplete, "Incomplete legal information in ip_log.xml. Element 'license' with license information is required!");
					} else {
						for (final Xpp3Dom license : licenses) {
							final Xpp3Dom name = license.getChild("name");
							if ((name == null) || Strings.isNullOrEmpty(name.getValue())) {
								logWarningOrFailBuild(failIfIpLogIsIncomplete, "Incomplete license information in ip_log.xml. Element 'name' is required and must not be empty!");
							}
							final Xpp3Dom reference = license.getChild("reference");
							if ((reference == null) || Strings.isNullOrEmpty(reference.getValue())) {
								logWarningOrFailBuild(failIfIpLogIsIncomplete, "Incomplete license information in ip_log.xml. Element 'reference' is required and must not be empty!");
							}
						}
					}
				}
			}
		}
	}
}
