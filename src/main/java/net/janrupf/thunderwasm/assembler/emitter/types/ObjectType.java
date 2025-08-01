package net.janrupf.thunderwasm.assembler.emitter.types;

import java.util.Objects;

/**
 * Represents a Java object type.
 */
public class ObjectType extends JavaType {
    public static final ObjectType OBJECT = ObjectType.of(Object.class);

    private final String packageName;
    private final String className;

    public ObjectType(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
    }

    /**
     * Retrieves the package name of this object type.
     * <p>
     * This is possibly null for types that are not in a package
     * or in the default package.
     *
     * @return the package name of this object type
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Retrieves the class name of this object type.
     *
     * @return the class name of this object type
     */
    public String getClassName() {
        return className;
    }

    /**
     * Retrieves the binary name of this object type.
     *
     * @return the binary name of this object type
     */
    public String getBinaryName() {
        if (packageName == null || packageName.isEmpty()) {
            return className;
        }

        return packageName.replace('.', '/') + "/" + className;
    }

    @Override
    public String toJvmDescriptor() {
        return "L" + getBinaryName() + ";";
    }

    @Override
    public int getSlotCount() {
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectType)) return false;
        ObjectType that = (ObjectType) o;
        return Objects.equals(packageName, that.packageName) && Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, className);
    }

    /**
     * Creates an object type from a class.
     *
     * @param clazz the class to create an object type for
     * @return the object type for the given class
     */
    public static ObjectType of(Class<?> clazz) {
        if (clazz.isPrimitive() || clazz.isArray()) {
            throw new IllegalArgumentException("Cannot create object type for primitive or array class");
        }

        String fqn = clazz.getName();
        int lastDotIdx = fqn.lastIndexOf('.');

        String packageName = lastDotIdx == -1 ? null : fqn.substring(0, lastDotIdx);
        String className = lastDotIdx == -1 ? fqn : fqn.substring(lastDotIdx + 1);

        return new ObjectType(packageName, className);
    }
}
