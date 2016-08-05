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
package org.eclipse.kura.core.system;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

import org.eclipse.kura.system.SystemConfigurationService;
import org.eclipse.kura.system.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegacySystemConfigurationService implements SystemConfigurationService {

	private static final Logger logger = LoggerFactory.getLogger(LegacySystemConfigurationService.class);

	private static final String CLOUDBEES_SECURITY_SETTINGS_PATH = "/private/eurotech/settings-security.xml";

	private Properties properties = new Properties(System.getProperties());

	public void start() {
		this.properties = loadProperties();
	}

	/**
	 * Load all properties
	 * <p>
	 * The order of properties is (later entries overriding previous ones):
	 * </p>
	 * <ol>
	 * <li>Content from kura.configuration system property</li>
	 * <li>Content from kura.custom.configuration system property</li>
	 * <li>System properties</li>
	 * <li>Cloudbees property</li>
	 * <li>Explicit defaults</li>
	 * </ol>
	 * @return the complete set of properties
	 */
	protected static Properties loadProperties() {
		final Properties p = new Properties();
		
		fillFrom(p, "kura.configuration");
		fillFrom(p, "kura.custom.configuration");
		fillFrom(p, System.getProperties());
		fillFromCloudbees(p);
		fillFromDefaults(p);
		
		return p;
	}

	private static void fillFrom(Properties properties, Properties otherProperties) {
		properties.putAll(otherProperties);
	}

	private static void fillFromDefaults(Properties properties) {
		final Boolean hasNetAdmin = Boolean
				.valueOf(properties.getProperty(SystemService.KEY_KURA_HAVE_NET_ADMIN, "true"));
		properties.put(SystemService.KEY_KURA_HAVE_NET_ADMIN, hasNetAdmin.toString());

		final Boolean hasWebInterface = Boolean
				.valueOf(properties.getProperty(SystemService.KEY_KURA_HAVE_WEB_INTER, "true"));
		properties.put(SystemService.KEY_KURA_HAVE_WEB_INTER, hasWebInterface.toString());

		final String kuraVersion = properties.getProperty(SystemService.KEY_KURA_VERSION, "version-unknown");
		properties.put(SystemService.KEY_KURA_VERSION, kuraVersion);
	}

	private static void fillFromCloudbees(Properties properties) {
		final boolean result = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			public Boolean run() {
				try {
					// privileged code goes here, for example:
					return (new File(CLOUDBEES_SECURITY_SETTINGS_PATH)).exists();
				} catch (Exception e) {
					System.out.println("Unable to execute privileged in SystemService");
					return Boolean.FALSE;
				}
			}
		});
		if (result) {
			properties.put(SystemService.KEY_OS_NAME, SystemService.OS_CLOUDBEES);
		}
	}

	private static void fillFrom(final Properties properties, String propertyName) {
		final String kuraConfiguration = System.getProperty(propertyName);
		if (kuraConfiguration == null || kuraConfiguration.isEmpty()) {
			logger.info("'{}' is not set. Not reading properties from this source", propertyName);
			return;
		}

		InputStream in = null;
		try {
			URL url = new URL(kuraConfiguration);
			in = url.openStream();

			Properties p = new Properties();
			logger.debug("Loading properties from: {}", url);
			p.load(in);
			logger.info("Loaded {} properties from {}", p.size(), url);
			properties.putAll(p);
		} catch (Exception e) {
			logger.warn("Failed to read properties from: " + kuraConfiguration, e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					logger.warn("Failed to close input stream", e);
				}
			}
		}
	}

	public void stop() {
		this.properties = new Properties(System.getProperties());
	}

	@Override
	public String getProperty(String key) {
		return this.properties.getProperty(key);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return this.properties.getProperty(key, defaultValue);
	}

	@Override
	public Properties getProperties() {
		return new Properties(this.properties);
	}

	protected File getLocation(String propertyName) {
		final String location = getProperty(propertyName);

		logger.debug("Location lookup : '{}' -> '{}'", propertyName, location);

		if (location == null) {
			return null;
		}

		return new File(location);
	}

	protected File resolve(File base, String relative) {
		if (base == null) {
			return null;
		}
		if (relative == null) {
			return base;
		}
		return new File(base, relative);
	}

	@Override
	public File getConfigurationLocation() {
		return getLocation("kura.home");
	}

	@Override
	public File getConfigurationLocation(String relativePath) {
		return resolve(getConfigurationLocation(), relativePath);
	}

	@Override
	public File getDataLocation() {
		return getLocation("kura.data");
	}

	@Override
	public File getDataLocation(String relativePath) {
		return resolve(getDataLocation(), relativePath);
	}

	@Override
	public File getTempLocation() {
		final File location = getLocation("kura.tmp");
		if (location != null) {
			return location;
		}

		final String tmpDir = System.getProperty("java.io.tmpdir");
		if (tmpDir != null) {
			File tmp = new File(tmpDir, ".kura");
			if (tmp.isDirectory() || tmp.mkdirs()) {
				return tmp;
			}
		}

		throw new RuntimeException("Unable to provide temp directory");
	}

}
