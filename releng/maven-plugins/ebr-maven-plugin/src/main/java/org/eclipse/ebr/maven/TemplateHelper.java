package org.eclipse.ebr.maven;

import static java.lang.String.format;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * A helper for loading templates.
 */
public class TemplateHelper {

	static InputStream getTemplate(final String name) throws FileNotFoundException {
		final ClassLoader cl = TemplateHelper.class.getClassLoader();
		final InputStream is = cl.getResourceAsStream(name);
		if (is == null)
			throw new FileNotFoundException(format("Template '%s' cannot be found! Please check the plug-in packaging.", name));
		return is;
	}

}
