package test;

import jvn.JvnServerImpl;

import java.util.Random;

public class Stress {
    private static final long N_TEST = 1_000_000;

    public static void main(String[] args) {
        try {
            JvnServerImpl js = JvnServerImpl.jvnGetServer();
            Count count = (Count) js.jvnLookupObject("Stress");

            if (count == null) {
                count = (Count) js.jvnCreateObject(new CountImpl());
                js.jvnRegisterObject("Stress", count);
            }
            Random rand = new Random();
            System.out.println("Entering stress test...");

            long value = 0;

            while (value < N_TEST) {
                boolean op = rand.nextBoolean();
                long newValue;

                if (op) {
                    newValue = read(count);
                } else {
                    newValue = increment(count);
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

    private static long read(Count count) throws Exception {
		long value = count.read();
        System.out.println("Read: " + value);
        return value;
    }

    private static long increment(Count count) throws Exception {
        long value = count.increment();
        System.out.println("Write: " + value);
        return value;
    }
}
