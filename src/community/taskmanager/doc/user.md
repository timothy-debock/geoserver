# GeoServer Task Manager - User's Guide

[Task Manager](../readme.md)

* [Installation](#installation)
* [Server Configuration](#server-configuration)
* [Security](#security)
* [Graphical User Interface](#graphical-user-interface)
* [Task Types](#task-types)
* [Examples](#examples)

## Installation

To install the GeoServer Task Manager extension:

* Download the extension from the [GeoServer Download Page](http://geoserver.org/download). The file name is called `geoserver-*-taskmanager-plugin.zip`, where `*` is the version/snapshot name.

* Extract this file and place the JARs in ``WEB-INF/lib``.

* Perform any configuration required by your servlet container, and then restart. On startup, Task Manager will create a configuration directory `taskmanager` in the GeoServer Data Directory. You will be able to see the Task Manager configuration pages from the GeoServer WebGUI menu.

## Server Configuration

### Configuration Database & Clustering

By default, Task Manager will create a H2 database in its configuration directory. This can be easily changed to any JDBC resource via the `taskmanager.properties` file.

The configuration directory also contains a Spring configuration file called `taskManager-applicationContext.xml` which allows more advanced configuration.

TaskManager uses [Quartz Scheduler](http://www.quartz-scheduler.org). If you are running Task Manager in a clustered environment, you must configure Quartz to use a database as well as Task Manager. See the commented block in the spring configuration and the [Quartz documentation](http://www.quartz-scheduler.org/documentation/quartz-2.x/configuration/ConfigJDBCJobStoreClustering.html) for further instructions. The database used by Quartz may be the same as the Task Manager configuration database.

Furthermore, a property should be added to the `taskmanager.properties` file each of the nodes except for one: ``batchJobService.init=false``. This is necessary because otherwise all of the nodes will attempt to load all of the same batches in to the clustered quartz database at the same time at start-up, which is likely to cause issues. This initialisation needs to happen only once for the entire cluster.

### Databases

Task Manager allows any number of databases to be used both as sources and targets for data transfer operations. These are configured via the Spring configuration file. Currently only PostGis is supported.

```xml
<bean class="org.geoserver.taskmanager.external.impl.PostgisDbSourceImpl"> 
  	<property name="name" value="testsourcedb"/> 
	<property name="host" value="hostname" /> 
	<property name="db" value="dbname" /> 
	<!-- optional --> <property name="schema" value="schema" /> 
	<property name="username" value="username" />
	<property name="password" value="password" /> 
</bean>
```

### External GeoServers

Task Manager allows any number of external geoservers to be used as targets for layer publications. These are configured via the Spring configuration file.

```xml
<bean class="org.geoserver.taskmanager.external.impl.ExternalGSImpl"> 
  	<property name="name" value="mygs"/> 
	<property name="url" value="http://my.geoserver/geoserver" /> 
    	<property name="username" value="admin" />
    	<property name="password" value="geoserver" />
</bean>
```

### File Services

File Services are used to upload and access files such as raster layers. They are configured via the Spring configuration file.

```xml
<bean class="org.geoserver.taskmanager.fileservice.impl.FileServiceImpl">
    <property name="rootFolder" value="/tmp"/>
    <property name="name" value="Temporary Directory"/>
</bean>
```

## Security

Each configuration and each independent batch is associated with a workspace in GeoServer (when the workspace field is empty, it is automatically associated with the default workspace in geoserver). The configuration or batch takes its security permissions directly from this workspace.

* If the user has reading permissions on the workspace, they may view the configuration or batch.

* If the user has writing permissions on the workspace, they may run the batch or the batches in the configuration.

* If the user has administrative permissions on the workspace, they may edit the configuration/batch.

## Graphical User Interface

Currently GeoServer Task Manager can only be configured and operated from the GeoServer WebGUI.

### Templates

From the templates page, new templates can be created (or copied from existing templates), existing templates can be edited and removed. 

Once you open a new or existing template, attributes, tasks and batches can be edited. The attribute table adjusts automatically based on the information in the tasks table; and only the values must be filled in. In the task table, the name and parameters of each task can be edited, and new tasks can be created. Batches can be created and edited from here as well, however the template must exist in order to be able to do that (in case of a new template, one must click `apply` once before creating new batches). Batches created or edited from here are not permanent until the template has been saved. 

### Configurations

From the configurations page, new configurations can be created from scratch or from templates (or copied from existing configurations), existing configurations can be edited and removed. 

Once you open a new or existing configuration, attributes, tasks and batches can be edited. The attribute table adjusts automatically based on the information in the tasks table; and only the values must be filled in. In the task table, the name and parameters of each task can be edited, and new tasks can be created. Batches can be created and edited from here as well, however the configuration must exist in order to be able to do that (in case of a new configuration, one must click `apply` once before creating batches). Batches created or edited from here are not permanent until the template has been saved. Also, in case that the [[conditions]](basic.md#batches) are met, batch runs can be started, and the status/history of current and past batch runs can be displayed.



### Batches

From the batches page, new templates can be batches, existing batches can be edited and removed. Also, in case that the [[conditions]](basic.md#batches) are met, batch runs can be started, and the status/history of current and past batch runs can be displayed.

## Task Types

* [CopyTableTask] Copy a database table from one database to another. The user can specify a source database, source table name, target database and target table name. Supports commit/rollback by creating a temporary table.

* [CreateViewTask] Create a view based on a single table. The user can specify the database, the table name, the selected fields and (optionally) a where condition. Supports commit/rollback by creating a temporary view.

* [CreateComplexViewTask] Create a view based on a multiple tables. The user can specify the database and a whole query, where it can use any other configuration attribute in the form of '${placeholder}'. Supports commit/rollback by creating a temporary view.

* [CopyFileTask] Copy a file from one file service to another. Commit/rollback is supported by a versioning system, where the version of the file is inserted into the file name. The location of the version number is specified in the path as `###`. On commit, the older version is removed. On rollback, the newer version is removed. The publication tasks will automatically publish the latest version.

* [LocalDbPublicationTask] Publish a database layer locally. The user can specify database, table and a layer name. Supports commit/rollback by advertising or removing the layer it created.

* [RemoteDbPublicationTask] Publish a database layer to another geoserver. The user can specify a target geoserver, a source layer and a target database. All information is taken from the source layer except for the target database which may be different. Supports commit/rollback through creating a temporary (unadvertised) layer.

* [LocalFilePublicationTask] Publish a file layer locally (taster or shapefile). The user can specify a file service, a file (which can be uploaded unto the service) and a layer name. Supports commit/rollback by advertising or removing the layer it created.

* [RemoteFilePublicationTask] Publish a file layer locally (taster or shapefile). The user can specify a target geoserver, a source layer and a target file service and path (optional). All information is taken from the source layer except for the file service and path which may be different. Supports commit/rollback through creating a temporary (unadvertised) layer.

* [MetaDataSyncTask] Synchronise the metadata between a local layer and a layer on another geoserver (without re-publishing). The user can specify a target geoserver, a local and a remote layer. Does not support commit/rollback.

## Import Tool

The import tool allows bulk creation of an unlimited amount of configurations on the basis of a template and a CSV file with attribute values. Contrary to the rest of the configuration, this function is only exposed via a REST service and not via the GUI. The import tool will generate a new configuration for each line in the CSV file, except for the first. The first line must specify the attribute names which should all match attributes that exist in the template. The CSV file must specify a valid attribute value for each required attribute.

To invoke the import tool, ``POST`` your CSV file to ``http://{geoserver-host}/geoserver/taskmanager-import/{template}``

## Examples


