package test;

public class CountImpl implements Count {
    private static final long serialVersionUID = 1L;

    private long value = 0;

    @Override
    public long read() {
        return value;
    }

    @Override
    public long increment() {
        return ++value;
    }
}
