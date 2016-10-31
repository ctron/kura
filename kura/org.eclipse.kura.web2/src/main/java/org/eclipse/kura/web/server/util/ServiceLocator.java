/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix generic types, Fix issue #599
 *******************************************************************************/
package org.eclipse.kura.web.server.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class ServiceLocator {

    private static final ServiceLocator s_instance = new ServiceLocator();

    private ServiceLocator() {
    }

    public static ServiceLocator getInstance() {
        return s_instance;
    }

    public <T> ServiceReference<T> getServiceReference(Class<T> serviceClass) throws GwtKuraException {
        BundleContext bundleContext = Console.getBundleContext();
        ServiceReference<T> sr = null;
        if (bundleContext != null) {
            sr = bundleContext.getServiceReference(serviceClass);
        }
        if (sr == null) {
            throw GwtKuraException.internalError(serviceClass.toString() + " not found.");
        }
        return sr;
    }

    public <T> Collection<ServiceReference<T>> getServiceReferences(Class<T> serviceClass, String filter) throws GwtKuraException {
        final BundleContext bundleContext = Console.getBundleContext();
        Collection<ServiceReference<T>> sr = null;
        if (bundleContext != null) {
            try {
                sr = bundleContext.getServiceReferences(serviceClass, filter);
            } catch (InvalidSyntaxException e) {
                throw GwtKuraException.internalError("Getting service references failed.");
            }
        }
        if (sr == null) {
            throw GwtKuraException.internalError(serviceClass.toString() + " not found.");
        }
        return sr;
    }

    public <T> T getService(Class<T> serviceClass) throws GwtKuraException {
        T service = null;

        ServiceReference<T> sr = getServiceReference(serviceClass);
        if (sr != null) {
            service = getService(sr);
        }
        return service;
    }

    public interface ServiceFunction<T, R, E extends Throwable> {
        public R apply(T service) throws E;
    }

    /**
     * Locate a service and execute the provided function
     * <p>
     * The function will also be called if the service could not be found. It will be called with a {@code null}
     * argument in that case.
     * </p>
     *
     * @param serviceClass
     *            the service to locate
     * @param function
     *            the function to execute
     * @return the return value of the function
     */
    public static <T, R, E extends Throwable> R withOptionalService(final Class<T> serviceClass, final ServiceFunction<T, R, E> function) throws E {
        final BundleContext ctx = FrameworkUtil.getBundle(ServiceLocator.class).getBundleContext();
        final ServiceReference<T> ref = ctx.getServiceReference(serviceClass);
        if (ref == null) {
            return function.apply(null);
        }

        return withReference(ctx, ref, function);
    }

    /**
     * Locate a service and execute the provided function
     * <p>
     * If a service reference for the provided class could not be found an exception will be thrown instead
     * </p>
     *
     * @param serviceClass
     *            the service to locate
     * @param function
     *            the function to execute
     * @throws GwtKuraException
     *             if no service instance for this class could be found
     * @return the return value of the function
     */
    public static <T, R, E extends Throwable> R withService(final Class<T> serviceClass, final ServiceFunction<T, R, E> function) throws GwtKuraException, E {
        final BundleContext ctx = FrameworkUtil.getBundle(ServiceLocator.class).getBundleContext();
        final ServiceReference<T> ref = ctx.getServiceReference(serviceClass);

        if (ref == null) {
            throw GwtKuraException.internalError(String.format("No instance of %s found", serviceClass.getName()));
        }

        return withReference(ctx, ref, function);
    }

    /**
     * Execute with a provided service reference
     * 
     * @param ctx
     *            the bundle context to use
     * @param ref
     *            the service reference to use
     * @param function
     *            the function to execute
     *
     * @return the return value of the function
     */
    private static <T, R, E extends Throwable> R withReference(final BundleContext ctx, final ServiceReference<T> ref, final ServiceFunction<T, R, E> function) throws E {
        final T service = ctx.getService(ref);
        try {
            return function.apply(service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ctx.ungetService(ref);
        }
    }

    public <T> T getService(ServiceReference<T> serviceReference) throws GwtKuraException {
        T service = null;
        BundleContext bundleContext = Console.getBundleContext();
        if (bundleContext != null && serviceReference != null) {
            service = bundleContext.getService(serviceReference);
        }
        if (service == null) {
            throw GwtKuraException.internalError("Service not found.");
        }
        return service;
    }

    public <T> List<T> getServices(Class<T> serviceClass) throws GwtKuraException {
        return getServices(serviceClass, null);
    }

    public <T> List<T> getServices(Class<T> serviceClass, String filter) throws GwtKuraException {
        List<T> services = null;

        BundleContext bundleContext = Console.getBundleContext();
        if (bundleContext != null) {
            Collection<ServiceReference<T>> serviceReferences = getServiceReferences(serviceClass, filter);

            if (serviceReferences != null) {
                services = new ArrayList<T>(serviceReferences.size());
                for (ServiceReference<T> sr : serviceReferences) {
                    services.add(getService(sr));
                }
            }
        }

        return services;
    }

    public boolean ungetService(ServiceReference<?> serviceReference) {
        BundleContext bundleContext = Console.getBundleContext();
        if (bundleContext != null && serviceReference != null) {
            return bundleContext.ungetService(serviceReference);
        }
        return false;
    }
}
