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
package org.eclipse.kura.system;

import java.io.File;
import java.util.Properties;

public interface SystemConfigurationService {
	public String getProperty(String key);
	public String getProperty(String key, String defaultValue);

	public File getConfigurationLocation();
	public File getConfigurationLocation(String relativePath);
	
	public File getDataLocation();
	public File getDataLocation(String relativePath);
	
	public File getTempLocation();
	
	public Properties getProperties();
	
}
