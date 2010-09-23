package org.lazydog.mbean.utilities;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
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
     * Get the MBean represented by the interface class.  The object name is
     * set to the object name returned by the getObjectName method.
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
    public static <T> T getMBean(Class<T> interfaceClass, ObjectName objectName)
            throws MBeanException {

        // Declare.
        MBeanServer mBeanServer;

        // Get the MBean server.
        mBeanServer = ManagementFactory.getPlatformMBeanServer();

        validateMBean(interfaceClass, objectName, mBeanServer);

        // Create the MBean.
        return JMX.newMXBeanProxy(mBeanServer, objectName, interfaceClass);
    }

    /**
     * Get the MBean represented by the interface class.  The object name is
     * set to the object name returned by the getObjectName method.
     *
     * @param  interfaceClass  the MBean interface class.
     * @param  environment     the JMX environment properties.
     *
     * @return  the MBean.
     *
     * @throws  MBeanException  if unable to get the MBean.
     */
    public static <T> T getMBean(Class<T> interfaceClass,
            Properties environment) throws MBeanException {
        return getMBean(interfaceClass, getObjectName(interfaceClass));
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
    public static <T> T getMBean(Class<T> interfaceClass, ObjectName objectName, 
            Properties environment) throws MBeanException {
        return RemoteMBeanInvocationHandler.getMBean(
                interfaceClass,
                objectName,
                environment);
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

        // Declare.
        ObjectName objectName;

        // Check if the interface class is valid.
        if (interfaceClass == null) {
            throw new IllegalArgumentException("The interface class is invalid.");
        }

        try {

            objectName = ObjectName.getInstance(
                    interfaceClass.getPackage().getName(),
                    "type",
                    interfaceClass.getSimpleName());
        }
        catch(MalformedObjectNameException e) {
            throw new IllegalArgumentException(
                    "Unable to get the MBean object name for "
                    + interfaceClass.getName() + ".", e);
        }

        return objectName;
    }

    /**
     * Register the MBean represented by the interface class.  The object name 
     * is set to the object name returned by the getObjectName method.
     *
     * @param  interfaceClass  the MBean interface class.
     *
     * @throws  MBeanException  if unable to register the MBean.
     */
    public static void register(Class interfaceClass) throws MBeanException {
        register(interfaceClass, getObjectName(interfaceClass));
    }

    /**
     * Register the MBean represented by the interface class and object name.
     *
     * @param  interfaceClass  the MBean interface class.
     * @param  objectName      the MBean object name.
     *
     * @throws  IllegalArgumentException  if the interface class is invalid.
     * @throws  MBeanException            if unable to register the MBean.
     */
    public static void register(Class interfaceClass, ObjectName objectName)
            throws MBeanException {

        // Check if the interface class is valid.
        if (interfaceClass == null) {
            throw new IllegalArgumentException("The interface class is invalid.");
        }

        try {

            // Declare.
            MBeanServer mBeanServer;

            // Get the MBean server.
            mBeanServer = ManagementFactory.getPlatformMBeanServer();

            // Check if the MBean is not registered with the MBean server.
            if (!mBeanServer.isRegistered(objectName)) {

                // Register the MBean with the MBean server.
                mBeanServer.createMBean(interfaceClass.getName(), objectName);
            }
        }
        catch(JMException e) {
            // ReflectionException
            // InstanceAlreadyExistsException
            // MBeanRegistrationException
            // MBeanException
            // NotCompliantMBeanException
            throw new MBeanException(e, "Unable to register the MBean.");
        }
    }

    /**
     * Unregister the MBean(s) represented by the interface class.
     *
     * @param  interfaceClass  the MBean interface class.
     *
     * @throws  IllegalArgumentException  if the interface class is invalid.
     * @throws  MBeanException            if unable to unregister the MBean(s).
     */
    public static void unregister(Class interfaceClass) throws MBeanException {

        // Declare.
        MBeanServer mBeanServer;
        Set<ObjectInstance> objectInstances;
        Set<ObjectName> objectNames;

        // Check if the interface class is valid.
        if (interfaceClass == null) {
            throw new IllegalArgumentException("The interface class is invalid.");
        }

        // Initialize the object names.
        objectNames = new HashSet<ObjectName>();

        // Get the MBean server.
        mBeanServer = ManagementFactory.getPlatformMBeanServer();

        // Get the registered object instances (interface class names and object name pairs).
        objectInstances = mBeanServer.queryMBeans(null, null);

        // Loop through the object instances.
        for (ObjectInstance objectInstance : objectInstances) {

            // Check if the object instance has the interface class.
            if (objectInstance.getClassName().equals(interfaceClass.getName())) {

                // Add the object name to the set.
                objectNames.add(objectInstance.getObjectName());
            }
        }

        // Loop through the object names.
        for (ObjectName objectName : objectNames) {

            // Unregister the MBean.
            unregister(objectName);
        }
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

            // Declare.
            MBeanServer mBeanServer;

            // Get the MBean server.
            mBeanServer = ManagementFactory.getPlatformMBeanServer();

            // Check if the MBean is registered with the MBean server.
            if (mBeanServer.isRegistered(objectName)) {

                // Unregister the MBean with the MBean server.
                mBeanServer.unregisterMBean(objectName);
            }
        }
        catch(Exception e) {
            // InstanceNotFoundException
            // MBeanRegistrationException
            throw new MBeanException(e, "Unable to unregister the MBean.");
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
    protected static void validateMBean(Class interfaceClass, ObjectName objectName,
            MBeanServerConnection mBeanServerConnection)  throws MBeanException {

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
        }
        catch(InstanceNotFoundException e) {
            throw new IllegalArgumentException(
                    "The object name " + objectName.getCanonicalName() +
                    " is not found.");
        }
        catch(IOException e) {
            throw new MBeanException(e,
                    "Unable to validate the MBean represented by the interface class " +
                    interfaceClass.getName() + " and object name " +
                    objectName.getCanonicalName() + ".");
        }
    }
}
