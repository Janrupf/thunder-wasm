package net.janrupf.thunderwasm.test.wast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A collection of multiple tests represented by a manifest.
 */
public class WastTestCollection {
    private final WastManifest manifest;
    private final String name;
    private final String resourceBase;

    public WastTestCollection(WastManifest manifest, String name, String resourceBase) {
        this.manifest = manifest;
        this.name = name;
        this.resourceBase = resourceBase;
    }

    /**
     * Open a stream to read a resource from this collection.
     *
     * @param name the name of the resource to read
     * @return the resource stream
     * @throws IOException if the resource could not be found or opened
     */
    public InputStream readResource(String name) throws IOException {
        InputStream stream = getClass().getResourceAsStream("/" + resourceBase + "/" + name);
        if (stream == null) {
            throw new FileNotFoundException("No such resource for collection " + name + ": " + resourceBase + "/" + name);
        }

        return stream;
    }

    /**
     * The manifest of the collection.
     *
     * @return the manifest
     */
    public WastManifest getManifest() {
        return manifest;
    }

    /**
     * Retrieve the name of the collection.
     *
     * @return the name of the collection
     */
    public String getName() {
        return name;
    }
}
