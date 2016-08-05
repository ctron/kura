/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jens Reimann <jreimann@redhat.com> - Initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.internal.deployment.base.legacy;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.kura.deployment.base.DeploymentLocation;
import org.eclipse.kura.system.SystemConfigurationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegacyDeploymentLocationFactory {

	private static final Logger logger = LoggerFactory.getLogger(LegacyDeploymentLocationFactory.class);

	private SystemConfigurationService systemConfigurationService;
	private ServiceRegistration<DeploymentLocation> handle;

	public void setSystemConfigurationService(SystemConfigurationService systemConfigurationService) {
		this.systemConfigurationService = systemConfigurationService;
	}

	public void start() {

		final String packages = this.systemConfigurationService.getProperty("kura.packages");
		if (packages == null || packages.isEmpty()) {
			logger.debug("'kura.packages' not set. Not providing legacy service");
			return;
		}

		final File packagesLocation = new File(packages);
		if (!packagesLocation.isDirectory()) {
			if (!packagesLocation.mkdirs()) {
				logger.warn("'kura.packages' points to invalid target: {}", packagesLocation);
				return;
			}
		}

		final BundleContext ctx = FrameworkUtil.getBundle(LegacyDeploymentLocationFactory.class).getBundleContext();

		final Dictionary<String, Object> properties = new Hashtable<String, Object>();

		this.handle = ctx.registerService(DeploymentLocation.class, new DeploymentLocation() {
			@Override
			public File getPackagesLocation() {
				return packagesLocation;
			}
		}, properties);
	}

	public void stop() {
		if (this.handle != null) {
			this.handle.unregister();
			this.handle = null;
		}
	}
}
