package jvn;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JvnObjectImpl implements JvnObject {
    private final int joi;
    private Serializable object;
    private LockState lockState;
    private boolean pendingLock = false;
    private boolean pendingInvalidate = false;

    private JvnObjectImpl(int id, Serializable o, LockState lock) throws JvnException {
        joi = id;
        object = o;
        lockState = lock;
    }

    public static Object newInstance(int id, Serializable o) throws JvnException {
        return Proxy.newProxyInstance(
                o.getClass().getClassLoader(),
                o.getClass().getInterfaces(),
                new JvnObjectImpl(id, o, LockState.WC));
    }

    public static Object newInstance(JvnObject jo) throws JvnException {
        int id = jo.jvnGetObjectId();
        Serializable o = jo.jvnGetSharedObject();

        return Proxy.newProxyInstance(
                o.getClass().getClassLoader(),
                o.getClass().getInterfaces(),
                new JvnObjectImpl(id, o, LockState.NL));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!method.isAnnotationPresent(JvnLock.class)) {
            throw new RuntimeException("Cannot invoke method " + method.getName() +
                    ": JvnLock annotation is missing");
        }
        JvnLockType type = method.getAnnotation(JvnLock.class).value();

        if (type == JvnLockType.READ) {
            jvnLockRead();
        } else {
            jvnLockWrite();
        }
        Object result = method.invoke(object, args);
        jvnUnLock();

        return result;
    }


    @Override
    public synchronized void jvnLockRead() throws JvnException {
        switch (lockState) {
            case NL -> {
                lockState = LockState.R;
                object = JvnServerImpl.jvnGetServer().jvnLockRead(joi);
            }
            case RC -> lockState = LockState.R;
            case WC -> lockState = LockState.RWC;
            case R, W, RWC -> throw new JvnException("Lock already being used.");
        }
    }

    @Override
    public void jvnLockWrite() throws JvnException {
        synchronized (this) {
            switch (lockState) {
                case NL -> {
                    lockState = LockState.W;
                    object = JvnServerImpl.jvnGetServer().jvnLockWrite(joi);
                    return;
                }
                case WC -> {
                    lockState = LockState.W;
                    return;
                }
                case R, W, RWC -> throw new JvnException("Lock already being used.");
            }
            lockState = LockState.W;
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
        switch (lockState) {
            case R -> lockState = LockState.RC;
            case W, RWC -> lockState = LockState.WC;
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
        switch (lockState) {
            case RC -> lockState = LockState.NL;
            case R -> {
                waitAndNotify();
                lockState = LockState.NL;
            }
            default -> {
                if (lockState != LockState.W || !pendingLock) {
                    throw new JvnException("No read lock to invalidate.");
                }
            }
        }
    }

    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException {
        switch (lockState) {
            case WC -> lockState = LockState.NL;
            case W, RWC -> {
                waitAndNotify();
                lockState = LockState.NL;
            }
            default -> throw new JvnException("No write lock to invalidate.");
        }
        return object;
    }

    @Override
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
        switch (lockState) {
            case WC -> lockState = LockState.RC;
            case W -> {
                waitAndNotify();
                lockState = LockState.RC;
            }
            case RWC -> lockState = LockState.R;
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
