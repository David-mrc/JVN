package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
    private final int joi;
    private Serializable object;
    private JvnLock jvnLock;
    private boolean pendingLock = false;
    private boolean pendingInvalidate = false;

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
                jvnLock = JvnLock.R;
                object = JvnServerImpl.jvnGetServer().jvnLockRead(joi);
            }
            case RC -> jvnLock = JvnLock.R;
            case WC -> jvnLock = JvnLock.RWC;
            case R, W, RWC -> throw new JvnException("Lock already being used.");
        }
    }

    @Override
    public void jvnLockWrite() throws JvnException {
        synchronized (this) {
            switch (jvnLock) {
                case NL -> {
                    jvnLock = JvnLock.W;
                    object = JvnServerImpl.jvnGetServer().jvnLockWrite(joi);
                    return;
                }
                case WC -> {
                    jvnLock = JvnLock.W;
                    return;
                }
                case R, W, RWC -> throw new JvnException("Lock already being used.");
            }
            jvnLock = JvnLock.W;
            pendingLock = true;
        }
        // case RC
        object = JvnServerImpl.jvnGetServer().jvnLockWrite(joi);

        synchronized (this) {
            pendingLock = false;
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

        if (pendingInvalidate) {
            waitLock();
        }
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
                waitAndNotify();
                jvnLock = JvnLock.NL;
            }
            default -> {
                if (jvnLock != JvnLock.W || !pendingLock) {
                    throw new JvnException("No read lock to invalidate.");
                }
            }
        }
    }

    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException {
        switch (jvnLock) {
            case WC -> jvnLock = JvnLock.NL;
            case W, RWC -> {
                waitAndNotify();
                jvnLock = JvnLock.NL;
            }
            default -> throw new JvnException("No write lock to invalidate.");
        }
        return object;
    }

    @Override
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
        switch (jvnLock) {
            case WC -> jvnLock = JvnLock.RC;
            case W -> {
                waitAndNotify();
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

    private void waitAndNotify() throws JvnException {
        pendingInvalidate = true;
        waitLock();
        pendingInvalidate = false;
        notify();
    }
}
