package net.janrupf.thunderwasm.assembler.emitter.types;

import net.janrupf.thunderwasm.assembler.emitter.InvokeType;

import java.util.List;

public final class JavaMethodHandle {
    private final JavaType owner;
    private final String name;
    private final JavaType returnType;
    private final List<JavaType> parameterTypes;
    private final InvokeType invokeType;
    private final boolean ownerIsInterface;

    public JavaMethodHandle(
            JavaType owner,
            String name,
            JavaType returnType,
            List<JavaType> parameterTypes,
            InvokeType invokeType,
            boolean ownerIsInterface
    ) {
        this.owner = owner;
        this.name = name;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.invokeType = invokeType;
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
     * Retrieves the return type of the method handle.
     *
     * @return the return type of the method handle
     */
    public JavaType getReturnType() {
        return returnType;
    }

    /**
     * Retrieves the parameter types of the method handle.
     *
     * @return the parameter types of the method handle
     */
    public List<JavaType> getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Retrieves the type of invocation for the method handle.
     *
     * @return the type of invocation for the method handle
     */
    public InvokeType getInvokeType() {
        return invokeType;
    }

    /**
     * Checks if the owner of the method handle is an interface.
     *
     * @return true if the owner is an interface, false otherwise
     */
    public boolean isOwnerIsInterface() {
        return ownerIsInterface;
    }
}
