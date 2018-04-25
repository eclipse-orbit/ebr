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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.License;

public class KnownLicenses {

	private static final KnownLicenses instance = new KnownLicenses();

	public static KnownLicenses getInstance() {
		return instance;
	}

	private final Map<String, KnownLicense> licensesByName = new HashMap<>();

	private KnownLicenses() {
		addLicense("Apache Software License 1.1", "http://www.apache.org/licenses/LICENSE-1.1").setAlternateNames("Apache License, Version 1.0");
		addLicense("Custom license based on Apache Software License 1.1", "(see about_files)");
		addLicense("Apache License, 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt", "http://www.apache.org/licenses/LICENSE-2.0", "http://www.apache.org/licenses/LICENSE-2.0.html", "http://opensource.org/licenses/Apache-2.0");

		addLicense("New BSD license", "http://opensource.org/licenses/BSD-3-Clause").setAlternateNames("The BSD 3-Clause License", "BSD New", "New BSD");

		addLicense("Common Development and Distribution License", "https://glassfish.java.net/public/CDDLv1.0.html", "http://opensource.org/licenses/CDDL-1.0").setAlternateNames("CDDL");
		addLicense("Common Public License 1.0", "http://opensource.org/licenses/cpl1.0.php", "http://www.ibm.com/developerworks/library/os-cpl.html").setAlternateNames("CPL");

		addLicense("Eclipse Public License", "http://www.eclipse.org/legal/epl-v10.html").setAlternateNames("EPL");

		addLicense("MIT license", "http://opensource.org/licenses/MIT");

		addLicense("Mozilla Public License 1.0 (MPL)", "https://www.mozilla.org/MPL/1.0/");
		addLicense("Mozilla Public License 1.1 (MPL)", "https://www.mozilla.org/MPL/1.1/");
		addLicense("Public Domain", "https://creativecommons.org/publicdomain/zero/1.0/");
		addLicense("SUN Industry Standards Source License 1.2", "http://gridscheduler.sourceforge.net/Gridengine_SISSL_license.html").setAlternateNames("SISSL-1.2");
		addLicense("Java Cup License (MIT Style)", "http://www2.cs.tum.edu/projekte/cup/licence.php");
	}

	private KnownLicense addLicense(final String name, final String... knownUrls) {
		final KnownLicense l = new KnownLicense(name);
		if (knownUrls != null) {
			for (final String url : knownUrls) {
				l.getKnownUrls().add(url);
			}
		}
		licensesByName.put(name, l);
		return l;
	}

	public KnownLicense findByUrl(final String url) {
		for (final KnownLicense l : licensesByName.values()) {
			for (final String knownUrl : l.getKnownUrls()) {
				if (StringUtils.getJaroWinklerDistance(url, knownUrl) >= 0.99)
					return l;
			}
		}

		return null;
	}

	public Set<KnownLicense> findSimilarLicensesByName(final String name) {
		final Set<KnownLicense> similarLicenses = new HashSet<KnownLicense>();
		for (final KnownLicense l : licensesByName.values()) {
			if (isSimilar(name, l.getName())) {
				similarLicenses.add(l);
				continue;
			}
			for (final String alternateName : l.getAlternateNames()) {
				if (isSimilar(name, alternateName)) {
					similarLicenses.add(l);
					continue;
				}
			}
		}
		return similarLicenses;
	}

	public SortedSet<String> getAllLicenseNames() {
		return new TreeSet<String>(licensesByName.keySet());
	}

	public KnownLicense getByName(final String name) {
		return licensesByName.get(name);
	}

	/**
	 * Determines if the specified license is a known dual license.
	 *
	 * @param license
	 * @return <code>true</code> if it's a dual license, <code>false</code>
	 *         otherwise
	 */
	public boolean isDualLicense(final License license) {
		final String upperCaseLicenseName = StringUtils.upperCase(license.getName());
		return StringUtils.containsAny(upperCaseLicenseName, "GPL") && StringUtils.containsAny(upperCaseLicenseName, "CDDL");
	}

	private boolean isSimilar(final String name, final String alternateName) {
		final double distance = StringUtils.getJaroWinklerDistance(name, alternateName);
		return distance >= 0.9;
	}

}
