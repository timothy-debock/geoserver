/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.metadata.web;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.model.impl.MetadataTemplateImpl;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.metadata.web.panel.MetadataPanel;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geotools.util.logging.Logging;

/**
 * The template page, view or edit the values in the template.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class MetadataTemplatePage extends GeoServerBasePage {

    private static final Logger LOGGER = Logging.getLogger(MetadataTemplatePage.class);

    private static final long serialVersionUID = 2273966783474224452L;

    private final IModel<List<MetadataTemplate>> templates;

    private final IModel<MetadataTemplate> metadataTemplateModel;

    public MetadataTemplatePage(IModel<List<MetadataTemplate>> templates) {
        this(templates, new Model<>(newTemplate()));
    }

    private static MetadataTemplate newTemplate() {
        MetadataTemplateImpl template = new MetadataTemplateImpl();
        template.setId(UUID.randomUUID().toString());
        return template;
    }

    public MetadataTemplatePage(
            IModel<List<MetadataTemplate>> templates,
            IModel<MetadataTemplate> metadataTemplateModel) {
        this.templates = templates;
        this.metadataTemplateModel = metadataTemplateModel;
    }

    public void onInitialize() {
        super.onInitialize();
        IModel<ComplexMetadataMap> metadataModel =
                new Model<ComplexMetadataMap>(
                        new ComplexMetadataMapImpl(
                                metadataTemplateModel.getObject().getMetadata()));

        Form<?> form = new Form<Object>("form");

        AjaxSubmitLink saveButton = createSaveButton();
        saveButton.setOutputMarkupId(true);
        form.add(saveButton);
        form.add(createCancelButton());

        TextField<String> nameField = createNameField(form, saveButton);
        form.add(nameField);

        TextField<String> desicription =
                new TextField<String>(
                        "description",
                        new PropertyModel<String>(metadataTemplateModel, "description"));
        form.add(desicription);

        MetadataPanel metadataTemplatePanel =
                new MetadataPanel("metadataTemplatePanel", metadataModel, null);
        form.add(metadataTemplatePanel);

        this.add(form);
    }

    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.AUTHENTICATED;
    }

    private TextField<String> createNameField(final Form<?> form, final AjaxSubmitLink saveButton) {
        return new TextField<String>(
                "name", new PropertyModel<String>(metadataTemplateModel, "name")) {
            private static final long serialVersionUID = -3736209422699508894L;

            @Override
            public boolean isRequired() {
                return form.findSubmittingButton() == saveButton;
            }
        };
    }

    private AjaxSubmitLink createSaveButton() {
        return new AjaxSubmitLink("save") {
            private static final long serialVersionUID = 8749672113664556346L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                MetadataTemplateService service =
                        GeoServerApplication.get()
                                .getApplicationContext()
                                .getBean(MetadataTemplateService.class);
                try {
                    service.save(metadataTemplateModel.getObject(), true);
                    if (templates.getObject().contains(metadataTemplateModel.getObject())) {
                        templates
                                .getObject()
                                .set(
                                        templates
                                                .getObject()
                                                .indexOf(metadataTemplateModel.getObject()),
                                        metadataTemplateModel.getObject());
                    } else {
                        templates.getObject().add(metadataTemplateModel.getObject());
                    }
                    doReturn();
                } catch (IOException | IllegalArgumentException e) {
                    if (e instanceof IOException) {
                        LOGGER.log(Level.WARNING, e.getMessage(), e);
                    }
                    Throwable rootCause = ExceptionUtils.getRootCause(e);
                    String message =
                            rootCause == null
                                    ? e.getLocalizedMessage()
                                    : rootCause.getLocalizedMessage();
                    if (message != null) {
                        form.error(message);
                    }
                    addFeedbackPanels(target);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                addFeedbackPanels(target);
            }
        };
    }

    private AjaxLink<Object> createCancelButton() {
        return new AjaxLink<Object>("cancel") {
            private static final long serialVersionUID = -6892944747517089296L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                doReturn();
            }
        };
    }
}
