/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Implementation of a Jvn server
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.HashMap;


public class JvnServerImpl
		extends UnicastRemoteObject
		implements JvnLocalServer, JvnRemoteServer {

	private static final long serialVersionUID = 1L;
	// A JVN server is managed as a singleton 
	private static JvnServerImpl js = null;

	private final JvnRemoteCoord coord;
	private final HashMap<Integer, JvnObject> objects = new HashMap<>();

	/**
	 * Default constructor
	 * @throws JvnException
	 **/
	private JvnServerImpl() throws Exception {
		super();
		Registry registry = LocateRegistry.getRegistry("localhost", 2001);
		coord = (JvnRemoteCoord) registry.lookup("Coord");
	}

	/**
	 * Static method allowing an application to get a reference to
	 * a JVN server instance
	 * @throws JvnException
	 **/
	public static JvnServerImpl jvnGetServer() throws JvnException {
		if (js == null){
			try {
				js = new JvnServerImpl();
			} catch (Exception e) {
				throw new JvnException(e.toString());
			}
		}
		return js;
	}

	/**
	 * The JVN service is not used anymore
	 * @throws JvnException
	 **/
	public void jvnTerminate() throws jvn.JvnException {
		try {
			coord.jvnTerminate(this);
		} catch (RemoteException e) {
			System.out.println("Connection to coord lost.");
			System.exit(1);
		}
	}

	/**
	 * creation of a JVN object
	 * @param o : the JVN object state
	 * @throws JvnException
	 **/
	public Object jvnCreateObject(Serializable o) throws jvn.JvnException {
		Object proxy = null;

		try {
			int joi = coord.jvnGetObjectId();
			proxy = JvnObjectImpl.newInstance(joi, o);
            JvnObject jo = (JvnObject) Proxy.getInvocationHandler(proxy);
			objects.put(joi, jo);
		} catch (RemoteException e) {
			System.out.println("Connection to coord lost.");
			System.exit(1);
		}
		return proxy;
	}

	/**
	 *  Associate a symbolic name with a JVN object
	 * @param jon : the JVN object name
	 * @param obj : the JVN object
	 * @throws JvnException
	 **/
	public void jvnRegisterObject(String jon, Object obj) throws jvn.JvnException {
		try {
            JvnObject jo = (JvnObject) Proxy.getInvocationHandler(obj);
			coord.jvnRegisterObject(jon, jo, this);
		} catch (RemoteException e) {
			System.out.println("Connection to coord lost.");
			System.exit(1);
		}
	}

	/**
	 * Provide the reference of a JVN object beeing given its symbolic name
	 * @param jon : the JVN object name
	 * @return the JVN object
	 * @throws JvnException
	 **/
	public Object jvnLookupObject(String jon) throws jvn.JvnException {
		Object proxy = null;

		try {
			JvnObject jo = coord.jvnLookupObject(jon, this);

			if (jo != null) {
				proxy = JvnObjectImpl.newInstance(jo);
				objects.put(jo.jvnGetObjectId(), (JvnObject) Proxy.getInvocationHandler(proxy));
			}
		} catch (RemoteException e) {
			System.out.println("Connection to coord lost.");
			System.exit(1);
		}
		return proxy;
	}

	/**
	 * Get a Read lock on a JVN object
	 * @param joi : the JVN object identification
	 * @return the current JVN object state
	 * @throws  JvnException
	 **/
	public Serializable jvnLockRead(int joi) throws JvnException {
		Serializable o = null;

		try {
			o = coord.jvnLockRead(joi, this);
		} catch (RemoteException e) {
			System.out.println("Connection to coord lost.");
			System.exit(1);
		}
		return o;
	}

	/**
	 * Get a Write lock on a JVN object
	 * @param joi : the JVN object identification
	 * @return the current JVN object state
	 * @throws  JvnException
	 **/
	public Serializable jvnLockWrite(int joi) throws JvnException {
		Serializable o = null;

		try {
			o = coord.jvnLockWrite(joi, this);
		} catch (RemoteException e) {
			System.out.println("Connection to coord lost.");
			System.exit(1);
		}
		return o;
	}


	/**
	 * Invalidate the Read lock of the JVN object identified by id
	 * called by the JvnCoord
	 * @param joi : the JVN object id
	 * @return void
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public void jvnInvalidateReader(int joi)
			throws java.rmi.RemoteException, jvn.JvnException {
		objects.get(joi).jvnInvalidateReader();
	}

	/**
	 * Invalidate the Write lock of the JVN object identified by id
	 * @param joi : the JVN object id
	 * @return the current JVN object state
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public Serializable jvnInvalidateWriter(int joi)
			throws java.rmi.RemoteException, jvn.JvnException {
		return objects.get(joi).jvnInvalidateWriter();
	}

	/**
	 * Reduce the Write lock of the JVN object identified by id
	 * @param joi : the JVN object id
	 * @return the current JVN object state
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public Serializable jvnInvalidateWriterForReader(int joi)
			throws java.rmi.RemoteException, jvn.JvnException {
		return objects.get(joi).jvnInvalidateWriterForReader();
	}
}
