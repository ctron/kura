/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jens Reimann <jreimann@redhat.com> - Refactor kura properties handling
 *******************************************************************************/
package org.eclipse.kura.core.internal;

import org.eclipse.kura.system.SystemConfigurationService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private static Activator instance;
	private ServiceTracker<SystemConfigurationService, SystemConfigurationService> tracker;

	@Override
	public void start(BundleContext context) throws Exception {
		instance = this;
		this.tracker = new ServiceTracker<SystemConfigurationService, SystemConfigurationService>(context,
				SystemConfigurationService.class, null);
		tracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		instance = null;
		if (tracker != null) {
			tracker.close();
			tracker = null;
		}
	}

	public static SystemConfigurationService getSystemConfigurationService () {
		return instance.tracker.getService();
	}

}
