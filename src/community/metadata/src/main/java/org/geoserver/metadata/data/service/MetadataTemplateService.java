/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import java.io.IOException;
import java.util.List;
import org.geoserver.metadata.data.model.MetadataTemplate;

/** @author Timothy De Bock */
public interface MetadataTemplateService {

    List<MetadataTemplate> list();

    void save(MetadataTemplate metadataTemplate, boolean updateLayers) throws IOException;

    void delete(List<MetadataTemplate> newList, MetadataTemplate metadataTemplate);

    void increasePriority(List<MetadataTemplate> newList, MetadataTemplate metadataTemplate);

    void decreasePriority(List<MetadataTemplate> newList, MetadataTemplate metadataTemplate);

    void saveList(List<MetadataTemplate> newList, boolean updateLayers) throws IOException;

    MetadataTemplate findByName(String string);
}
