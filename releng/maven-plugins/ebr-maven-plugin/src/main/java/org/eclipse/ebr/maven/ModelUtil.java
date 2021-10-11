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

import static com.google.common.base.Strings.nullToEmpty;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.ebr.maven.shared.BaseUtility;

import org.eclipse.aether.repository.RepositoryPolicy;

import org.apache.commons.lang3.StringUtils;
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
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.repository.RepositorySystem;

/**
 * A utility for working with dependencies.
 */
public class ModelUtil extends BaseUtility {

	private final ModelBuilder modelBuilder;
	private final RepositorySystem repositorySystem;
	private final RepositoryMetadataManager repositoryMetadataManager;
	private final List<ArtifactRepository> remoteRepositories;

	public ModelUtil(final Log log, final MavenSession mavenSession, final RepositorySystem repositorySystem, final RepositoryMetadataManager repositoryMetadataManager, final ModelBuilder modelBuilder, final List<ArtifactRepository> remoteRepositories) {
		super(log, mavenSession);
		this.repositorySystem = repositorySystem;
		this.repositoryMetadataManager = repositoryMetadataManager;
		this.modelBuilder = modelBuilder;
		this.remoteRepositories = remoteRepositories;
	}

	public Model buildEffectiveModel(final Artifact artifact) throws MojoExecutionException {
		return buildEffectiveModel(resolveArtifactPom(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion().toString()).getFile());
	}

	public Model buildEffectiveModel(final File pomFile) throws MojoExecutionException {
		getLog().debug(format("Building effective model for pom '%s'.", pomFile));

		final DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
		request.setModelResolver(getModelResolver());
		request.setPomFile(pomFile);
		request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
		request.setProcessPlugins(false);
		request.setTwoPhaseBuilding(false);
		request.setUserProperties(getMavenSession().getUserProperties());
		request.setSystemProperties(getMavenSession().getSystemProperties());
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

	public SortedMap<Artifact, Model> buildEffectiveModels(final Set<Artifact> artifacts) throws MojoExecutionException {
		getLog().debug("Building effective POM models");
		final SortedMap<Artifact, Model> result = new TreeMap<>();
		for (final Artifact artifact : artifacts) {
			result.put(artifact, buildEffectiveModel(artifact));
		}
		return result;
	}

	void configureRepositoryRequest(final RepositoryRequest request) throws MojoExecutionException {
		request.setLocalRepository(getMavenSession().getLocalRepository());
		if (!getMavenSession().isOffline()) {
			try {
				request.setRemoteRepositories(getRepositories(false));
			} catch (final InvalidRepositoryException e) {
				getLog().debug(e);
				throw new MojoExecutionException(format("Unable to create the default remote repository. Please verify the Maven configuration. %s", e.getMessage()));
			}
		}
		request.setOffline(getMavenSession().isOffline());
		request.setForceUpdate(RepositoryPolicy.UPDATE_POLICY_ALWAYS.equals(getMavenSession().getRepositorySession().getUpdatePolicy()));
	}

	public ModelBuilder getModelBuilder() {
		return modelBuilder;
	}

	public MavenModelResolver getModelResolver() throws MojoExecutionException {
		if (!getMavenSession().isOffline()) {
			try {
				return new MavenModelResolver(getRepositories(true), getRepositorySystem(), getLog());
			} catch (final InvalidRepositoryException e) {
				getLog().debug(e);
				throw new MojoExecutionException(format("Unable to create the default remote repository. Please verify the Maven configuration. %s", e.getMessage()));
			}
		} else {
			getLog().debug("Using local repository in offline mode!");
			return new MavenModelResolver(Arrays.asList(getMavenSession().getLocalRepository()), getRepositorySystem(), getLog());
		}
	}

	public List<ArtifactRepository> getRemoteRepositories() {
		return remoteRepositories != null ? remoteRepositories : Collections.emptyList();
	}

	private List<ArtifactRepository> getRepositories(final boolean includeLocal) throws InvalidRepositoryException {
		final List<ArtifactRepository> repositories = new ArrayList<>();
		if (includeLocal) {
			repositories.add(getMavenSession().getLocalRepository());
		}
		repositories.addAll(getRemoteRepositories());
		repositories.add(getRepositorySystem().createDefaultRemoteRepository());
		getLog().debug(format("Using repositories: %s", repositories.stream().map((r) -> nullToEmpty(r.getId())).distinct().collect(Collectors.joining(", "))));
		return repositories;
	}

	public RepositoryMetadataManager getRepositoryMetadataManager() {
		return repositoryMetadataManager;
	}

	public RepositorySystem getRepositorySystem() {
		return repositorySystem;
	}

	public Artifact resolveArtifactPom(final String groupId, final String artifactId, final String artifactVersion) throws MojoExecutionException {
		getLog().debug(format("Resolving POM for artifact %s:%s:%s.", groupId, artifactId, artifactVersion));

		try {
			return getModelResolver().resolveArtifactPom(groupId, artifactId, artifactVersion);
		} catch (final UnresolvableModelException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to resolve POM for artifact %s:%s:%s. %s", groupId, artifactId, artifactVersion, e.getMessage()));
		}
	}

	/**
	 * Resolves an artifact version to either a specific given version or the
	 * latest available or released version (in case
	 * <code>artifactVersion</code> is blank, equal to <code>LATEST</code> or
	 * <code>RELEASE</code>).
	 *
	 * @param groupId
	 *            group id
	 * @param artifactId
	 *            artifact id
	 * @param artifactVersion
	 *            a version string, <code>LATEST</code>, <code>RELEASE</code>,
	 *            blank or <code>null</code>
	 * @return resolved version
	 * @throws MojoExecutionException
	 */
	public ArtifactVersion resolveArtifactVersion(final String groupId, final String artifactId, final String artifactVersion) throws MojoExecutionException {
		getLog().debug(format("Reading version metadata for artifact %s:%s.", groupId, artifactId));

		final RepositoryRequest request = new DefaultRepositoryRequest();
		configureRepositoryRequest(request);

		final Artifact artifact = repositorySystem.createArtifact(groupId, artifactId, "[0,)", null, "pom");
		try {
			final RepositoryMetadata metadata = new ArtifactRepositoryMetadata(artifact);
			getRepositoryMetadataManager().resolve(metadata, request);

			final Metadata repositoryMetadata = requireNonNull(metadata.getMetadata(), "No repository metadata loaded.");
			if (StringUtils.equals("LATEST", artifactVersion)) {
				final Versioning metadataVersions = requireNonNull(repositoryMetadata.getVersioning(), "No versioning information available in repository metadata.");
				getLog().debug(format("Resolving '%s' to latest version.", artifactVersion));
				return new DefaultArtifactVersion(metadataVersions.getLatest());
			} else if (StringUtils.equals("RELEASE", artifactVersion) || StringUtils.isBlank(artifactVersion)) {
				final Versioning metadataVersions = requireNonNull(repositoryMetadata.getVersioning(), "No versioning information available in repository metadata.");
				getLog().debug(format("Resolving '%s' to release version.", artifactVersion));
				return new DefaultArtifactVersion(metadataVersions.getRelease());
			} else {
				getLog().debug(format("Resolving '%s' to version.", artifactVersion));
				return new DefaultArtifactVersion(artifactVersion);
			}
		} catch (final Exception e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Unable to retrieve available versions for artifact %s:%s:%s. %s", groupId, artifactId, artifactVersion, e.getMessage()));
		}
	}

}
