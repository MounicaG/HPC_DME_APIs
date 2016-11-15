/**
 * HpcMetadataDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcHierarchicalMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.exception.HpcException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;

/**
 * <p>
 * HPC Metadata DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMetadataDAOImpl implements HpcMetadataDAO 
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	private static final String GET_COLLECTION_IDS_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where collection.meta_attr_name = ? and collection.meta_attr_value = ?";
	
	private static final String GET_COLLECTION_IDS_NOT_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where collection.meta_attr_name = ? and collection.meta_attr_value <> ?";
	
	private static final String GET_COLLECTION_IDS_LIKE_SQL = 
			"select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
		    "where collection.meta_attr_name = ? and collection.meta_attr_value like ?";
	
	private static final String GET_COLLECTION_IDS_NUM_LESS_THAN_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where collection.meta_attr_name = ? and num_less_than(collection.meta_attr_value, ?) = true";
	
	private static final String GET_COLLECTION_IDS_NUM_LESS_OR_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where collection.meta_attr_name = ? and num_less_or_equal(collection.meta_attr_value, ?) = true";
	
	private static final String GET_COLLECTION_IDS_NUM_GREATER_OR_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where collection.meta_attr_name = ? and num_greater_or_equal(collection.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_IDS_EQUAL_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
		    "where dataObject.meta_attr_name = ? and dataObject.meta_attr_value = ?";
	
	private static final String GET_DATA_OBJECT_IDS_NOT_EQUAL_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
		    "where dataObject.meta_attr_name = ? and dataObject.meta_attr_value <> ?";
	
	private static final String GET_DATA_OBJECT_IDS_LIKE_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
		    "where dataObject.meta_attr_name = ? and dataObject.meta_attr_value like ?";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_LESS_THAN_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
	        "where dataObject.meta_attr_name = ? and num_less_than(dataObject.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_LESS_OR_EQUAL_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
	        "where dataObject.meta_attr_name = ? and num_less_or_equal(dataObject.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_GREATER_OR_EQUAL_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
	        "where dataObject.meta_attr_name = ? and num_greater_or_equal(dataObject.meta_attr_value, ?) = true";
	
	private static final String DATA_OBJECT_LEVEL_EQUAL_FILTER = " and dataObject.level = ?";
	private static final String DATA_OBJECT_LEVEL_NOT_EQUAL_FILTER = " and dataObject.level <> ?";
	private static final String DATA_OBJECT_LEVEL_NUM_LESS_THAN_FILTER = " and dataObject.level < ?";
	private static final String DATA_OBJECT_LEVEL_NUM_LESS_OR_EQUAL_FILTER = " and dataObject.level <= ?";
	private static final String DATA_OBJECT_LEVEL_NUM_GREATER_OR_EQUAL_FILTER = " and dataObject.level >= ?";
	
	private static final String COLLECTION_LEVEL_EQUAL_FILTER = " and collection.level = ?";
	private static final String COLLECTION_LEVEL_NOT_EQUAL_FILTER = " and collection.level <> ?";
	private static final String COLLECTION_LEVEL_NUM_LESS_THAN_FILTER = " and collection.level < ?";
	private static final String COLLECTION_LEVEL_NUM_LESS_OR_EQUAL_FILTER = " and collection.level <= ?";
	private static final String COLLECTION_LEVEL_NUM_GREATER_OR_EQUAL_FILTER = " and collection.level >= ?";
		   
	private static final String COLLECTION_USER_ACCESS_SQL = 
			"select access.object_id from public.\"r_objt_access\" access, " +
			"public.\"r_user_main\" account where account.user_name = ? and access.user_id = account.user_id";
	
	private static final String DATA_OBJECT_USER_ACCESS_SQL = 
			"select access.object_id from public.\"r_objt_access\" access, " +
			"public.\"r_user_main\" account where account.user_name = ? and access.user_id = account.user_id";
	
	private static final String LIMIT_OFFSET_SQL = " order by object_path limit ? offset ?";
	
	private static final String GET_COLLECTION_PATHS_SQL = 
			"select distinct object_path from public.\"r_coll_hierarchy_meta_main\" where object_id in ";
	
	private static final String GET_DATA_OBJECT_PATHS_SQL = 
			"select distinct object_path from public.\"r_data_hierarchy_meta_main\" where object_id in ";
	
	private static final String GET_COLLECTION_METADATA_SQL = 
			"select meta_attr_name,  meta_attr_value, level " + 
	        "from public.\"r_coll_hierarchy_meta_main\" where object_path = ? ";
	
	private static final String GET_DATA_OBJECT_METADATA_SQL = 
			"select meta_attr_name, meta_attr_value, level " + 
	        "from public.\"r_data_hierarchy_meta_main\" where object_path = ? ";
	
	private static final String REFRESH_VIEW_SQL = "refresh materialized view concurrently";
	
	private static final String GET_METADATA_ATTRIBUTES_SQL = 
			"select distinct meta_attr_name from public.\"r_data_hierarchy_meta_main\" union " +
			"select distinct meta_attr_name from public.\"r_coll_hierarchy_meta_main\"";
			
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mappers.
	private SingleColumnRowMapper<String> objectPathRowMapper = new SingleColumnRowMapper<>();
	private SingleColumnRowMapper<String> metadataAttributeRowMapper = new SingleColumnRowMapper<>();
	HpcHierarchicalMetadataEntryRowMapper hierarchicalMetadataEntryRowMapper = 
			                              new HpcHierarchicalMetadataEntryRowMapper();
	
	// Maps between metadata query operator to its SQL query.
	private Map<HpcMetadataQueryOperator, String> dataObjectSQLQueries = new HashMap<>();
	private Map<HpcMetadataQueryOperator, String> collectionSQLQueries = new HashMap<>();
	
	// Maps between metadata query operator to a level SQL filter ('where' condition)
	private Map<HpcMetadataQueryOperator, String> dataObjectLevelFilters = new HashMap<>();
	private Map<HpcMetadataQueryOperator, String> collectionLevelFilters = new HashMap<>();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcMetadataDAOImpl()
    {
    	dataObjectSQLQueries.put(HpcMetadataQueryOperator.EQUAL, 
    			                 GET_DATA_OBJECT_IDS_EQUAL_SQL);
    	dataObjectSQLQueries.put(HpcMetadataQueryOperator.NOT_EQUAL, 
    			                 GET_DATA_OBJECT_IDS_NOT_EQUAL_SQL);
    	dataObjectSQLQueries.put(HpcMetadataQueryOperator.LIKE, 
    			                 GET_DATA_OBJECT_IDS_LIKE_SQL);
    	dataObjectSQLQueries.put(HpcMetadataQueryOperator.NUM_LESS_THAN, 
    			                 GET_DATA_OBJECT_IDS_NUM_LESS_THAN_SQL);
    	dataObjectSQLQueries.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL, 
    			                 GET_DATA_OBJECT_IDS_NUM_LESS_OR_EQUAL_SQL);
    	dataObjectSQLQueries.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL, 
    			                 GET_DATA_OBJECT_IDS_NUM_GREATER_OR_EQUAL_SQL);
    	
    	collectionSQLQueries.put(HpcMetadataQueryOperator.EQUAL, 
    			                 GET_COLLECTION_IDS_EQUAL_SQL);
    	collectionSQLQueries.put(HpcMetadataQueryOperator.NOT_EQUAL, 
    			                 GET_COLLECTION_IDS_NOT_EQUAL_SQL);
    	collectionSQLQueries.put(HpcMetadataQueryOperator.LIKE, 
    			                 GET_COLLECTION_IDS_LIKE_SQL);
    	collectionSQLQueries.put(HpcMetadataQueryOperator.NUM_LESS_THAN, 
    			                 GET_COLLECTION_IDS_NUM_LESS_THAN_SQL);
    	collectionSQLQueries.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL, 
    			                 GET_COLLECTION_IDS_NUM_LESS_OR_EQUAL_SQL);
    	collectionSQLQueries.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL, 
    			                 GET_COLLECTION_IDS_NUM_GREATER_OR_EQUAL_SQL);
    	
    	dataObjectLevelFilters.put(HpcMetadataQueryOperator.EQUAL, 
    			                   DATA_OBJECT_LEVEL_EQUAL_FILTER);
    	dataObjectLevelFilters.put(HpcMetadataQueryOperator.NOT_EQUAL, 
                                   DATA_OBJECT_LEVEL_NOT_EQUAL_FILTER);
    	dataObjectLevelFilters.put(HpcMetadataQueryOperator.NUM_LESS_THAN, 
                                   DATA_OBJECT_LEVEL_NUM_LESS_THAN_FILTER);
    	dataObjectLevelFilters.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL, 
                                   DATA_OBJECT_LEVEL_NUM_LESS_OR_EQUAL_FILTER);
    	dataObjectLevelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL, 
                                   DATA_OBJECT_LEVEL_NUM_GREATER_OR_EQUAL_FILTER);
    	
    	collectionLevelFilters.put(HpcMetadataQueryOperator.EQUAL, 
    			                   COLLECTION_LEVEL_EQUAL_FILTER);
    	collectionLevelFilters.put(HpcMetadataQueryOperator.NOT_EQUAL, 
                                   COLLECTION_LEVEL_NOT_EQUAL_FILTER);
    	collectionLevelFilters.put(HpcMetadataQueryOperator.NUM_LESS_THAN, 
                                   COLLECTION_LEVEL_NUM_LESS_THAN_FILTER);
    	collectionLevelFilters.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL, 
                                   COLLECTION_LEVEL_NUM_LESS_OR_EQUAL_FILTER);
    	collectionLevelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL, 
                                   COLLECTION_LEVEL_NUM_GREATER_OR_EQUAL_FILTER);
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcMetadataDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
    public List<String> getCollectionPaths(List<HpcMetadataQuery> metadataQueries,
    		                               String dataManagementUsername,
    		                               int offset, int limit) 
                                          throws HpcException
    {
		return getPaths(prepareQuery(GET_COLLECTION_PATHS_SQL, 
                                     toQuery(collectionSQLQueries, metadataQueries, 
                                    		 HpcCompoundMetadataQueryOperator.ALL,
                                    		 collectionLevelFilters),
                                     COLLECTION_USER_ACCESS_SQL, 
                                     dataManagementUsername, offset, limit));
    }
	
	@Override
    public List<String> getCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               String dataManagementUsername,
    		                               int offset, int limit) 
                                          throws HpcException
    {
		return getPaths(prepareQuery(GET_COLLECTION_PATHS_SQL, 
                                     toQuery(collectionSQLQueries, compoundMetadataQuery, 
                                    		 collectionLevelFilters),
                                     COLLECTION_USER_ACCESS_SQL, 
                                     dataManagementUsername, offset, limit));
    }

	@Override 
	public List<String> getDataObjectPaths(List<HpcMetadataQuery> metadataQueries,
			                               String dataManagementUsername,
			                               int offset, int limit) 
                                          throws HpcException
    {
		return getPaths(prepareQuery(GET_DATA_OBJECT_PATHS_SQL, 
                                     toQuery(dataObjectSQLQueries, metadataQueries,
                                    		 HpcCompoundMetadataQueryOperator.ALL,
                                    		 dataObjectLevelFilters),
                                     DATA_OBJECT_USER_ACCESS_SQL, 
                                     dataManagementUsername, offset, limit));
    }
	
	@Override 
	public List<String> getDataObjectPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
			                               String dataManagementUsername,
			                               int offset, int limit) 
                                          throws HpcException
    {
		return getPaths(prepareQuery(GET_DATA_OBJECT_PATHS_SQL, 
                                     toQuery(dataObjectSQLQueries, compoundMetadataQuery,
                                    		 dataObjectLevelFilters),
                                     DATA_OBJECT_USER_ACCESS_SQL, 
                                     dataManagementUsername, offset, limit));
    }
	
    @Override
    public List<HpcHierarchicalMetadataEntry> getCollectionMetadata(String path) throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_COLLECTION_METADATA_SQL, hierarchicalMetadataEntryRowMapper, path);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get collection hierarchical metadata: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}	
    }
    
    @Override
    public List<HpcHierarchicalMetadataEntry> getDataObjectMetadata(String path) throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_DATA_OBJECT_METADATA_SQL, hierarchicalMetadataEntryRowMapper, path);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get data object hierarchical metadata: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}	
    }
    
    @Override
    public List<String> getMetadataAttributes() throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_METADATA_ATTRIBUTES_SQL, metadataAttributeRowMapper);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get metadata attributes: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}	
    }
    
	@Override
	public void refreshViews() throws HpcException
    {
		try {
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_coll_hierarchy_metamap");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_coll_hierarchy_meta_main");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_data_hierarchy_metamap");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_data_hierarchy_meta_main");
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to refresh materialized views: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
	// Row Mapper
	private class HpcHierarchicalMetadataEntryRowMapper implements RowMapper<HpcHierarchicalMetadataEntry>
	{
		@Override
		public HpcHierarchicalMetadataEntry mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcHierarchicalMetadataEntry hierarchicalMetadataEntry = new HpcHierarchicalMetadataEntry();
			Long level = rs.getLong("LEVEL");
			hierarchicalMetadataEntry.setLevel(level != null ? level.intValue() : null);
			HpcMetadataEntry metadataEntry = new HpcMetadataEntry();
			metadataEntry.setAttribute(rs.getString("META_ATTR_NAME"));
			metadataEntry.setValue(rs.getString("META_ATTR_VALUE"));
			hierarchicalMetadataEntry.setMetadataEntry(metadataEntry);
			
			return hierarchicalMetadataEntry;
		}
	}
	
	// Prepared query.
	private class HpcPreparedQuery
	{
		public String sql = null;
		public Object[] args = null;
	}
	
    /**
     * Prepare a SQL query. Map operators to SQL and concatenate them with 'intersect'.
     *
     * @param getObjectPathsQuery The query to get object paths based on object IDs.
     * @param userQuery The calculated SQL query based on user input (represented by query domain objects).
     * @param userAccessQuery The user access query to append.
     * @param dataManagementUsername The data management user name.
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * 
     * @throws HpcException
     */
    private HpcPreparedQuery prepareQuery(String getObjectPathsQuery,
    		                              HpcPreparedQuery userQuery,
    		                              String userAccessQuery,
    		                              String dataManagementUsername,
    		                              int offset, int limit) 
    		                             throws HpcException
    {
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();
    	
    	// Combine the metadata queries into a single SQL statement.
    	sqlQueryBuilder.append(getObjectPathsQuery + "(");
    	sqlQueryBuilder.append(userQuery.sql);
    	args.addAll(Arrays.asList(userQuery.args));
    	
    	// Add a query to only include entities the user can access.
    	sqlQueryBuilder.append(" intersect ");
    	sqlQueryBuilder.append(userAccessQuery + ")");
    	args.add(dataManagementUsername);
    	
    	sqlQueryBuilder.append(LIMIT_OFFSET_SQL);
    	args.add(limit);
    	args.add(offset);
    	
    	HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
    	preparedQuery.sql = sqlQueryBuilder.toString();
    	preparedQuery.args = args.toArray();
    	return preparedQuery;
    }
    
    /**
     * Create a SQL statement from List<HpcMetadataQuery>
     *
     * @param sqlQueries The map from metadata query operator to SQL queries.
     * @param metadataQueries The metadata queries.
     * @param operator The compound metadata query operator to use.
     * @param levelFilters The map from query operator to level filter ('where' condition).
     * 
     * @throws HpcException
     */
    private HpcPreparedQuery toQuery(Map<HpcMetadataQueryOperator, String> sqlQueries, 
    		                         List<HpcMetadataQuery> metadataQueries,
    		                         HpcCompoundMetadataQueryOperator operator,
    		                         Map<HpcMetadataQueryOperator, String> levelFilters) 
    		                        throws HpcException
    {
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();
    
		sqlQueryBuilder.append("(");
		for(HpcMetadataQuery metadataQuery : metadataQueries) {
			String sqlQuery = sqlQueries.get(metadataQuery.getOperator());
			if(sqlQuery == null) {
			   throw new HpcException("Invalid metadata query operator: " + metadataQuery.getOperator(),
					                  HpcErrorType.INVALID_REQUEST_INPUT);
			}
			
			// Append the compound metadata query operator if not the first query in the list.
			if(!args.isEmpty()) {
			   sqlQueryBuilder.append(" " + toSQLOperator(operator) + " ");
			}
			
			// Append the SQL query representing the requested metadata query operator and its arguments.
			sqlQueryBuilder.append(sqlQuery);
			args.add(metadataQuery.getAttribute());
			args.add(metadataQuery.getValue());
			
			// Add a filter for level. 
			Integer level = metadataQuery.getLevel();
			HpcMetadataQueryOperator levelOperator = metadataQuery.getLevelOperator();
			if(level == null || levelOperator == null) {
			   // Default filter is 'EQUAL at level 1'.
			   level = 1;
			   levelOperator = HpcMetadataQueryOperator.EQUAL;
			}
			
			String levelFilter = levelFilters.get(levelOperator);
			if(levelFilter == null) {
			   throw new HpcException("Invalid level operator: " + metadataQuery.getLevelOperator(),
			                          HpcErrorType.INVALID_REQUEST_INPUT); 
			}
			sqlQueryBuilder.append(levelFilter);
			args.add(level);
		}
		
		sqlQueryBuilder.append(")");
		
    	HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
    	preparedQuery.sql = sqlQueryBuilder.toString();
    	preparedQuery.args = args.toArray();
    	return preparedQuery;
    }
    
    /**
     * Create a SQL statement from HpcCompoundMetadataQuery
     *
     * @param sqlQueries The map from metadata query operator to SQL queries.
     * @param metadataQueries The metadata queries.
     * @param levelFilters The map from query operator to level filter ('where' condition).
     * 
     * @throws HpcException
     */
    private HpcPreparedQuery toQuery(Map<HpcMetadataQueryOperator, String> sqlQueries, 
    		                         HpcCompoundMetadataQuery compoundMetadataQuery,
    		                         Map<HpcMetadataQueryOperator, String> levelFilters) 
    		                        throws HpcException
    {
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();
    
		sqlQueryBuilder.append("(");
		// Append the simple queries.
		if(compoundMetadataQuery.getQueries() != null && !compoundMetadataQuery.getQueries().isEmpty()) {
			HpcPreparedQuery query = toQuery(sqlQueries, compoundMetadataQuery.getQueries(), 
					                         compoundMetadataQuery.getOperator(), levelFilters);	
			sqlQueryBuilder.append(query.sql);
			args.addAll(Arrays.asList(query.args));
		}
		
		// Append the nested compound queries.
		if(compoundMetadataQuery.getCompoundQueries() != null && 
		   !compoundMetadataQuery.getCompoundQueries().isEmpty()){
		   if(!args.isEmpty()) {
			  sqlQueryBuilder.append(" " + toSQLOperator(compoundMetadataQuery.getOperator()) + " ");
		   }
		   boolean firstNestedQuery = true;
		   for(HpcCompoundMetadataQuery nestedCompoundQuery : compoundMetadataQuery.getCompoundQueries()) {
			   if(!firstNestedQuery) {
				  sqlQueryBuilder.append(" " + toSQLOperator(compoundMetadataQuery.getOperator()) + " ");
			   } else {
				       firstNestedQuery = false;
			   }
			   HpcPreparedQuery query = toQuery(sqlQueries, nestedCompoundQuery, levelFilters);	
               sqlQueryBuilder.append(query.sql);
               args.addAll(Arrays.asList(query.args));			   
		   }
		}
		sqlQueryBuilder.append(")");
		
    	HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
    	preparedQuery.sql = sqlQueryBuilder.toString();
    	preparedQuery.args = args.toArray();
    	return preparedQuery;
    }
    
    /**
     * Execute a SQL query to get collection or data object paths
     *
     * @param prepareQuery The prepared query to execute.
     * 
     * @throws HpcException
     */
    private List<String> getPaths(HpcPreparedQuery prepareQuery) throws HpcException
    {
		try {
		     return jdbcTemplate.query(prepareQuery.sql, objectPathRowMapper, prepareQuery.args);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get collection/data-object Paths: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}		
    }
    
    /**
     * Coverts a query operator enum to SQL
     *
     * @param operator The operator to convert.
     * 
     * @throws HpcException
     */
    private String toSQLOperator(HpcCompoundMetadataQueryOperator operator)
    {
    	return operator.equals(HpcCompoundMetadataQueryOperator.ALL) ? "intersect" : "union";  
    }
}

 