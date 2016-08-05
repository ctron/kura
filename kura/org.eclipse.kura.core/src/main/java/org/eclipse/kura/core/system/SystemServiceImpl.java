/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Jens Reimann <jreimann@redhat.com> - Refactor system properties and paths
 *******************************************************************************/
package org.eclipse.kura.core.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.system.SystemConfigurationService;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemServiceImpl implements SystemService {
	private static final Logger s_logger = LoggerFactory.getLogger(SystemServiceImpl.class);

	private ComponentContext m_ctx;

	private NetworkService m_networkService;

	private SystemConfigurationService systemConfigurationService;

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------
	public void setNetworkService(NetworkService networkService) {
		m_networkService = networkService;
	}

	public void unsetNetworkService(NetworkService networkService) {
		m_networkService = null;
	}

	public void setSystemConfigurationService(SystemConfigurationService systemConfigurationService) {
		this.systemConfigurationService = systemConfigurationService;
	}

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext) {
		m_ctx = componentContext;

		try {
			s_logger.info("Kura has net admin? " + getProperty(KEY_KURA_HAVE_NET_ADMIN));
			s_logger.info("Kura has web interface? " + getProperty(KEY_KURA_HAVE_WEB_INTER));
			s_logger.info("Kura version? " + getProperty(KEY_KURA_VERSION));

			if (getKuraSnapshotsDirectory() == null) {
				s_logger.error("Did not initialize kura.snapshots");
			} else {
				s_logger.info("Kura snapshots directory is " + getKuraSnapshotsDirectory());
				createDirIfNotExists(getKuraSnapshotsDirectory());
			}
			if (getKuraTemporaryConfigDirectory() == null) {
				s_logger.error("Did not initialize kura.tmp");
			} else {
				s_logger.info("Kura tmp directory is " + getKuraTemporaryConfigDirectory());
				createDirIfNotExists(getKuraTemporaryConfigDirectory());
			}

			s_logger.info(new StringBuffer().append("Kura version ").append(getKuraVersion()).append(" is starting")
					.toString());
		} catch (Exception e) {
			throw new ComponentException("Error loading default properties", e);
		}
	}

	protected void deactivate(ComponentContext componentContext) {
		m_ctx = null;
	}

	public void updated(Map<String, Object> properties) {
		// nothing to do
		// all properties of the System service are read-only
	}

	// ----------------------------------------------------------------
	//
	// Service APIs
	//
	// ----------------------------------------------------------------

	/**
	 * Returns all KuraProperties for this system. The returned instances is
	 * initialized by loading the kura.properties file. Properties defined at
	 * the System level - for example using the java -D command line flag - are
	 * used to overwrite the values loaded from the kura.properties file in a
	 * hierarchical configuration fashion.
	 */
	@Override
	public Properties getProperties() {
		return this.systemConfigurationService.getProperties();
	}

	protected String getProperty(String key) {
		return this.systemConfigurationService.getProperty(key);
	}

	@Override
	public String getPrimaryMacAddress() {
		String primaryNetworkInterfaceName = getPrimaryNetworkInterfaceName();
		String macAddress = null;

		if (OS_MAC_OSX.equals(getOsName())) {
			SafeProcess proc = null;
			try {
				s_logger.info("executing: ifconfig and looking for " + primaryNetworkInterfaceName);
				proc = ProcessUtil.exec("ifconfig");
				BufferedReader br = null;
				try {
					proc.waitFor();
					br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
					String line = null;
					while ((line = br.readLine()) != null) {
						if (line.startsWith(primaryNetworkInterfaceName)) {
							// get the next line and save the MAC
							line = br.readLine();
							if (line == null) {
								throw new IOException("Null imput!");
							}
							if (!line.trim().startsWith("ether")) {
								line = br.readLine();
							}
							String[] splitLine = line.split(" ");
							if (splitLine.length > 0) {
								return splitLine[1].toUpperCase();
							}
						}
					}
				} catch (InterruptedException e) {
					s_logger.error("Exception while executing ifconfig!", e);
				} finally {
					if (br != null) {
						try {
							br.close();
						} catch (IOException ex) {
							s_logger.error("I/O Exception while closing BufferedReader!");
						}
					}
				}
			} catch (Exception e) {
				s_logger.error("Failed to get network interfaces", e);
			} finally {
				if (proc != null) {
					ProcessUtil.destroy(proc);
				}
			}
		} else {
			try {
				List<NetInterface<? extends NetInterfaceAddress>> interfaces = m_networkService.getNetworkInterfaces();
				if (interfaces != null) {
					for (NetInterface<? extends NetInterfaceAddress> iface : interfaces) {
						if (iface.getName() != null && getPrimaryNetworkInterfaceName().equals(iface.getName())) {
							macAddress = NetUtil.hardwareAddressToString(iface.getHardwareAddress());
							break;
						}
					}
				}
			} catch (KuraException e) {
				s_logger.error("Failed to get network interfaces", e);
			}
		}

		return macAddress;
	}

	@Override
	public String getPrimaryNetworkInterfaceName() {
		if (getProperty(KEY_PRIMARY_NET_IFACE) != null) {
			return getProperty(KEY_PRIMARY_NET_IFACE);
		} else {
			if (OS_MAC_OSX.equals(getOsName())) {
				return "en0";
			} else if (OS_LINUX.equals(getOsName())) {
				return "eth0";
			} else {
				s_logger.error("Unsupported platform");
				return null;
			}
		}
	}

	@Override
	public String getPlatform() {
		return getProperty(KEY_PLATFORM);
	}

	@Override
	public String getOsArch() {
		String override = getProperty(KEY_OS_ARCH);
		if (override != null)
			return override;

		return System.getProperty(KEY_OS_ARCH);
	}

	@Override
	public String getOsName() {
		String override = getProperty(KEY_OS_NAME);
		if (override != null)
			return override;

		return System.getProperty(KEY_OS_NAME);
	}

	@Override
	public String getOsVersion() {
		String override = getProperty(KEY_OS_VER);
		if (override != null) {
			return override;
		}

		StringBuilder sbOsVersion = new StringBuilder();
		sbOsVersion.append(System.getProperty(KEY_OS_VER));
		if (OS_LINUX.equals(getOsName())) {
			BufferedReader in = null;
			File linuxKernelVersion = null;
			FileReader fr = null;
			try {
				linuxKernelVersion = new File("/proc/sys/kernel/version");
				if (linuxKernelVersion.exists()) {
					StringBuilder kernelVersionData = new StringBuilder();
					fr = new FileReader(linuxKernelVersion);
					in = new BufferedReader(fr);
					String tempLine = null;
					while ((tempLine = in.readLine()) != null) {
						kernelVersionData.append(" ");
						kernelVersionData.append(tempLine);
					}
					sbOsVersion.append(kernelVersionData.toString());
				}
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			} finally {
				try {
					if (fr != null) {
						fr.close();
					}
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
					s_logger.error("Exception while closing resources!", e);
				}
			}
		}

		return sbOsVersion.toString();
	}

	@Override
	public String getOsDistro() {
		return getProperty(KEY_OS_DISTRO);
	}

	@Override
	public String getOsDistroVersion() {
		return getProperty(KEY_OS_DISTRO_VER);
	}

	@Override
	public String getJavaVendor() {
		String override = getProperty(KEY_JAVA_VENDOR);
		if (override != null) {
			return override;
		}

		return System.getProperty(KEY_JAVA_VENDOR);
	}

	@Override
	public String getJavaVersion() {
		String override = getProperty(KEY_JAVA_VERSION);
		if (override != null) {
			return override;
		}

		return System.getProperty(KEY_JAVA_VERSION);
	}

	@Override
	public String getJavaVmName() {
		String override = getProperty(KEY_JAVA_VM_NAME);
		if (override != null) {
			return override;
		}

		return System.getProperty(KEY_JAVA_VM_NAME);
	}

	@Override
	public String getJavaVmVersion() {
		String override = getProperty(KEY_JAVA_VM_VERSION);
		if (override != null) {
			return override;
		}

		return System.getProperty(KEY_JAVA_VM_VERSION);
	}

	@Override
	public String getJavaVmInfo() {
		String override = getProperty(KEY_JAVA_VM_INFO);
		if (override != null) {
			return override;
		}

		return System.getProperty(KEY_JAVA_VM_INFO);
	}

	@Override
	public String getOsgiFwName() {
		String override = getProperty(KEY_OSGI_FW_NAME);
		if (override != null) {
			return override;
		}

		return System.getProperty(KEY_OSGI_FW_NAME);
	}

	@Override
	public String getOsgiFwVersion() {
		String override = getProperty(KEY_OSGI_FW_VERSION);
		if (override != null) {
			return override;
		}

		return System.getProperty(KEY_OSGI_FW_VERSION);
	}

	@Override
	public int getNumberOfProcessors() {
		try {
			return Runtime.getRuntime().availableProcessors();
		} catch (Throwable t) {
			// NoSuchMethodError on pre-1.4 runtimes
		}
		return -1;
	}

	@Override
	public long getTotalMemory() {
		return Runtime.getRuntime().totalMemory() / 1024;
	}

	@Override
	public long getFreeMemory() {
		return Runtime.getRuntime().freeMemory() / 1024;
	}

	@Override
	public String getFileSeparator() {
		String override = getProperty(KEY_FILE_SEP);
		if (override != null) {
			return override;
		}

		return System.getProperty(KEY_FILE_SEP);
	}

	@Override
	public String getJavaHome() {
		String override = getProperty(KEY_JAVA_HOME);
		if (override != null) {
			return override;
		}

		return System.getProperty(KEY_JAVA_HOME);
	}

	public String getKuraName() {
		return getProperty(KEY_KURA_NAME);
	}

	@Override
	public String getKuraVersion() {
		return getProperty(KEY_KURA_VERSION);
	}

	@Override
	public String getKuraHome() {
		/*
		 * The only real user of this method is SystemServiceImpl itself 
		 */
		throw new UnsupportedOperationException("'kura.home' is no longer support. Please update your implementation to use getDataLocation() or getConfiguration() using SystemConfigurationService");
	}

	@Override
	public String getKuraDataDirectory() {
		return this.systemConfigurationService.getDataLocation().getAbsolutePath();
	}

	@Override
	public String getKuraTemporaryConfigDirectory() {
		return this.systemConfigurationService.getTempLocation ().getAbsolutePath();
	}

	@Override
	public String getKuraSnapshotsDirectory() {
		String snapshots = this.systemConfigurationService.getProperty("kura.snapshots");
		if ( snapshots != null && !snapshots.isEmpty() ) {
			return snapshots;
		}
		return systemConfigurationService.getDataLocation("snapshots").getAbsolutePath();
	}

	@Override
	public int getKuraSnapshotsCount() {
		int iMaxCount = 10;
		String maxCount = getProperty(KEY_KURA_SNAPSHOTS_COUNT);
		if (maxCount != null && maxCount.trim().length() > 0) {
			try {
				iMaxCount = Integer.parseInt(maxCount);
			} catch (NumberFormatException nfe) {
				s_logger.error("Error - Invalid kura.snapshots.count setting. Using default.", nfe);
			}
		}
		return iMaxCount;
	}

	@Override
	public int getKuraWifiTopChannel() {
		String topWifiChannel = getProperty(KEY_KURA_WIFI_TOP_CHANNEL);
		if (topWifiChannel != null && topWifiChannel.trim().length() > 0) {
			return Integer.parseInt(topWifiChannel);
		}

		s_logger.warn("The last wifi channel is not defined for this system - setting to lowest common value of 11");
		return 11;
	}

	@Override
	public String getKuraStyleDirectory() {
		// FIXME: provide solution
		return getProperty(KEY_KURA_STYLE_DIR);
	}

	@Override
	public String getKuraWebEnabled() {
		return getProperty(KEY_KURA_HAVE_WEB_INTER);
	}

	@Override
	public int getFileCommandZipMaxUploadSize() {
		String commandMaxUpload = getProperty(KEY_FILE_COMMAND_ZIP_MAX_SIZE);
		if (commandMaxUpload != null && commandMaxUpload.trim().length() > 0) {
			return Integer.parseInt(commandMaxUpload);
		}
		s_logger.warn("Maximum command line upload size not available. Set default to 100 MB");
		return 100;
	}

	@Override
	public int getFileCommandZipMaxUploadNumber() {
		String commandMaxFilesUpload = getProperty(KEY_FILE_COMMAND_ZIP_MAX_NUMBER);
		if (commandMaxFilesUpload != null && commandMaxFilesUpload.trim().length() > 0) {
			return Integer.parseInt(commandMaxFilesUpload);
		}
		s_logger.warn(
				"Missing the parameter that specifies the maximum number of files uploadable using the command servlet. Set default to 1024 files");
		return 1024;
	}

	@Override
	public String getBiosVersion() {
		String override = getProperty(KEY_BIOS_VERSION);
		if (override != null) {
			return override;
		}

		String biosVersion = UNSUPPORTED;

		if (OS_LINUX.equals(this.getOsName())) {
			if ("2.6.34.9-WR4.2.0.0_standard".equals(getOsVersion())
					|| "2.6.34.12-WR4.3.0.0_standard".equals(getOsVersion())) {
				biosVersion = runSystemInfoCommand("eth_vers_bios");
			} else {
				String biosTmp = runSystemInfoCommand("dmidecode -s bios-version");
				if (biosTmp.length() > 0 && !biosTmp.contains("Permission denied")) {
					biosVersion = biosTmp;
				}
			}
		} else if (OS_MAC_OSX.equals(this.getOsName())) {
			String[] cmds = { "/bin/sh", "-c", "system_profiler SPHardwareDataType | grep 'Boot ROM'" };
			String biosTmp = runSystemInfoCommand(cmds);
			if (biosTmp.contains(": ")) {
				biosVersion = biosTmp.split(":\\s+")[1];
			}
		}

		return biosVersion;
	}

	@Override
	public String getDeviceName() {
		String override = getProperty(KEY_DEVICE_NAME);
		if (override != null) {
			return override;
		}

		String deviceName = UNKNOWN;
		if (OS_MAC_OSX.equals(this.getOsName())) {
			String displayTmp = runSystemInfoCommand("scutil --get ComputerName");
			if (displayTmp.length() > 0) {
				deviceName = displayTmp;
			}
		} else if (OS_LINUX.equals(this.getOsName()) || OS_CLOUDBEES.equals(this.getOsName())) {
			String displayTmp = runSystemInfoCommand("hostname");
			if (displayTmp.length() > 0) {
				deviceName = displayTmp;
			}
		}
		return deviceName;
	}

	@Override
	public String getFirmwareVersion() {
		String override = getProperty(KEY_FIRMWARE_VERSION);
		if (override != null) {
			return override;
		}

		String fwVersion = UNSUPPORTED;

		if (OS_LINUX.equals(getOsName()) && (getOsVersion() != null)) {
			if (getOsVersion().startsWith("2.6.34.9-WR4.2.0.0_standard")
					|| getOsVersion().startsWith("2.6.34.12-WR4.3.0.0_standard")) {
				fwVersion = runSystemInfoCommand("eth_vers_cpld") + " " + runSystemInfoCommand("eth_vers_uctl");
			} else if (getOsVersion().startsWith("3.0.35-12.09.01+yocto")) {
				fwVersion = runSystemInfoCommand("eth_vers_avr");
			}
		}
		return fwVersion;
	}

	@Override
	public String getModelId() {
		String override = getProperty(KEY_MODEL_ID);
		if (override != null) {
			return override;
		}

		String modelId = UNKNOWN;

		if (OS_MAC_OSX.equals(this.getOsName())) {
			String modelTmp = runSystemInfoCommand("sysctl -b hw.model");
			if (modelTmp.length() > 0) {
				modelId = modelTmp;
			}
		} else if (OS_LINUX.equals(this.getOsName())) {
			String modelTmp = runSystemInfoCommand("dmidecode -t system");
			if (modelTmp.contains("Version: ")) {
				modelId = modelTmp.split("Version:\\s+")[1].split("\n")[0];
			}
		}

		return modelId;
	}

	@Override
	public String getModelName() {
		String override = getProperty(KEY_MODEL_NAME);
		if (override != null) {
			return override;
		}

		String modelName = UNKNOWN;

		if (OS_MAC_OSX.equals(this.getOsName())) {
			String[] cmds = { "/bin/sh", "-c", "system_profiler SPHardwareDataType | grep 'Model Name'" };
			String modelTmp = runSystemInfoCommand(cmds);
			if (modelTmp.contains(": ")) {
				modelName = modelTmp.split(":\\s+")[1];
			}
		} else if (OS_LINUX.equals(this.getOsName())) {
			String modelTmp = runSystemInfoCommand("dmidecode -t system");
			if (modelTmp.contains("Product Name: ")) {
				modelName = modelTmp.split("Product Name:\\s+")[1].split("\n")[0];
			}
		}

		return modelName;
	}

	@Override
	public String getPartNumber() {
		String override = getProperty(KEY_PART_NUMBER);
		if (override != null) {
			return override;
		}

		String partNumber = UNSUPPORTED;

		if (OS_LINUX.equals(this.getOsName())) {
			if ("2.6.34.9-WR4.2.0.0_standard".equals(getOsVersion())
					|| "2.6.34.12-WR4.3.0.0_standard".equals(getOsVersion())) {
				partNumber = runSystemInfoCommand("eth_partno_bsp") + " " + runSystemInfoCommand("eth_partno_epr");
			}
		}

		return partNumber;
	}

	@Override
	public String getSerialNumber() {
		String override = getProperty(KEY_SERIAL_NUM);
		if (override != null) {
			return override;
		}

		String serialNum = UNKNOWN;

		if (OS_MAC_OSX.equals(this.getOsName())) {
			String[] cmds = { "/bin/sh", "-c", "system_profiler SPHardwareDataType | grep 'Serial Number'" };
			String serialTmp = runSystemInfoCommand(cmds);
			if (serialTmp.contains(": ")) {
				serialNum = serialTmp.split(":\\s+")[1];
			}
		} else if (OS_LINUX.equals(this.getOsName())) {
			String serialTmp = runSystemInfoCommand("dmidecode -t system");
			if (serialTmp.contains("Serial Number: ")) {
				serialNum = serialTmp.split("Serial Number:\\s+")[1].split("\n")[0];
			}
		}

		return serialNum;
	}

	@Override
	public char[] getJavaKeyStorePassword() throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
		String keyStorePwd = getProperty(KEY_KURA_KEY_STORE_PWD);
		if (keyStorePwd != null) {
			return keyStorePwd.toCharArray();
		}
		return null;
	}

	@Override
	public char[] getJavaTrustStorePassword() throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
		String trustStorePwd = getProperty(KEY_KURA_TRUST_STORE_PWD);
		if (trustStorePwd != null) {
			return trustStorePwd.toCharArray();
		}
		return null;
	}

	@Override
	public Bundle[] getBundles() {
		if (m_ctx == null) {
			return null;
		}
		return m_ctx.getBundleContext().getBundles();
	}

	@Override
	public List<String> getDeviceManagementServiceIgnore() {
		String servicesToIgnore = getProperty(CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE);
		if (servicesToIgnore != null && !servicesToIgnore.trim().isEmpty()) {
			String[] servicesArray = servicesToIgnore.split(",");
			if (servicesArray != null && servicesArray.length > 0) {
				List<String> services = new ArrayList<String>();
				for (String service : servicesArray) {
					services.add(service);
				}
				return services;
			}
		}

		return null;
	}

	// ----------------------------------------------------------------
	//
	// Private Methods
	//
	// ----------------------------------------------------------------

	private String runSystemInfoCommand(String command) {
		return runSystemInfoCommand(command.split("\\s+"));
	}

	private static String runSystemInfoCommand(String[] commands) {
		StringBuffer response = new StringBuffer();
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			proc = ProcessUtil.exec(commands);
			proc.waitFor();
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			String newLine = "";
			while ((line = br.readLine()) != null) {
				response.append(newLine);
				response.append(line);
				newLine = "\n";
			}
		} catch (Exception e) {
			StringBuilder command = new StringBuilder();
			String delim = "";
			for (int i = 0; i < commands.length; i++) {
				command.append(delim);
				command.append(commands[i]);
				delim = " ";
			}
			s_logger.error("failed to run commands " + command.toString(), e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ex) {
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
		return response.toString();
	}

	private static void createDirIfNotExists(String fileName) {
		// Make sure the configuration directory exists - create it if not
		File file = new File(fileName);
		if (!file.exists() && !file.mkdirs()) {
			s_logger.error("Failed to create the temporary configuration directory: " + fileName);
			System.exit(-1);
		}
	}
}
