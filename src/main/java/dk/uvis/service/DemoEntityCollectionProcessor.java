package dk.uvis.service;

import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.ContextURL;
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
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;

import dk.uvis.data.Storage;
import dk.uvis.util.Util;

public class DemoEntityCollectionProcessor implements EntityCollectionProcessor {

	private OData odata;
	private ServiceMetadata serviceMetadata;
	private Storage storage;

	public DemoEntityCollectionProcessor(Storage storage) {
		this.storage = storage;
	}

	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, SerializerException {

		EdmEntitySet responseEdmEntitySet = null;
		EntityCollection responseEntityCollection = null;
		Entity sourceEntity = null;
		// 1st we have retrieve the requested EntitySet from the uriInfo object
		// (representation of the parsed service URI)
		List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		int segmentCount = resourceParts.size();

		UriResource uriResource = resourceParts.get(0);
		if (!(uriResource instanceof UriResourceEntitySet)) {
			throw new ODataApplicationException("Only EntitySet is supported",
					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
		}

		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
		EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();

		// 2. fetch data from backend
		if (segmentCount == 1) {
			responseEdmEntitySet = startEdmEntitySet;
			responseEntityCollection = storage.readEntitySetData(startEdmEntitySet);
			List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
			sourceEntity = storage.readEntityData(startEdmEntitySet, keyPredicates);
		} else if (segmentCount == 2) {
			UriResource lastSegment = resourceParts.get(1);
			if (lastSegment instanceof UriResourceNavigation) {
				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
				EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
				EdmEntityType targetEntityType = edmNavigationProperty.getType();
				responseEdmEntitySet = Util.getNavigationTargetEntitySet(startEdmEntitySet, edmNavigationProperty);
				List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
				sourceEntity = storage.readEntityData(startEdmEntitySet, keyPredicates);

				if (sourceEntity == null) {
					throw new ODataApplicationException("Entity not found.", HttpStatusCode.NOT_FOUND.getStatusCode(),
							Locale.ROOT);
				}

				responseEntityCollection = storage.getRelatedEntityCollection(sourceEntity, targetEntityType);
			}
		} else {
			throw new ODataApplicationException("Not supported.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
					Locale.ROOT);
		}

		// 3rd apply System Query Options
		List<Entity> entityList = responseEntityCollection.getEntities();
		responseEntityCollection = new EntityCollection();

		// $select
		SelectOption selectOption = uriInfo.getSelectOption();
		ExpandOption expandOption = uriInfo.getExpandOption();
		// handle $expand
		// in our example:
		// http://localhost:8080/DemoService/DemoService.svc/Categories/$expand=Products
		// or
		// http://localhost:8080/DemoService/DemoService.svc/Products?$expand=Category
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
				uriResource = expandItem.getResourcePath().getUriResourceParts().get(0);
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
				String navPropName = edmNavigationProperty.getName();
				EdmEntityType expandEdmEntityType = edmNavigationProperty.getType();

				List<Entity> responseEntityList = responseEntityCollection.getEntities();
				for (Entity entity : entityList) {
					Link link = new Link();
					link.setTitle(navPropName);
					link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
					link.setRel(Constants.NS_ASSOCIATION_LINK_REL + navPropName);

					if (edmNavigationProperty.isCollection()) { 
						// in case of Categories/$expand=Products fetch the data for the $expand (to-many navigation) from backend
						EntityCollection expandEntityCollection = storage.getRelatedEntityCollection(entity,
								expandEdmEntityType);
						link.setInlineEntitySet(expandEntityCollection);
						link.setHref(expandEntityCollection.getId().toASCIIString());
					} else { 
						// in case of Products?$expand=Category fetch the data for the $expand (to-one navigation) from backend
						// here we get the data for the expand
						Entity expandEntity = storage.getRelatedEntity(entity, expandEdmEntityType);
						link.setInlineEntity(expandEntity);
						link.setHref(expandEntity.getId().toASCIIString());
					}

					// set the link - containing the expanded data - to the
					// current entity
					entity.getNavigationLinks().add(link);
				}
			}
		}

		// $count
		CountOption cntOpt = uriInfo.getCountOption();
		if (cntOpt != null) {
			boolean isCnt = cntOpt.getValue();
			if (isCnt) {
				responseEntityCollection.setCount(entityList.size());
			}
		}

		// $skip
		SkipOption skipOpt = uriInfo.getSkipOption();
		if (skipOpt != null) {
			int skipNbr = skipOpt.getValue();
			if (skipNbr >= 0) {
				if (skipNbr <= entityList.size()) {
					entityList = entityList.subList(skipNbr, entityList.size());
				} else {
					entityList.clear();
				}
			} else {
				throw new ODataApplicationException("Invalid value for $skip.",
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
			}
		}

		// $top
		TopOption topOpt = uriInfo.getTopOption();
		if (topOpt != null) {
			int topNbr = topOpt.getValue();
			if (topNbr >= 0) {
				if (topNbr <= entityList.size()) {
					entityList = entityList.subList(0, topNbr);
				}
			} else {
				throw new ODataApplicationException("Invalid value for $top.",
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
			}
		}

		// copy back from entityList to responseEntityCollection
		for (Entity e : entityList)
			responseEntityCollection.getEntities().add(e);

		// 4th: create a serializer based on the requested format (json)
		ODataSerializer serializer = odata.createSerializer(responseFormat);
		// serialize the content. EntitySet -> InputStream
		EdmEntityType edmEntityType = responseEdmEntitySet.getEntityType();

		String selectList = odata.createUriHelper().buildContextURLSelectList(edmEntityType, null, selectOption);

		ContextURL contextUrl = ContextURL.with().entitySet(responseEdmEntitySet).selectList(selectList).build();

		final String id = request.getRawBaseUri() + "/" + responseEdmEntitySet.getName();
		EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions
				.with()
				.contextURL(contextUrl)
				.select(selectOption)
				.expand(expandOption)
				.id(id)
				.build();

		SerializerResult serializerResult = serializer.entityCollection(this.serviceMetadata, edmEntityType,
				responseEntityCollection, opts);

		// 4th Finally: configure the response object: set the body, headers and
		// status code
		response.setContent(serializerResult.getContent()); // <- getContent() =
															// inputStream
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());

	}
}
