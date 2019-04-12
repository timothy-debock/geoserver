Metadata User Manual
====================
To add metadata to a layer follow the steps in `Adding metadata to Layer`_ . When the metadata is repeated in multiple layers it is easier to create a template and reuse the data in the template for all the layers. See `Managing templates`_ .


Adding metadata to Layer
------------------------

#. `Manually`_
#. `Import from geonetwork`_
#. `Link with metadata template`_




Manually
--------
Open the layer: navigate to :menuselection:`Layers --> Choose the layer --> Metadata tab`.

The metadata fields are available in the panel :guilabel:`Metadata fields`.

Import from geonetwork
----------------------
Choose a geonetwork from the drop downbox, add the UUID from the metadata record and click import.
This will delete the current metadata and replace with the metadata from geonetwork.

Link with metadata template
---------------------------
A metadata template contains the content for metadata fields that are repeated in multiple layers. 
By defining these fields in a template you create one source for the content making it easier to maintain.

To link a layer with template navigate to :menuselection:`Layers --> Choose the layer --> Metadata tab` in the :guilabel:`Link with Template` panel choose a template from the dropdown and click `Link with template`
This will override any values in fields that are not a list. 
For Fields that are a list the values from the template will be added as read only fields. The duplicate values in list will be removed if there are any.

When multiple templates are linked with a layer the priority of the template will determine which values are added. If a field is present in both templates the value of the template with the highest priority will be shown.


Managing templates
------------------
Templates can be created, edited, deleted and ordered in :menuselection:`Metadata --> Templates` .
Editing a template will update all the linked layers, templates that are linked to a layer cannot be removed.


