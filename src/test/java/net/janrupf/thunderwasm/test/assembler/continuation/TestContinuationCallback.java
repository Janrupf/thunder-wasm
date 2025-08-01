package net.janrupf.thunderwasm.test.assembler.continuation;

import net.janrupf.thunderwasm.runtime.continuation.ContinuationCallback;

public class TestContinuationCallback implements ContinuationCallback {
    private boolean doSuspend;

    public void suspendNow() {
        this.doSuspend = true;
    }

    public void clearSuspension() {
        this.doSuspend = false;
    }

    public boolean isSuspended() {
        return this.doSuspend;
    }

    @Override
    public boolean shouldPause() {
        return doSuspend;
    }
}
