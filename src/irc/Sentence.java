package irc;

import jvn.JvnLock;
import jvn.JvnLockType;

import java.io.Serializable;

public interface Sentence extends Serializable {

    @JvnLock(JvnLockType.READ)
	String read();

    @JvnLock(JvnLockType.WRITE)
	void write(String text);
}
