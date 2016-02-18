package org.eclipse.ebr.maven.shared;

import static java.lang.String.format;
import static org.apache.commons.lang3.CharEncoding.UTF_8;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * A helper for loading templates.
 */
public class TemplateHelper extends BaseUtility {

	private final ClassLoader resourceLoader;

	public TemplateHelper(final Log log, final Class<?> resourceLoaderScope) {
		super(log, null);
		resourceLoader = resourceLoaderScope.getClassLoader();
	}

	public InputStream openTemplate(final String name) throws FileNotFoundException {
		final InputStream is = resourceLoader.getResourceAsStream(name);
		if (is == null)
			throw new FileNotFoundException(format("Template '%s' cannot be found! Please check the plug-in packaging.", name));
		return is;
	}

	public String readTemplateAsString(final String name) throws MojoExecutionException {
		try {
			return IOUtils.toString(openTemplate(name), UTF_8);
		} catch (final Exception e) {
			getLog().debug(e);
			throw new MojoExecutionException(format("Error reading template '%s': %s", name, e.getMessage()));
		}
	}

}
