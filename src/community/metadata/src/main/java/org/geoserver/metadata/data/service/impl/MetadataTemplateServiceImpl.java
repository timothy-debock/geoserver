/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import com.thoughtworks.xstream.io.StreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.model.impl.ComplexMetadataIndexReference;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.model.impl.MetadataTemplateImpl;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service that manages the list of templates. When the config of a template is updated all linked
 * metadata is also updated.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
@Component
public class MetadataTemplateServiceImpl implements MetadataTemplateService, ResourceListener {

    private static final Logger LOGGER = Logging.getLogger(MetadataTemplateServiceImpl.class);

    private XStreamPersister persister;

    private static String LIST_FILE = "templates.xml";

    @Autowired private GeoServerDataDirectory dataDirectory;

    @Autowired private ComplexMetadataService metadataService;

    @Autowired private GeoServer geoServer;

    private List<MetadataTemplate> templates = new ArrayList<>();

    public MetadataTemplateServiceImpl() {
        this.persister = new XStreamPersisterFactory().createXMLPersister();
        this.persister
                .getXStream()
                .allowTypesByWildcard(new String[] {"org.geoserver.metadata.data.model.**"});
        this.persister.getXStream().processAnnotations(MetadataTemplateImpl.class);
        this.persister.getXStream().processAnnotations(ComplexMetadataMapImpl.class);
        this.persister.getXStream().processAnnotations(ComplexMetadataIndexReference.class);
    }

    private Resource getFolder() {
        return dataDirectory.get(MetadataConstants.TEMPLATES_DIRECTORY);
    }

    @PostConstruct
    public void init() {
        reload();
        getFolder().addListener(this);
    }

    @Override
    public void changed(ResourceNotification notify) {
        reload();
    }

    @SuppressWarnings("unchecked")
    public void reload() {
        templates.clear();

        Resource folder = getFolder();
        Resource listFile = folder.get(LIST_FILE);

        if (Resources.exists(listFile)) {
            try (InputStream inPriorities = listFile.in()) {
                List<String> priorities = persister.load(inPriorities, List.class);

                for (String id : priorities) {
                    Resource templateFile = folder.get(id + ".xml");
                    try (InputStream inTemplate = templateFile.in()) {
                        MetadataTemplate template =
                                persister.load(inTemplate, MetadataTemplate.class);
                        templates.add(template);
                    } catch (StreamException | IOException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            } catch (StreamException e) {
                LOGGER.warning("Priorities file is empty.");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    @Override
    public void save(MetadataTemplate template, boolean updateLayers) throws IOException {
        // validate
        if (template.getId() == null) {
            throw new IllegalArgumentException("template without id not allowed.");
        }
        if (template.getName() == null) {
            throw new IllegalArgumentException("template without name not allowed.");
        }
        for (MetadataTemplate other : templates) {
            if (!other.equals(template) && other.getName().equals(template.getName())) {
                throw new IllegalArgumentException(
                        "template name " + template.getName() + " not unique.");
            }
        }

        // add or replace in list
        boolean isNew = !templates.contains(template);
        if (isNew) {
            templates.add(template);
        } else {
            templates.set(templates.indexOf(template), template);
        }

        // update layers
        if (updateLayers) {
            Set<String> deletedLayers = new HashSet<>();
            for (String key : template.getLinkedLayers()) {
                ResourceInfo resource = geoServer.getCatalog().getResource(key, ResourceInfo.class);

                if (resource != null) {
                    updateLayer(resource);
                } else {
                    // remove the link because the layer cannot be found.
                    deletedLayers.add(key);
                    LOGGER.log(
                            Level.INFO,
                            "Link to resource "
                                    + key
                                    + " link removed from template "
                                    + template.getName()
                                    + " because it doesn't exist anymore.");
                }
            }
            template.getLinkedLayers().removeAll(deletedLayers);
        }

        getFolder().removeListener(this);

        // persist
        try (OutputStream out = getFolder().get(template.getId() + ".xml").out()) {
            persister.save(template, out);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }

        if (isNew) {
            persistList();
        }

        getFolder().addListener(this);
    }

    @Override
    public void delete(List<MetadataTemplate> newList, MetadataTemplate metadataTemplate) {
        if (!metadataTemplate.getLinkedLayers().isEmpty()) {
            throw new IllegalArgumentException("template is still linked!");
        }
        newList.remove(metadataTemplate);
    }

    @Override
    public void saveList(List<MetadataTemplate> newList, boolean updateLayers) throws IOException {
        if (!templates.containsAll(newList)) {
            throw new IllegalArgumentException("Use save to add new templates.");
        }
        List<MetadataTemplate> deleted = new ArrayList<>(templates);
        deleted.removeAll(newList);

        templates.clear();
        templates.addAll(newList);

        // update layers
        if (updateLayers) {
            for (ResourceInfo resource : geoServer.getCatalog().getResources(ResourceInfo.class)) {
                updateLayer(resource);
            }
        }

        getFolder().removeListener(this);

        // persist
        persistList();

        // remove deleted
        for (MetadataTemplate item : deleted) {
            if (!getFolder().get(item.getId() + ".xml").delete()) {
                LOGGER.warning("Failed to delete template " + item + " from hard drive.");
            }
        }
        deleted.clear();

        getFolder().addListener(this);
    }

    private void persistList() throws IOException {
        List<String> priorities =
                templates.stream().map(template -> template.getId()).collect(Collectors.toList());
        try (OutputStream out = getFolder().get(LIST_FILE).out()) {
            persister.save(priorities, out);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<MetadataTemplate> list() {
        return templates.stream().map(template -> template.clone()).collect(Collectors.toList());
    }

    @Override
    public void increasePriority(List<MetadataTemplate> newList, MetadataTemplate template) {
        int index = newList.indexOf(template);
        newList.add(index - 1, newList.remove(index));
    }

    @Override
    public void decreasePriority(List<MetadataTemplate> newList, MetadataTemplate template) {
        int index = newList.indexOf(template);
        newList.add(index + 1, newList.remove(index));
    }

    private void updateLayer(ResourceInfo resource) {
        @SuppressWarnings("unchecked")
        HashMap<String, List<Integer>> derivedAtts =
                (HashMap<String, List<Integer>>)
                        resource.getMetadata().get(MetadataConstants.DERIVED_KEY);

        Serializable custom = resource.getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY);
        @SuppressWarnings("unchecked")
        ComplexMetadataMapImpl model =
                new ComplexMetadataMapImpl((HashMap<String, Serializable>) custom);

        ArrayList<ComplexMetadataMap> sources = new ArrayList<>();
        for (MetadataTemplate template : templates) {
            if (template.getLinkedLayers() != null
                    && template.getLinkedLayers().contains(resource.getId())) {
                sources.add(new ComplexMetadataMapImpl(template.getMetadata()));
            }
        }

        metadataService.merge(model, sources, derivedAtts);

        geoServer.getCatalog().save(resource);
    }

    @Override
    public MetadataTemplate findByName(String name) {
        for (MetadataTemplate template : templates) {
            if (template.getName().equals(name)) {
                return template.clone();
            }
        }
        return null;
    }
}
