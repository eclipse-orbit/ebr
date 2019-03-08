package org.eclipse.ebr.maven.tests.integration;

import static io.takari.maven.testing.TestResources.assertFilesPresent;
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
public class CreateRecipeTest {

	@Rule
	public final TestResources resources = new TestResources();

	private final MavenRuntimeBuilder mvnRuntimeBuilder;

	public CreateRecipeTest(final MavenRuntimeBuilder mvnRuntimeBuilder) throws Exception {
		this.mvnRuntimeBuilder = mvnRuntimeBuilder;
	}

	private File getProjectDir(final String project) throws IOException {
		final File projectDir = resources.getBasedir("it-tests/" + project);
		assertTrue("Project directory '" + projectDir + "' not found!", projectDir.isDirectory());
		return projectDir;
	}

	@Test
	public void test() throws Exception {
		final File baseDir = getProjectDir("create");

		// mvn -U -e -V ebr:create-recipe -DgroupId=com.google.code.gson -DartifactId=gson -DbundleSymbolicName=com.google.gson.ebr -Dversion=2.7
		final MavenRuntime verifier = mvnRuntimeBuilder.withCliOptions("-X", "ebr:create-recipe", "-DgroupId=org.jsoup", "-DartifactId=jsoup", "-DbundleSymbolicName=ebr.it.test.jsoup", "-Dversion=1.10.2").build();

		final MavenExecutionResult result = verifier.forProject(baseDir).execute("package");

		// check for no errors in log
		assertNoErrorsInMavenLog(baseDir, result);

		assertFilesPresent(baseDir, "ebr.it.test.jsoup_1.10.2/pom.xml");
		assertFilesPresent(baseDir, "ebr.it.test.jsoup_1.10.2/osgi.bnd");
	}
}