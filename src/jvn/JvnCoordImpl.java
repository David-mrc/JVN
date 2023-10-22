/***
 * JAVANAISE Implementation
 * JvnCoordImpl class
 * This class implements the Javanaise central coordinator
 * Contact:  
 *
 * Authors: 
 */

package jvn;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class JvnCoordImpl 	
        extends UnicastRemoteObject
        implements JvnRemoteCoord {

    private static final long serialVersionUID = 1L;

    private int objCount = 0;
    private int serverCount = 0;
    private final HashMap<JvnRemoteServer, Integer> servers = new HashMap<>();
    private final HashMap<Integer, Serializable> objectId_object = new HashMap<>();
    private final HashMap<String, JvnObject> symbolicName_jo = new HashMap<>();
    private final HashMap<Integer, Set<JvnRemoteServer>> readers = new HashMap<>();
    private final HashMap<Integer, JvnRemoteServer> writers = new HashMap<>();

    public static void main(String[] args) throws Exception {
        JvnRemoteCoord obj = new JvnCoordImpl();
        Registry r = LocateRegistry.createRegistry(2001);
        r.bind("Coord", obj);
	}

    /**
     * Default constructor
     * @throws JvnException
     **/
	private JvnCoordImpl() throws Exception {
		System.out.println("Coordinator started.");
	}

    /**
     *  Allocate a NEW JVN object id (usually allocated to a
     *  newly created JVN object)
     * @throws java.rmi.RemoteException,JvnException
     **/
    public synchronized int jvnGetObjectId()
            throws java.rmi.RemoteException, jvn.JvnException {
        readers.put(++objCount, new HashSet<>());
		System.out.println("New object id allocated: " + objCount);
        return objCount;
    }

    /**
     * Associate a symbolic name with a JVN object
     * @param jon : the JVN object name
     * @param jo  : the JVN object
     * @param js  : the remote reference of the JVNServer
     * @throws java.rmi.RemoteException,JvnException
     **/
    public synchronized void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
            throws java.rmi.RemoteException, jvn.JvnException {
        addServer(js);

        int joi = jo.jvnGetObjectId();
        objectId_object.put(joi, jo.jvnGetSharedObject());
        symbolicName_jo.put(jon, jo);
        writers.put(joi, js);

        print(js, "Object " + joi + " registered with name: \"" + jon + "\"");
    }

    /**
     * Get the reference of a JVN object managed by a given JVN server
     * @param jon : the JVN object name
     * @param js : the remote reference of the JVNServer
     * @throws java.rmi.RemoteException,JvnException
     **/
    public synchronized JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
            throws java.rmi.RemoteException,jvn.JvnException {
        addServer(js);
        print(js, "Looking up for object: \"" + jon + "\"");
        return symbolicName_jo.get(jon);
    }

    /**
     * Get a Read lock on a JVN object managed by a given JVN server
     * @param joi : the JVN object identification
     * @param js  : the remote reference of the server
     * @return the current JVN object state
     * @throws java.rmi.RemoteException, JvnException
     **/
    public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        invalidateWriterForReader(joi, js);
        readers.get(joi).add(js);

        print(js, "Read lock acquired on object: " + joi);
        return objectId_object.get(joi);
    }

    /**
     * Get a Write lock on a JVN object managed by a given JVN server
     * @param joi : the JVN object identification
     * @param js  : the remote reference of the server
     * @return the current JVN object state
     * @throws java.rmi.RemoteException, JvnException
     **/
    public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        invalidateReaders(joi, js);
        invalidateWriter(joi, js);
        writers.put(joi, js);

        print(js, "Write lock acquired on object: " + joi);
        return objectId_object.get(joi);
    }

    /**
     * A JVN server terminates
     * @param js  : the remote reference of the server
     * @throws java.rmi.RemoteException, JvnException
     **/
    public synchronized void jvnTerminate(JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        for (int joi : readers.keySet()) {
            Set<JvnRemoteServer> set = readers.get(joi);

            if (set.contains(js)) {
                js.jvnInvalidateReader(joi);
                set.remove(js);
            }
        }

        for (int joi : writers.keySet()) {
            if (writers.get(joi) == js) {
                Serializable obj = js.jvnInvalidateWriter(joi);
                objectId_object.put(joi, obj);
                writers.remove(joi);
            }
        }
        print(js, "Server terminated.");
        servers.remove(js);
    }


    private void addServer(JvnRemoteServer js) {
        servers.computeIfAbsent(js, k -> ++serverCount);
    }

    private void print(JvnRemoteServer js, String message) {
        System.out.println("[" + servers.get(js) + "] " + message);
    }

    private void invalidateReaders(int joi, JvnRemoteServer js)
            throws RemoteException, JvnException {
        Iterator<JvnRemoteServer> it = readers.get(joi).iterator();

        while (it.hasNext()) {
            JvnRemoteServer reader = it.next();
            print(js, "Invalidating reader: " + servers.get(reader));
            it.remove();

            if (!reader.equals(js)) {
                try {
                    reader.jvnInvalidateReader(joi);
                } catch (RemoteException e) {
                    removeLostServers(reader);
                }
            }
        }
    }

    private void invalidateWriter(int joi, JvnRemoteServer js)
            throws RemoteException,JvnException {
        JvnRemoteServer writer = writers.get(joi);

        if (writer != null) {
            print(js, "Invalidating writer: " + servers.get(writer));

            try {
                Serializable obj = writer.jvnInvalidateWriter(joi);
                objectId_object.put(joi, obj);
                writers.remove(joi);
            } catch (RemoteException e) {
                removeLostServers(writer);
            }
        }
    }

    private void invalidateWriterForReader(int joi, JvnRemoteServer js)
            throws RemoteException,JvnException {
        JvnRemoteServer writer = writers.get(joi);

        if (writer != null) {
            print(js, "Invalidating writer for reader: " + servers.get(writer));

            try {
                Serializable obj = writer.jvnInvalidateWriterForReader(joi);
                objectId_object.put(joi, obj);
                writers.remove(joi);
                readers.get(joi).add(writer);
            } catch (RemoteException e) {
                removeLostServers(writer);
            }
        }
    }

    private void removeLostServers(JvnRemoteServer js) {
        for (Set<JvnRemoteServer> set : readers.values()) {
            set.remove(js);
        }

        for (int joi : writers.keySet()) {
            if (writers.get(joi) == js) {
                writers.remove(joi);
            }
        }
        print(js, "Connection lost with server.");
        servers.remove(js);
    }
}
