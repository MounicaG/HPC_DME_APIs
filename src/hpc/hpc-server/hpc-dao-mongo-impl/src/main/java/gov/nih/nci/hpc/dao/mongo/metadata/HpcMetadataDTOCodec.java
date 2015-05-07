/**
 * HpcMetadataDTOCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.metadata;

import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Metadata DTO Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMetadataDTOCodec 
             implements CollectibleCodec<HpcMetadataBsonDocument>
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Mongo DB name.
    //private final static String DB_NAME = "hpc"; 
    
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
	// The Document codec.
	private Codec<Document> documentCodec;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcMetadataDTOCodec() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                                HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor w/ Document codec
     * 
     * @param documentCodec Document Codec.
     * 
     * @throws HpcException If documentCodec is null.
     */
    public HpcMetadataDTOCodec(Codec<Document> documentCodec) 
                              throws HpcException
    {
    	if(documentCodec == null) {
    	   throw new HpcException("Null Document Codec", 
    			                  HpcErrorType.INVALID_INPUT);
    	}
    	
    	this.documentCodec = documentCodec;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // CollectibleCodec<HpcMetadataBsonDocument> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, 
			           HpcMetadataBsonDocument metadataDocument,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the DTO.
		ObjectId objectId = metadataDocument.getObjectId();
		Double size = metadataDocument.getMetadataDTO().getSize();
		String userId = metadataDocument.getMetadataDTO().getUserId();
 
		// Set the data on the BSON document.
		if(objectId != null) {
		   document.put("_id", objectId);
		}
 
		if(size != null) {
		   document.put("size", size);
		}
 
		if(userId != null) {
		   document.put("user_id", userId);
		}
 
		documentCodec.encode(writer, document, encoderContext);
 
	}
 
	@Override
	public Class<HpcMetadataBsonDocument> getEncoderClass() 
	{
		return HpcMetadataBsonDocument.class;
	}
 
	@Override
	public HpcMetadataBsonDocument decode(BsonReader reader, 
			                              DecoderContext decoderContext) 
	{
		Document document = documentCodec.decode(reader, decoderContext);
		
		HpcMetadataBsonDocument metadataDocument = new HpcMetadataBsonDocument();
 
		metadataDocument.setObjectId(document.getObjectId("_id"));
 
		metadataDocument.getMetadataDTO().setUserId(document.getString("user_id"));
 
		metadataDocument.getMetadataDTO().setSize(document.getDouble("size"));
		
		return metadataDocument;
	}
 
	@Override
	public HpcMetadataBsonDocument generateIdIfAbsentFromDocument(
			                       HpcMetadataBsonDocument metadataDocument) 
	{
		return documentHasId(metadataDocument) ? metadataDocument : 
			                                     new HpcMetadataBsonDocument();
	}
 
	@Override
	public boolean documentHasId(HpcMetadataBsonDocument metadataDocument) 
	{
		return metadataDocument.getObjectId() != null;
	}
 
	@Override
	public BsonValue getDocumentId(HpcMetadataBsonDocument metadataDocument) 
	{
	    return new BsonString(metadataDocument.getObjectId().toHexString());
	}
}

 