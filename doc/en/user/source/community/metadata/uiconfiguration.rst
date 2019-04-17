.. _community_metadata_uiconfiguration:

UI components overview
======================
The ui for metadata fields is made from a list of components.
The type of the component and how the behave can be configured in the yaml file.
All compontes should be configured as a list wich has the the parent key ``attributes``.


Components options
------------------
A component is defined in the yaml following key-value pairs:

    - `key`_
    - `fieldType`_
    - `label`_
    - `occurrence`_
    - `values`_  (specific components)
    - `derivedFrom`_  (specific components)
    - `typename`_  (specific components)


key
^^^

The key is the identifier for the component and should therefore be unique.
Other configurations can refer the component by using this identifier. E.g the geonetwork mapping, internationalization.

:required:
    yes
:value:
    a unique string

    

fieldType
^^^^^^^^^

Chooses the type of input widget for the component.
A detailed description for each type can be found in the `Field Types`_ section.

:required:
    yes
:value: supported constants

        - COMPLEX
        - TEXT
        - NUMBER
        - TEXT_AREA
        - DATE
        - DATETIME
        - BOOLEAN
        - UUID
        - DROPDOWN
        - SUGGESTBOX
        - DERIVED
    


label
^^^^^

If present this value will be used as the label for the component.
When the label is not present in the yaml cofiguration the key will be used as label. 
Note: when the key is present in the internationalization (i18n) file see `Internationalization support`_  than the value from that file wil be used as the label.

:required:
    no
:value:
    any string
    


occurrence
^^^^^^^^^^

The value for ``occurrence`` determins whether or not the component should displayed as a table or as a single input field.
``SINGLE`` will result in one input field.

    .. figure:: images/single-value.png

        e.g. single value input component of fieldType ``TEXT``.

    Choosing ``REPEAT`` will render the component in a table allowing the user to input multiple values.

    .. figure:: images/repeat.png

        e.g. component of fieldType ``TEXT`` rendered as a table.

    The data in table can be sorted by using the green arrow buttons.

    The default value is ``SINGLE``.

:required:
        no
:value: supported constants

        - SINGLE
        - REPEAT


values
^^^^^^
The choices in a `DROPDOWN`_ or a `SUGGESTBOX`_ can be set using the ``values``  attribute in the yaml. 
This is useful for small list, for larger list it can be better to list the choices in a sepparate .csv file.

derivedFrom
^^^^^^^^^^^
Only used in the `DERIVED`_ component. The attribute ``derivedFrom`` contains the key for the parent on wich the `DERIVED`_ component depends.
Follow the link for more information on the `DERIVED`_ component.

typename
^^^^^^^^
The ``typename`` is a required attribute for `COMPLEX`_ components. It contains the key pointing to the definition of the `COMPLEX`_ component.

Field Types
-----------

        - `TEXT`_
        - `TEXT_AREA`_
        - `UUID`_
        - `NUMBER`_
        - `BOOLEAN`_
        - `DATE`_
        - `DATETIME`_
        - `DROPDOWN`_
        - `SUGGESTBOX`_
        - `DERIVED`_
        - `COMPLEX`_

TEXT
^^^^
Input field that allows any text.

 .. figure:: images/fieldtext.png



.. code:: YAML

  attributes:
    - key: text-field
      fieldType: TEXT

TEXT_AREA
^^^^^^^^^
A multiline input.

 .. figure:: images/fieldtextarea.png



.. code:: YAML

  attributes:
    - key: text-area-field
        fieldType: TEXT_AREA

UUID
^^^^
Input field for a UUID, it allows any text input or the user can generate a UUID.

 .. figure:: images/fielduuid.png



.. code:: YAML

  attributes:
    - key: uuid-field
      fieldType: UUID

NUMBER
^^^^^^
Only numbers are accepted as valid input.

 .. figure:: images/fieldnumber.png



.. code:: YAML

  attributes:
    - key: numer-field
      fieldType: NUMBER

BOOLEAN
^^^^^^^
Input field with checkbox.

 .. figure:: images/fieldboolean.png



.. code:: YAML

  attributes:
    - key: boolean-field
      fieldType: BOOLEAN

DATE
^^^^

Date selection without time information.

 .. figure:: images/fielddate.png



.. code:: YAML

  attributes:
    - key: date-field
      fieldType: DATE


