/**
 * HpcDatasetRegistrationRestService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs;

import gov.nih.nci.hpc.dto.project.HpcProjectRegistrationDTO;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * <p>
 * HPC Project REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id:  $
 */

@Path("/")
public interface HpcProjectRestService
{   
    /**
     * POST registration request.
     *
     * @param registrationInput The data registration input DTO.
     */
    @POST
    @Path("/project")
    @Consumes("application/json,application/xml")
    public Response registerProject(HpcProjectRegistrationDTO projectRegistrationDTO);
    
    /**
     * GET project by ID.
     *
     * @param id The Project ID.
     * @return gov.nih.nci.hpc.dto.project.HpcProjectDTO entity.
     */
    @GET
    @Path("/project/{id}")
    @Produces("application/json,application/xml")
    public Response getProject(@PathParam("id") String id); 
    
    /**
     * GET Projects by Registrator ID.
     *
     * @param creatorId Get projects associated with this Registrator ID.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/project/query/registrator/{id}")
    @Produces("application/json,application/xml")
    public Response getProjectsByRegistratorId(@PathParam("id") String registratorId); 
    
    /**
     * GET Projects by PI ID.
     *
     * @param creatorId Get projects associated with this Registrator ID.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/project/query/inverstigator/{id}")
    @Produces("application/json,application/xml")
    public Response getProjectsByInvestigatorId(@PathParam("id") String inverstigatorId); 
}

 