package irc;

import java.io.Serializable;
import java.util.Random;

import jvn.JvnObject;
import jvn.JvnServerImpl;

public class Stress {
	JvnObject       sentence;
    public static void main(String argv[]) {
	   try {

		// initialize JVN
		JvnServerImpl js = JvnServerImpl.jvnGetServer();
		
		// look up the Stress object in the JVN server
		// if not found, create it, and register it in the JVN server
		JvnObject jo = js.jvnLookupObject("Stress");
		   
		if (jo == null) {
			jo = js.jvnCreateObject((Serializable) new Sentence());
			// after creation, I have a write lock on the object
			jo.jvnUnLock();
			js.jvnRegisterObject("Stress", jo);
		}
		
		 new Stress(jo);
	   
	   } catch (Exception e) {
		   System.out.println("Stress problem : " + e.getMessage());
		   e.printStackTrace();
	   }

	}

    /**
   * Stress Constructor
   @param jo the JVN object representing the Chat
   **/
	public Stress(JvnObject jo) throws Exception {
        this.sentence = jo;

        long N_TEST = 100_000_000;
        Random rand = new Random();

        System.out.println("Entering stress test");
        for(int i = 0; i< N_TEST ; i++){
            int op = rand.nextInt(2);
            performRandomOperation(this.sentence, this, op);
        }
        System.out.println("End of stress test");
    }

    private void performRandomOperation(JvnObject obj, Stress stress, int op) throws Exception {
        if(op<1){
            System.out.println("performing write");
            write();
        } else {
            System.out.println("performing read");
            read();
        }
    }

    private void read() throws Exception {
        this.sentence.jvnLockRead();

		((Sentence)(this.sentence.jvnGetSharedObject())).read();

		this.sentence.jvnUnLock();
    }

    private void write () throws Exception {
        this.sentence.jvnLockWrite();
		
		((Sentence)(this.sentence.jvnGetSharedObject())).write("abcdefghijkm");
	
		this.sentence.jvnUnLock();
    }

    

}
