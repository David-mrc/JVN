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
//todo: Names -> id -> object !!!! currently incorrect

public class JvnCoordImpl 	
              extends UnicastRemoteObject 
							implements JvnRemoteCoord{
	
  private Integer objCount = 0;
  private HashMap<Integer, JvnObject> objectId_object = new HashMap<>();
  private HashMap<String, Integer> symbolicName_objectId = new HashMap<>();
  private HashMap<Integer, JvnRemoteServer> LockW_objectId_remoteServer = new HashMap<>();
  private HashMap<Integer, JvnRemoteServer> LockR_objectId_remoteServer = new HashMap<>();
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

  public static void main(String args[]) throws Exception {
		try{
		JvnCoordImpl obj = new JvnCoordImpl();
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
  throws java.rmi.RemoteException,jvn.JvnException {
    this.objCount++;
    return this.objCount;
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
  throws java.rmi.RemoteException,jvn.JvnException{
    Integer id = 0;

    if(this.objectId_object.values().contains(jo)){ 
      for(Integer key: objectId_object.keySet()){
        if(objectId_object.get(key) == jo){
          id = key;
        }
      }
      if(id == 0 || id == null){
        throw new JvnException("objectId_object key-value pair error.");
      }

    } else {
      id = this.jvnGetObjectId();
      this.objectId_object.put(id, jo);
    }

    this.symbolicName_objectId.put(jon, id);
  }
  
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
    Integer id = this.symbolicName_objectId.get(jon);
    if(id == null){
      return null;
    }
    JvnObject obj = this.objectId_object.get(id);
    if(obj == null){
      throw new JvnException("No object with id" + id);
    }
    return obj;
  }
  
  /**
  * Get a Read lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockRead(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
    this.LockR_objectId_remoteServer.put(joi, js);
    return this.objectId_object.get(joi); //no checks for now
   }

  /**
  * Get a Write lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockWrite(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
    this.LockW_objectId_remoteServer.put(joi, js);
    return this.objectId_object.get(joi); //no checks for now
   }

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {
    for(Integer key: this.LockR_objectId_remoteServer.keySet()){
      if(this.LockR_objectId_remoteServer.get(key) == js){
        this.LockR_objectId_remoteServer.remove(key);
      }
    }

    for(Integer key: this.LockW_objectId_remoteServer.keySet()){
      if(this.LockW_objectId_remoteServer.get(key) == js){
        this.LockW_objectId_remoteServer.remove(key);
      }
    }
    }
}

 
