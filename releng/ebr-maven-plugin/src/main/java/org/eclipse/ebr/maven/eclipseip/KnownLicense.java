/**
 * Copyright (c) 2014 Tasktop Technologies and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.ebr.maven.eclipseip;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.License;

public class KnownLicense {

	private final String name;

	private LinkedHashSet<String> knownUrls;
	private Set<String> alternateNames;

	/**
	 * Creates a new instance.
	 *
	 * @param name
	 *            the name in the Eclipse Foundation IP database
	 */
	public KnownLicense(final String name) {
		this.name = name;
	}

	public Set<String> getAlternateNames() {
		if (alternateNames == null) {
			alternateNames = new HashSet<>();
		}
		return alternateNames;
	}

	public Set<String> getKnownUrls() {
		if (knownUrls == null) {
			knownUrls = new LinkedHashSet<>();
		}
		return knownUrls;
	}

	/**
	 * Returns the name used in the Eclipse Foundation IP database.
	 *
	 * @return
	 */
	public String getName() {
		return name;
	}

	public String getUrl() {
		return getKnownUrls().size() > 0 ? getKnownUrls().iterator().next() : StringUtils.EMPTY;
	}

	public void setAlternateNames(final String... names) {
		if (names != null) {
			for (final String name : names) {
				getAlternateNames().add(name);
			}
		}
	}

	public License toMavenLicense() {
		final License l = new License();
		l.setName(getName());
		if ((knownUrls != null) && (knownUrls.size() >= 1)) {
			l.setUrl(knownUrls.iterator().next());
		}
		return l;
	}

}
