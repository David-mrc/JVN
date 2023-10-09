package jvn;

import java.io.Serializable;

public enum JvnLock implements Serializable {
    NL,  // no lock
    RC,  // read lock cached
    WC,  // write lock cached
    R,   // read lock taken
    W,   // write lock taken
    RWC, // write lock cached & read taken
}
