/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.wicket.util.string.Strings;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.SqlUtil;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The copy table task type.
 * 
 * @author Niels Charlier
 *
 */
@Component
public class CopyTableTaskTypeImpl implements TaskType {
    
    public static final String NAME = "CopyTable";

    public static final String PARAM_SOURCE_DB_NAME = "source-database";

    public static final String PARAM_TARGET_DB_NAME = "target-database";

    public static final String PARAM_TABLE_NAME = "table-name";
    
    //public static final String PARAM_TYPE_SCHEMA_MAPPING = "type-schema-mapping";

    public static final String PARAM_TARGET_TABLE_NAME = "target-table-name";
        
    private static final Logger LOGGER = Logging.getLogger(CopyTableTaskTypeImpl.class);
    
    private static final int BATCH_SIZE = 1000;
    
    @Autowired
    ExtTypes extTypes;

    private final Map<String, ParameterInfo> paramInfo = new LinkedHashMap<String, ParameterInfo>();

    @PostConstruct
    public void initParamInfo() {
        paramInfo.put(PARAM_SOURCE_DB_NAME, new ParameterInfo(PARAM_SOURCE_DB_NAME, extTypes.dbName, true));
        paramInfo.put(PARAM_TARGET_DB_NAME, new ParameterInfo(PARAM_TARGET_DB_NAME, extTypes.dbName, true));
        paramInfo.put(PARAM_TABLE_NAME, new ParameterInfo(PARAM_TABLE_NAME, extTypes.tableName(true), true)
                .dependsOn(paramInfo.get(PARAM_SOURCE_DB_NAME)));
        /*paramInfo.put(PARAM_TYPE_SCHEMA_MAPPING, new ParameterInfo(PARAM_TYPE_SCHEMA_MAPPING, 
                ParameterType.STRING, false));*/
        paramInfo.put(PARAM_TARGET_TABLE_NAME, new ParameterInfo(PARAM_TARGET_TABLE_NAME, extTypes.tableName(false), false)
                .dependsOn(paramInfo.get(PARAM_TARGET_DB_NAME)));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }
    
