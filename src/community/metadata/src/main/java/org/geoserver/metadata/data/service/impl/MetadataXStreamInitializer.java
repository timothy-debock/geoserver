/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.springframework.stereotype.Component;

@Component
public class MetadataXStreamInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType("list", ArrayList.class);
        persister.registerBreifMapComplexType("map", HashMap.class);
        persister.getXStream().alias("list", List.class, ArrayList.class);
        persister.getXStream().alias("map", Map.class, HashMap.class);
    }
}
