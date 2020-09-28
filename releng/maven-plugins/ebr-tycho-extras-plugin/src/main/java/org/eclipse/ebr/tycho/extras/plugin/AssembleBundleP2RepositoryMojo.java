package org.eclipse.ebr.tycho.extras.plugin;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.text.StringEscapeUtils.escapeXml10;
import static org.eclipse.ebr.maven.shared.BundleUtil.getBundleSymbolicName;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.ebr.maven.shared.BundleUtil;
import org.eclipse.ebr.maven.shared.TemplateHelper;

import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentConfigurationImpl;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.osgitools.AbstractTychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.resolver.DefaultTargetPlatformConfigurationReader;
import org.eclipse.tycho.core.resolver.DefaultTychoResolver;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.resolver.shared.PlatformPropertiesUtils;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.model.Category;
import org.eclipse.tycho.osgi.adapters.MavenReactorProjectIdentities;
import org.eclipse.tycho.p2.facade.RepositoryReferenceTool;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;
import org.eclipse.tycho.plugins.p2.repository.AbstractRepositoryMojo;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManagerFacade;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.logging.Logger;

/**
 * Aggregates the project content into a p2 repository in
 * <code>${project.build.directory}/repository</code>.
 */
@Mojo(name = "assemble-bundle-p2-repository", defaultPhase = LifecyclePhase.PACKAGE)
public class AssembleBundleP2RepositoryMojo extends AbstractRepositoryMojo {

	@Parameter(property = "project", readonly = true, required = true)
	private MavenProject project;

	@Parameter(property = "session", readonly = true, required = true)
	private MavenSession session;

	/**
	 * <p>
	 * Compress the repository index files <tt>content.xml</tt> and
	 * <tt>artifacts.xml</tt>.
	 * </p>
	 */
	@Parameter(defaultValue = "false")
	private boolean compress;

	/**
	 * <p>
	 * Add XZ-compressed repository index files. XZ offers better compression
	 * ratios esp. for highly redundant file content.
	 * </p>
	 */
	@Parameter(defaultValue = "true")
	private boolean xzCompress;

	/**
	 * <p>
	 * If {@link #xzCompress} is <code>true</code>, whether jar or xml index
	 * files should be kept in addition to XZ-compressed index files. This
	 * fallback provides backwards compatibility for pre-Mars p2 clients which
	 * cannot read XZ-compressed index files.
	 * </p>
	 */
	@Parameter(defaultValue = "true")
	private boolean keepNonXzIndexFiles;

	/**
	 * <p>
	 * The name attribute stored in the created p2 repository.
	 * </p>
	 */
	@Parameter(defaultValue = "${project.name}")
	private String repositoryName;

	/**
	 * <p>
	 * Additional properties against which p2 filters are evaluated while
	 * aggregating.
	 * </p>
	 */
	@Parameter
	private Map<String, String> profileProperties;

	/**
	 * Build qualifier. Recommended way to set this parameter is using
	 * build-qualifier goal.
	 */
	@Parameter(defaultValue = "${buildQualifier}")
	private String qualifier;

	@Component
	private RepositoryReferenceTool repositoryReferenceTool;

	@Component
	private EquinoxServiceFactory p2;

	@Component(role = TychoProject.class)
	private Map<String, TychoProject> projectTypes;

	@Component
	private DefaultTargetPlatformConfigurationReader configurationReader;

	@Component
	private Logger logger;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Assembling p2 repository");

		final File destination = getAssemblyRepositoryLocation();
		destination.mkdirs();

		getLog().debug("Setting up Tycho...");
		setupProjectForTycho(getSession(), getProject(), getReactorProject());

		publishCategoryForBundle();

