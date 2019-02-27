package org.geoserver.metadata.rest;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.metadata.data.service.impl.MetadataConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/metadata")
public class MetaDataRestService {

    @Autowired private Catalog catalog;

    @Autowired private MetadataTemplateService templateService;

    @Autowired private ComplexMetadataService metadataService;

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
                metadataService.init(
                        new ComplexMetadataMapImpl((Map<String, Serializable>) custom));
            }
            catalog.save(info);
        }
        return "Success.";
    }
}
