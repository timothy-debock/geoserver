/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import it.geosolutions.geoserver.rest.encoder.GSAbstractStoreEncoder;
import it.geosolutions.geoserver.rest.encoder.datastore.GSPostGISDatastoreEncoder;
import org.geoserver.taskmanager.util.NamedImpl;
import org.geoserver.taskmanager.util.SqlUtil;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic datasource that only takes the connection url and driver class as input in
 * addition the the user name and password.
 * The schema attribute is optional.
 *
 * @author Timothy De Bock
 */
public class GenericDbSourceImpl extends NamedImpl implements DbSource {

    private String connectionUrl;

    private String driver;

    private String username;

    private String password;

    private String schema;

    private Dialect dialect = new DefaultDialectImpl();

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Override
    public DataSource getDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setUrl(connectionUrl);
        return dataSource;
    }

    @Override
    public GSAbstractStoreEncoder getStoreEncoder(String name) {
        GSPostGISDatastoreEncoder encoder = new GSPostGISDatastoreEncoder(name);
        encoder.setUser(username);
        encoder.setPassword(password);
        return encoder;
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
        params.put(PostgisNGDataStoreFactory.SCHEMA.key, schema);
        params.put(PostgisNGDataStoreFactory.USER.key, username);
        params.put(PostgisNGDataStoreFactory.PASSWD.key, password);
        return params;
    }

    @Override
    public GSAbstractStoreEncoder postProcess(GSAbstractStoreEncoder encoder, DbTable table) {
        if (table != null) {
            String schema = SqlUtil.schema(table.getTableName());
            if (schema != null) {
                ((GSPostGISDatastoreEncoder) encoder).setSchema(schema);
            }
        }
        return encoder;
    }

    @Override
    public Dialect getDialect() {
        return new GenericDialectImpl();
    }
}
