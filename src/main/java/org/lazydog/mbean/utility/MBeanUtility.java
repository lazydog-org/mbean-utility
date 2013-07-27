/**
 * Copyright 2010-2013 lazydog.org.
 *
 * This file is part of repository.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.lazydog.mbean.utility;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.Properties;
import java.util.ServiceLoader;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

/**
 * MBean utility.
 *
 * @author  Ron Rickard
 */
public final class MBeanUtility {

    public static final String JMX_HOST_KEY = "org.lazydog.mbean.jmxHost";
    public static final String JMX_PORT_KEY = "org.lazydog.mbean.jmxPort";
    public static final String JMX_LOGIN_KEY = "org.lazydog.mbean.jmxLogin";
    public static final String JMX_PASSWORD_KEY = "org.lazydog.mbean.jmxPassword";

    /**
     * Create the MBean object.
     *
     * @param  interfaceClass  the MBean interface class.
     *
     * @return  the MBean object.
     */
    private static <T> T createObject(Class<T> interfaceClass) {

        ServiceLoader<T> loader = ServiceLoader.load(interfaceClass);

        T object = null;
        
        // Loop through the services.
        for (T loadedObject : loader) {

            // Check if a factory has not been found.
            if (object == null) {

                // Set the factory.
                object = loadedObject;
            } else {
                throw new IllegalArgumentException("More than one MBean object found.");
            }
        }

        // Check if a object has not been found.
        if (object == null) {
            throw new IllegalArgumentException("No MBean object found.");
        }

        return object;
    }

    /**
     * Get the MBean represented by the interface class.  The object name is
     * set to the object name returned by the getObjectName(Class interfaceClass) method.
     *
     * @param  interfaceClass  the MBean interface class.
     *
     * @return  the MBean.
     *
     * @throws  MBeanException  if unable to get the MBean.
     */
    public static <T> T getMBean(Class<T> interfaceClass) throws MBeanException {
        return getMBean(interfaceClass, getObjectName(interfaceClass));
    }

    /**
     * Get the MBean represented by the interface class and object name.
     *
     * @param  interfaceClass  the MBean interface class.
     * @param  objectName      the MBean object name.
     *
     * @return  the MBean.
     *
     * @throws  MBeanException  if unable to get the MBean.
     */
    public static <T> T getMBean(Class<T> interfaceClass, ObjectName objectName) throws MBeanException {

        // Get the MBean server.
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        validateMBean(interfaceClass, objectName, mBeanServer);

        // Create the MBean.
        return JMX.newMXBeanProxy(mBeanServer, objectName, interfaceClass);
    }

    /**
     * Get the MBean represented by the interface class.  The object name is
     * set to the object name returned by the getObjectName(Class interfaceClass) method.
     *
     * @param  interfaceClass  the MBean interface class.
     * @param  environment     the JMX environment properties.
     *
     * @return  the MBean.
     *
     * @throws  MBeanException  if unable to get the MBean.
     */
    public static <T> T getMBean(Class<T> interfaceClass, Properties environment) throws MBeanException {
        return getMBean(interfaceClass, getObjectName(interfaceClass), environment);
    }

    /**
     * Get the MBean represented by the interface class and object name.
     *
     * @param  interfaceClass  the MBean interface class.
     * @param  objectName      the MBean object name.
     * @param  environment     the JMX environment properties.
     *
     * @return  the MBean.
     *
     * @throws  MBeanException  if unable to get the MBean.
     */
    public static <T> T getMBean(Class<T> interfaceClass, ObjectName objectName, Properties environment) throws MBeanException {
        return RemoteMBeanInvocationHandler.getMBean(interfaceClass, objectName, environment);
    }

    /**
     * Get the MBean object name.
     *
     * @param  interfaceClass  the MBean interface class.
     *
     * @return  the MBean object name.
     *
     * @throws  IllegalArgumentException  if the interface class is invalid or
     *                                    unable to get the MBean object name.
     */
    public static ObjectName getObjectName(Class interfaceClass) {
        return getObjectName(interfaceClass, null);
    }

    /**
     * Get the MBean object name.
     *
     * @param  interfaceClass  the MBean interface class.
     * @param  key             the property key.
     * @param  value           the property value.
     *
     * @return  the MBean object name.
     *
     * @throws  IllegalArgumentException  if the interface class is invalid or
     *                                    unable to get the MBean object name.
     */
    public static ObjectName getObjectName(Class interfaceClass, String key, String value) {

        // Set the table.
        Hashtable<String,String> table = new Hashtable<String,String>();
        table.put(key, value);

        return getObjectName(interfaceClass, table);
    }

    /**
     * Get the MBean object name.
     *
     * @param  interfaceClass  the MBean interface class.
     * @param  table           the properties table.
     *
     * @return  the MBean object name.
     *
     * @throws  IllegalArgumentException  if the interface class is invalid or
     *                                    unable to get the MBean object name.
     */
    public static ObjectName getObjectName(Class interfaceClass, Hashtable<String,String> table) {

        

        // Check if the interface class is valid.
        if (interfaceClass == null) {
            throw new IllegalArgumentException("The interface class is invalid.");
        }

        ObjectName objectName;
        
        try {

            // Check if the table is null.
            if (table == null) {

                // Initialize the table.
                table = new Hashtable<String,String>();
            }

            // Add the "type" to the table.
            table.put("type", interfaceClass.getSimpleName());

            // Get the MBean object name.
            objectName = ObjectName.getInstance(interfaceClass.getPackage().getName(), table);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(
                    "Unable to get the MBean object name for "
                    + interfaceClass.getName() + ".", e);
        }

        return objectName;
    }

