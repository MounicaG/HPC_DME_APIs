/**
 * HpcDataRegistrationDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.List;

import gov.nih.nci.hpc.domain.datamanagement.HpcBulkDataObjectRegistrationTaskStatus;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationResult;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationTask;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Registration DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcDataRegistrationDAO {
	/**
	 * Delete a bulk data object registration task.
	 *
	 * @param id
	 *            The data object list registration task ID.
	 * @throws HpcException
	 *             on database error.
	 */
	public void deleteBulkDataObjectRegistrationTask(String id) throws HpcException;

	/**
	 * Get a bulk data object registration result.
	 *
	 * @param id
	 *            The registration task ID.
	 * @return The registration task result object, or null if not found.
	 * @throws HpcException
	 *             on database error.
	 */
	public HpcBulkDataObjectRegistrationResult getBulkDataObjectRegistrationResult(String id) throws HpcException;

	/**
	 * Get a bulk data object list registration task.
	 *
	 * @param id
	 *            The bulk data object registration task ID.
	 * @return The registration task object, or null if not found.
	 * @throws HpcException
	 *             on database error.
	 */
	public HpcBulkDataObjectRegistrationTask getBulkDataObjectRegistrationTask(String id) throws HpcException;

	/**
	 * Get bulk data object registration tasks for a user.
	 *
	 * @param status
	 *            Get tasks in this status.
	 * @return A list of data object list registration tasks.
	 * @throws HpcException
	 *             on database error.
	 */
	public List<HpcBulkDataObjectRegistrationTask> getBulkDataObjectRegistrationTasks(
			HpcBulkDataObjectRegistrationTaskStatus status) throws HpcException;

	/**
	 * Get bulk data object Registration tasks for a user.
	 *
	 * @param userId
	 *            The user ID to query for.
	 * @return A list of active bulk data object registration tasks.
	 * @throws HpcException
	 *             on database error.
	 */
	public List<HpcBulkDataObjectRegistrationTask> getBulkDataObjectRegistrationTasks(String userId)
			throws HpcException;

	/**
	 * Store a new bulk data object registration result, or updated an task result.
	 *
	 * @param registrationResult
	 *            The registration task result to persist.
	 * @throws HpcException
	 *             on database error.
	 */
	public void upsertBulkDataObjectRegistrationResult(HpcBulkDataObjectRegistrationResult registrationResult)
			throws HpcException;

	/**
	 * Store a new bulk data object registration task (if
	 * dataObjectListRegistrationTask.getId() is provided NULL), or update an
	 * existing task. Note: If a new task is inserted,
	 * dataObjectDownloadTask.getId() will be updated with the generated ID.
	 *
	 * @param dataObjectListRegistrationTask
	 *            The data object registration task to persist.
	 * @throws HpcException
	 *             on database error.
	 */
	public void upsertBulkDataObjectRegistrationTask(HpcBulkDataObjectRegistrationTask dataObjectListRegistrationTask)
			throws HpcException;

	/**
	 * Get a bulk data object registration results for a user.
	 *
	 * @param userId
	 *            The user ID to query for.
	 * @param offset
	 *            Skip that many download-results in the returned results.
	 * @param limit
	 *            No more than 'limit' download-results will be returned.
	 * 
	 * @return A list of completed bulk data object registration tasks.
	 * @throws HpcException
	 *             on database error.
	 */
	public List<HpcBulkDataObjectRegistrationResult> getBulkDataObjectRegistrationResults(String userId, int offset,
			int limit) throws HpcException;

	/**
	 * Get bulk data object registration results count for a user.
	 *
	 * @param userId
	 *            The user ID to query for.
	 * @return A total count of completed registration tasks.
	 * @throws HpcException
	 *             on database error.
	 */
	public int getBulkDataObjectRegistrationResultsCount(String userId) throws HpcException;
}
