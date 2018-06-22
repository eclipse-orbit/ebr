package org.eclipse.ebr.maven.tests.integration;

import static io.takari.maven.testing.TestResources.assertFilesPresent;
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
@MavenVersions({ "3.3.9" })
public class RecipeWihtoutUnpackTest {

	@Rule
	public final TestResources resources = new TestResources();

	public final MavenRuntime verifier;

	public RecipeWihtoutUnpackTest(final MavenRuntimeBuilder builder) throws Exception {
		verifier = builder.withCliOptions("-X").build();
	}

	private File getProjectDir(final String project) throws IOException {
		final File projectDir = resources.getBasedir("it-tests/" + project);
		assertTrue("Project directory '" + projectDir + "' not found!", projectDir.isDirectory());
		return projectDir;
	}

	private void packageProjectAndAssertBundleClasspathHeader(final String testProject, final String expectedBundleClasspathValue) throws Exception, IOException {
		final File baseDir = getProjectDir(testProject);

		final MavenExecutionResult result = verifier.forProject(baseDir).execute("package");

		// check for no errors in log
		assertNoErrorsInMavenLog(baseDir, result);

		assertFilesPresent(baseDir, "target/classes/lib/ant-1.10.1.jar");
		assertFilesPresent(baseDir, "target/classes/lib/ant-launcher-1.10.1.jar");

		// check Bundle-ClassPath
		assertManifestHeaderValue(baseDir, "target/MANIFEST.MF", "Bundle-ClassPath", expectedBundleClasspathValue);

		// check jar
		assertFilesPresentInJar(baseDir, "target/recipe-without-unpack-it-1.0.0-SNAPSHOT.jar", "lib/ant-1.10.1.jar", "lib/ant-launcher-1.10.1.jar");
	}

	@Test
	public void test() throws Exception {
		packageProjectAndAssertBundleClasspathHeader("recipe-without-unpack", ".,lib/ant-1.10.1.jar,lib/ant-launcher-1.10.1.jar");
	}

	@Test
	public void testWithoutDot() throws Exception {
		packageProjectAndAssertBundleClasspathHeader("recipe-without-unpack2", "lib/ant-1.10.1.jar,lib/ant-launcher-1.10.1.jar");
	}
}