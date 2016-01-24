/**
 * HpcDataManagementService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Data Management Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDataManagementService 
{         
    /**
     * Create a collection's directory.
     *
     * @param path The collection path.
     * @return true if the directory was created, or false if it already exists.
     * 
     * @throws HpcException
     */
    public boolean createDirectory(String path) throws HpcException;
    
    /**
     * Create a data object's file.
     *
     * @param path The data object path.
     * @param createParentPathDirectory If set to true, create the directory for the file.
     * 
     * @throws HpcException
     */
    public void createFile(String path, boolean createParentPathDirectory) 
    		              throws HpcException;

    /**
     * Add metadata to a collection.
     *
     * @param path The collection path.
     * @param metadataEntries The metadata entries to add.
     * 
     * @throws HpcException
     */
    public void addMetadataToCollection(String path, 
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException; 
    
    /**
     * Generate system metadata and attach to a collection.
     * System generated metadata is:
     * 		1. Registrar user ID.
     * 		2. Registrar name.
     *
     * @param path The collection path.
     * 
     * @throws HpcException
     */
    public void addSystemGeneratedMetadataToCollection(String path) throws HpcException; 
    
    /**
     * Update a collection's metadata.
     *
     * @param path The collection path.
     * @param metadataEntries The metadata entries to update.
     * 
     * @throws HpcException
     */
    public void updateCollectionMetadata(String path, 
    		                             List<HpcMetadataEntry> metadataEntries) 
    		                            throws HpcException; 
    
    /**
     * Add metadata to a data object.
     *
     * @param path The data object path.
     * @param metadataEntries The metadata entries to add.
     * 
     * @throws HpcException
     */
    public void addMetadataToDataObject(String path, 
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException; 
    
    /**
     * Generate system metadata and attach to the data object.
     * System generated metadata is:
     * 		1. Physical file source.
     * 		2. Physical file location.
     * 		3. Registrar user ID.
     * 		4. Registrar name.
     *
     * @param path The data object path.
     * @param fileLocation The physical file location.
     * @param fileSource The source location of the file.
     * 
     * @throws HpcException
     */
    public void addSystemGeneratedMetadataToDataObject(String path, 
    		                                           HpcFileLocation fileLocation,
    		                                           HpcFileLocation fileSource) 
    		                                          throws HpcException; 
    
    /**
     * Get the physical file location of a data object.
     *
     * @param path The data object path.
     * @return HpcFileLocation
     * 
     * @throws HpcException
     */
    public HpcFileLocation getFileLocation(String path) throws HpcException; 
    
    /**
     * Get collection by its path.
     *
     * @param path The collection's path.
     * @return HpcCollection.
     * 
     * @throws HpcException
     */
    public HpcCollection getCollection(String path) throws HpcException;
    
    /**
     * Get collections by metadata query.
     *
     * @param metadataQueries The metadata queries.
     * @return HpcCollection list.
     * 
     * @throws HpcException
     */
    public List<HpcCollection> getCollections(
    		    List<HpcMetadataQuery> metadataQueries) throws HpcException;
    
    /**
     * Get metadata of a collection.
     *
     * @param path The collection path.
     * @return HpcMetadataEntry collection.
     * 
     * @throws HpcException
     */
    public List<HpcMetadataEntry> getCollectionMetadata(String path) throws HpcException;
    
    /**
     * Get data object by its path.
     *
     * @param path The data object's path.
     * @return HpcDataObject.
     * 
     * @throws HpcException
     */
    public HpcDataObject getDataObject(String path) throws HpcException;
    
    /**
     * Get data objects by metadata query.
     *
     * @param metadataQueries The metadata queries.
     * @return HpcDataObject list.
     * 
     * @throws HpcException
     */
    public List<HpcDataObject> getDataObjects(
    		    List<HpcMetadataQuery> metadataQueries) throws HpcException;
    
    /**
     * Get metadata of a data object.
     *
     * @param path The collection path.
     * @return HpcMetadataEntry collection.
     * 
     * @throws HpcException
     */
    public List<HpcMetadataEntry> getDataObjectMetadata(String path) throws HpcException;
    
    /**
     * Get the user type of this service invoker.
     *
     * @return The user's type
     * 
     * @throws HpcException
     */
    public String getUserType() throws HpcException;  
    
    /**
     * Add a user.
     *
     * @param nciAccount The NCI account of the user to be added to data management.
     * @param userType The iRODS user type to assign to the new user.
     * 
     * @throws HpcException
     */
    public void addUser(HpcNciAccount nciAccount, String userType) throws HpcException;
    
    /**
     * Close connection to Data Management system for the current service call.
     */
    public void closeConnection();
    
    /**
     * Set permission of an entity (collection or data object) for a user. 
     *
     * @param path The entity path.
     * @param permissionRequest The permission request.
     * 
     * @throws HpcException
     */
    public void setPermission(String path, HpcUserPermission permissionRequest) 
    		                 throws HpcException;
}

 