    /**
     * Register the MBean represented by the interface class.  The MBean object
     * name is set to the object name returned by the
     * getObjectName(Class interfaceClass) method.
     *
     * @param  interfaceClass  the MBean interface class.
     *
     * @return  the MBean object name.
     *
     * @throws  IllegalArgumentException  if the interface class is invalid or
     *                                    unable to get the MBean object name.
     * @throws  MBeanException            if unable to register the MBean.
     */
    public static <T> ObjectName register(Class<T> interfaceClass) throws MBeanException {
        return register(interfaceClass, getObjectName(interfaceClass));
    }

    /**
     * Register the MBean represented by the interface class.  The MBean object 
     * name is set to the object name returned by the 
     * getObjectName(Class interfaceClass, String key, String value) method.
     *
     * @param  interfaceClass  the MBean interface class.
     * @param  key             the property key.
     * @param  value           the property value.
     *
     * @return  the MBean object name.
     *
     * @throws  IllegalArgumentException  if the interface class is invalid or
     *                                    unable to get the MBean object name.
     * @throws  MBeanException            if unable to register the MBean.
     */
    public static <T> ObjectName register(Class<T> interfaceClass, String key, String value) throws MBeanException {
        return register(interfaceClass, getObjectName(interfaceClass, key, value));
    }

    /**
     * Register the MBean represented by the interface class.  The MBean object
     * name is set to the object name returned by the
     * getObjectName(Class interfaceClass, Hashtable<String,String> table)
     * method.
     *
     * @param  interfaceClass  the MBean interface class.
     * @param  table           the properties table.
     *
     * @return  the MBean object name.
     *
     * @throws  IllegalArgumentException  if the interface class is invalid or
     *                                    unable to get the MBean object name.
     * @throws  MBeanException            if unable to register the MBean.
     */
    public static <T> ObjectName register(Class<T> interfaceClass, Hashtable<String,String> table) throws MBeanException {
        return register(interfaceClass, getObjectName(interfaceClass, table));
    }

    /**
     * Register the MBean represented by the interface class.
     *
     * @param  interfaceClass  the MBean interface class.
     * @param  objectName      the MBean object name.
     *
     * @return  the MBean object name.
     *
     * @throws  IllegalArgumentException  if the interface class is invalid.
     * @throws  MBeanException            if unable to register the MBean.
     */
    public static <T> ObjectName register(Class<T> interfaceClass, ObjectName objectName) throws MBeanException {

        // Check if the interface class is valid.
        if (interfaceClass == null) {
            throw new IllegalArgumentException("The interface class is invalid.");
        }

        try {

            // Get the MBean server.
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

            // Check if the MBean is not registered with the MBean server.
            if (!mBeanServer.isRegistered(objectName)) {

                // Register the MBean with the MBean server.
                ObjectInstance objectInstance = mBeanServer.registerMBean(createObject(interfaceClass), objectName);

                // Get the object name for the registered MBean.
                objectName = objectInstance.getObjectName();
            }
        } catch (Exception e) {
            throw new MBeanException(e, "Unable to register the MBean.");
        }

        return objectName;
    }

    /**
     * Unregister the MBean represented by the object name.
     *
     * @param  objectName  the MBean object name.
     *
     * @throws  MBeanException  if unable to unregister the MBean.
     */
    public static void unregister(ObjectName objectName) throws MBeanException {

        try {

            // Get the MBean server.
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

            // Check if the MBean is registered with the MBean server.
            if (mBeanServer.isRegistered(objectName)) {

                // Unregister the MBean with the MBean server.
                mBeanServer.unregisterMBean(objectName);
            }
        } catch (Exception e) {
            throw new MBeanException(e, 
                    "Unable to unregister the MBean " +
                    objectName.getCanonicalName() + ".");
        }
    }

    /**
     * Validate the MBean represented by the interface class and object name.
     *
     * @param  interfaceClass         the MBean interface class.
     * @param  objectName             the MBean object name.
     * @param  mBeanServerConnection  the MBean server connection.
     *
     * @throws  IllegalArgumentException  if the MBean is invalid.
     * @throws  IOException               if unable to validate the MBean.
     */
    protected static void validateMBean(Class interfaceClass, ObjectName objectName, MBeanServerConnection mBeanServerConnection)  throws MBeanException {

        try {

            // Check if the interface class is null.
            if (interfaceClass == null) {
                throw new IllegalArgumentException(
                        "The interface class is null.");
            }
            
            // Check if the interface class is not a MXBean interface.
            if (!JMX.isMXBeanInterface(interfaceClass)) {
                throw new IllegalArgumentException(
                        "The interface class " + interfaceClass.getName() +
                        " is not a MXBean interface.");
            }

            // Check if the object name is not registered.
            if (!mBeanServerConnection.isRegistered(objectName)) {
                throw new IllegalArgumentException(
                        "The object name " + objectName.getCanonicalName() +
                        " is not registered.");
            }

            // Check if the object name is not an instance of the interface class.
            if (!mBeanServerConnection.isInstanceOf(objectName, interfaceClass.getName())) {
                throw new IllegalArgumentException(
                        "The object name " + objectName.getCanonicalName() +
                        " is not an instance of the interface class " +
                        interfaceClass.getName() + ".");
            }
        } catch (InstanceNotFoundException e) {
            throw new IllegalArgumentException(
                    "The object name " + objectName.getCanonicalName() +
                    " is not found.");
        } catch (IOException e) {
            throw new MBeanException(e,
                    "Unable to validate the MBean represented by the interface class " +
                    interfaceClass.getName() + " and object name " +
                    objectName.getCanonicalName() + ".");
        }
    }
}
