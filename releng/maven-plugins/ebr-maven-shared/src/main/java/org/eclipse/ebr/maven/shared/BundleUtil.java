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
package org.eclipse.ebr.maven.shared;

import org.osgi.framework.Version;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * A utility for OSGi meta data.
 */
public class BundleUtil extends BaseUtility {

	public static String getBundleSymbolicName(final MavenProject project) {
		return project.getArtifactId(); // Tycho convention
	}

	public static String getBundleVersion(final String version) {
		if (version.endsWith("-SNAPSHOT"))
			return version.replace("-SNAPSHOT", ".qualifier");
		return version;
	}

	public static String getExpandedVersion(final String bundleVersion, final String qualifier) {
		final Version version = Version.parseVersion(bundleVersion);
		return new Version(version.getMajor(), version.getMinor(), version.getMicro(), qualifier).toString();
	}

	public static String getSourceBundleSymbolicName(final MavenProject project) {
		return getBundleSymbolicName(project) + ".source";
	}

	public BundleUtil(final Log log, final MavenSession mavenSession) {
		super(log, mavenSession);
	}

}
