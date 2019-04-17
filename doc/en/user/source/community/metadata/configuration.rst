 .. _community_metadata_configuration:

Metadata Module configuration
=============================

.. contents:: :local:
    :depth: 2

Installation
------------

To install the GeoServer Metadata extension:

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

The content of the :guilabel:`Metadata fields` is configured by placing one or multiple `yaml <https://yaml.org/>`__ files describing the UI compontents in the metadata configuration folder, see :ref:`tutorial_metadata` for a real life example.

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

    - **attributes:** a list of GUI components that will be rendered in the tab. They can be a basic type or a complex type, a complex type is a collection of basic types.
    - **types:** a list that defines the fields in each complex type.

:ref:`community_metadata_uiconfiguration` gives an overview of all supported types and advanced features.


Import from Geonetwork
----------------------
The :guilabel:`Import from Geonetwork` option allows the user to import existing metadata from `GeoNetwork <https://geonetwork-opensource.org//>`_.
Two confurations are needed for the import to work:

    - **geonetworks:** configure a list geonetwork endpoints
    - **geonetworkmapping:** define the mapping between the geonetwork fields and the fields configured in the metadata module.

The configuration can be added to the same `yaml <https://yaml.org/>`__ file as the UI configuration or it can be put in a separate file.

Geonetwork endpoint configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
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

Geonetwork mapping configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Each field from Geonetwork can be mapped to a native field from GeoServer or a field from the metadata module. 
The configuration for simple components are added under the yaml attribute `geonetworkmapping`. 
The fields of the type ``COMPLEX`` are mapped under the attribute  `objectmapping`.

The example will map one field (UUID) from the geonetwork xml to UI.

.. code:: YAML    
    
    geonetworkmapping:
        -  geoserver: metadata-identifier
           geonetwork: //gmd:fileIdentifier/gco:CharacterString/text()

A complex object is mapped in the following example:

.. code:: YAML

    objectmapping:
        - typename: responsible-party
          mapping:
            - geoserver: organisation
              geonetwork: .//gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString/text()
            - geoserver: contactinfo
              geonetwork: .//gmd:CI_ResponsibleParty/gmd:contactInfo
            - geoserver: role
              geonetwork: .//gmd:CI_ResponsibleParty/gmd:role/gmd:CI_RoleCode/@codeListValue

Metadata from geonetwork can aslo be mapped to native fields. Do this by setting the `mappingType` to ``NATIVE``

.. code:: YAML

    -  geoserver: title
       geonetwork: //gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString/text()
       mappingType: NATIVE
    -  geoserver: alias
       geonetwork: //gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:alternateTitle/gco:CharacterString/text()
       mappingType: NATIVE

================  ========  ============================
Key               Required  Description
================  ========  ============================
**geoserver**      yes      the key for the attributes in geoserver
**geonetwork**     yes      The `xpath <https://developer.mozilla.org/en-US/docs/Web/XPath>`__ expression pointing to the content from the geonetwork metadata xml file.
**mappingType:**   no        | CUSTOM (default; map to fields from the metadata module configuration)
                             | NATIVE (map to geoserver native fields)
================  ========  ============================

Custom to Native Mapping
------------------------
Sometimes your custom metadata configuration may contain a more complex version of fields already present in geoserver native metadata,
or you may want to derive geoserver native fields (such as URL's, keywords, etcetera) from information in your custom metadata. Native fields
are used by ``GetCapabilities`` requests, and you want to avoid filling in the same information twice. We can automatise deriving these
native fields from custom fields using a custom-to-native mapping configuration. For example in the following configuration:

.. code:: YAML

      customNativeMappings:
        - type: KEYWORDS
          mapping:
            value: KEY_${keywords/name}
            vocabulary: ${keywords/vocabulary}
        - type: IDENTIFIERS
          mapping:
            value: ${identifiers/id}
            authority: ${identifiers/authority}
        - type: METADATALINKS
          mapping:
            value: https://my-host/geonetwork/?uuid=${uuid}
            type: text/html
            metadataType: ISO191156:2003
        - type: METADATALINKS
          mapping:
            value: https://my-host/geonetwork/srv/nl/csw?Service=CSW&Request=GetRecordById&Version=2.0.2&outputSchema=http://www.isotc211.org/2005/gmd&elementSetName=full&id=${uuid}
            type: text/xml
            metadataType: ISO191156:2003

================  ========  ============================
Key               Required  Description
================  ========  ============================
**type**           yes      currently supported: KEYWORDS, IDENTIFIERS, METADATALINKS
**mapping**        yes      | List of key to value pairs. Value contains a literal with or without placeholder that contains custom attribute path (the ``/`` symbol denoting subfields inside complex fields).
                            | Possible keys for KEYWORDS: value, vocabulary
                            | Possible keys for METADATALINKS: value, type, metadataType, about
                            | Possible keys for IDENTIFIERS: value, authority
================  ========  ============================

The synchronisation of the metadata takes place each time a layer is saved. Any information that has been entered by the user in mapped native fields via the GUI will be lost.

CSW extension configuration
---------------------------

The CSW module is a service that exposes the metadata as xml file that can be harvested by GeoNetwork. The documentation for the CSW module can be found here :ref:`csw`

The :ref:`tutorial_metadata` contains a complete mapping producing a valid geonetwork xml.

Geonetwork Harvesting
---------------------
Configure a Geonetwork Harvester pointing to the CSW endpoint.

e.g. `https://localhost:8080/geoserver/csw?Service=CSW&Request=Getcapabilities`

