/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Implementation of a Jvn server
 * Contact: 
 *
 * Authors: 
 */

package jvn;

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
	private final HashMap<Integer, JvnObject> objects;

	/**
	 * Default constructor
	 * @throws JvnException
	 **/
	private JvnServerImpl() throws Exception {
		super();
		Registry registry = LocateRegistry.getRegistry("localhost", 2001);
		coord = (JvnRemoteCoord) registry.lookup("Coord");
		objects = new HashMap<>();
	}

	/**
	 * Static method allowing an application to get a reference to
	 * a JVN server instance
	 * @throws JvnException
	 **/
	public static JvnServerImpl jvnGetServer() {
		if (js == null){
			try {
				js = new JvnServerImpl();
			} catch (Exception e) {
				return null;
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
		} catch (Exception e) {
			throw new JvnException(e.getMessage());
		}
	}

	/**
	 * creation of a JVN object
	 * @param o : the JVN object state
	 * @throws JvnException
	 **/
	public JvnObject jvnCreateObject(Serializable o) throws jvn.JvnException {
		try {
			int joi = coord.jvnGetObjectId();
			return null;
		} catch (Exception e) {
			throw new JvnException(e.getMessage());
		}
	}

	/**
	 *  Associate a symbolic name with a JVN object
	 * @param jon : the JVN object name
	 * @param jo : the JVN object
	 * @throws JvnException
	 **/
	public void jvnRegisterObject(String jon, JvnObject jo) throws jvn.JvnException {
		try {
			coord.jvnRegisterObject(jon, jo, this);
		} catch (Exception e) {
			throw new JvnException(e.getMessage());
		}
	}

	/**
	 * Provide the reference of a JVN object beeing given its symbolic name
	 * @param jon : the JVN object name
	 * @return the JVN object
	 * @throws JvnException
	 **/
	public JvnObject jvnLookupObject(String jon) throws jvn.JvnException {
		try {
			return coord.jvnLookupObject(jon, this);
		} catch (Exception e) {
			throw new JvnException(e.getMessage());
		}
	}

	/**
	 * Get a Read lock on a JVN object
	 * @param joi : the JVN object identification
	 * @return the current JVN object state
	 * @throws  JvnException
	 **/
	public Serializable jvnLockRead(int joi) throws JvnException {
		try {
			return coord.jvnLockRead(joi, this);
		} catch (Exception e) {
			throw new JvnException(e.getMessage());
		}
	}

	/**
	 * Get a Write lock on a JVN object
	 * @param joi : the JVN object identification
	 * @return the current JVN object state
	 * @throws  JvnException
	 **/
	public Serializable jvnLockWrite(int joi) throws JvnException {
		try {
			return coord.jvnLockWrite(joi, this);
		} catch (Exception e) {
			throw new JvnException(e.getMessage());
		}
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
