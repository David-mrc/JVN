package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
    private final int joi;
    private Serializable object;
    private JvnLock jvnLock;

    public JvnObjectImpl(int id, Serializable o) throws JvnException {
        joi = id;
        object = o;
        jvnLock = JvnLock.W;
    }

    public JvnObjectImpl(JvnObject jo) throws JvnException {
        joi = jo.jvnGetObjectId();
        object = jo.jvnGetSharedObject();
        jvnLock = JvnLock.NL;
    }

    @Override
    public synchronized void jvnLockRead() throws JvnException {
        switch (jvnLock) {
            case NL -> {
                object = JvnServerImpl.jvnGetServer().jvnLockRead(joi);
                jvnLock = JvnLock.R;
            }
            case RC -> jvnLock = JvnLock.R;
            case WC -> jvnLock = JvnLock.RWC;
            case R, W, RWC -> throw new JvnException("Lock already being used.");
        }
    }

    @Override
    public synchronized void jvnLockWrite() throws JvnException {
        switch (jvnLock) {
            case NL, RC -> {
                object = JvnServerImpl.jvnGetServer().jvnLockWrite(joi);
                jvnLock = JvnLock.W;
            }
            case WC -> jvnLock = JvnLock.W;
            case R, W, RWC -> throw new JvnException("Lock already being used.");
        }
    }

    @Override
    public synchronized void jvnUnLock() throws JvnException {
        switch (jvnLock) {
            case R -> jvnLock = JvnLock.RC;
            case W, RWC -> jvnLock = JvnLock.WC;
            case NL, RC, WC -> throw new JvnException("Lock not currently being used.");
        }
        notify();
    }

    @Override
    public synchronized int jvnGetObjectId() throws JvnException {
        return joi;
    }

    @Override
    public synchronized Serializable jvnGetSharedObject() throws JvnException {
        return object;
    }

    @Override
    public synchronized void jvnInvalidateReader() throws JvnException {
        switch (jvnLock) {
            case RC -> jvnLock = JvnLock.NL;
            case R -> {
                waitLock();
                jvnLock = JvnLock.NL;
            }
            default -> throw new JvnException("No read lock to invalidate.");
        }
    }

    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException {
        switch (jvnLock) {
            case WC -> jvnLock = JvnLock.NL;
            case W -> {
                waitLock();
                jvnLock = JvnLock.NL;
            }
            case RWC -> jvnLock = JvnLock.R;
            default -> throw new JvnException("No write lock to invalidate.");
        }
        return object;
    }

    @Override
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
        switch (jvnLock) {
            case WC -> jvnLock = JvnLock.RC;
            case W -> {
                waitLock();
                jvnLock = JvnLock.RC;
            }
            case RWC -> jvnLock = JvnLock.R;
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
