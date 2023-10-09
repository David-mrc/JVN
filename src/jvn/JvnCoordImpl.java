/***
 * JAVANAISE Implementation
 * JvnCoordImpl class
 * This class implements the Javanaise central coordinator
 * Contact:  
 *
 * Authors: 
 */

package jvn;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
//todo: Names -> id -> object !!!! currently incorrect

public class JvnCoordImpl 	
        extends UnicastRemoteObject
        implements JvnRemoteCoord {

    private static final long serialVersionUID = 1L;

    private int objCount = 0;
    private final HashMap<Integer, Serializable> objectId_object = new HashMap<>();
    private final HashMap<String, JvnObject> symbolicName_jo = new HashMap<>();
    private final HashMap<Integer, Set<JvnRemoteServer>> readers = new HashMap<>();
    private final HashMap<Integer, JvnRemoteServer> writers = new HashMap<>();

    public static void main(String[] args) throws Exception {
		try{
		    JvnRemoteCoord obj = new JvnCoordImpl();
		    Registry r = LocateRegistry.createRegistry(2001);
		    r.bind("Coord", obj);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

    /**
     * Default constructor
     * @throws JvnException
     **/
	private JvnCoordImpl() throws Exception {
		System.out.println("Coordinator started");
	}

    /**
     *  Allocate a NEW JVN object id (usually allocated to a
     *  newly created JVN object)
     * @throws java.rmi.RemoteException,JvnException
     **/
    public int jvnGetObjectId()
            throws java.rmi.RemoteException, jvn.JvnException {
        readers.put(++objCount, new HashSet<>());
		System.out.println("New object id allocated: " + objCount);
        return objCount;
    }

    /**
     * Associate a symbolic name with a JVN object
     * @param jon : the JVN object name
     * @param jo  : the JVN object
     * @param joi : the JVN object identification
     * @param js  : the remote reference of the JVNServer
     * @throws java.rmi.RemoteException,JvnException
     **/
    public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
            throws java.rmi.RemoteException,jvn.JvnException {
        int joi = jo.jvnGetObjectId();
        objectId_object.put(joi, jo.jvnGetSharedObject());
        symbolicName_jo.put(jon, jo);
        writers.put(joi, js);
        System.out.println("Object " + joi + " registered with name: " + jon);
    }

    /**
     * Get the reference of a JVN object managed by a given JVN server
     * @param jon : the JVN object name
     * @param js : the remote reference of the JVNServer
     * @throws java.rmi.RemoteException,JvnException
     **/
    public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
            throws java.rmi.RemoteException,jvn.JvnException {
        System.out.println("Looking up for object: " + jon);
        return symbolicName_jo.get(jon);
    }

    /**
     * Get a Read lock on a JVN object managed by a given JVN server
     * @param joi : the JVN object identification
     * @param js  : the remote reference of the server
     * @return the current JVN object state
     * @throws java.rmi.RemoteException, JvnException
     **/
    public Serializable jvnLockRead(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        Set<JvnRemoteServer> servers = readers.get(joi);
        JvnRemoteServer writer = writers.get(joi);

        if (writer != null) {
            objectId_object.put(joi, writer.jvnInvalidateWriterForReader(joi));
            writers.remove(joi);
            servers.add(writer);
        }
        servers.add(js);
        System.out.println("Read lock acquired by object with id: " + joi);
        return objectId_object.get(joi);
    }

    /**
     * Get a Write lock on a JVN object managed by a given JVN server
     * @param joi : the JVN object identification
     * @param js  : the remote reference of the server
     * @return the current JVN object state
     * @throws java.rmi.RemoteException, JvnException
     **/
    public Serializable jvnLockWrite(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        JvnRemoteServer writer = writers.get(joi);
        Set<JvnRemoteServer> servers = readers.get(joi);

        if (writer != null) {
            objectId_object.put(joi, writer.jvnInvalidateWriter(joi));
        }
        for (JvnRemoteServer reader: servers) {
            reader.jvnInvalidateReader(joi);
        }
        servers.clear();
        writers.put(joi, js);
        System.out.println("Write lock acquired by object with id: " + joi);
        return objectId_object.get(joi);
    }

    /**
     * A JVN server terminates
     * @param js  : the remote reference of the server
     * @throws java.rmi.RemoteException, JvnException
     **/
    public void jvnTerminate(JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        for(Integer key: this.readers.keySet()){
          if(this.readers.get(key).contains(js)){
            this.readers.remove(key);
          }
        }

        for(Integer key: this.writers.keySet()){
          if(this.writers.get(key) == js){
            this.writers.remove(key);
          }
        }
        System.out.println("Server terminated");
    }
}
