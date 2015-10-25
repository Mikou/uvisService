package dk.uvis.service;

import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.ContextURL.Suffix;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.edm.EdmElement;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmNavigationPropertyBinding;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.uvis.data.Storage;
import dk.uvis.util.Util;

public class DemoEntityProcessor implements EntityProcessor {

	private OData odata;
	private ServiceMetadata serviceMetadata;
	private Storage storage;

	public DemoEntityProcessor(Storage storage) {
		this.storage = storage;
	}

	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;

	}

	@Override
	public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, DeserializerException, SerializerException {

		// 1. Retrieve the entity type from the URI
		EdmEntitySet edmEntitySet = Util.getEdmEntitySet(uriInfo);
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();

		// 2. create the data in backend
		// 2.1. retrieve the payload from the POST request for the entity to
		// create and deserialize it
		InputStream requestInputStream = request.getBody();
		ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
		DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
		Entity requestEntity = result.getEntity();
		// 2.2 do the creation in backend, which returns the newly created
		// entity
		Entity createdEntity = storage.createEntityData(edmEntitySet, requestEntity);

		// 3. serialize the response (we have to return the created entity)
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();
		// expand and select currently not supported
		EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();

		ODataSerializer serializer = this.odata.createSerializer(responseFormat);
		SerializerResult serializedResponse = serializer.entity(serviceMetadata, edmEntityType, createdEntity, options);

		// 4. configure the response object
		response.setContent(serializedResponse.getContent());
		response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
	}

	@Override
	public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, SerializerException {
		Entity responseEntity               = null; // required for serialization of the response body
		EdmEntitySet currentEdmEntitySet    = null; // required to keep track of the EntitySet
		EdmEntitySet responseEdmEntitySet   = null; // we'll need this to build the ContextURL
		EdmEntityType responseEdmEntityType = null; // we'll need this to build the ContextURL

		// 1st step: retrieve the requested Entity: can be "normal" read operation, or navigation (to-one)
		List<UriResource> resourceParts           = uriInfo.getUriResourceParts();
		Iterator<UriResource> uriResourceIterator = resourceParts.iterator();

		Logger logger = LoggerFactory.getLogger(getClass());
		UriResourceEntitySet uriResourceEntitySet = null;
		int count=0;
		while(uriResourceIterator.hasNext()) {
			UriResource currentUriResource = uriResourceIterator.next();
			logger.info("currentSegment: " + currentUriResource.toString() + ", Iteration : " + ++count);
			
			if(currentUriResource instanceof UriResourceEntitySet) {
				
				uriResourceEntitySet  = (UriResourceEntitySet) currentUriResource;
				currentEdmEntitySet   = uriResourceEntitySet.getEntitySet();
				responseEdmEntitySet  = currentEdmEntitySet;
				responseEdmEntityType = currentEdmEntitySet.getEntityType();
				
				// fetch from backend
				List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
				responseEntity = storage.readEntityData(currentEdmEntitySet, keyPredicates);
				
			} else if(currentUriResource instanceof UriResourceNavigation) {
				logger.info("resourceNavigation");
				
				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) currentUriResource;
				EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
				responseEdmEntityType = edmNavigationProperty.getType();
				responseEdmEntitySet  = Util.getNavigationTargetEntitySet(currentEdmEntitySet, edmNavigationProperty);
				
				// fetch from backend
				List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
				Entity sourceEntity = storage.readEntityData(currentEdmEntitySet, keyPredicates);
				List<UriParameter> navKeyPredicates = uriResourceNavigation.getKeyPredicates();
				if (navKeyPredicates.isEmpty()) { // e.g. DemoService.svc/Products(1)/Category
					responseEntity = storage.getRelatedEntity(sourceEntity, responseEdmEntityType);
				} else { // e.g. DemoService.svc/Categories(3)/Products(5)
					responseEntity = storage.getRelatedEntity(sourceEntity, responseEdmEntityType, navKeyPredicates);
				}
				
				// we set the currentEdmEntitySet to be the value of responseEdmEntitySet
				// to handle the case where there is an additional URL segment
				currentEdmEntitySet = responseEdmEntitySet;
				
			}
		}
		
		// handle expand
		ExpandOption expandOption = uriInfo.getExpandOption();
		// in our example:
		// http://localhost:8080/DemoService/DemoService.svc/Categories(1)?$expand=Products
		// or
		// http://localhost:8080/DemoService/DemoService.svc/Products(1)?$expand=Category
		if (expandOption != null) {
			// retrieve the EdmNavigationProperty from the expand expression
			// Note: in our example, we have only one NavigationProperty, so we
			// can directly access it
			EdmNavigationProperty edmNavigationProperty = null;
			ExpandItem expandItem = expandOption.getExpandItems().get(0);
			if (expandItem.isStar()) {
				List<EdmNavigationPropertyBinding> bindings = responseEdmEntitySet.getNavigationPropertyBindings();
				// we know that there are navigation bindings
				// however normally in this case a check if navigation bindings
				// exists is done
				if (!bindings.isEmpty()) {
					// can in our case only be 'Category' or 'Products', so we
					// can take the first
					EdmNavigationPropertyBinding binding = bindings.get(0);
					EdmElement property = responseEdmEntitySet.getEntityType().getProperty(binding.getPath());
					// we don't need to handle error cases, as it is done in the
					// Olingo library
					if (property instanceof EdmNavigationProperty) {
						edmNavigationProperty = (EdmNavigationProperty) property;
					}
				}
			} else {
				// can be 'Category' or 'Products', no path supported
				UriResource uriResource = expandItem.getResourcePath().getUriResourceParts().get(0);
				// we don't need to handle error cases, as it is done in the
				// Olingo library
				if (uriResource instanceof UriResourceNavigation) {
					edmNavigationProperty = ((UriResourceNavigation) uriResource).getProperty();
				}
			}

			// can be 'Category' or 'Products', no path supported
			// we don't need to handle error cases, as it is done in the Olingo
			// library
			if (edmNavigationProperty != null) {
				EdmEntityType expandEdmEntityType = edmNavigationProperty.getType();
				String navPropName = edmNavigationProperty.getName();

				// build the inline data
				Link link = new Link();
				link.setTitle(navPropName);
				link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
				link.setRel(Constants.NS_ASSOCIATION_LINK_REL + navPropName);

				if (edmNavigationProperty.isCollection()) { 
					// in case of Categories(1)/$expand=Products fetch the data for the $expand (to-many navigation) from
					// backend here we get the data for the expand
					EntityCollection expandEntityCollection = storage.getRelatedEntityCollection(responseEntity, expandEdmEntityType);
					link.setInlineEntitySet(expandEntityCollection);
					URI expandEntityCollectionID = expandEntityCollection.getId();
					link.setHref(expandEntityCollectionID.toASCIIString());
				} else { // in case of Products(1)?$expand=Category fetch the data for the $expand (to-one navigation) from
					// backend here we get the data for the expand
					Entity expandEntity = storage.getRelatedEntity(responseEntity, expandEdmEntityType);
					link.setInlineEntity(expandEntity);
					link.setHref(expandEntity.getId().toASCIIString());
				}

				// set the link - containing the expanded data - to the current
				// entity
				responseEntity.getNavigationLinks().add(link);
			}
		}

		if (responseEntity == null) {
			// this is the case for e.g. DemoService.svc/Categories(4) or
			// DemoService.svc/Categories(3)/Products(999)
			throw new ODataApplicationException("Nothing found.", HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ROOT);
		}

		// 3. serialize
		ContextURL contextUrl = ContextURL.with().entitySet(responseEdmEntitySet).suffix(Suffix.ENTITY).build();
		EntitySerializerOptions opts = EntitySerializerOptions
										.with()
										.contextURL(contextUrl)
										.expand(expandOption)
										.build();

		ODataSerializer serializer = this.odata.createSerializer(responseFormat);
		SerializerResult serializerResult = serializer.entity(this.serviceMetadata, responseEdmEntityType, responseEntity,
				opts);

		// 4. configure the response object
		response.setContent(serializerResult.getContent());
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());

	}

	@Override
	public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// Note: only in our example we can assume that the first segment is the
		// EntitySet
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();

		// 2. update the data in backend
		// 2.1. retrieve the payload from the PUT request for the entity to be
		// updated
		InputStream requestInputStream = request.getBody();
		ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
		DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
		Entity requestEntity = result.getEntity();
		// 2.2 do the modification in backend
		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
		// Note that this updateEntity()-method is invoked for both PUT or PATCH
		// operations
		HttpMethod httpMethod = request.getMethod();
		storage.updateEntityData(edmEntitySet, keyPredicates, requestEntity, httpMethod);

		// 3. configure the response object
		response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
	}

	@Override
	public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {

		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// Note: only in our example we can assume that the first segment is the
		// EntitySet
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		// 2. delete the data in backend
		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
		storage.deleteEntityData(edmEntitySet, keyPredicates);

		// 3. configure the response object
		response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());

	}

}
