package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
    private final int joi;
    private Serializable object;
    private JvnLock lock;

    public JvnObjectImpl(int id, Serializable o) throws JvnException {
        joi = id;
        object = o;
        lock = JvnLock.W;
    }

    public JvnObjectImpl(JvnObject jo) throws JvnException {
        joi = jo.jvnGetObjectId();
        object = jo.jvnGetSharedObject();
        lock = JvnLock.NL;
    }

    @Override
    public synchronized void jvnLockRead() throws JvnException {
        switch (lock) {
            case NL -> {
                object = JvnServerImpl.jvnGetServer().jvnLockRead(joi);
                lock = JvnLock.R;
            }
            case RC -> lock = JvnLock.R;
            case WC -> lock = JvnLock.RWC;
            case R, W, RWC -> throw new JvnException("Lock already being used.");
        }
    }

    @Override
    public synchronized void jvnLockWrite() throws JvnException {
        switch (lock) {
            case NL, RC -> {
                object = JvnServerImpl.jvnGetServer().jvnLockWrite(joi);
                lock = JvnLock.W;
            }
            case WC -> lock = JvnLock.W;
            case R, W, RWC -> throw new JvnException("Lock already being used.");
        }
    }

    @Override
    public synchronized void jvnUnLock() throws JvnException {
        switch (lock) {
            case R -> lock = JvnLock.RC;
            case W, RWC -> lock = JvnLock.WC;
            case NL, RC, WC -> throw new JvnException("Lock not currently being used.");
        }
        notify();
    }

    @Override
    public int jvnGetObjectId() throws JvnException {
        return joi;
    }

    @Override
    public Serializable jvnGetSharedObject() throws JvnException {
        return object;
    }

    @Override
    public synchronized void jvnInvalidateReader() throws JvnException {
        switch (lock) {
            case RC -> lock = JvnLock.NL;
            case R -> {
                waitLock();
                lock = JvnLock.NL;
            }
            default -> throw new JvnException("No read lock to invalidate.");
        }
    }

    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException {
        switch (lock) {
            case WC -> lock = JvnLock.NL;
            case W -> {
                waitLock();
                lock = JvnLock.NL;
            }
            case RWC -> lock = JvnLock.R;
            default -> throw new JvnException("No write lock to invalidate.");
        }
        return object;
    }

    @Override
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
        switch (lock) {
            case WC -> lock = JvnLock.RC;
            case W -> {
                waitLock();
                lock = JvnLock.RC;
            }
            case RWC -> lock = JvnLock.R;
            default -> throw new JvnException("No write lock to invalidate.");
        }
        return object;
    }

    private void waitLock() throws JvnException {
        try {
            wait();
        } catch (Exception e) {
            throw new JvnException(e.getMessage());
        }
    }
}
