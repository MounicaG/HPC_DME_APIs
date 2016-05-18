package gov.nih.nci.hpc.cli;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.easybatch.core.mapper.RecordMappingException;
import org.easybatch.core.processor.RecordProcessingException;
import org.easybatch.core.processor.RecordProcessor;
import org.easybatch.core.record.Record;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

public class HPCDataFileRecordProcessor implements RecordProcessor{

	@Override
	public Record processRecord(Record record) throws RecordProcessingException {
		// TODO Auto-generated method stub
        InputStream inputStream = null;
        HpcExceptionDTO response = null;
		HPCDataObject hpcObject = (HPCDataObject)record.getPayload();
		HpcDataObjectRegistrationDTO hpcDataObjectRegistrationDTO = hpcObject.getDto();
		List<Attachment> atts = new LinkedList<Attachment>();
		if (hpcDataObjectRegistrationDTO.getSource().getFileContainerId() == null) {
			if (hpcDataObjectRegistrationDTO.getSource().getFileId() == null) {
				throw new RecordMappingException("Invalid or missing file source location");
			} else {

				try {
					inputStream = new BufferedInputStream(
							new FileInputStream(hpcDataObjectRegistrationDTO.getSource().getFileId()));
					ContentDisposition cd2 = new ContentDisposition(
							"attachment;filename=" + hpcDataObjectRegistrationDTO.getSource().getFileId());
					atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObject", inputStream, cd2));
					hpcDataObjectRegistrationDTO.setSource(null);
				} catch (FileNotFoundException e) {
					throw new RecordMappingException("Invalid or missing file source location. Message: "+e.getMessage());
				} catch (IOException e) {
					throw new RecordMappingException("Invalid or missing file source location. Message: "+e.getMessage());
				}
				finally
				{
					if(inputStream != null)
						try {
							inputStream.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
				}
			}
		}
		atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration", "application/json",
				hpcDataObjectRegistrationDTO));
		
		WebClient client = HpcClientUtil.getWebClient(hpcObject.getBasePath()+hpcObject.getObjectPath(), hpcObject.getHpcCertPath(), hpcObject.getHpcCertPassword());
		String token = DatatypeConverter.printBase64Binary((hpcObject.getUserId() + ":" + hpcObject.getPassword()).getBytes());
		client.header("Authorization", "Basic " + token);
		client.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON);
		//client.type(MediaType.MULTIPART_FORM_DATA);

		try {
			System.out.println("Processing: "+hpcObject.getBasePath() + hpcObject.getObjectPath());
			Response restResponse = client.put(new MultipartBody(atts));
			//System.out.println("Processing done: "+hpcObject.getBasePath() + hpcObject.getObjectPath() + " Status: " + restResponse.getStatus());
			if (!(restResponse.getStatus() == 201 || restResponse.getStatus() == 200)) {
				MappingJsonFactory factory = new MappingJsonFactory();
				JsonParser parser = factory.createJsonParser((InputStream) restResponse.getEntity());
				try {
					response = parser.readValueAs(HpcExceptionDTO.class);
				} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
					if (restResponse.getStatus() == 401)
						throw new RecordProcessingException("Unauthorized access: response status is: " + restResponse.getStatus());
					else
						throw new RecordProcessingException("Unalbe process error response: response status is: " + restResponse.getStatus());
				}

				if (response != null) {
					// System.out.println(response);
					StringBuffer buffer = new StringBuffer();
					if (response.getMessage() != null)
						buffer.append("Failed to process record due to: " + response.getMessage());
					else
						buffer.append("Failed to process record due to unkown reason");
					if (response.getErrorType() != null)
						buffer.append(" Error Type:" + response.getErrorType().value());

					if (response.getRequestRejectReason() != null)
						buffer.append(" Request reject reason:" + response.getRequestRejectReason().value());

					throw new RecordProcessingException(buffer.toString());
				} else {
					throw new RecordProcessingException("Failed to process record due to unknown error. Return code: " + restResponse.getStatus());
				}
			}
		} catch (HpcBatchException e) {
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			throw new RecordProcessingException(exceptionAsString);
		} catch (RestClientException e) {
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			throw new RecordProcessingException(exceptionAsString);
		} catch (Exception e) {
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			throw new RecordProcessingException(exceptionAsString);
		}
		return null;
	}

}
