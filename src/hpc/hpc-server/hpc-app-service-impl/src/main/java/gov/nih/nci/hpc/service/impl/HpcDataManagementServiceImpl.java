/**
 * HpcDataManagementServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_STATUS_ATTRIBUTE;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcEntityPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcDataManagementService;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Management Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataManagementServiceImpl implements HpcDataManagementService
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//
	
	// Data Management permissions.
	private static final String OWN_PERMISSION = "OWN"; 
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
    // The Data Management Authenticator.
	@Autowired
    private HpcDataManagementAuthenticator dataManagementAuthenticator = null;
	
	// System Accounts locator.
	@Autowired
	private HpcSystemAccountLocator systemAccountLocator = null;
	
	// Data Hierarchy Validator.
	@Autowired
	private HpcDataHierarchyValidator dataHierarchyValidator = null;
	
	// DOC base paths.
	@Autowired
	private HpcDocBasePath docBasePath = null;
	
	// Prepared query to get data objects that have their data transfer in-progress to archive.
	private List<HpcMetadataQuery> dataTransferInProgressToArchiveQuery = new ArrayList<>();
	
	// Prepared query to get data objects that have their data transfer in-progress to temporary archive.
	private List<HpcMetadataQuery> dataTransferInProgressToTemporaryArchiveQuery = new ArrayList<>();
	
	// Prepared query to get data objects that have their data in temporary archive.
	private List<HpcMetadataQuery> dataTransferInTemporaryArchiveQuery = new ArrayList<>();
	
    // The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     */
    private HpcDataManagementServiceImpl()
    {
    	// Prepare the query to get data objects in data transfer in-progress to archive.
        dataTransferInProgressToArchiveQuery.add(
            toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE, 
            		        HpcMetadataQueryOperator.EQUAL, 
        	                HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE.value()));
        
        // Prepare the query to get data objects in data transfer in-progress to temporary archive.
        dataTransferInProgressToTemporaryArchiveQuery.add(
        	toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE, 
        			        HpcMetadataQueryOperator.EQUAL, 
        			        HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE.value()));
        
        // Prepare the query to get data objects in temporary archive.
        dataTransferInTemporaryArchiveQuery.add(
        	toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE, 
        			        HpcMetadataQueryOperator.EQUAL, 
        			        HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE.value()));
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataManagementService Interface Implementation
    //---------------------------------------------------------------------//  

    @Override
    public boolean createDirectory(String path) throws HpcException
    {
    	Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
    	String relativePath = dataManagementProxy.getRelativePath(path);
    	// Validate the path is not a DOC base path.
    	if(docBasePath.containsValue(relativePath)) {
    	   throw new HpcException("Invalid collection path: " + path, 
	                              HpcErrorType.INVALID_REQUEST_INPUT); 
    	}
    	
    	// Validate the path is not root
    	if(relativePath.equals("/")) {
    	   throw new HpcException("Invalid path: " + path, 
	                              HpcErrorType.INVALID_REQUEST_INPUT); 
    	}
    	
    	// Validate the directory path doesn't exist.
    	HpcPathAttributes pathAttributes = 
    	   dataManagementProxy.getPathAttributes(authenticatedToken, path);
    	if(pathAttributes.getExists()) {
    	   if(pathAttributes.getIsDirectory()) {
    		  // Directory already exists.
    		  return false;
    	   }
    	   if(pathAttributes.getIsFile()) {
    		  throw new HpcException("Path already exists as a file: " + path, 
    				                 HpcErrorType.INVALID_REQUEST_INPUT); 
    	   }
    	}
    	
    	//  Validate the parent directory exists.
    	if(!dataManagementProxy.isPathParentDirectory(authenticatedToken, path)) {
    		throw new HpcException("Invalid collection path. Parent directory doesn't exist: " + path, 
                                   HpcRequestRejectReason.INVALID_DATA_OBJECT_PATH);
    	}
    	
    	// Create the directory.
    	dataManagementProxy.createCollectionDirectory(authenticatedToken, path);
    	return true;
    }
    
    @Override
    public boolean isPathParentDirectory(String path) throws HpcException
    {
    	return dataManagementProxy.isPathParentDirectory(dataManagementAuthenticator.getAuthenticatedToken(), path);
    }
    
    @Override
    public boolean createFile(String path) throws HpcException
    {
    	Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
    	
    	// Validate the file path.
    	HpcPathAttributes pathAttributes = 
    	   dataManagementProxy.getPathAttributes(authenticatedToken, path);
    	if(pathAttributes.getExists()) {
    	   if(pathAttributes.getIsFile()) {
    		  // File already exists.
    		  return false;
    	   }
    	   if(pathAttributes.getIsDirectory()) {
    		  throw new HpcException("Path already exists as a directory: " + path, 
    				                 HpcErrorType.INVALID_REQUEST_INPUT); 
    	   }
    	}
    	
    	//  Validate the parent directory exists.
    	if(!dataManagementProxy.isPathParentDirectory(authenticatedToken, path)) {
    		throw new HpcException("Invalid data object path. Parent directory doesn't exist: " + path, 
                                   HpcRequestRejectReason.INVALID_DATA_OBJECT_PATH);
    	}
    	
    	// Create the data object file.
    	dataManagementProxy.createDataObjectFile(authenticatedToken, path);
    	return true;
    }
    
    @Override
    public void delete(String path) throws HpcException
    {
    	// Delete the data object file.
    	dataManagementProxy.delete(dataManagementAuthenticator.getAuthenticatedToken(), path);
    }
    
    @Override
    public void setPermission(String path, HpcEntityPermission permissionRequest) 
                             throws HpcException
    {
    	// Input validation.
    	if(path == null || permissionRequest == null) {
           throw new HpcException("Null path or permission request", 
		                          HpcErrorType.INVALID_REQUEST_INPUT);    	   	
    	}
    	
    	// Get the path attributes.
    	Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
    	HpcPathAttributes pathAttributes = 
    		              dataManagementProxy.getPathAttributes(authenticatedToken, 
    				                                            path);
    	
    	// Set collection or data-object permission.
    	if(pathAttributes.getIsDirectory()) {
    	   dataManagementProxy.setCollectionPermission(authenticatedToken, 
    			                                       path, 
    			                                       permissionRequest);
    	} else if(pathAttributes.getIsFile()) {
    		      dataManagementProxy.setDataObjectPermission(authenticatedToken, 
    		    		                                      path, 
                                                              permissionRequest);
    	} else {
    		    throw new HpcException("Entity path doesn't exist", 
                                       HpcErrorType.INVALID_REQUEST_INPUT);   
    	}
    }    
    
    @Override
    public void assignSystemAccountPermission(String path) throws HpcException
    {
    	HpcIntegratedSystemAccount dataManagementAccount = 
    	    	     systemAccountLocator.getSystemAccount(HpcIntegratedSystem.IRODS);
    	if(dataManagementAccount == null) {
    	   throw new HpcException("System Data Management Account not configured",
    	      	                  HpcErrorType.UNEXPECTED_ERROR);
    	}
    	
    	setPermission(path, dataManagementAccount.getUsername(), OWN_PERMISSION);
    }
    
    @Override
    public void validateHierarchy(String path, String doc,
    		                      boolean dataObjectRegistration) 
    		                     throws HpcException
    {
    	// Calculate the collection path to validate.
    	String validationCollectionPath = dataManagementProxy.getRelativePath(path);
    	validationCollectionPath = validationCollectionPath.substring(1, validationCollectionPath.length());
    	
    	// Build the collection path types list.
    	List<String> collectionPathTypes = new ArrayList<>();
    	StringBuilder subCollectionPath = new StringBuilder();
		for(String s : validationCollectionPath.split("/")) {
			subCollectionPath.append("/" + s);
			String collectionType = getCollectionType(subCollectionPath.toString());
			if(collectionType == null) {
			   if(!collectionPathTypes.isEmpty()) {
				  throw new HpcException("Invalid collection path hierarchy: " + path,
						                 HpcErrorType.INVALID_REQUEST_INPUT);
			   }
			} else {
			        collectionPathTypes.add(collectionType);
			}
		}

		// Perform the hierarchy validation.
		dataHierarchyValidator.validateHierarchy(doc, collectionPathTypes, dataObjectRegistration);
    }
    
    @Override
    public HpcCollection getCollection(String path, boolean list) throws HpcException
    {
    	Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
    	if(dataManagementProxy.getPathAttributes(authenticatedToken, path).getIsDirectory()) {
    	   return dataManagementProxy.getCollection(authenticatedToken, path, list);
    	}
    	
    	return null;
    }
    
    @Override
    public HpcDataObject getDataObject(String path) throws HpcException
    {
    	Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
    	if(dataManagementProxy.getPathAttributes(authenticatedToken, path).getIsFile()) {
    	   return dataManagementProxy.getDataObject(authenticatedToken, path);
    	}
    	
    	return null;
    }
    
    @Override
    public HpcDataHierarchy getDataHierarchy(String doc) throws HpcException
    {
    	return dataHierarchyValidator.getDataHierarchy(doc);
    }
    
    @Override
    public List<HpcDataObject> getDataObjectsInProgress() throws HpcException
    {
    	Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
    	List<HpcDataObject> objectsInProgress = new ArrayList<>();
    	objectsInProgress.addAll(
    		   dataManagementProxy.getDataObjects(authenticatedToken, 
    			                                  dataTransferInProgressToArchiveQuery));
    	objectsInProgress.addAll(
    		   dataManagementProxy.getDataObjects(authenticatedToken, 
    			                                  dataTransferInProgressToTemporaryArchiveQuery));
    	
    	return objectsInProgress;
    }
    
    @Override
    public List<HpcDataObject> getDataObjectsInTemporaryArchive() throws HpcException
    {
    	return dataManagementProxy.getDataObjects(
    			             dataManagementAuthenticator.getAuthenticatedToken(),
    			             dataTransferInTemporaryArchiveQuery);
    }
    
    @Override
    public void closeConnection()
    {
    	try {
    	     dataManagementProxy.disconnect(dataManagementAuthenticator.getAuthenticatedToken());
    	     
    	} catch(HpcException e) {
    		    // Ignore.
    		    logger.error("Failed to close data management connection", e);
    	}
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Generate a metadata query.
     * 
     * @param attribute The metadata entry attribute.
     * @param operator The query operator.
     * @param value The metadata entry value.
     * @return HpcMetadataEntry instance
     */
    private HpcMetadataQuery toMetadataQuery(String attribute, HpcMetadataQueryOperator operator, 
    		                                 String value)
    {
		HpcMetadataQuery query = new HpcMetadataQuery();
		query.setAttribute(attribute);
	    query.setOperator(operator);
	    query.setValue(value);
	    
	    return query;
    }
    
    /**
     * Get a collection type of a path.
     *
     * @param path The collection path.
     * @return The collection type.
     * @throws HpcException on service failure.
     */
    private String getCollectionType(String path) throws HpcException
    {
    	for(HpcMetadataEntry metadataEntry : 
    		dataManagementProxy.getCollectionMetadata(
    				               dataManagementAuthenticator.getAuthenticatedToken(), path)) {
    		if(metadataEntry.getAttribute().equals(HpcMetadataValidator.COLLECTION_TYPE_ATTRIBUTE)) {
    		   return metadataEntry.getValue();
    		}
    	}
    	
    	return null;
    }
    
    /**
     * Set permission.
     *
     * @param path The entity path.
     * @param userId The user ID.
     * @param permission The permission.
     * @throws HpcException on service failure.
     */
    private void setPermission(String path, String userId, String permission) throws HpcException
    {
    	HpcUserPermission permissionRequest = new HpcUserPermission();
        permissionRequest.setPermission(permission);
        permissionRequest.setUserId(userId);
        setPermission(path, permissionRequest);
    }
}
