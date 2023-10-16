package irc;

import jvn.JvnObject;
import jvn.JvnServerImpl;

import java.util.Random;

public class Stress {
    private static final long N_TEST = 1_000_000;

    public static void main(String[] args) {
        JvnServerImpl js = null;
        try {
            js = JvnServerImpl.jvnGetServer();
            JvnObject jo = js.jvnLookupObject("Stress");

            if (jo == null) {
                jo = js.jvnCreateObject(new Sentence());
                jo.jvnUnLock();
                js.jvnRegisterObject("Stress", jo);
            }
            Random rand = new Random();
            System.out.println("Entering stress test...");

            long value = 0;

            while (value < N_TEST) {
                boolean op = rand.nextBoolean();
                long newValue;

                if (op) {
                    newValue = read(jo);
                    value = newValue;
                } else {
                    newValue = write(jo);
                }

                if (newValue < value) {
                    System.err.println("Invalid new value read.");
                    System.exit(1);
                }
                value = newValue;
            }
            js.jvnTerminate();

            System.out.println("End of stress test.");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static long read(JvnObject jo) throws Exception {
        jo.jvnLockRead();

		String s = ((Sentence)(jo.jvnGetSharedObject())).read();
        long value = s.isEmpty() ? 0 : Long.parseLong(s);
        System.out.println("Read: " + s);

		jo.jvnUnLock();
        return value;
    }

    private static long write(JvnObject jo) throws Exception {
        jo.jvnLockWrite();

		String s = ((Sentence) jo.jvnGetSharedObject()).read();

        long newValue = s.isEmpty() ? 1 : Long.parseLong(s) + 1;
        String newStr = String.valueOf(newValue);

        ((Sentence) jo.jvnGetSharedObject()).write(newStr);
        System.out.println("Write: " + newStr);

		jo.jvnUnLock();
        return newValue;
    }
}