DATETIME
^^^^^^^^

Selection date with time information.

 .. figure:: images/fielddatetime.png



.. code:: YAML

  attributes:
    - key: datetime-field
      fieldType: DATETIME

DROPDOWN
^^^^^^^^
A component for selecting a value from a dropdown. 
The values can be configure with the ``values`` attribute in the yaml or they can be configured in an other .csv file which is best for dropdowns with a lot of choices.


 .. figure:: images/fielddropdown.png


Configuration in the yaml file.

.. code:: YAML

  attributes:
    - key: dropdown-field
      fieldType: DROPDOWN
      values:
            - first
            - second
            - third

To configure the values in a sepparate file add a yaml key ``csvImports`` on the same level as ``attributes`` and add the list of CSV files under this key. 
The first line in each CSV file should contain the key of the dropdown component for wich you want to add the choices.

``metadata-ui.yaml``

.. code:: YAML

  attributes:
    - key: dropdown-field
      fieldType: DROPDOWN
   csvImports:
    - dropdowncontent.csv   
        
``dropdowncontent.csv``

.. code::

    dropdown-field
    first
    second
    third

SUGGESTBOX
^^^^^^^^^^
A component for selecting a value from a suggestbox. Suggestion will be given for the values where the input matches the beginning of the possible values.
The values can be put in a sepparate CSV file in thes same way as for the DROPDOWN_ component. 

.. figure:: images/fieldsuggest.png

.. code:: YAML

  attributes:
    - key: suggestbox-field
      fieldType: SUGGESTBOX
      values:
            - first
            - second
            - third

DERIVED
^^^^^^^
A derived field is a hidden field field whose value depends on an other component. The yaml key ``derivedFrom`` should contain the key of the component it depends on.
When a value is selected in the parent component a matching value for the derived component is seached in csv file or the value with the same index is picked from the values list.


The CSV file should have at least two columns and start wiht the key of the parent component in the first column followed by the values for the parent component, the other columns should contain the key of the derived component in the first row followed by the matching values. 

Example derived field with config in a CSV file:

.. figure:: images/fielddireved.png

``metadata-ui.yaml``

.. code:: YAML

  attributes:
    - key: derived-parent-field
      fieldType: DROPDOWN
    - key: hidden-field
      fieldType: DERIVED
      derivedFrom: derived-parent-field
  csvImports:
    - derived-mapping.csv

``derivedmapping.csv``

.. code::

    derived-parent-field;hidden-field
    parent-value01;hidden-value01
    parent-value02;hidden-value02
    parent-value03;hidden-value03
  
Example derived field with values lists:

``metadata-ui.yaml``

.. code:: YAML

  attributes:
    - key: derived-parent-field
      fieldType: DROPDOWN
      values:
          - parent-value01
          - parent-value02
          - parent-value03
    - key: hidden-field
      fieldType: DERIVED
      derivedFrom: derived-parent-field
      values:
          - hidden-value01
          - hidden-value02
          - hidden-value03

COMPLEX
^^^^^^^
A complex component is composed of multiple ohter components.  The yaml key ``typename`` is added to the component configuration.
On the root level the yaml key ``types`` indecates the beginning of all complex type definition. 
A type definition should contain the ``typename`` followed by the key ``attributes`` wich contains the configuration for the subcomponents.

.. figure:: images/fieldcomplex.png

.. code:: YAML

  attributes:
    - key: complex-type
      fieldType: COMPLEX
      typename: complex-component
  
  types:
     - typename: complex-component
       attributes:
            - key: object-text
              fieldType: TEXT
            - key: object-numer
              fieldType: NUMBER

Advanced concepts
-----------------

secret hardcode  component
^^^^^^^^^^^^^^^^^^^^^^^^^^
.. warning:: TODO


Internationalization support
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
All metadata field labels that appears in the :guilabel:`Metadata fields` can be interationalized.
This is performed by creating an internationalization (i18n) file named metadata.properties. 
Create an entry for each key in the gui configuriation following this pattern:  `PREFIX.attribute-key`

e.g.

``metadata.properties``

.. code::

  metadata.generated.form.metadata-identifier=Unique identifier for the metadata


``metadata_nl.properties``

.. code::

  metadata.generated.form.metadata-identifier=Metadata identificator