package org.eclipse.ebr.maven.tests.integration;

import static org.eclipse.ebr.maven.tests.integration.RecipeAsserts.assertFilesPresentInJar;
import static org.eclipse.ebr.maven.tests.integration.RecipeAsserts.assertManifestHeaderValue;
import static org.eclipse.ebr.maven.tests.integration.RecipeAsserts.assertNoErrorsInMavenLog;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

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
@MavenVersions({ "3.6.0" })
public class SimpleIntegrationTest {

	@Rule
	public final TestResources resources = new TestResources();

	public final MavenRuntime verifier;

	public SimpleIntegrationTest(final MavenRuntimeBuilder builder) throws Exception {
		verifier = builder.withCliOptions("-X").build();
	}

	private File getProjectDir(final String project) throws IOException {
		final File projectDir = resources.getBasedir("it-tests/" + project);
		assertTrue("Project directory '" + projectDir + "' not found!", projectDir.isDirectory());
		return projectDir;
	}

	@Test
	public void test() throws Exception {
		final File baseDir = getProjectDir("simple");

		final MavenExecutionResult result = verifier.forProject(baseDir).execute("package");

		// check for no errors in log
		assertNoErrorsInMavenLog(baseDir, result);

		// check Bundle-ClassPath
		assertManifestHeaderValue(baseDir, "target/MANIFEST.MF", "Bundle-ClassPath", null);

		// check jar
		assertFilesPresentInJar(baseDir, "target/simple-it-1.0.0-SNAPSHOT.jar", "org/junit/Test.class");
	}

}