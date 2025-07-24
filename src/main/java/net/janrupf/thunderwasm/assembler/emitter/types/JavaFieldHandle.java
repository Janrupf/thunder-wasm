package net.janrupf.thunderwasm.assembler.emitter.types;

public final class JavaFieldHandle {
    private final JavaType owner;
    private final String name;
    private final JavaType type;
    private final boolean isStatic;
    private final boolean isSet;
    private final boolean ownerIsInterface;

    public JavaFieldHandle(
            JavaType owner,
            String name,
            JavaType type,
            boolean isStatic,
            boolean isSet,
            boolean ownerIsInterface
    ) {
        this.owner = owner;
        this.name = name;
        this.type = type;
        this.isSet = isSet;
        this.isStatic = isStatic;
        this.ownerIsInterface = ownerIsInterface;
    }

    /**
     * The owner of the method handle, which is the class or interface that defines the method.
     *
     * @return the owner of the method handle
     */
    public JavaType getOwner() {
        return owner;
    }

    /**
     * Retrieves the name of the method handle.
     *
     * @return the name of the method handle
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the type of the field.
     *
     * @return the type of the field
     */
    public JavaType getType() {
        return type;
    }

    /**
     * Determines whether the handle is a setter.
     *
     * @return true if the handle is a setter, false if it is a getter
     */
    public boolean isSet() {
        return isSet;
    }

    /**
     * Determines whether the underlying field is static.
     *
     * @return true if the field is static, false otherwise
     */
    public boolean isStatic() {
        return isStatic;
    }

    /**
     * Determines whether the owner is an interface.
     *
     * @return true if the field owner is an interface, false otherwise
     */
    public boolean isOwnerInterface() {
        return ownerIsInterface;
    }
}
