package org.eclipse.ebr.maven.tests.integration;

import static io.takari.maven.testing.TestResources.assertFilesPresent;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class RecipeAsserts {

	public static void assertFilesPresentInJar(final File baseDir, final String jarFile, final String... entries) throws IOException {
		assertNotNull(entries);
		assertNotNull(baseDir);
		assertNotNull(jarFile);
		assertFilesPresent(baseDir, jarFile);
		try (JarFile recipeJar = new JarFile(baseDir.toPath().resolve(jarFile).toFile())) {
			for (final String entry : entries) {
				assertNotNull(format("'%s' expected in recipe bundle jar '%s'", entry, jarFile), recipeJar.getEntry(entry));
			}
		}
	}

	public static void assertManifestHeaderValue(final File baseDir, final String manifestFile, final String manifestHeader, final String expectedValue) throws IOException {
		assertNotNull(manifestFile);
		assertNotNull(baseDir);
		assertNotNull(manifestHeader);
		assertFilesPresent(baseDir, manifestFile);
		try (InputStream is = new BufferedInputStream(Files.newInputStream(baseDir.toPath().resolve(manifestFile)))) {
			final Manifest manifest = new Manifest(is);
			final String actualValue = manifest.getMainAttributes().getValue(manifestHeader);
			if (expectedValue == null) {
				assertNull(format("No value expected for manifest header '%s'. Got: '%s'", manifestHeader, actualValue), actualValue);
			} else {
				assertEquals(format("Manifest header '%s' mismatch!", manifestHeader), expectedValue, actualValue);
			}
		}
	}

	private RecipeAsserts() {
		// only static
	}

}
