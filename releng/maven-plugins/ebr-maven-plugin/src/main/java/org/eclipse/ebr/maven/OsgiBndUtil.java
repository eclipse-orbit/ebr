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
import static org.eclipse.ebr.maven.TemplateHelper.getTemplate;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.ebr.maven.shared.BaseUtility;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * A utility for working with osgi.bnd.
 */
public class OsgiBndUtil extends BaseUtility {

	private static final String OSGI_BND = "osgi.bnd";

	public OsgiBndUtil(final Log log, final MavenSession mavenSession, final boolean force) {
		super(log, mavenSession);
		setForce(force);
	}

	public void generateOsgiBndFile(final File outputDirectory, final Collection<Dependency> compileTimeDependencies) throws MojoExecutionException {
		final File osgiBndFile = new File(outputDirectory, OSGI_BND);
		if (osgiBndFile.isFile() && !isForce()) {
			getLog().warn(format("Found existing osgi.bnd file at '%s'. %s", osgiBndFile, REQUIRES_FORCE_TO_OVERRIDE_MESSAGE));
			return;
		}

		String osgiBndText = readOsgiBndTemplate();
		osgiBndText = StringUtils.replaceEach(osgiBndText, new String[] { // @formatter:off
				"@VERSION_VARIABLES@" }, new String[] { getVersionDeclarations(compileTimeDependencies) });
		// @formatter:on

		try {
			FileUtils.writeStringToFile(osgiBndFile, osgiBndText, UTF_8);
		} catch (final IOException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to write osgi.bnd file '%s'. %s", osgiBndFile, e.getMessage()));
		}
	}

	private String getVersionDeclarations(final Collection<Dependency> dependencies) {
		final StrBuilder declarations = new StrBuilder();
		declarations.append("package-version=${version;===;${Bundle-Version}}");
		for (final Dependency dependency : dependencies) {
			declarations.appendNewLine();
			declarations.append(format("%s=${version;===;%s}", getVersionVariableName(dependency), dependency.getVersion()));
		}
		return declarations.toString();
	}

	private String getVersionVariableName(final Dependency dependency) {
		return format("%s-version", dependency.getArtifactId());
	}

	private String readOsgiBndTemplate() throws MojoExecutionException {
		try {
			return IOUtils.toString(getTemplate("recipe-osgi.bnd"), UTF_8);
		} catch (final Exception e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Error reading osgi.bnd template: %s", e.getMessage()));
		}
	}
}
