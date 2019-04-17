 .. _community_metadata_configuration:

Metadata Module configuration
=============================

Installation
------------

To install the GeoServer Task Manager extension:

-  Download the extension from the `GeoServer Download
   Page <http://geoserver.org/download>`__. The file name is called
   ``geoserver-*-metadata-plugin.zip``, where ``*`` is the
   version/snapshot name.

-  Extract this file and place the JARs in ``WEB-INF/lib``.

-  Perform any configuration required by your servlet container, and
   then restart.  On startup, Metadata module will create a configuration
   directory ``metadata`` in the GeoServer Data Directory. The module will scan all `yaml <https://yaml.org/>`__ files in the ``metadata`` directory.

Gui configuration
-----------------
By default the metadata module will add an extra tab to the edit layer page. Open the layer: navigate to :menuselection:`Layers --> Choose the layer --> Metadata tab`.

.. figure:: images/empty-default.png
  
  The initial UI. Note the :guilabel:`Metadata fields` panel is still empty

The content of the :guilabel:`Metadata fields` is configured by placing one or multiple `yaml <https://yaml.org/>`__ files describing the UI compontents in the metadata configuration folder, see `Example configuration`_ for a real life example.

Example UI configuration:

.. code:: YAML

  attributes:
    - key: metadata-identifier
      fieldType: UUID
    - key: metadata-datestamp
      label: Date
      fieldType: DATETIME
    - key: data-language
      fieldType: DROPDOWN
      values:
            - dut
            - eng
            - fre
            - ger
    - key: topic-category
      fieldType: SUGGESTBOX
      occurrence: REPEAT
      values:
            - farming
            - biota
            - boundaries
            - climatologyMeteorologyAtmosphere
            - economy
            - elevation 
    - key: data-date
      fieldType: COMPLEX
      typename: data-identification-date
      occurrence: REPEAT            
  types:    
     - typename: data-identification-date
       attributes:
        - key: date
          fieldType: DATE
        - key: date-type
          fieldType: DROPDOWN
          values:
            - creation
            - publication
            - revision  

This configuration results in the following GUI:

.. figure:: images/basic-gui.png



There are 2 main parts in the `yaml <https://yaml.org/>`__:

    - **attributes:** a list of GUI components that will be renderd in the tab. The can be a basic type or a complex type, a complex type is a collection of basic types.
    - **types:** a list that defines the fields in each complex type.

:ref:`community_metadata_uiconfiguration` gives an overview of all suported types and advanced features.


Import from Geonetwork mapping
------------------------------
The :guilabel:`Import from Geonetwork` option allows the user to import existing metadata from `GeoNetwork <https://geonetwork-opensource.org//>`_.
Two confurations are needed for the import to work:

    - **geonetworks:** configure a list geonetwork endpoints
    - **geonetworkmapping:** define the mapping between the geonetwork fields and the fields configured in the metadata module.

The configuration can be added to the same `yaml <https://yaml.org/>`__ file as the UI configuration or it can be put in a sepparate file.

Enpoint configuration
^^^^^^^^^^^^^^^^^^^^^
The example will configure 2 endpoints. 

.. code:: YAML

    geonetworks:
        - name: Geonetwork DOV production
          url: https://www.dov.vlaanderen.be/geonetwork/
        - name: Geonetwork test
          url: https://geonetwork-opensource.org/test



================  ========  ============================
Key               Required  Description
================  ========  ============================
**name**           yes       The name fof the geonetwork endpoint that will be shown in the dropdown
**url**            yes       The url of the geonetwork
================  ========  ============================

Mapping configuration
^^^^^^^^^^^^^^^^^^^^^
The example will map one field (UUID) from the geonetwork xml to UI.

.. code:: YAML    
    
    geonetworkmapping:
        -  geoserver: metadata-identifier
           geonetwork: //gmd:fileIdentifier/gco:CharacterString/text()

================  ========  ============================
Key               Required  Description
================  ========  ============================
**geoserver**      yes      the key for the attributes in geoserver
**geonetwork**     yes      The `xpath <https://developer.mozilla.org/en-US/docs/Web/XPath>`__ expression pointing to the content from the geonetwork metadata xml file.
================  ========  ============================

Native attribute mapping
------------------------
.. warning:: TODO


CSW extension configuration
---------------------------

.. warning:: TODO fix link

The CSW module is a service that exposes the metadata as xml file that can be harvested by GeoNetwork. The documentation for the CSW module can be found here :ref:`_services_csw`

The `Example configuration`_ contains a complete mapping producing a valid geonetwork xml.

Geonetwork Harvesting
---------------------
Configure a Geonetwork Harvester pointing to the CSW endpoint.

e.g. `https://localhost:8080/geoserver/csw?Service=CSW&Request=Getcapabilities`


Example configuration
---------------------
.. warning:: At the time of writing 04/2019 the Pull Requests for CSW module are not yet merged into the master. The following configuration depends on features and bugfixes these that pull requests.

    - https://github.com/geoserver/geoserver/pull/3414
    - https://github.com/geoserver/geoserver/pull/3376
    - https://github.com/geoserver/geoserver/pull/3346
    - https://github.com/geoserver/geoserver/pull/3344
    - https://github.com/geoserver/geoserver/pull/3343
    - https://github.com/geoserver/geoserver/pull/3342
    - https://github.com/geoserver/geoserver/pull/3336
    - https://github.com/geoserver/geoserver/pull/3334

Metadata configuration
^^^^^^^^^^^^^^^^^^^^^^

Place the following files in the ``metadata`` folder



UI configuration :download:`metadata-ui.yaml <files/metadata-ui.yaml>`

Translate keys to labels  :download:`metadata.properties <files/metadata.properties>`

Translate keys to Dutch labels  :download:`metadata_nl.properties <files/metadata_nl.properties>`

Content for gemet-concept dropdown  :download:`keyword-gemet-concept.csv <files/keyword-gemet-concept.csv>`

Content for inspire-theme-label & inspire-theme-ref  :download:`keyword-inspire-theme.csv <files/keyword-inspire-theme.csv>`

Geonetwork mapping  :download:`metadata-mapping.yaml <files/metadata-mapping.yaml>`

Geonetwork endpoints  :download:`metadata-geonetwork.yaml <files/metadata-geonetwork.yaml>`

Syncronize native fields  :download:`metadata-native-mapping.yaml <files/metadata-native-mapping.yaml>`


CSW configuration
^^^^^^^^^^^^^^^^^

Map metadata attributes to xml :download:`MD_Metadata.properties <files/MD_Metadata.properties>`

Map Feature Catalogue attributes to xml :download:`FC_FeatureCatalogue.properties <files/FC_FeatureCatalogue.properties>`

Map Record attributes to xml :download:`Record.properties <files/Record.properties>`
