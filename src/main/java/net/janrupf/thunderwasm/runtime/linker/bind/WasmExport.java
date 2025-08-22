package net.janrupf.thunderwasm.runtime.linker.bind;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a member of a class to be exported to WebAssembly.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface WasmExport {
    /**
     * The name of the export.
     * <p>
     * If not specified (as in, empty), the name of the export will be
     * the name of the field or method.
     *
     * @return the name of the export
     */
    String value() default "";

    /**
     * The name of the module this export belongs to.
     * <p>
     * If not specified (as in, empty), the export will be linked to the
     * default module name of the runtime linker.
     *
     * @return the name of the module
     */
    String module() default "";

    /**
     * Whether to export this field read-only.
     * <p>
     * Has no effect other than on fields which are not final. By default
     * a non-final field is exported read-write.
     *
     * @return true if the field is forced read-only, false otherwise
     */
    boolean readOnly() default false;

    /**
     * The type of the export.
     *
     * @return the type of the export
     */
    Type type() default Type.AUTO;

    enum Type {
        /**
         * Derive the type based on the field or method type.
         */
        AUTO,

        /**
         * Force the export to be a function.
         */
        FUNCTION,

        /**
         * Force the export to be a function that is dynamically
         * retrieved at runtime, e.g. a getter method.
         * <p>
         * This can be used for a method that itself is not to be exported,
         * but its return value is to be exported as a function.
         */
        FUNCTION_GETTER,

        /**
         * Force the export to be a global.
         */
        GLOBAL,

        /**
         * Force the export to be a table.
         */
        TABLE,

        /**
         * Force the export to be a memory.
         */
        MEMORY
    }
}
