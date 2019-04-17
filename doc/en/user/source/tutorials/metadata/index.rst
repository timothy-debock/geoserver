.. _tutorial_metadata:

Metadata example configuration
------------------------------
Creating all the needed configuration files needed to harvest real life data from geoserver can be a tedious and complex task.
Therefore we have added this example configuration.

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

Synchronize native fields  :download:`metadata-native-mapping.yaml <files/metadata-native-mapping.yaml>`


CSW configuration
^^^^^^^^^^^^^^^^^

Map metadata attributes to xml :download:`MD_Metadata.properties <files/MD_Metadata.properties>`

Map Feature Catalogue attributes to xml :download:`FC_FeatureCatalogue.properties <files/FC_FeatureCatalogue.properties>`

Map Record attributes to xml :download:`Record.properties <files/Record.properties>`
