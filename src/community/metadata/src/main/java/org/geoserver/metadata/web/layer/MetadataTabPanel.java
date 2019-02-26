/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.layer;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.metadata.data.service.GeonetworkXmlParser;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.metadata.data.service.RemoteDocumentReader;
import org.geoserver.metadata.data.service.impl.MetadataConstants;
import org.geoserver.metadata.web.panel.ImportGeonetworkPanel;
import org.geoserver.metadata.web.panel.ImportTemplatePanel;
import org.geoserver.metadata.web.panel.MetadataPanel;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.publish.PublishedEditTabPanel;
import org.geotools.util.logging.Logging;
import org.w3c.dom.Document;

/**
 * A tabpanel that adds the metadata configuration to the layer.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class MetadataTabPanel extends PublishedEditTabPanel<LayerInfo> {

    private static final long serialVersionUID = -552158739086379566L;

    private static final Logger LOGGER = Logging.getLogger(MetadataTabPanel.class);

    private IModel<List<MetadataTemplate>> templatesModel;

    private HashMap<String, List<Integer>> derivedAtts;

    private IModel<ComplexMetadataMap> metadataModel;

    public MetadataTabPanel(
            String id, IModel<LayerInfo> model, IModel<List<MetadataTemplate>> templatesModel) {
        super(id, model);
        this.templatesModel = templatesModel;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onInitialize() {
        super.onInitialize();

        ResourceInfo resource = findParent(ResourceConfigurationPage.class).getResourceInfo();
        derivedAtts =
                (HashMap<String, List<Integer>>)
                        resource.getMetadata().get(MetadataConstants.DERIVED_KEY);

        Serializable custom = resource.getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY);
        if (!(custom instanceof HashMap<?, ?>)) {
            custom = new HashMap<String, Serializable>();
            resource.getMetadata().put(MetadataConstants.CUSTOM_METADATA_KEY, custom);
        }
        if (!(derivedAtts instanceof HashMap<?, ?>)) {
            derivedAtts = new HashMap<String, List<Integer>>();
            resource.getMetadata().put(MetadataConstants.DERIVED_KEY, derivedAtts);
        }

        metadataModel =
                new Model<ComplexMetadataMap>(
                        new ComplexMetadataMapImpl((HashMap<String, Serializable>) custom));

        ComplexMetadataService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ComplexMetadataService.class);
        service.init(metadataModel.getObject());

        // Link with templates panel
        this.add(
                new ImportTemplatePanel(
                        "importTemplatePanel",
                        resource.getId(),
                        (IModel<List<MetadataTemplate>>) templatesModel) {
                    private static final long serialVersionUID = -8056914656580115202L;

                    @Override
                    protected void handleUpdate(AjaxRequestTarget target) {
                        updateModel();
                        target.add(
                                metadataPanel()
                                        .replaceWith(
                                                new MetadataPanel(
                                                        "metadataPanel",
                                                        metadataModel,
                                                        derivedAtts)));
                    }
                });

        add(new MetadataPanel("metadataPanel", metadataModel, derivedAtts).setOutputMarkupId(true));

        // Geonetwork import panel
        ImportGeonetworkPanel geonetworkPanel =
                new ImportGeonetworkPanel("geonetworkPanel") {
                    private static final long serialVersionUID = -4620394948554985874L;

                    @Override
                    public void handleImport(
                            String url, AjaxRequestTarget target, FeedbackPanel feedbackPanel) {
                        try {
                            // First unlink all templates
                            importTemplatePanel()
                                    .unlinkTemplate(
                                            target, importTemplatePanel().getLinkedTemplates());
                            // Read the file
                            RemoteDocumentReader geonetworkReader =
                                    GeoServerApplication.get()
                                            .getApplicationContext()
                                            .getBean(RemoteDocumentReader.class);
                            GeonetworkXmlParser xmlParser =
                                    GeoServerApplication.get()
                                            .getApplicationContext()
                                            .getBean(GeonetworkXmlParser.class);
                            // import metadata
                            Document doc = geonetworkReader.readDocument(new URL(url));
                            xmlParser.parseMetadata(doc, resource, metadataModel.getObject());
                        } catch (IOException e) {
                            LOGGER.severe(e.getMessage());
                            feedbackPanel.error(e.getMessage());
                            target.add(feedbackPanel);
                        }
                        target.add(
                                metadataPanel()
                                        .replaceWith(
                                                new MetadataPanel(
                                                        "metadataPanel",
                                                        metadataModel,
                                                        derivedAtts)));
                    }
                };
        add(geonetworkPanel);

        // bit of a hack - no other hook to do something before save
        findParent(Form.class)
                .add(
                        new IFormValidator() {
                            private static final long serialVersionUID = 2524175705938882253L;

                            @Override
                            public FormComponent<?>[] getDependentFormComponents() {
                                // TODO Auto-generated method stub
                                return null;
                            }

                            @Override
                            public void validate(Form<?> form) {
                                updateModel();
                            }
                        });
    }

    protected MetadataPanel metadataPanel() {
        return (MetadataPanel) get("metadataPanel");
    }

    protected ImportTemplatePanel importTemplatePanel() {
        return (ImportTemplatePanel) get("importTemplatePanel");
    }

    /** Merge the model and the linked templates. */
    private void updateModel() {
        MetadataTemplateService templateService =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(MetadataTemplateService.class);
        ComplexMetadataService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ComplexMetadataService.class);
        ArrayList<ComplexMetadataMap> maps = new ArrayList<>();
        for (MetadataTemplate template : templatesModel.getObject()) {
            template = templateService.getById(template.getId());
            if (template != null) {
                maps.add(new ComplexMetadataMapImpl(template.getMetadata()));
            }
        }

        service.merge(metadataModel.getObject(), maps, derivedAtts);
    }

    @Override
    public void save() throws IOException {
        MetadataTemplateService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(MetadataTemplateService.class);
        for (MetadataTemplate template : templatesModel.getObject()) {
            service.save(template);
        }
    }
}
