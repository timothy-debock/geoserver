/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeConfiguration;
import org.geoserver.metadata.data.dto.FieldTypeEnum;
import org.geoserver.metadata.data.dto.MetadataConfiguration;
import org.geoserver.metadata.data.dto.OccurrenceEnum;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Implementation.
 *
 * <p>Node: values for templates that are lists are added in front of the user defined values in
 * order to keep the indexes in the description map constant even when the user modifies the list.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 * @author Niels Charlier
 */
@Repository
public class ComplexMetadataServiceImpl implements ComplexMetadataService {

    @Autowired ConfigurationService configService;

    @Override
    public void merge(
            ComplexMetadataMap destination,
            List<ComplexMetadataMap> sources,
            HashMap<String, List<Integer>> derivedAtts) {

        MetadataConfiguration config = configService.getMetadataConfiguration();

        clearTemplateData(destination, derivedAtts);

        ArrayList<ComplexMetadataMap> reversed = new ArrayList<ComplexMetadataMap>(sources);
        Collections.reverse(reversed);
        for (ComplexMetadataMap source : reversed) {
            mergeAttributes(destination, source, config.getAttributes(), config, derivedAtts);
        }
    }

    @Override
    public void merge(ComplexMetadataMap destination, ComplexMetadataMap source, String typeName) {

        MetadataConfiguration config = configService.getMetadataConfiguration();

        mergeAttributes(
                destination,
                source,
                typeName == null
                        ? config.getAttributes()
                        : config.findType(typeName).getAttributes(),
                config,
                null);
    }

    private void mergeAttributes(
            ComplexMetadataMap destination,
            ComplexMetadataMap source,
            List<AttributeConfiguration> attributes,
            MetadataConfiguration config,
            HashMap<String, List<Integer>> derivedAtts) {
        for (AttributeConfiguration attribute : attributes) {
            if (attribute.getFieldType() == FieldTypeEnum.COMPLEX) {
                mergeComplexField(
                        attribute,
                        config.findType(attribute.getTypename()),
                        config,
                        destination,
                        source,
                        derivedAtts);
            } else {
                mergeSimpleField(attribute, destination, source, derivedAtts);
            }
        }
    }

    private void mergeSimpleField(
            AttributeConfiguration attribute,
            ComplexMetadataMap destination,
            ComplexMetadataMap source,
            HashMap<String, List<Integer>> derivedAtts) {

        if (derivedAtts != null && !derivedAtts.containsKey(attribute.getKey())) {
            derivedAtts.put(attribute.getKey(), new ArrayList<>());
        }
        ArrayList<Integer> indexes = new ArrayList<>();

        switch (attribute.getOccurrence()) {
            case SINGLE:
                String sourceValue = source.get(String.class, attribute.getKey()).getValue();
                if (sourceValue != null) {
                    destination.get(String.class, attribute.getKey()).setValue(sourceValue);
                    indexes.add(0);
                }
                break;
            case REPEAT:
                int startIndex = 0;
                int sourceSize = source.size(attribute.getKey());
                if (derivedAtts != null) {
                    startIndex = derivedAtts.get(attribute.getKey()).size();
                    // SHIFT user content
                    for (int i = destination.size(attribute.getKey()) - 1; i >= 0; i--) {
                        if (i >= startIndex) {
                            String value =
                                    destination.get(String.class, attribute.getKey(), i).getValue();
                            destination
                                    .get(String.class, attribute.getKey(), i + sourceSize)
                                    .setValue(value);
                        }
                    }
                }
                // insert template content
                for (int i = 0; i < sourceSize; i++) {
                    sourceValue = source.get(String.class, attribute.getKey(), i).getValue();
                    int index = startIndex + i;
                    indexes.add(index);
                    destination.get(String.class, attribute.getKey(), index).setValue(sourceValue);
                }
        }
        // keep track of the values that are from the template
        if (derivedAtts != null) {
            derivedAtts.get(attribute.getKey()).addAll(indexes);
        }
    }