		try {
			final Collection<DependencySeed> projectSeeds = TychoProjectUtils.getDependencySeeds(getProject());
			if (projectSeeds.size() == 0)
				throw new MojoFailureException("No content specified for p2 repository");

			final RepositoryReferences sources = getVisibleRepositories();

			final TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(getProject());

			final MirrorApplicationService mirrorApp = p2.getService(MirrorApplicationService.class);
			final DestinationRepositoryDescriptor destinationRepoDescriptor = new DestinationRepositoryDescriptor(destination, repositoryName, compress, xzCompress, keepNonXzIndexFiles, false, true, emptyMap(), Collections.emptyList());
			mirrorApp.mirrorReactor(sources, destinationRepoDescriptor, projectSeeds, getBuildContext(), false, configuration.isIncludePackedArtifacts(), profileProperties);
		} catch (final FacadeException e) {
			throw new MojoExecutionException("Could not assemble p2 repository", e);
		}
	}

	private Collection<DependencySeed> generateCategoryForBundle() throws MojoExecutionException {
		getLog().debug("Generating category.xml...");
		final PublisherServiceFactory publisherServiceFactory = p2.getService(PublisherServiceFactory.class);
		final PublisherService publisherService = publisherServiceFactory.createPublisher(getReactorProject(), getEnvironments());

		try {
			final TemplateHelper templateHelper = new TemplateHelper(getLog(), this.getClass());
			final String categoryXml = StringUtils.replaceEach(templateHelper.readTemplateAsString("category.xml"),
					new String[] { // @formatter:off
							"@BUNDLE_SYMBOLIC_NAME@",
							"@BUNDLE_VERSION@",
							"@CATEGORY_ID@",
							"@CATEGORY_LABEL@",
							"@CATEGORY_DESCRIPTION@"
					},
					new String[] {
							getBundleSymbolicName(getProject()),
							getExpandedVersion(),
							"ebr_category_" + getBundleSymbolicName(getProject()),
							escapeXml10(trimToEmpty(getProject().getName())),
							escapeXml10(trimToEmpty(getProject().getDescription()))
					});
			// @formatter:on

			final Category category = Category.read(new ByteArrayInputStream(categoryXml.getBytes(UTF_8)));
			final File buildCategoryFile = prepareBuildCategory(category, getBuildDirectory());
			return publisherService.publishCategories(buildCategoryFile);
		} catch (final IOException | FacadeException e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Error generating category.xml: %s", e.getMessage()));

		}
	}

	@Override
	protected BuildContext getBuildContext() {
		// only overridden because injection into super class did not work
		// FIXME figure out while injection in super class is broken
		final List<TargetEnvironment> environments = TychoProjectUtils.getTargetPlatformConfiguration(project).getEnvironments();
		return new BuildContext(getProjectIdentities(), qualifier, environments);
	}

	private String getBundleVersion() {
		return BundleUtil.getBundleVersion(getProject().getVersion());
	}

	@Override
	protected List<TargetEnvironment> getEnvironments() {
		return TychoProjectUtils.getTargetPlatformConfiguration(getProject()).getEnvironments();
	}

	private String getExpandedVersion() {
		return BundleUtil.getExpandedVersion(getBundleVersion(), qualifier);
	}

	@Override
	protected MavenProject getProject() {
		// only overridden because injection into super class did not work
		// FIXME figure out while injection in super class is broken
		return requireNonNull(project, "MavenProject not set!");
	}

	@Override
	protected ReactorProjectIdentities getProjectIdentities() {
		return new MavenReactorProjectIdentities(getProject());
	}

	@Override
	protected ReactorProject getReactorProject() {
		return DefaultReactorProject.adapt(getProject());
	}

	@Override
	protected MavenSession getSession() {
		// only overridden because injection into super class did not work
		// FIXME figure out while injection in super class is broken
		return requireNonNull(session, "MavenSession not set!");
	}

	private RepositoryReferences getVisibleRepositories() throws MojoExecutionException, MojoFailureException {
		final RepositoryReferences repositories = new RepositoryReferences();

		// just the content produced by this recipe is visible
		final File publisherResults = new File(getProject().getBuild().getDirectory());
		repositories.addMetadataRepository(publisherResults);
		repositories.addArtifactRepository(publisherResults);

		return repositories;
	}

	private File prepareBuildCategory(final Category category, final BuildOutputDirectory buildFolder) throws MojoExecutionException {
		try {
			final File ret = buildFolder.getChild("category.xml");
			buildFolder.getLocation().mkdirs();
			Category.write(category, ret);
			return ret;
		} catch (final IOException e) {
			getLog().debug(e);
			throw new MojoExecutionException("I/O exception while writing category definition to disk. " + e.getMessage());
		}
	}

	private void publishCategoryForBundle() throws MojoExecutionException {
		TychoProjectUtils.getDependencySeeds(getProject()).addAll(generateCategoryForBundle());
	}

	protected void setTychoEnvironmentProperties(final Properties properties, final MavenProject project) {
		final String arch = PlatformPropertiesUtils.getArch(properties);
		final String os = PlatformPropertiesUtils.getOS(properties);
		final String ws = PlatformPropertiesUtils.getWS(properties);
		project.getProperties().put(DefaultTychoResolver.TYCHO_ENV_OSGI_WS, ws);
		project.getProperties().put(DefaultTychoResolver.TYCHO_ENV_OSGI_OS, os);
		project.getProperties().put(DefaultTychoResolver.TYCHO_ENV_OSGI_ARCH, arch);
	}

	private void setupProjectForTycho(final MavenSession session, final MavenProject project, final ReactorProject reactorProject) {
		// based on DefaultTychoResolved and modified to the bundle recipe project

		final AbstractTychoProject dr = (AbstractTychoProject) projectTypes.get(project.getPackaging());
		if (dr == null)
			return;

		// skip if setup was already done
		if (project.getContextValue(TychoConstants.CTX_MERGED_PROPERTIES) != null)
			return;

		// generic Eclipse/OSGi metadata

		dr.setupProject(session, project);

		// p2 metadata

		final Properties properties = new Properties();
		properties.putAll(project.getProperties());
		properties.putAll(session.getSystemProperties()); // session wins
		properties.putAll(session.getUserProperties());
		project.setContextValue(TychoConstants.CTX_MERGED_PROPERTIES, properties);

		setTychoEnvironmentProperties(properties, project);

		final TargetPlatformConfiguration configuration = configurationReader.getTargetPlatformConfiguration(session, project);
		project.setContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION, configuration);

		final ExecutionEnvironmentConfiguration eeConfiguration = new ExecutionEnvironmentConfigurationImpl(logger, !configuration.isResolveWithEEConstraints(), null, session);
		dr.readExecutionEnvironmentConfiguration(project, session, eeConfiguration);
		project.setContextValue(TychoConstants.CTX_EXECUTION_ENVIRONMENT_CONFIGURATION, eeConfiguration);

		// we assume dependency resolution was done when generating p2 metadata
		// would that cause ClassCast issues?

		// setup a minimal target platform
		final TargetPlatformConfigurationStub tpConfiguration = new TargetPlatformConfigurationStub();
		tpConfiguration.setIncludePackedArtifacts(configuration.isIncludePackedArtifacts());

		final P2ResolverFactory resolverFactory = p2.getService(P2ResolverFactory.class);
		final PomDependencyCollector pomDependencies = resolverFactory.newPomDependencyCollector();
		pomDependencies.setProjectLocation(project.getBasedir());

		tpConfiguration.setEnvironments(configuration.getEnvironments());

		final ReactorRepositoryManagerFacade repositoryManager = p2.getService(ReactorRepositoryManagerFacade.class);
		repositoryManager.computePreliminaryTargetPlatform(getReactorProject(), tpConfiguration, eeConfiguration, Collections.emptyList(), pomDependencies);
		repositoryManager.computeFinalTargetPlatform(getReactorProject(), Collections.emptyList());
		getLog().debug("final target platform: " + getReactorProject().getContextValue(TargetPlatform.FINAL_TARGET_PLATFORM_KEY));
	}

}
