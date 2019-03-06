/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.AttributeMappingConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeMappingConfiguration;
import org.geoserver.metadata.data.dto.GeonetworkConfiguration;
import org.geoserver.metadata.data.dto.MappingConfiguration;
import org.geoserver.metadata.data.dto.MetadataConfiguration;
import org.geoserver.metadata.data.dto.impl.MappingConfigurationImpl;
import org.geoserver.metadata.data.dto.impl.MetadataConfigurationImpl;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service responsible for interaction with yaml files. It will search for all *.yaml files in a
 * given directory and try to parse the files. Yaml files that cannot do parsed will be ignored.
 *
 * @author Timothy De Bock
 */
@Component
public class ConfigurationServiceImpl implements ConfigurationService {

    private static final java.util.logging.Logger LOGGER =
            Logging.getLogger(ConfigurationServiceImpl.class);

    @Autowired private GeoServerDataDirectory dataDirectory;

    private MetadataConfiguration configuration;

    private MappingConfiguration mappingConfig;

    private Resource getFolder() {
        return dataDirectory.get(MetadataConstants.DIRECTORY);
    }

    @PostConstruct
    public void init() {
        readConfiguration();
        getFolder()
                .addListener(
                        new ResourceListener() {
                            @Override
                            public void changed(ResourceNotification notify) {
                                readConfiguration();
                            }
                        });
    }

    @Override
    public MetadataConfiguration getMetadataConfiguration() {
        return configuration;
    }

    @Override
    public MappingConfiguration getMappingConfiguration() {
        return mappingConfig;
    }

    private void readConfiguration() {
        Resource folder = getFolder();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        configuration = new MetadataConfigurationImpl();
        mappingConfig = new MappingConfigurationImpl();
        List<Resource> files = Resources.list(folder, new Resources.ExtensionFilter("YAML"));
        Collections.sort(files, (o1, o2) -> o1.name().compareTo(o2.name()));

        for (Resource file : files) {
            try (InputStream in = file.in()) {
                readConfiguration(in, mapper);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            try (InputStream in = file.in()) {
                readMapping(in, mapper);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        // add feature catalog
        try (InputStream in =
                getClass().getResourceAsStream(MetadataConstants.FEATURE_CATALOG_CONFIG_FILE)) {
            readConfiguration(in, mapper);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void readConfiguration(InputStream in, ObjectMapper mapper) throws IOException {
        // read label from propertie file
        MetadataConfiguration config = mapper.readValue(in, MetadataConfigurationImpl.class);
        // Merge attribute configuration and remove duplicates
        Set<String> attributeKeys = new HashSet<>();
        for (AttributeConfiguration attribute : config.getAttributes()) {
            if (attribute.getKey() == null) {
                throw new IOException(
                        "The key of an attribute may not be null. " + attribute.getLabel());
            }
            if (attribute.getLabel() == null) {
                attribute.setLabel(attribute.getKey());
            }
            if (!attributeKeys.contains(attribute.getKey())) {
                configuration.getAttributes().add(attribute);
                attributeKeys.add(attribute.getKey());
            }
        }

        // Merge geonetwork configuration and remove duplicates
        Set<String> geonetworkKeys = new HashSet<>();
        for (GeonetworkConfiguration geonetwork : config.getGeonetworks()) {
            if (!geonetworkKeys.contains(geonetwork.getName())) {
                configuration.getGeonetworks().add(geonetwork);
                geonetworkKeys.add(geonetwork.getName());
            }
        }
        // Merge Types configuration and remove duplicates
        Set<String> typesKeys = new HashSet<>();
        for (AttributeTypeConfiguration type : config.getTypes()) {
            if (!typesKeys.contains(type.getTypename())) {
                for (AttributeConfiguration attribute : type.getAttributes()) {
                    if (attribute.getLabel() == null) {
                        attribute.setLabel(attribute.getKey());
                    }
                }
                configuration.getTypes().add(type);
                typesKeys.add(type.getTypename());
            }
        }
    }

    private void readMapping(InputStream in, ObjectMapper mapper) throws IOException {
        MappingConfiguration config = mapper.readValue(in, MappingConfigurationImpl.class);
        Set<String> attKeys = new HashSet<>();
        for (AttributeMappingConfiguration mapping : config.getGeonetworkmapping()) {
            if (!attKeys.contains(mapping.getGeoserver())) {
                mappingConfig.getGeonetworkmapping().add(mapping);
                attKeys.add(mapping.getGeoserver());
            }
        }

        Set<String> objectKay = new HashSet<>();
        for (AttributeTypeMappingConfiguration mapping : config.getObjectmapping()) {
            if (!objectKay.contains(mapping.getTypename())) {
                mappingConfig.getObjectmapping().add(mapping);
                objectKay.add(mapping.getTypename());
            }
        }
    }
}
