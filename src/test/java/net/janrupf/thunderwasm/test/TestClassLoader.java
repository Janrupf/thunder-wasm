package net.janrupf.thunderwasm.test;

public class TestClassLoader extends ClassLoader {
    public TestClassLoader() {}

    public Class<?> loadFromBytes(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }
}
