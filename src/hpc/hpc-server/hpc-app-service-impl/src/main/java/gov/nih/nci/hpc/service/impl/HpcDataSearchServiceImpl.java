/**
 * HpcDataSearchServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidCompoundMetadataQuery;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataQueryLevelFilter;
import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcDataSearchService;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Search Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataSearchServiceImpl implements HpcDataSearchService
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// Metadata DAO.
	@Autowired
	private HpcMetadataDAO metadataDAO = null;
	
    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
	// The max page size of search results.
	private int searchResultsPageSize = 0;
	
	// Default level filters for collection and data object search.
	HpcMetadataQueryLevelFilter defaultCollectionLevelFilter = null;
    HpcMetadataQueryLevelFilter defaultDataObjectLevelFilter = null;

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param searchResultsPageSize The max page size of search results.
     * @param defaultCollectionLevelFilter The default collection search level filter.
     * @param defaultDataObjectLevelFilter The default data-object search level filter.
     * @throws HpcException
     */
    private HpcDataSearchServiceImpl(int searchResultsPageSize,
    		                         HpcMetadataQueryLevelFilter defaultCollectionLevelFilter,
    		                         HpcMetadataQueryLevelFilter defaultDataObjectLevelFilter) 
    		                         throws HpcException
    {
    	// Input Validation.
    	if(!isValidMetadataQueryLevelFilter(defaultCollectionLevelFilter) ||
    	   !isValidMetadataQueryLevelFilter(defaultDataObjectLevelFilter)) {
    	   throw new HpcException("Invalid default collection/data object level filter",
	                              HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	this.defaultCollectionLevelFilter = defaultCollectionLevelFilter;
    	this.defaultDataObjectLevelFilter = defaultDataObjectLevelFilter;
    	this.searchResultsPageSize = searchResultsPageSize;
    }   
    
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
	private HpcDataSearchServiceImpl() throws HpcException
    {
    	throw new HpcException("Default Constructor disabled",
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataSearchService Interface Implementation
    //---------------------------------------------------------------------//  

    @Override
    public List<String> getCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery, int page) 
    		                              throws HpcException
    {
       	if(!isValidCompoundMetadataQuery(compoundMetadataQuery)) {
           throw new HpcException("Invalid or null metadata query", 
        			              HpcErrorType.INVALID_REQUEST_INPUT);
        }
       	
    	// Use the hierarchical metadata views to perform the search.
       	String dataManagementUsername = 
       			   HpcRequestContext.getRequestInvoker().getDataManagementAccount().getUsername();
       	return toRelativePaths(metadataDAO.getCollectionPaths(
       			                              compoundMetadataQuery, dataManagementUsername,
       	 	                                  getOffset(page), searchResultsPageSize,
       			                              defaultCollectionLevelFilter));
    }
    
    @Override
    public int getSearchResultsPageSize()
    {
    	return searchResultsPageSize;
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Calculate search offset by requested page.
     *
     * @param page The requested page.
     * @return The calculated offset
     * @throws HpcException if the page is invalid.
     * 
     */
    private int getOffset(int page) throws HpcException
    {
    	if(page < 1) {
    	   throw new HpcException("Invalid search results page: " + page,
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return (page - 1) * searchResultsPageSize;
    }
    
    /**
     * Convert a list of absolute paths, to relative paths.
     *
     * @param paths The list of absolute paths.
     * @return List of relative paths.
     * 
     */
    private List<String> toRelativePaths(List<String> paths) 
    {
    	List<String> relativePaths = new ArrayList<>();
    	for(String path : paths) {
    		relativePaths.add(dataManagementProxy.getRelativePath(path));
    	}
    	
    	return relativePaths;
    }
    
}