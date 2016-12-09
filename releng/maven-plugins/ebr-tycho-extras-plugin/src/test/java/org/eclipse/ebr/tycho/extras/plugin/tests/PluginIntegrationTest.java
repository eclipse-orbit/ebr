package org.eclipse.ebr.tycho.extras.plugin.tests;

import static io.takari.maven.testing.TestResources.assertFilesPresent;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({ "3.3.9" })
public class PluginIntegrationTest {

	@Rule
	public final TestResources resources = new TestResources();

	public final MavenRuntime verifier;

	public PluginIntegrationTest(final MavenRuntimeBuilder builder) throws Exception {
		verifier = builder.withCliOptions("-X").build();
	}

	@Test
	public void test() throws Exception {
		final File basedir = resources.getBasedir("recipes");
		final File jsoupRecipe = new File(basedir, "jsoup");

		final MavenExecutionResult result = verifier.forProject(jsoupRecipe).execute("package");

		result.assertErrorFreeLog();
		assertFilesPresent(jsoupRecipe, "target/MANIFEST.MF");
	}
}