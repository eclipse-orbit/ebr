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
package org.eclipse.ebr.maven.shared;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;

/**
 * Base class for Mojo utilities.
 */
public abstract class BaseUtility {

	public static final String REQUIRES_FORCE_TO_OVERRIDE_MESSAGE = "Please set the force property to true in order to update/override it (eg. '-Dforce=true' via command line).";

	private final Log log;

	private final MavenSession mavenSession;

	private boolean force;

	public BaseUtility(final Log log, final MavenSession mavenSession) {
		this.log = log;
		this.mavenSession = mavenSession;
	}

	public Log getLog() {
		return log;
	}

	public MavenSession getMavenSession() {
		return mavenSession;
	}

	public boolean isForce() {
		return force;
	}

	public void setForce(final boolean force) {
		this.force = force;
	}
}
