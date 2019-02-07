/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import java.io.IOException;
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.AttributeMappingConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeMappingConfiguration;
import org.geoserver.metadata.data.dto.FieldTypeEnum;
import org.geoserver.metadata.data.dto.OccurrenceEnum;
import org.geoserver.metadata.data.model.ComplexMetadataAttribute;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geoserver.metadata.data.service.GeonetworkXmlParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Repository
public class GeonetworkXmlParserImpl implements GeonetworkXmlParser {

    private static final long serialVersionUID = -4931070325217885824L;

    @Autowired private ConfigurationService configService;

    @Override
    public void parseMetadata(Document doc, ComplexMetadataMap metadataMap) throws IOException {
        for (AttributeMappingConfiguration attributeMapping :
                configService.getMappingConfiguration().getGeonetworkmapping()) {
            AttributeConfiguration att =
                    configService
                            .getMetadataConfiguration()
                            .findAttribute(attributeMapping.getGeoserver());
            if (att == null) {
                throw new IOException(
                        "attribute "
                                + attributeMapping.getGeoserver()
                                + " not found in configuration");
            }
            addAttribute(metadataMap, attributeMapping, att, doc, null);
        }
    }

    private void addAttribute(
            ComplexMetadataMap metadataMap,
            AttributeMappingConfiguration attributeMapping,
            AttributeConfiguration attConfig,
            Document doc,
            Node node)
            throws IOException {
        NodeList nodes = findNode(doc, attributeMapping.getGeonetwork(), node);

        switch (attConfig.getOccurrence()) {
            case SINGLE:
                if (nodes != null && nodes.getLength() > 0) {
                    mapNode(metadataMap, attributeMapping, attConfig, doc, nodes.item(0));
                } else {
                    mapNode(metadataMap, attributeMapping, attConfig, doc, null);
                }
                break;
            case REPEAT:
                if (nodes != null) {
                    for (int count = 0; count < nodes.getLength(); count++) {
                        mapNode(metadataMap, attributeMapping, attConfig, doc, nodes.item(count));
                    }
                }
                break;
        }
    }

    private void mapNode(
            ComplexMetadataMap metadataMap,
            AttributeMappingConfiguration attributeMapping,
            AttributeConfiguration attConfig,
            Document doc,
            Node node)
            throws IOException {
        if (FieldTypeEnum.COMPLEX.equals(attConfig.getFieldType())) {
            AttributeTypeMappingConfiguration typeMapping =
                    configService.getMappingConfiguration().findType(attConfig.getTypename());
            if (typeMapping == null) {
                throw new IOException(
                        "type mapping " + attConfig.getTypename() + " not found in configuration");
            }
            AttributeTypeConfiguration type =
                    configService.getMetadataConfiguration().findType(attConfig.getTypename());
            if (type == null) {
                throw new IOException(
                        "type " + attConfig.getTypename() + " not found in configuration");
            }
            ComplexMetadataMap submap;
            if (OccurrenceEnum.SINGLE.equals(attConfig.getOccurrence())) {
                submap = metadataMap.subMap(attributeMapping.getGeoserver());
            } else {
                int currentSize = metadataMap.size(attributeMapping.getGeoserver());
                submap = metadataMap.subMap(attributeMapping.getGeoserver(), currentSize);
            }
            for (AttributeMappingConfiguration aMapping : typeMapping.getMapping()) {
                AttributeConfiguration att = type.findAttribute(aMapping.getGeoserver());
                if (att == null) {
                    throw new IOException(
                            "attribute "
                                    + aMapping.getGeoserver()
                                    + " not found in type "
                                    + type.getTypename());
                }
                addAttribute(submap, aMapping, att, doc, node);
            }
        } else {
            ComplexMetadataAttribute<String> att;
            if (OccurrenceEnum.SINGLE.equals(attConfig.getOccurrence())) {
                att = metadataMap.get(String.class, attributeMapping.getGeoserver());
            } else {
                int currentSize = metadataMap.size(attributeMapping.getGeoserver());
                att = metadataMap.get(String.class, attributeMapping.getGeoserver(), currentSize);
            }
            att.setValue(node == null ? null : node.getNodeValue());
        }
    }

    private NodeList findNode(Document doc, String geonetwork, Node node) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            xpath.setNamespaceContext(new NamespaceResolver(doc));
            XPathExpression expr = xpath.compile(geonetwork);
            Object result;
            if (node != null) {
                result = expr.evaluate(node, XPathConstants.NODESET);
            } else {
                result = expr.evaluate(doc, XPathConstants.NODESET);
            }
            NodeList nodes = (NodeList) result;
            return nodes;
        } catch (XPathExpressionException e) {

        }
        return null;
    }

    public class NamespaceResolver implements NamespaceContext {
        // Store the source document to search the namespaces
        private Document sourceDocument;

        public NamespaceResolver(Document document) {
            sourceDocument = document;
        }

        // The lookup for the namespace uris is delegated to the stored document.
        public String getNamespaceURI(String prefix) {
            if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
                return sourceDocument.lookupNamespaceURI(null);
            } else {
                return sourceDocument.lookupNamespaceURI(prefix);
            }
        }

        public String getPrefix(String namespaceURI) {
            return sourceDocument.lookupPrefix(namespaceURI);
        }

        @SuppressWarnings("rawtypes")
        public Iterator getPrefixes(String namespaceURI) {
            return null;
        }
    }
}