    private void mergeComplexField(
            AttributeConfiguration attribute,
            AttributeTypeConfiguration type,
            MetadataConfiguration config,
            ComplexMetadataMap destination,
            ComplexMetadataMap source,
            HashMap<String, List<Integer>> derivedAtts) {

        ArrayList<Integer> indexes = new ArrayList<>();
        if (derivedAtts != null) {
            if (!derivedAtts.containsKey(attribute.getKey())) {
                derivedAtts.put(attribute.getKey(), new ArrayList<>());
            }
        }

        switch (attribute.getOccurrence()) {
            case SINGLE:
                if (source.size(attribute.getKey()) > 0) {
                    destination.delete(attribute.getKey());
                    ComplexMetadataMap sourceMap = source.subMap(attribute.getKey());
                    ComplexMetadataMap destinationMap = destination.subMap(attribute.getKey());
                    mergeAttributes(destinationMap, sourceMap, type.getAttributes(), config, null);
                    indexes.add(0);
                }
                break;
            case REPEAT:
                int startIndex = 0;
                int sourceSize = source.size(attribute.getKey());
                if (derivedAtts != null) {
                    startIndex = derivedAtts.get(attribute.getKey()).size();
                    // SHIFT user content
                    for (int i = destination.size(attribute.getKey()) - 1; i >= 0; i--) {
                        if (i >= startIndex) {
                            ComplexMetadataMap orig = destination.subMap(attribute.getKey(), i);
                            ComplexMetadataMap shifted =
                                    destination.subMap(attribute.getKey(), i + sourceSize);
                            mergeAttributes(shifted, orig, type.getAttributes(), config, null);
                        }
                    }
                }
                // insert template content
                for (int i = 0; i < source.size(attribute.getKey()); i++) {
                    ComplexMetadataMap sourceMap = source.subMap(attribute.getKey(), i);
                    int index = startIndex + i;
                    ComplexMetadataMap destinationMap =
                            destination.subMap(attribute.getKey(), index);
                    indexes.add(index);
                    mergeAttributes(destinationMap, sourceMap, type.getAttributes(), config, null);
                }
        }
        // keep track of the values that are from the template
        if (derivedAtts != null) {
            derivedAtts.get(attribute.getKey()).addAll(indexes);
        }
    }

    private void clearTemplateData(
            ComplexMetadataMap destination, HashMap<String, List<Integer>> derivedAtts) {
        if (derivedAtts != null) {
            for (String key : derivedAtts.keySet()) {
                if (configService.getMetadataConfiguration().findAttribute(key).getOccurrence()
                        == OccurrenceEnum.REPEAT) {
                    for (Integer index : derivedAtts.get(key)) {
                        destination.delete(key, index);
                    }
                } else {
                    destination.delete(key);
                }
            }
            derivedAtts.clear();
        }
    }

    @Override
    public void init(ComplexMetadataMap map) {
        init(map, configService.getMetadataConfiguration().getAttributes());
    }

    @Override
    public void init(ComplexMetadataMap subMap, AttributeConfiguration attributeConfiguration) {
        AttributeTypeConfiguration typeConfiguration =
                configService
                        .getMetadataConfiguration()
                        .findType(attributeConfiguration.getTypename());
        if (typeConfiguration != null) {
            init(subMap, typeConfiguration.getAttributes());
        }
    }

    private void init(ComplexMetadataMap map, List<AttributeConfiguration> attributes) {
        for (AttributeConfiguration config : attributes) {
            if (config.getFieldType() != FieldTypeEnum.COMPLEX) {
                map.get(Serializable.class, config.getKey()).init();
            } else {
                AttributeTypeConfiguration typeConfiguration =
                        configService.getMetadataConfiguration().findType(config.getTypename());
                if (typeConfiguration != null) {
                    int size;
                    if (config.getOccurrence() == OccurrenceEnum.REPEAT
                            && (size = map.size(config.getKey())) > 0) {
                        for (int i = 0; i < size; i++) {
                            init(map.subMap(config.getKey(), i), typeConfiguration.getAttributes());
                        }
                    } else {
                        init(map.subMap(config.getKey()), typeConfiguration.getAttributes());
                    }
                }
            }
        }
    }

    @Override
    public void copy(ComplexMetadataMap source, ComplexMetadataMap dest, String typeName) {
        AttributeTypeConfiguration typeConfiguration =
                configService.getMetadataConfiguration().findType(typeName);

        for (AttributeConfiguration config : typeConfiguration.getAttributes()) {
            if (config.getFieldType() != FieldTypeEnum.COMPLEX) {
                dest.get(Serializable.class, config.getKey())
                        .setValue(
                                ComplexMetadataMapImpl.dimCopy(
                                        source.get(Serializable.class, config.getKey())
                                                .getValue()));
            } else {
                copy(
                        source.subMap(config.getKey()),
                        dest.subMap(config.getKey()),
                        config.getTypename());
            }
        }
    }
}
