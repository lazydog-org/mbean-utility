package org.lazydog.mbean.utilities;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;


/**
 * Remote MBean invocation handler.
 *
 * @author  Ron Rickard
 */
public class RemoteMBeanInvocationHandler<T> implements InvocationHandler {

    private Class<T> interfaceClass;
    private String host;
    private String login;
    private ObjectName objectName;
    private String password;
    private String port;
    
    /**
     * Create a remote MBean invocation handler.
     *
     * @param  interfaceClass  the MBean interface class.
     * @param  objectName      the MBean object name.
     * @param  host            the JMX host.
     * @param  port            the JMX port.
     * @param  login           the JMX login.
     * @param  password        the JMX password;
     */
    private RemoteMBeanInvocationHandler(Class<T> interfaceClass, 
            ObjectName objectName, String host, String port, String login,
            String password) {
        this.interfaceClass = interfaceClass;
        this.objectName = objectName;
        this.host = host;
        this.port = port;
        this.login = login;
        this.password = password;
    }

    /**
     * Close the JMX service.
     *
     * @param  connector  the JMX connector.
     */
    private static void close(JMXConnector connector) {

        try {

            // Check to see if the JMX connector exists.
            if (connector != null) {

                // Close the connection to the JMX service.
                connector.close();
            }
        }
        catch(IOException e) {
            // Ignore.
        }
    }

    /**
     * Connect to the JMX service.
     *
     * @param  host      the host.
     * @param  port      the port.
     * @param  login     the login.
     * @param  password  the password.
     *
     * @return  the JMX connector.
     *
     * @throws  IOException  if unable to connect to the JMX service.
     */
    private static JMXConnector connect(String host, String port,
            String login, String password) throws IOException {

        // Declare.
        JMXConnector connector;
        Map<String,Object> serviceEnv;
        JMXServiceURL serviceUrl;

        // Set the service environment.
        serviceEnv = new HashMap<String,Object>();
        serviceEnv.put(
                "jmx.remote.credentials",
                new String[]{login, password});

        // Set the service URL.
        serviceUrl = new JMXServiceURL(
                new StringBuffer()
                    .append("service:jmx:rmi://")
                    .append(host)
                    .append(":")
                    .append(port)
                    .append("/jndi/rmi://")
                    .append(host)
                    .append(":")
                    .append(port)
                    .append("/jmxrmi")
                    .toString());

        // Connect to the JMX service.
        connector = JMXConnectorFactory.connect(serviceUrl, serviceEnv);

        return connector;
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
    @SuppressWarnings("unchecked")
    public static <T> T getMBean(Class<T> interfaceClass, ObjectName objectName, 
            Properties environment) throws MBeanException {

        // Declare.
        JMXConnector connector;
        String host;
        String port;
        String login;
        String password;

        // Initialize.
        connector = null;

        // Get the JMX environment properties.
        host = getProperty(environment, MBeanUtility.JMX_HOST_KEY);
        port = getProperty(environment, MBeanUtility.JMX_PORT_KEY);
        login = getProperty(environment, MBeanUtility.JMX_LOGIN_KEY);
        password = getProperty(environment, MBeanUtility.JMX_PASSWORD_KEY);

        try {

            // Declare.
            MBeanServerConnection mBeanServerConnection;

            // Connect to the JMX service.
            connector = connect(host, port, login, password);

            // Connect to the MBean server.
            mBeanServerConnection = connector.getMBeanServerConnection();

            // Validate the MBean.
            MBeanUtility.validateMBean(interfaceClass, objectName, mBeanServerConnection);
        }
        catch(IOException e) {
            throw new MBeanException(e,
                    "Unable to create the MBean represented by the interface class " +
                    interfaceClass.getName() + " and object name " +
                    objectName.getCanonicalName() + ".");
        }
        finally {
            close(connector);
        }

        // Create the MBean.
        return (T)Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class[]{interfaceClass},
                new RemoteMBeanInvocationHandler<T>(interfaceClass, objectName,
                host, port, login, password));
    }

    /**
     * Get the property.
     *
     * @param  properties  the properties.
     * @param  key         the property key.
     *
     * @return  the property.
     *
     * @throws  IllegalArgumentException  if the property does not exist.
     */
    private static String getProperty(Properties properties, String key) {

        // Check if the properties do not exist.
        if (properties == null || properties.getProperty(key) == null) {
            throw new IllegalArgumentException("The property " + key + " does not exist.");
        }

        return properties.getProperty(key);
    }

    /**
     * Invoke the method and return the result.
     *
     * @param  proxy   the proxy.
     * @param  method  the method.
     * @param  args    the method arguments.
     *
     * @return  the result of invoking the method.
     *
     * @throws  Throwable  if unable to invoke the method.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // Declare.
        JMXConnector connector;
        T object;
        Object result;

        // Initialize.
        connector = null;

        try {

            // Connect to the JMX service.
            connector = connect(host, port, login, password);

            // Create the MBean.
            object = JMX.newMXBeanProxy(
                    connector.getMBeanServerConnection(),
                    objectName,
                    interfaceClass);

            // Invoke a method on the MBean.
            result = method.invoke(object, args);
        }
        finally {

            // Close the JMX service.
            close(connector);
        }

        return result;
    }
}
