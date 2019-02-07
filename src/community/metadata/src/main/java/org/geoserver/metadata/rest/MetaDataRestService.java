package org.geoserver.metadata.rest;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.metadata.data.service.impl.MetadataConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/metadata")
public class MetaDataRestService {

    @Autowired private Catalog catalog;

    @Autowired private MetadataTemplateService templateService;

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
            for (MetadataTemplate template : templateService.list()) {
                try {
                    template.getLinkedLayers().clear();
                    templateService.save(template, false);
                    templateService.delete(template);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
