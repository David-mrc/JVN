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
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.io.Serializable;


public class JvnCoordImpl 	
              extends UnicastRemoteObject 
							implements JvnRemoteCoord{
	
  private Integer objCount = 0;
  private HashMap<String, JvnObject> symbolicName_object = new HashMap<>();
  private HashMap<Integer, String> objectId_symbolicName = new HashMap<>();
  private HashMap<Integer, JvnRemoteServer> LockW_objectId_remoteServer = new HashMap<>();
  private HashMap<Integer, JvnRemoteServer> LockR_objectId_remoteServer = new HashMap<>();
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

  public static void main(String args[]){
		try{
		JvnCoordImpl obj = new JvnCoordImpl();
		LocateRegistry.createRegistry(2001);
		java.rmi.Naming.bind("localhost:2001/javanaise", obj);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

/**
  * Default constructor
  * @throws JvnException
  **/
	private JvnCoordImpl() throws Exception {
		// to be completed
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
    if(this.symbolicName_object.containsKey(jon)){
      System.out.println("[DEBUG] Key " + jon + " already present in symbolicName_object map. Updating mapping.");
      this.symbolicName_object.put(jon, jo);
    } else {
      this.objectId_symbolicName.put(jvnGetObjectId(), jon);
      System.out.println("[DEBUG] Created mapping id-name: "+ this.objCount+ " - " + jon);
      this.symbolicName_object.put(jon, jo);
      System.out.println("[DEBUG] Created mapping name-object: " + jon); 
    }
    
  }
  
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
    return this.symbolicName_object.containsKey(jon) ? this.symbolicName_object.get(jon) : null ;
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
    System.out.println("[INFO] Server " + js + " acquires read lock for " + joi);
    return this.symbolicName_object.get(this.objectId_symbolicName.get(joi));
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
    System.out.println("[INFO] Server " + js + " acquires write lock for " + joi);
    return this.symbolicName_object.get(this.objectId_symbolicName.get(joi));
   }

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {
	 // to be completed
    }
}

 
