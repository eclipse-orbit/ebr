package org.eclipse.ebr.maven;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.repository.RepositorySystem;

public class MavenModelResolver implements ModelResolver {

	private final Map<String, ArtifactRepository> repositoryById = new LinkedHashMap<>();
	private final RepositorySystem repositorySystem;
	private final Log log;

	public MavenModelResolver(final Collection<ArtifactRepository> repositories, final RepositorySystem repositorySystem, final Log log) {
		this.repositorySystem = repositorySystem;
		this.log = log;
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

	public Log getLog() {
		return log;
	}

	public RepositorySystem getRepositorySystem() {
		return repositorySystem;
	}

	@Override
	public ModelResolver newCopy() {
		return new MavenModelResolver(repositoryById.values(), getRepositorySystem(), getLog());
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
	public ModelSource resolveModel(final Dependency dependency) throws UnresolvableModelException {
		return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
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