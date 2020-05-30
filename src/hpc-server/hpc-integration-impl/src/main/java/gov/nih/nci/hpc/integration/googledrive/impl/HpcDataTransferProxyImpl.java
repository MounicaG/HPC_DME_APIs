package gov.nih.nci.hpc.integration.googledrive.impl;

import static gov.nih.nci.hpc.util.HpcUtil.toNormalizedPath;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;

/**
 * HPC Data Transfer Proxy Google Drive Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataTransferProxyImpl implements HpcDataTransferProxy {
  // ---------------------------------------------------------------------//
  // Constants
  // ---------------------------------------------------------------------//

  // Google drive folder mime-type.
  private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

  // Google drive search query for folders.
  private static final String FOLDER_QUERY =
      "mimeType = '" + FOLDER_MIME_TYPE + "' and name = '%s' and '%s' in parents";

  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The Google Drive download executor.
  @Autowired
  @Qualifier("hpcGoogleDriveDownloadExecutor")
  Executor googleDriveExecutor = null;

  // The S3 connection instance.
  @Autowired
  private HpcGoogleDriveConnection googleDriveConnection = null;

  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(getClass().getName());

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /** Constructor for spring injection. */
  private HpcDataTransferProxyImpl() {}

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // HpcDataTransferProxy Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  public Object authenticate(String accessToken) throws HpcException {
    return googleDriveConnection.authenticate(accessToken);
  }

  @Override
  public String downloadDataObject(Object authenticatedToken,
      HpcDataObjectDownloadRequest downloadRequest, HpcArchive baseArchiveDestination,
      HpcDataTransferProgressListener progressListener) throws HpcException {
    // Input validation
    if (progressListener == null) {
      throw new HpcException(
          "[Google Drive] No progress listener provided for a download to Google Drive destination",
          HpcErrorType.UNEXPECTED_ERROR);
    }

    // Authenticate the Google Drive access token.
    final Drive drive = googleDriveConnection.getDrive(googleDriveConnection
        .authenticate(downloadRequest.getGoogleDriveDestination().getAccessToken()));

    // Stream the file to Google Drive.
    CompletableFuture<Void> googleDriveDownloadFuture = CompletableFuture.runAsync(() -> {
      try {
        // Find / Create the folder in Google Drive where we download the file to
        String destinationPath = toNormalizedPath(
            downloadRequest.getGoogleDriveDestination().getDestinationLocation().getFileId());
        int lastSlashIndex = destinationPath.lastIndexOf('/');
        String destinationFolderPath = destinationPath.substring(0, lastSlashIndex);
        String destinationFileName = destinationPath.substring(lastSlashIndex + 1);
        String folderId = getFolderId(drive, destinationFolderPath);

        // Transfer the file to Google Drive, and complete the download task.
        progressListener.transferCompleted(drive.files().create(
            new File().setName(destinationFileName).setParents(Collections.singletonList(folderId)),
            new InputStreamContent("application/octet-stream",
                new URL(downloadRequest.getArchiveLocationURL()).openStream()))
            .setFields("size").execute().getSize());

      } catch (IOException e) {
        String message = "[GoogleDrive] Failed to download object: " + e.getMessage();
        logger.error(message, HpcErrorType.DATA_TRANSFER_ERROR, e);
        progressListener.transferFailed(message);
      }

    }, googleDriveExecutor);

    return String.valueOf(googleDriveDownloadFuture.hashCode());
  }

  @Override
  public HpcPathAttributes getPathAttributes(Object authenticatedToken,
      HpcFileLocation fileLocation, boolean getSize) throws HpcException {

    Drive drive = googleDriveConnection.getDrive(authenticatedToken);

    HpcPathAttributes pathAttributes = new HpcPathAttributes();
    pathAttributes.setIsAccessible(true);
    
    try {
      File file = getFile(drive, fileLocation.getFileContainerId());
      if (file == null) {
        pathAttributes.setExists(false);
      } else {
        pathAttributes.setExists(true);
        if (file.getMimeType().equals(FOLDER_MIME_TYPE)) {
          pathAttributes.setIsDirectory(true);
          pathAttributes.setIsFile(false);
        } else {
          pathAttributes.setIsDirectory(false);
          pathAttributes.setIsFile(true);
          if (getSize) {
            pathAttributes.setSize(file.getSize());
          }
        }
      }

    } catch(GoogleJsonResponseException e) {
      if(e.getStatusCode() == 401) {
        pathAttributes.setIsAccessible(false);
      } else {
        throw new HpcException("[Google Drive] Failed to get file: " + e.getMessage(),
            HpcErrorType.DATA_TRANSFER_ERROR, e);
      }
    } 
    catch (IOException e) {
      throw new HpcException("[Google Drive] Failed to get file: " + e.getMessage(),
          HpcErrorType.DATA_TRANSFER_ERROR, e);
    }
    
    return pathAttributes;
  }

  @Override
  public String generateDownloadRequestURL(Object authenticatedToken,
      HpcFileLocation archiveLocation, Integer downloadRequestURLExpiration) throws HpcException {
    throw new HpcException("Generating download URL is not supported",
        HpcErrorType.UNEXPECTED_ERROR);
  }

  @Override
  public List<HpcDirectoryScanItem> scanDirectory(Object authenticatedToken,
      HpcFileLocation directoryLocation) throws HpcException {
    throw new HpcException("scanDirectory() not supported", HpcErrorType.UNEXPECTED_ERROR);
  }

  // ---------------------------------------------------------------------//
  // Helper Methods
  // ---------------------------------------------------------------------//

  /**
   * Find the ID of a folder in Google Drive. If not found, the folder is created.
   *
   * @param drive A Google drive instance.
   * @param folderPath The folder path to find / create in Google Drive.
   * @return The folder ID.
   * @throws IOException on data transfer system failure.
   */
  private String getFolderId(Drive drive, String folderPath) throws IOException {
    String parentFolderId = "root";
    if (!StringUtils.isEmpty(folderPath)) {
      boolean createNewFolder = false;
      for (String subFolderName : folderPath.split("/")) {
        if (StringUtils.isEmpty(subFolderName)) {
          continue;
        }
        if (!createNewFolder) {
          // Search for the sub-folder.
          FileList result =
              drive.files().list().setQ(String.format(FOLDER_QUERY, subFolderName, parentFolderId))
                  .setFields("files(id)").execute();
          if (!result.getFiles().isEmpty()) {
            // Sub-folder was found. Note: In Google Drive, it's possible to have multiple folders
            // with the same name
            // We simply grab the first one on the list.
            parentFolderId = result.getFiles().get(0).getId();
            continue;

          } else {
            // Sub-folder was not found.
            createNewFolder = true;
          }
        }

        // Creating a new sub folder.
        parentFolderId = drive.files()
            .create(new File().setName(subFolderName)
                .setParents(Collections.singletonList(parentFolderId))
                .setMimeType("application/vnd.google-apps.folder"))
            .setFields("id").execute().getId();
      }
    }

    return parentFolderId;
  }

  /**
   * Get a file/folder by ID or path.
   *
   * @param drive A Google drive instance.
   * @param idOrPath The file/folder ID or path to get.
   * @return A File instance or null if not found
   * @throws IOException on data transfer system failure.
   */
  private File getFile(Drive drive, String idOrPath) throws IOException {
    File file = null;
    try {
      file = drive.files().get(idOrPath).setFields("id,name,mimeType,size").execute();
      
    } catch(GoogleJsonResponseException e) {
      if(e.getStatusCode() != 404) {
        throw e;
      }
    }
    
    return file;
  }
}
