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
 *     Jens Reimann <jreimann@redhat.com> - Clear warnings
 *******************************************************************************/
package org.eclipse.kura.emulator.usb;

import java.util.List;

import javax.usb.UsbServices;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.usb.UsbBlockDevice;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.component.ComponentContext;

public class UsbServiceImpl implements UsbService {
	
	protected void activate(ComponentContext componentContext) {
	}

	protected void deactivate(ComponentContext componentContext) {
	}

	@Override
	public UsbServices getUsbServices()
			throws KuraException {
		return null;
	}

	@Override
	public List<? extends UsbDevice> getUsbDevices() {
		return null;
	}

	@Override
	public List<UsbBlockDevice> getUsbBlockDevices() {
		return null;
	}

	@Override
	public List<UsbNetDevice> getUsbNetDevices() {
		return null;
	}

	@Override
	public List<UsbTtyDevice> getUsbTtyDevices() {
		return null;
	}

}
