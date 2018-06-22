package org.eclipse.ebr.maven.tests.integration;

import static io.takari.maven.testing.TestResources.assertFilesPresent;
import static java.lang.String.format;
import static java.nio.file.Files.readAllLines;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.text.TextStringBuilder;

import io.takari.maven.testing.executor.MavenExecutionResult;
import junit.framework.AssertionFailedError;

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

	public static void assertNoErrorsInMavenLog(final File baseDir, final MavenExecutionResult result) throws AssertionFailedError, Exception {
		final File logFile = new File(baseDir, "log.txt");
		assertTrue("No Maven log found at: " + logFile.getAbsolutePath(), logFile.isFile());

		try {
			final List<String> logLines = readAllLines(logFile.toPath());

			// collect errors
			// note: we traverse backwards to allow capturing a few lines before the errors
			final List<String> errors = new ArrayList<String>();
			int lastLineWithError = -1;
			for (int i = logLines.size() - 1; i >= 0; i--) {
				final String line = logLines.get(i);
				if (line.contains("[ERROR]")) {
					errors.add(0, line);
					lastLineWithError = i;
				} else if ((lastLineWithError >= 0) && (i >= (lastLineWithError - 20))) {
					// we also capture up to 20 proceeding lines before the error
					errors.add(0, line);
				} else if (lastLineWithError >= 0) {
					// there was at least one error
					// and we finished capturing proceeding lines
					// thus, it's save to abort here
					break;
				}
			}

			if (errors.size() > 0) {
				final TextStringBuilder errorMessage = new TextStringBuilder(errors.size() * 80);
				errorMessage.appendln("[ERROR] Maven execution failed! Check Maven log file at: %s", logFile);
				errorMessage.appendFixedWidthPadRight("<Log Output> ", 80, '=').appendNewLine();
				errorMessage.appendWithSeparators(errors, System.lineSeparator()).appendNewLine();
				errorMessage.appendFixedWidthPadLeft(" </Log Output> ", 80, '=').appendNewLine();
				throw new AssertionFailedError(errorMessage.build());
			}

			// sanity check our logic is safe
			result.assertErrorFreeLog();
		} catch (IOException | RuntimeException e) {
			// last resort fallback
			result.assertErrorFreeLog();
			throw e;
		}
	}

	private RecipeAsserts() {
		// only static
	}
}