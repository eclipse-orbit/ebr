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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.ebr.maven.shared.BaseUtility;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * A utility for working with dependencies.
 */
public class DependencyUtil extends BaseUtility {

	private Set<String> excludedArtifactIds;

	public DependencyUtil(final Log log, final MavenSession mavenSession) {
		super(log, mavenSession);
	}

	public Set<Artifact> getDependenciesToInclude(final MavenProject project) {
		getLog().debug("Computing direct dependencies to include");

		// start with direct dependencies
		final Set<Artifact> dependencies = new LinkedHashSet<Artifact>(project.getDependencyArtifacts());

		// prepare set of artifact ids to exclude
		final Set<String> excludedArtifactIds = checkNotNull(this.excludedArtifactIds, "programming error: list of dependencies not initialized");

		// remove all dependencies which should not be in the list
		for (final Iterator<Artifact> stream = dependencies.iterator(); stream.hasNext();) {
			final Artifact artifact = stream.next();
			if (!isAllowedDependency(artifact, excludedArtifactIds)) {
				stream.remove();
			}
		}
		return dependencies;
	}

	/**
	 * Initialize the set of artifact ids to exclude from a comma separated list
	 *
	 * @param excludeDependencies
	 */
	public void initializeExcludeDependencies(final String excludeDependencies) {
		excludedArtifactIds = new HashSet<String>();
		if (StringUtils.isNotBlank(excludeDependencies)) {
			final String[] tokens = StringUtils.split(excludeDependencies, ',');
			for (final String t : tokens) {
				excludedArtifactIds.add(t.trim());
			}
		}
	}

	private boolean isAllowedDependency(final Artifact artifact, final Set<String> excludedArtifactIds) {
		if (excludedArtifactIds.contains(artifact.getArtifactId())) {
			getLog().debug(format("Dependency '%s' excluded per configuration.", artifact.getArtifactId()));
			return false;
		}
		if (!artifact.getArtifactHandler().isAddedToClasspath()) {
			getLog().debug(format("Dependency '%s' not part of classpath.", artifact.getArtifactId()));
			return false;
		}
		if (!StringUtils.equals(artifact.getScope(), Artifact.SCOPE_COMPILE)) {
			getLog().debug(format("Dependency '%s' scope is not COMPILE.", artifact.getArtifactId()));
			return false;
		}
		getLog().debug(format("Dependency '%s' allowed.", artifact.getArtifactId()));
		return true;
	}

}
