package org.geoserver.metadata.data.service;

import org.geoserver.catalog.LayerInfo;

public interface CustomNativeMappingService {

    void mapCustomToNative(LayerInfo layer);

    void mapNativeToCustom(LayerInfo layer);
}
