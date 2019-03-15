package org.geoserver.metadata.rest;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.metadata.data.service.CustomNativeMappingService;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.metadata.data.service.impl.MetadataConstants;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/metadata")
public class MetaDataRestService {

    private static final Logger LOGGER = Logging.getLogger(MetaDataRestService.class);

    @Autowired private Catalog catalog;

    @Autowired private MetadataTemplateService templateService;

    @Autowired private ComplexMetadataService metadataService;

    @Autowired private CustomNativeMappingService nativeToCustomService;

    @DeleteMapping
    public void clearAll(
            @RequestParam(required = false, defaultValue = "false") boolean iAmSure,
            HttpServletResponse response)
            throws IOException {
        if (!iAmSure) {
            response.sendError(400, "You must be sure.");
        } else {
            for (ResourceInfo info : catalog.getResources(ResourceInfo.class)) {
                info.getMetadata().remove(MetadataConstants.CUSTOM_METADATA_KEY);
                info.getMetadata().remove(MetadataConstants.DERIVED_KEY);
                catalog.save(info);
            }
            templateService.saveList(Collections.emptyList());
        }
    }

    @SuppressWarnings("unchecked")
    @GetMapping("fix")
    public String fixAll() {
        for (ResourceInfo info : catalog.getResources(ResourceInfo.class)) {
            Serializable custom = info.getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY);
            if (custom instanceof HashMap<?, ?>) {
                ComplexMetadataMapImpl complex =
                        new ComplexMetadataMapImpl((Map<String, Serializable>) custom);
                metadataService.init(complex);
                metadataService.derive(complex);
            }
            catalog.save(info);
        }
        return "Success.";
    }

    @PostMapping("nativeToCustom")
    public void nativeToCustom(@RequestBody String csvFile) {
        for (String resourceName : csvFile.split("\n")) {
            LayerInfo info = catalog.getLayerByName(resourceName.trim());
            if (info != null) {
                info.setResource(
                        catalog.getResource(info.getResource().getId(), ResourceInfo.class));
                nativeToCustomService.mapNativeToCustom(info);
                catalog.save(info.getResource());
            } else {
                LOGGER.warning("Couldn't find layer " + resourceName);
            }
        }
    }
}
