
package org.janelia.horta;

import java.io.InputStream;
import java.util.Optional;

import org.janelia.rendering.Streamable;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JadeBasedRawTileLoader.
 */
public class JadeBasedRawTileLoader implements RawTileLoader {

    private static final Logger LOG = LoggerFactory.getLogger(JadeBasedRawTileLoader.class);

    private final JadeServiceClient jadeClient;

    JadeBasedRawTileLoader(JadeServiceClient jadeClient) {
        this.jadeClient = jadeClient;
    }

    @Override
    public Optional<String> findStorageLocation(String tileLocation) {
        return jadeClient.findStorageURL(tileLocation);
    }

    @Override
    public Streamable<InputStream> streamTileContent(String storageLocation, String tileLocation) {
        long startTime = System.currentTimeMillis();
        try {
            return jadeClient.streamContent(storageLocation, tileLocation);
        } finally {
            LOG.info("Opened content for reading raw tile bytes from {} for {} in {} ms",
                    storageLocation, tileLocation,
                    System.currentTimeMillis() - startTime);
        }
    }
}