package net.zdremann.util;

public final class AsyncTaskResult<T> {
    private final T result;
    private final Exception error;
    private final boolean isResult;

    public boolean isResult() { return isResult; }

    public T getResult() {
        return result;
    }

    public Exception getError() {
        return error;
    }

    public AsyncTaskResult(T result) {
        this.isResult = true;
        this.error = null;
        this.result = result;
    }

    public AsyncTaskResult(Exception error) {
        this.isResult = false;
        this.error = error;
        this.result = null;
    }
}
