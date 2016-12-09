package org.eclipse.ebr.tycho.extras.plugin.tests;

import static io.takari.maven.testing.TestMavenRuntime.newParameter;
import static io.takari.maven.testing.TestResources.assertFilesPresent;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

import io.takari.maven.testing.TestMavenRuntime;
import io.takari.maven.testing.TestResources;

public class PluginUnitTest {
	@Rule
	public final TestResources resources = new TestResources();

	@Rule
	public final TestMavenRuntime maven = new TestMavenRuntime();

	@Test
	public void test() throws Exception {
		final File basedir = resources.getBasedir("testproject");
		maven.executeMojo(basedir, "mymojo", newParameter("name", "value"));
		assertFilesPresent(basedir, "target/output.txt");
	}
}