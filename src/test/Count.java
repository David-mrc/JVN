package test;

import jvn.JvnLock;
import jvn.JvnLockType;

import java.io.Serializable;

public interface Count extends Serializable {

    @JvnLock(JvnLockType.READ)
	long read();

    @JvnLock(JvnLockType.WRITE)
	long increment();
}
