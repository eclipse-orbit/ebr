package org.eclipse.ebr.maven.tests.integration;

import static io.takari.maven.testing.TestResources.assertFilesPresent;
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

		result.assertErrorFreeLog();
		assertFilesPresent(baseDir, "target/MANIFEST.MF");
	}
}