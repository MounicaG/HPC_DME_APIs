/**
 * HpcDataSearchRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import java.util.ArrayList;
import java.util.List;

import gov.nih.nci.hpc.bus.HpcDataSearchBusService;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcNamedCompoundMetadataQueryListDTO;
import gov.nih.nci.hpc.dto.metadata.HpcMetadataQueryParam;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataSearchRestService;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Search REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataSearchRestServiceImpl extends HpcRestServiceImpl
             implements HpcDataSearchRestService
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//   
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Business Service instance.
	@Autowired
    private HpcDataSearchBusService dataSearchBusService = null;
	
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcDataSearchRestServiceImpl() 
    {
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataSearchRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response getCollections(List<HpcMetadataQueryParam> metadataQueries, 
    		                       Boolean detailedResponse, Integer page)
    {
    	long start = System.currentTimeMillis();
    	logger.info("Invoking RS: GET /collection/" + metadataQueries);
    	
    	HpcCollectionListDTO collections = null;
		try {
			 collections = dataSearchBusService.getCollections(
					                    unmarshallQueryParams(metadataQueries), 
					                    detailedResponse != null ? detailedResponse : false,
					                    page != null ? page : 1);
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /collection/" + metadataQueries + 
			    		     " failed:", e);
			    return errorResponse(e);
		}
		long stop = System.currentTimeMillis();
		logger.info((stop-start) + " getCollections: Total time - " + metadataQueries);
		
		return okResponse(!collections.getCollections().isEmpty() ||
				          !collections.getCollectionPaths().isEmpty() ? collections : null , true);
    }
    
    @Override
    public Response queryCollections(List<HpcMetadataQuery> metadataQueries,
    		                         Boolean detailedResponse, Integer page)
    {
    	long start = System.currentTimeMillis();
    	logger.info("Invoking RS: POST /collection/query" + metadataQueries);
    	
    	HpcCollectionListDTO collections = null;
		try {
			 collections = dataSearchBusService.getCollections(
					           metadataQueries,
					           detailedResponse != null ? detailedResponse : false,
					           page != null ? page : 1);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /collection/query" + metadataQueries + 
			    		     " failed:", e);
			    return errorResponse(e);
		}
		long stop = System.currentTimeMillis();
		logger.info((stop-start) + " getCollections: Total time - " + metadataQueries);
		
		return okResponse(!collections.getCollections().isEmpty() ||
				          !collections.getCollectionPaths().isEmpty() ? collections : null , true);
    }
    
    @Override
    public Response queryCollections(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO)
    {
    	long start = System.currentTimeMillis();
    	logger.info("Invoking RS: POST /collection/query/compound" + compoundMetadataQueryDTO);
    	
    	HpcCollectionListDTO collections = null;
		try {
			 collections = dataSearchBusService.getCollections(compoundMetadataQueryDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /collection/query/compound" + compoundMetadataQueryDTO + 
			    		     " failed:", e);
			    return errorResponse(e);
		}
		long stop = System.currentTimeMillis();
		logger.info((stop-start) + " getCollections: Total time - " + compoundMetadataQueryDTO);
		
		return okResponse(!collections.getCollections().isEmpty() ||
				          !collections.getCollectionPaths().isEmpty() ? collections : null , true);
    }
    
    @Override
    public Response queryCollections(String queryName, Boolean detailedResponse, Integer page)
    {
    	long start = System.currentTimeMillis();
    	logger.info("Invoking RS: GET /collection/query/compound/{queryName}" + queryName);
    	
    	HpcCollectionListDTO collections = null;
		try {
			 collections = dataSearchBusService.getCollections(
					           queryName,
					           detailedResponse != null ? detailedResponse : false,
					           page != null ? page : 1);
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /collection/query/compound/{queryName}" + queryName + 
			    		     " failed:", e);
			    return errorResponse(e);
		}
		long stop = System.currentTimeMillis();
		logger.info((stop-start) + " getCollections: Total time - " + queryName);
		
		return okResponse(!collections.getCollections().isEmpty() ||
				          !collections.getCollectionPaths().isEmpty() ? collections : null , true);
    }
    
    @Override
    public Response saveQuery(String queryName,
    		                  HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO)
    {
    	logger.info("Invoking RS: POST /query/{queryName}: " + queryName);
    	long start = System.currentTimeMillis();
		try {
			 dataSearchBusService.saveQuery(queryName, compoundMetadataQueryDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /query/{nciUserId}/{queryName}: " + "," + queryName +
			    		     " failed:", e);
			    return errorResponse(e);
		}
		long stop = System.currentTimeMillis();
		logger.info((stop-start) + " saveQuery: Total time");
		
    	return okResponse(null, false);
    }
    
    @Override
    public Response deleteQuery(String queryName)
    {
    	logger.info("Invoking RS: DELETE /query/{queryName}: " +  queryName);
    	long start = System.currentTimeMillis();
		try {
			 dataSearchBusService.deleteQuery(queryName);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /query/{nciUserId}/{queryName}: " + queryName +
			    		     " failed:", e);
			    return errorResponse(e);
		}
		long stop = System.currentTimeMillis();
		logger.info((stop-start) + " deleteQuery: Total time");
		
    	return okResponse(null, false);
    }

    @Override
    public Response getQueries()
    {
    	logger.info("Invoking RS: GET /query");
    	long start = System.currentTimeMillis();
    	HpcNamedCompoundMetadataQueryListDTO queries = null;
		try {
			 queries = dataSearchBusService.getQueries();
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /query/{nciUserId}: failed:", e);
			    return errorResponse(e);
		}
		long stop = System.currentTimeMillis();
		logger.info((stop-start) + " getQueries: Total time");
		
    	return okResponse(!queries.getQueries().isEmpty() ? queries : null, true);
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Unmarshall metadata query passed as JSON in a URL parameter.
     * 
     * @param metadataQueries The query params to unmarshall.
     * @return List of HpcMetadataQuery.
     * 
     * @throws HpcException if the params unmarshalling failed.
     */
    private List<HpcMetadataQuery> unmarshallQueryParams(
    		                                 List<HpcMetadataQueryParam> metadataQueries)
    		                                 throws HpcException
    {
		 // Validate the metadata entries input (JSON) was parsed successfully.
		 List<HpcMetadataQuery> queries = new ArrayList<HpcMetadataQuery>();
		 for(HpcMetadataQueryParam queryParam : metadataQueries) {
		     if(queryParam.getJSONParsingException() != null) {
			    throw queryParam.getJSONParsingException();
		     }
		     queries.add(queryParam);
		 }
		 
		 return queries;
    }
}

 