    @Override
    public TaskResult run(Batch batch, Task task, Map<String, Object> parameterValues) throws TaskException {
        final DbSource sourcedb = (DbSource) parameterValues.get(PARAM_SOURCE_DB_NAME);
        final DbSource targetdb = (DbSource) parameterValues.get(PARAM_TARGET_DB_NAME);
        final String tableName = (String) parameterValues.get(PARAM_TABLE_NAME);
        //final Map<String, String> typeSchemaMapping = 
        //        buildMap((String) parameterValues.get(PARAM_TYPE_SCHEMA_MAPPING));
        
        String _targetTableName = (String) parameterValues.get(PARAM_TARGET_TABLE_NAME);
        if (Strings.isEmpty(_targetTableName)) {
            _targetTableName = tableName;
        }
        String _nqTargetTableName = _targetTableName;
        String _tempTableName = "_temp_" + UUID.randomUUID().toString().replace('-', '_');
        String[] split = _targetTableName.split("\\.", 2);
        if (split.length == 2) {
            _tempTableName = split[0] + "." + _tempTableName;
            _nqTargetTableName = split[1];
        }
        
        final String targetTableName = _targetTableName; //qualified
        final String nqTargetTableName = _nqTargetTableName; //not qualified
        final String tempTableName = _tempTableName;
        
        try (Connection sourceConn = sourcedb.getDataSource().getConnection()) {
            try (Connection destConn = targetdb.getDataSource().getConnection()) {
                try (Statement stmt = sourceConn.createStatement()) {
                    try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + SqlUtil.quote(tableName) + "")) {

                        ResultSetMetaData rsmd = rs.getMetaData();

                        // create the temp table structure                        
                        StringBuilder sb = new StringBuilder("CREATE TABLE ").append(tempTableName)
                                .append(" ( ");
                        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                            sb.append(rsmd.getColumnLabel(i)).append(" ")
                                   // .append(targetType(rsmd.getColumnTypeName(i), typeSchemaMapping));  
                                    .append(rsmd.getColumnTypeName(i));
                            switch (rsmd.isNullable(i)) {
                            case ResultSetMetaData.columnNoNulls:
                                 sb.append(" NOT NULL");   
                                 break;
                            case ResultSetMetaData.columnNullable:
                                sb.append("  NULL");                                                                                                                                                                                                                              sb.append(" NULL");   
                                break;
                            }
                            sb.append(", ");
                        }
                        sb.append("PRIMARY KEY (").append(getPrimaryKey(sourceConn, tableName))
                                .append("), ");
                        for (String unique : getUniques(sourceConn, tableName)) {
                            sb.append("UNIQUE (").append(unique).append("), ");
                        }
                        sb.setLength(sb.length() - 2);
                        sb.append(" ) ");
                        String dump = sb.toString();
                        LOGGER.log(Level.FINE, "creating temporary table: " + dump);

                        try (Statement stmt2 = destConn.createStatement()) {
                            stmt2.executeUpdate(dump);
                        }
                        
                        // copy the data
                        sb = new StringBuilder("INSERT INTO ").append(tempTableName)
                                .append(" VALUES (");
                        for (int i = 0; i < rsmd.getColumnCount(); i++) {
                            if (i > 0) {
                                sb.append(",");
                            }
                            sb.append("?");
                        }
                        sb.append(")");
                        
                        LOGGER.log(Level.FINE, "inserting records: " + sb.toString());

                        try (PreparedStatement pstmt = destConn.prepareStatement(sb.toString())) {
                            int batchSize = 0;
                            while (rs.next()) {
                                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                                    pstmt.setObject(i, rs.getObject(i));
                                }
                                pstmt.addBatch();
                                batchSize++;
                                if (batchSize >= BATCH_SIZE) {
                                    pstmt.executeBatch();
                                    batchSize = 0;
                                }
                            }
                            if (batchSize > 0) {
                                pstmt.executeBatch();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            //clean-up if necessary 
            try (Connection conn = targetdb.getDataSource().getConnection()) {
                try (Statement stmt = conn.createStatement()){
                    stmt.executeUpdate("DROP TABLE IF EXISTS " + SqlUtil.quote(tempTableName));
                }
            } catch (SQLException e2) {}
            
            throw new TaskException(e);
        }
        
        return new TaskResult() {            
            @Override
            public void commit() throws TaskException {
                try (Connection conn = targetdb.getDataSource().getConnection()) {
                    try (Statement stmt = conn.createStatement()){
                        stmt.executeUpdate("DROP TABLE IF EXISTS " + SqlUtil.quote(targetTableName));
                        stmt.executeUpdate("ALTER TABLE " + tempTableName + " RENAME TO \"" + 
                                SqlUtil.quote(nqTargetTableName) + "\"");
                    }
                } catch (SQLException e) {
                    throw new TaskException(e);
                }
            }

            @Override
            public void rollback() throws TaskException {
                try (Connection conn = targetdb.getDataSource().getConnection()) {
                    try (Statement stmt = conn.createStatement()){
                        stmt.executeUpdate("DROP TABLE \""+ tempTableName+ "\"");
                    }
                } catch (SQLException e) {
                    throw new TaskException(e);
                }
            }

        };
    }

    @Override
    public void cleanup(Task task, Map<String, Object> parameterValues) throws TaskException {
        final DbSource targetDb = (DbSource) parameterValues.get(PARAM_TARGET_DB_NAME);
        String tableName = (String) parameterValues.get(PARAM_TARGET_TABLE_NAME);
        if (Strings.isEmpty(tableName)) {
            tableName = (String) parameterValues.get(PARAM_TABLE_NAME);
        }
        
        try (Connection conn = targetDb.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()){
                stmt.executeUpdate("DROP TABLE IF EXISTS \"" + SqlUtil.quote(tableName) + "\"");
            }
        } catch (SQLException e) {
            throw new TaskException(e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
    
    //helper methods
    
    /*private static Map<String, String> buildMap(String mapping) {
        Map<String, String> map = new HashMap<String, String>();
        
        if (mapping != null) {
            for (String element : mapping.split(";")) {
                String[] kvp = element.split(":");
                if (kvp.length == 2) {
                    map.put(kvp[0].trim(), kvp[1].trim());
                }
            }
        }
        
        return map;
    }

    private String targetType(String columnTypeName, Map<String, String> typeSchemaMapping) {
        String[] parts = columnTypeName.split("\\.", 2);
        if (parts.length == 2) {
            String targetSchema = typeSchemaMapping.get(parts[0].replaceAll("\"", ""));
            if (targetSchema != null) {
                return "\"" + targetSchema + "\"" + "." + parts[1];
            }
        }
        return columnTypeName;
    }*/
        
    private static String getPrimaryKey(Connection conn, String tableName) throws SQLException {
        String schema = null;
        String[] split = tableName.split("\\.", 2);
        if (split.length == 2) {
            schema = split[0];
            tableName = split[1];
        }
        try (ResultSet rsPrimaryKeys = conn.getMetaData().getPrimaryKeys(null, schema, tableName)) {
            StringBuilder sb = new StringBuilder();            
            while (rsPrimaryKeys.next()) {
                sb.append(rsPrimaryKeys.getString("COLUMN_NAME"))
                    .append(", ");
            }
            sb.setLength(sb.length() - 2);
            return sb.toString();
        }
    }
    
    private static List<String> getUniques(Connection conn, String tableName) throws SQLException {
        try (ResultSet rsUnique = conn.getMetaData().getIndexInfo(null, null, tableName, true, false)) {
            List<String> pkColumns = new ArrayList<>();
            String lastIndexName = null;
            StringBuilder sb = null;
            while (rsUnique.next()) {
                String indexName = rsUnique.getString("INDEX_NAME");
                if (lastIndexName == null || !indexName.equals(lastIndexName)) {
                    if (sb != null) {
                        pkColumns.add(sb.toString());
                    }
                    sb = new StringBuilder(rsUnique.getString("COLUMN_NAME"));
                    lastIndexName = indexName;
                } else {
                    sb.append(",").append(rsUnique.getString("COLUMN_NAME"));
                }
            }
            return pkColumns;
        }
    }

}