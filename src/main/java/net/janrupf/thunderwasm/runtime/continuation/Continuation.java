package net.janrupf.thunderwasm.runtime.continuation;

import java.util.ArrayList;
import java.util.List;

/**
 * The state of a function invocation which may suspend.
 */
public final class Continuation {
    private final ContinuationCallback callback;
    private final List<ContinuationLayer> layers;
    private boolean isPaused;

    public Continuation(ContinuationCallback callback) {
        this.callback = callback;
        this.layers = new ArrayList<>();
        this.isPaused = false;
    }

    /**
     * Reset the latched pause status to not paused.
     */
    public void unpause() {
        this.isPaused = false;
    }

    /**
     * Push a layer to this continuation.
     *
     * @param layer the layer to push
     */
    public void pushLayer(ContinuationLayer layer) {
        this.layers.add(layer);
    }

    /**
     * Pop a layer from this continuation.
     *
     * @return the layer to pop
     */
    public ContinuationLayer popLayer() {
        if (layers.isEmpty()) {
            return null;
        }

        return layers.remove(layers.size() - 1);
    }

    /**
     * Retrieves the callback this continuation uses to determine whether to suspend.
     *
     * @return the continuation callback
     */
    public ContinuationCallback getCallback() {
        return callback;
    }

    /**
     * Check whether the continuation requests a pause.
     *
     * @param continuation the continuation to check
     * @return true if the given continuation requests a pause, false if not or if the continuation is invalid
     */
    public static boolean shouldPause(Continuation continuation) {
        if (continuation == null || continuation.getCallback() == null) {
            return false;
        } else if (continuation.isPaused) {
            return true;
        }

        continuation.isPaused = continuation.getCallback().shouldPause();
        return continuation.isPaused;
    }

    /**
     * Check whether a continuation is paused already.
     *
     * @param continuation the continuation to check
     * @return true if the continuation is paused, false otherwise
     */
    public static boolean isPaused(Continuation continuation) {
        if (continuation == null) {
            return false;
        }

        return continuation.isPaused;
    }

    /**
     * Push a layer into the given continuation.
     * <p>
     * This function exists for simpler bytecode generation.
     *
     * @param layer        the layer to push
     * @param continuation the continuation to push the layer to
     */
    public static void pushLayerStatic(ContinuationLayer layer, Continuation continuation) {
        continuation.pushLayer(layer);
    }

    /**
     * Pop a layer from a continuation.
     * <p>
     * This function exists for simpler bytecode generation.
     *
     * @param continuation the continuation to pop a layer from
     * @return the popped layer, or null, if there is no layer to pop
     */
    public static ContinuationLayer popLayerStatic(Continuation continuation) {
        if (continuation == null) {
            return null;
        }

        return continuation.popLayer();
    }
}
