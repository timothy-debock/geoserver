/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.metadata.data.dto.AttributeCollection;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.FieldTypeEnum;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class AttributeDataProvider extends GeoServerDataProvider<AttributeConfiguration> {

    private static final long serialVersionUID = -4454769618643460913L;

    public static Property<AttributeConfiguration> NAME =
            new BeanProperty<AttributeConfiguration>("name", "label");

    public static Property<AttributeConfiguration> VALUE =
            new AbstractProperty<AttributeConfiguration>("value") {
                private static final long serialVersionUID = -1889227419206718295L;

                @Override
                public Object getPropertyValue(AttributeConfiguration item) {
                    return null;
                }
            };

    private List<AttributeConfiguration> items = new ArrayList<>();

    public AttributeDataProvider() {
        ConfigurationService metadataConfigurationService =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ConfigurationService.class);
        for (AttributeConfiguration config :
                metadataConfigurationService.getMetadataConfiguration().getAttributes()) {
            if (config.getFieldType() != FieldTypeEnum.DERIVED) { // don't display derived fields!
                items.add(config);
            }
        }
    }

    /**
     * Provide attributes for the given complex type configuration.
     *
     * @param typename
     */
    public AttributeDataProvider(String typename) {
        super();
        ConfigurationService metadataConfigurationService =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ConfigurationService.class);
        AttributeCollection typeConfiguration =
                metadataConfigurationService.getMetadataConfiguration().findType(typename);
        if (typeConfiguration != null) {
            for (AttributeConfiguration config : typeConfiguration.getAttributes()) {
                items.add(config);
            }
        }
    }

    @Override
    protected List<Property<AttributeConfiguration>> getProperties() {
        return Arrays.asList(NAME, VALUE);
    }

    @Override
    protected List<AttributeConfiguration> getItems() {
        return items;
    }
}
