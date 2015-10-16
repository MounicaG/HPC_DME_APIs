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

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
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

public interface HpcCollectionService 
{         
    /**
     * Create a collection's directory.
     *
     * @param path The collection path.
     * 
     * @throws HpcException
     */
    public void createDirectory(String path) throws HpcException;

    /**
     * Add metadata to a collection.
     *
     * @param path The collection path.
     * @param metadataEntries The metadata entries to add.
     * 
     * @throws HpcException
     */
    public void addMetadata(String path, List<HpcMetadataEntry> metadataEntries) 
    		               throws HpcException; 
}

 