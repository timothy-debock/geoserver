/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.model.ComplexMetadataAttribute;
import org.geoserver.metadata.data.model.ComplexMetadataAttributeModel;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;

/**
 * Factory to generate a component based on the configuration.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class EditorFactory {

    private static final EditorFactory instance = new EditorFactory();

    // private constructor to avoid client applications to use constructor
    private EditorFactory() {}

    public static EditorFactory getInstance() {
        return instance;
    }

    public <T extends Serializable> Component create(
            AttributeConfiguration configuration, String id, ComplexMetadataMap metadataMap) {

        ComplexMetadataAttribute<T> metadataModel =
                metadataMap.get(getItemClass(configuration), configuration.getKey());
        metadataModel.init();
        IModel<T> model = new ComplexMetadataAttributeModel<T>(metadataModel);

        return create(configuration, id, model, metadataMap.subMap(configuration.getKey()));
    }

    public <T extends Serializable> Component create(
            AttributeConfiguration configuration,
            String id,
            ComplexMetadataAttribute<T> metadataAttribute) {

        IModel<T> model = new ComplexMetadataAttributeModel<T>(metadataAttribute);
        return create(
                configuration,
                id,
                model,
                new ComplexMetadataMapImpl(new HashMap<String, Serializable>()));
    }

    @SuppressWarnings("unchecked")
    private Component create(
            AttributeConfiguration configuration,
            String id,
            IModel<?> model,
            ComplexMetadataMap submap) {

        switch (configuration.getFieldType()) {
            case TEXT:
                return new TextFieldPanel(id, (IModel<String>) model);
            case NUMBER:
                return new NumberFieldPanel(id, (IModel<Integer>) model);
            case BOOLEAN:
                return new CheckBoxPanel(id, (IModel<Boolean>) model);
            case DROPDOWN:
                return new DropDownPanel(id, (IModel<String>) model, configuration.getValues());
            case TEXT_AREA:
                return new TextAreaPanel(id, (IModel<String>) model);
            case DATE:
                return new DateTimeFieldPanel(id, (IModel<Date>) model, false);
            case DATETIME:
                return new DateTimeFieldPanel(id, (IModel<Date>) model, true);
            case UUID:
                return new UUIDFieldPanel(id, (IModel<String>) model);
            case SUGGESTBOX:
                return new AutoCompletePanel(id, (IModel<String>) model, configuration.getValues());
            case COMPLEX:
                return new AttributesTablePanel(
                        id,
                        new AttributeDataProvider(configuration.getTypename()),
                        new Model<ComplexMetadataMap>(submap),
                        null);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> Class<T> getItemClass(
            AttributeConfiguration attributeConfiguration) {
        switch (attributeConfiguration.getFieldType()) {
            case NUMBER:
                return (Class<T>) Integer.class;
            case DATE:
            case DATETIME:
                return (Class<T>) Date.class;
            case BOOLEAN:
                return (Class<T>) Boolean.class;
            default:
                break;
        }
        return (Class<T>) String.class;
    }
}
