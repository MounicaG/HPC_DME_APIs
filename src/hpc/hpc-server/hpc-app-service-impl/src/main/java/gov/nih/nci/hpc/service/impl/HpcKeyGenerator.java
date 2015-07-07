/**
 * HpcKeyGenerator.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

/**
 * <p>
 * HPC Key Generator Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcKeyGenerator 
{         
    /**
     * Generate a Key.
     *
     * @return A unique key
     */
    public String generateKey();
    
    /**
     * Validate the key.
     *
     * @param key The key to validate.
     * @return True if the key is valid, or false otherwise.
     */
    public boolean validateKey(String key);
}

 