package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper to trick which gadgets have already been created.
 */
public final class LocalGadgets {
    private final Map<String, CodeLabel> gadgetEntryPoints;
    private final Map<String, CodeEmitter> gadgets;

    public LocalGadgets() {
        this.gadgetEntryPoints = new HashMap<>();
        this.gadgets = new HashMap<>();
    }

    /**
     * Add a new entry point for gadgets.
     *
     * @param key the key of the entry point
     * @param entry the entry point
     * @throws WasmAssemblerException if an entry point with that key exists already
     */
    public void addEntryPoint(String key, CodeLabel entry) throws WasmAssemblerException {
        if (this.gadgetEntryPoints.containsKey(key)) {
            throw new WasmAssemblerException("Gadget entry point " + key + " exists already");
        }

        this.gadgetEntryPoints.put(key, entry);
    }

    /**
     * Retrieves an entry point from the gadget list.
     *
     * @param key the key of the entry point
     * @return the gadget entry point, or null, if no such entry point
     */
    public CodeLabel getEntryPoint(String key) {
        if (!this.gadgetEntryPoints.containsKey(key)) {
            return null;
        }

        return this.gadgetEntryPoints.get(key);
    }

    /**
     * Add a new gadget.
     *
     * @param key the key of the gadget
     * @param emitter the emitter that contains the gadgets code
     * @throws WasmAssemblerException if a gadget with that key exists already
     */
    public void addGadget(String key, CodeEmitter emitter) throws WasmAssemblerException {
        if (this.gadgets.containsKey(key)) {
            throw new WasmAssemblerException("Gadget " + key + " exists already");
        }

        this.gadgets.put(key, emitter);
    }

    /**
     * Determines whether a gadget exists.
     *
     * @param key the key of the gadget
     * @return true if the gadget exists, false otherwise
     */
    public boolean hasGadget(String key) {
        return this.gadgets.containsKey(key);
    }

    /**
     * Temporarily remove a gadget so that it can be modified.
     * <p>
     * Certain rules apply to gadget modification:
     * <ul>
     *     <li>
     *         The gadget must be re-added after modification with {@link #addGadget(String, CodeEmitter)}, even
     *         if with another key.
     *     </li>
     *     <li>Existing entry points produced by this gadget must stay valid</li>
     * </ul>
     *
     * @param key the key of the gadget to remove
     * @return the emitter for the gadget
     * @throws WasmAssemblerException if there is no gadget with the given key
     */
    public CodeEmitter removeGadgetForModification(String key) throws WasmAssemblerException {
        if (!this.gadgets.containsKey(key)) {
            throw new WasmAssemblerException("Gadget " + key + " does not exist");
        }

        return this.gadgets.remove(key);
    }
}
