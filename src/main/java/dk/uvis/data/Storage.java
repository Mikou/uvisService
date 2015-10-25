package dk.uvis.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmKeyPropertyRef;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import dk.uvis.service.DemoEdmProvider;
import dk.uvis.util.Util;

public class Storage {

	private List<Entity> productList;
	private List<Entity> categoryList;
	private List<Entity> personList;
	private List<Entity> projectList;
	private List<Entity> activityList;

	public Storage() {
		productList = new ArrayList<Entity>();
		categoryList = new ArrayList<Entity>();
		personList = new ArrayList<Entity>();
		projectList = new ArrayList<Entity>();
		activityList = new ArrayList<Entity>();
		
		initProductSampleData();
		initCategorySampleData();
		initPersonSampleData();
		initProjectSampleData();
		initActivitySampleData();
	}

	/* PUBLIC FACADE */

	public Entity createEntityData(EdmEntitySet edmEntitySet, Entity entityToCreate) {
	
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
	
		// actually, this is only required if we have more than one Entity Type
		if (edmEntityType.getName().equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
			return createProduct(edmEntityType, entityToCreate);
		}
	
		return null;
	}

	public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet) throws ODataApplicationException {
		EntityCollection entitySet = null;
		// actually, this is only required if we have more than one Entity Sets
		if (edmEntitySet.getName().equals(DemoEdmProvider.ES_PRODUCTS_NAME)) {
			entitySet = getProducts();
		} else if(edmEntitySet.getName().equals(DemoEdmProvider.ES_CATEGORIES_NAME)) {
			entitySet = getCategories();
		} else if(edmEntitySet.getName().equals(DemoEdmProvider.ES_PEOPLE_NAME)) {
			entitySet = getPeople();
		} else if(edmEntitySet.getName().equals(DemoEdmProvider.ES_PROJECTS_NAME)) {
			entitySet = getProjects();
		} else if(edmEntitySet.getName().equals(DemoEdmProvider.ES_ACTIVITIES_NAME)) {
			entitySet = getActivities();
		}

		return entitySet;
	}

	public Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams)
			throws ODataApplicationException {

		Entity entity = null;
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();

		// actually, this is only required if we have more than one Entity Type
		if (edmEntityType.getName().equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
			entity = getProduct(edmEntityType, keyParams);
		} else if (edmEntityType.getName().equals(DemoEdmProvider.ET_CATEGORY_NAME)) {
			entity = getCategory(edmEntityType, keyParams);
		} else if (edmEntityType.getName().equals(DemoEdmProvider.ET_ACTIVITY_NAME)) {
			entity = getActivity(edmEntityType, keyParams);
		} else if (edmEntityType.getName().equals(DemoEdmProvider.ET_PROJECT_NAME)) {
			entity = getProject(edmEntityType, keyParams);
		} else if (edmEntityType.getName().equals(DemoEdmProvider.ET_PERSON_NAME)) {
			entity = getPerson(edmEntityType, keyParams);
		}

		return entity;
	}

	/**
	 * This method is invoked for PATCH or PUT requests
	 */
	public void updateEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams, Entity updateEntity,
			HttpMethod httpMethod) throws ODataApplicationException {
	
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
	
		// actually, this is only required if we have more than one Entity Type
		if (edmEntityType.getName().equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
			updateProduct(edmEntityType, keyParams, updateEntity, httpMethod);
		}
	}

	public void deleteEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams)
			throws ODataApplicationException {
	
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
	
		// actually, this is only required if we have more than one Entity Type
		if (edmEntityType.getName().equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
			deleteProduct(edmEntityType, keyParams);
		}
	}

	// Navigation
	
	public Entity getRelatedEntity(Entity entity, EdmEntityType relatedEntityType) {
		EntityCollection collection = getRelatedEntityCollection(entity, relatedEntityType);
		if(collection.getEntities().isEmpty()){
			return null;
		}
		return collection.getEntities().get(0);
	}

	public Entity getRelatedEntity(Entity entity, EdmEntityType relatedEntityType, List<UriParameter> keyPredicates) throws ODataApplicationException {
	
		EntityCollection relatedEntities = getRelatedEntityCollection(entity, relatedEntityType);
		return Util.findEntity(relatedEntityType, relatedEntities, keyPredicates);
	
	}

	public EntityCollection getRelatedEntityCollection(Entity sourceEntity, EdmEntityType targetEntityType) {
		EntityCollection navigationTargetEntityCollection = new EntityCollection();
	
		FullQualifiedName relatedEntityFqn = targetEntityType.getFullQualifiedName();
		String sourceEntityFqn = sourceEntity.getType();
	
		if (sourceEntityFqn.equals(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString())
				&& relatedEntityFqn.equals(DemoEdmProvider.ET_CATEGORY_FQN)) {
			// relation Products->Category (result all categories)
			navigationTargetEntityCollection.setId(createId(sourceEntity, "ID", DemoEdmProvider.NAV_TO_CATEGORY));
			int productID = (Integer) sourceEntity.getProperty("ID").getValue();
			if (productID == 1 || productID == 2) {
				navigationTargetEntityCollection.getEntities().add(categoryList.get(0));
			} else if (productID == 3 || productID == 4) {
				navigationTargetEntityCollection.getEntities().add(categoryList.get(1));
			} else if (productID == 5 || productID == 6) {
				navigationTargetEntityCollection.getEntities().add(categoryList.get(2));
			}
		} else if (sourceEntityFqn.equals(DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString())
				&& relatedEntityFqn.equals(DemoEdmProvider.ET_PRODUCT_FQN)) {
			// relation Category->Products (result all products)
			navigationTargetEntityCollection.setId(createId(sourceEntity, "ID", DemoEdmProvider.NAV_TO_PRODUCT));
			int categoryID = (Integer) sourceEntity.getProperty("ID").getValue();
			if (categoryID == 1) {
				// the first 2 products are notebooks
				navigationTargetEntityCollection.getEntities().addAll(productList.subList(0, 2));
			} else if (categoryID == 2) {
				// the next 2 products are organizers
				navigationTargetEntityCollection.getEntities().addAll(productList.subList(2, 4));
			} else if (categoryID == 3) {
				// the first 2 products are monitors
				navigationTargetEntityCollection.getEntities().addAll(productList.subList(4, 6));
			}
		} else if(sourceEntityFqn.equals(DemoEdmProvider.ET_PERSON_FQN.getFullQualifiedNameAsString())
				&& relatedEntityFqn.equals(DemoEdmProvider.ET_ACTIVITY_FQN)) {
			
			navigationTargetEntityCollection.setId(createId(sourceEntity, "ID", DemoEdmProvider.NAV_TO_ACTIVITY));
			int personID = (Integer) sourceEntity.getProperty("ID").getValue();
			if(personID == 1 || personID == 2) {
				navigationTargetEntityCollection.getEntities().add(activityList.get(0));
				navigationTargetEntityCollection.getEntities().add(activityList.get(1));
			} else if(personID == 3 || personID == 4) {
				navigationTargetEntityCollection.getEntities().add(activityList.get(1));
			} else if(personID == 5 || personID == 6) {
				navigationTargetEntityCollection.getEntities().add(activityList.get(2));
			} else if(personID == 7 || personID == 8) {
				navigationTargetEntityCollection.getEntities().add(activityList.get(3));
			}
			
		} else if (sourceEntityFqn.equals(DemoEdmProvider.ET_ACTIVITY_FQN.getFullQualifiedNameAsString())
				&& relatedEntityFqn.equals(DemoEdmProvider.ET_PERSON_FQN)) {
			
			navigationTargetEntityCollection.setId(createId(sourceEntity, "ID", DemoEdmProvider.NAV_TO_PERSON));
			int activityId = (Integer) sourceEntity.getProperty("ID").getValue();
			if(activityId == 1) {
				navigationTargetEntityCollection.getEntities().addAll(personList.subList(0, 2));
			} else if (activityId == 2) {
				navigationTargetEntityCollection.getEntities().addAll(personList.subList(2, 4));
			} else if (activityId == 3) {
				navigationTargetEntityCollection.getEntities().addAll(personList.subList(4, 6));
			} else if (activityId == 4) {
				navigationTargetEntityCollection.getEntities().addAll(personList.subList(6, 8));
			}
		} else if(sourceEntityFqn.equals(DemoEdmProvider.ET_PROJECT_FQN.getFullQualifiedNameAsString())
				&& relatedEntityFqn.equals(DemoEdmProvider.ET_ACTIVITY_FQN)) {
			
			navigationTargetEntityCollection.setId(createId(sourceEntity, "ID", DemoEdmProvider.NAV_TO_ACTIVITY));
			int projectID = (Integer) sourceEntity.getProperty("ID").getValue();
			if(projectID == 1) {
				navigationTargetEntityCollection.getEntities().addAll(activityList.subList(0, 1));
			} else if(projectID == 2) {
				navigationTargetEntityCollection.getEntities().addAll(activityList.subList(0, 3));
			} else if(projectID == 3) {
				navigationTargetEntityCollection.getEntities().addAll(activityList.subList(2, 3));
			} else if(projectID == 4) {
				navigationTargetEntityCollection.getEntities().add(activityList.get(2));
			}
			
		} else if (sourceEntityFqn.equals(DemoEdmProvider.ET_ACTIVITY_FQN.getFullQualifiedNameAsString())
				&& relatedEntityFqn.equals(DemoEdmProvider.ET_PROJECT_FQN)) {
			
			navigationTargetEntityCollection.setId(createId(sourceEntity, "ID", DemoEdmProvider.NAV_TO_PROJECT));
			int activityId = (Integer) sourceEntity.getProperty("ID").getValue();
			if(activityId == 1) {
				navigationTargetEntityCollection.getEntities().add(projectList.get(3));
			} else if (activityId == 2) {
				navigationTargetEntityCollection.getEntities().add(projectList.get(2));
			} else if (activityId == 3) {
				navigationTargetEntityCollection.getEntities().add(projectList.get(1));
			} else if (activityId == 4) {
				navigationTargetEntityCollection.getEntities().add(projectList.get(0));
			}
		}
	
		if (navigationTargetEntityCollection.getEntities().isEmpty()) {
			return null;
		}
	
		return navigationTargetEntityCollection;
	
	}

	private EntityCollection getActivities() {
		EntityCollection retEntitySet = new EntityCollection();

		for (Entity activityEntity : this.activityList) {
			retEntitySet.getEntities().add(activityEntity);
		}

		return retEntitySet;
	}
	
	private Entity getActivity(EdmEntityType edmEntityType, List<UriParameter> keyParams)
			throws ODataApplicationException {

		// the list of entities at runtime
		EntityCollection entitySet = getActivities();

		return Util.findEntity(edmEntityType, entitySet, keyParams);
		
	}

	private EntityCollection getProjects() {
		EntityCollection retEntitySet = new EntityCollection();

		for (Entity projectEntity : this.projectList) {
			retEntitySet.getEntities().add(projectEntity);
		}

		return retEntitySet;
	}
	
	private Entity getProject(EdmEntityType edmEntityType, List<UriParameter> keyParams)
			throws ODataApplicationException {

		// the list of entities at runtime
		EntityCollection entitySet = getProjects();

		return Util.findEntity(edmEntityType, entitySet, keyParams);
		
	}

	private EntityCollection getPeople() {
		EntityCollection retEntitySet = new EntityCollection();

		for (Entity personEntity : this.personList) {
			retEntitySet.getEntities().add(personEntity);
		}

		return retEntitySet;
	}

	private Entity getPerson(EdmEntityType edmEntityType, List<UriParameter> keyParams)
			throws ODataApplicationException {

		// the list of entities at runtime
		EntityCollection entitySet = getPeople();

		return Util.findEntity(edmEntityType, entitySet, keyParams);
		
	}
	
	private Entity createProduct(EdmEntityType edmEntityType, Entity entity) {
	
		// the ID of the newly created product entity is generated automatically
		int newId = 1;
		while (productIdExists(newId)) {
			newId++;
		}
	
		Property idProperty = entity.getProperty("ID");
		if (idProperty != null) {
			idProperty.setValue(ValueType.PRIMITIVE, Integer.valueOf(newId));
		} else {
			// as of OData v4 spec, the key property can be omitted from the
			// POST request body
			entity.getProperties().add(new Property(null, "ID", ValueType.PRIMITIVE, newId));
		}
		entity.setId(createId(entity, "Products"));
		this.productList.add(entity);
	
		return entity;
	
	}

	/* INTERNAL */

	private EntityCollection getProducts() {
		EntityCollection retEntitySet = new EntityCollection();

		for (Entity productEntity : this.productList) {
			retEntitySet.getEntities().add(productEntity);
		}

		return retEntitySet;
	}

	private Entity getProduct(EdmEntityType edmEntityType, List<UriParameter> keyParams)
			throws ODataApplicationException {

		// the list of entities at runtime
		EntityCollection entitySet = getProducts();

		return Util.findEntity(edmEntityType, entitySet, keyParams);
		
	}

	private void updateProduct(EdmEntityType edmEntityType, List<UriParameter> keyParams, Entity entity,
			HttpMethod httpMethod) throws ODataApplicationException {
	
		Entity productEntity = getProduct(edmEntityType, keyParams);
		if (productEntity == null) {
			throw new ODataApplicationException("Entity not found", HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		}
	
		// loop over all properties and replace the values with the values of
		// the given payload
		// Note: ignoring ComplexType, as we don't have it in our odata model
		List<Property> existingProperties = productEntity.getProperties();
		for (Property existingProp : existingProperties) {
			String propName = existingProp.getName();
	
			// ignore the key properties, they aren't updateable
			if (isKey(edmEntityType, propName)) {
				continue;
			}
	
			Property updateProperty = entity.getProperty(propName);
			// the request payload might not consider ALL properties, so it can
			// be null
			if (updateProperty == null) {
				// if a property has NOT been added to the request payload
				// depending on the HttpMethod, our behavior is different
				if (httpMethod.equals(HttpMethod.PATCH)) {
					// in case of PATCH, the existing property is not touched
					continue; // do nothing
				} else if (httpMethod.equals(HttpMethod.PUT)) {
					// in case of PUT, the existing property is set to null
					existingProp.setValue(existingProp.getValueType(), null);
					continue;
				}
			}
	
			// change the value of the properties
			existingProp.setValue(existingProp.getValueType(), updateProperty.getValue());
		}
	}

	private void deleteProduct(EdmEntityType edmEntityType, List<UriParameter> keyParams)
			throws ODataApplicationException {
	
		Entity productEntity = getProduct(edmEntityType, keyParams);
		if (productEntity == null) {
			throw new ODataApplicationException("Entity not found", HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		}
	
		this.productList.remove(productEntity);
	}

	private boolean productIdExists(int id) {

		for (Entity entity : this.productList) {
			Integer existingID = (Integer) entity.getProperty("ID").getValue();
			if (existingID.intValue() == id) {
				return true;
			}
		}

		return false;
	}

	private EntityCollection getCategories() {
		EntityCollection entitySet = new EntityCollection();
		
		for(Entity categoryEntity : this.categoryList) {
			entitySet.getEntities().add(categoryEntity);
		}
		
		return entitySet;
	}
	
	private Entity getCategory(EdmEntityType edmEntityType, List<UriParameter> keyParams) throws ODataApplicationException {
		EntityCollection entitySet = getCategories();
		
		return Util.findEntity(edmEntityType, entitySet, keyParams);
	}
	
	/* HELPER */

	private boolean isKey(EdmEntityType edmEntityType, String propertyName) {
		List<EdmKeyPropertyRef> keyPropertyRefs = edmEntityType.getKeyPropertyRefs();
		for (EdmKeyPropertyRef propRef : keyPropertyRefs) {
			String keyPropertyName = propRef.getName();
			if (keyPropertyName.equals(propertyName)) {
				return true;
			}
		}
		return false;
	}
	
	private void initPersonSampleData() {
		Entity entity;
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Alice"));
		entity.addProperty(new Property(null, "Age", ValueType.PRIMITIVE, "30"));
		entity.setType(DemoEdmProvider.ET_PERSON_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		personList.add(entity);
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Noël"));
		entity.addProperty(new Property(null, "Age", ValueType.PRIMITIVE, "20"));
		entity.setType(DemoEdmProvider.ET_PERSON_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		personList.add(entity);
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Peter"));
		entity.addProperty(new Property(null, "Age", ValueType.PRIMITIVE, "40"));
		entity.setType(DemoEdmProvider.ET_PERSON_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		personList.add(entity);

		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 4));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "John"));
		entity.addProperty(new Property(null, "Age", ValueType.PRIMITIVE, "38"));
		entity.setType(DemoEdmProvider.ET_PERSON_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		personList.add(entity);
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 5));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Ada"));
		entity.addProperty(new Property(null, "Age", ValueType.PRIMITIVE, "22"));
		entity.setType(DemoEdmProvider.ET_PERSON_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		personList.add(entity);
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 6));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Julie"));
		entity.addProperty(new Property(null, "Age", ValueType.PRIMITIVE, "21"));
		entity.setType(DemoEdmProvider.ET_PERSON_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		personList.add(entity);
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 7));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Eric"));
		entity.addProperty(new Property(null, "Age", ValueType.PRIMITIVE, "39"));
		entity.setType(DemoEdmProvider.ET_PERSON_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		personList.add(entity);
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 8));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Bob"));
		entity.addProperty(new Property(null, "Age", ValueType.PRIMITIVE, "20"));
		entity.setType(DemoEdmProvider.ET_PERSON_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		personList.add(entity);
	}
	
	private void initProjectSampleData() {
		Entity entity;
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "P1"));
		entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE, "Brief description for project 1."));
		entity.setType(DemoEdmProvider.ET_PROJECT_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		projectList.add(entity);
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "P2"));
		entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE, "Brief description for project 2."));
		entity.setType(DemoEdmProvider.ET_PROJECT_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		projectList.add(entity);
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "P3"));
		entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE, "Brief description for project 3."));
		entity.setType(DemoEdmProvider.ET_PROJECT_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		projectList.add(entity);
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 4));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "P4"));
		entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE, "Brief description for project 4."));
		entity.setType(DemoEdmProvider.ET_PROJECT_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		projectList.add(entity);

	}

	private void initActivitySampleData() {
		
		final Date lastWeekAnd2Days = new Date(System.currentTimeMillis()-9*24*60*60*1000);
		final Date lastWeek = new Date(System.currentTimeMillis()-7*24*60*60*1000);
		final Date yesturday = new Date(System.currentTimeMillis()-1*24*60*60*1000);
		final Date today = new Date();
		final Date tomorrow = new Date(System.currentTimeMillis()+24*60*60*1000);
		
		Entity entity;
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Activity1"));
		entity.addProperty(new Property(null, "Start", ValueType.PRIMITIVE, lastWeekAnd2Days));
		entity.addProperty(new Property(null, "End", ValueType.PRIMITIVE, lastWeek));
		entity.setType(DemoEdmProvider.ET_ACTIVITY_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		activityList.add(entity);
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Activity2"));
		entity.addProperty(new Property(null, "Start", ValueType.PRIMITIVE, lastWeekAnd2Days));
		entity.addProperty(new Property(null, "End", ValueType.PRIMITIVE, yesturday));
		entity.setType(DemoEdmProvider.ET_ACTIVITY_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		activityList.add(entity);
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Activity3"));
		entity.addProperty(new Property(null, "Start", ValueType.PRIMITIVE, yesturday));
		entity.addProperty(new Property(null, "End", ValueType.PRIMITIVE, today));
		entity.setType(DemoEdmProvider.ET_ACTIVITY_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		activityList.add(entity);
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 4));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Activity3"));
		entity.addProperty(new Property(null, "Start", ValueType.PRIMITIVE, today));
		entity.addProperty(new Property(null, "End", ValueType.PRIMITIVE, tomorrow));
		entity.setType(DemoEdmProvider.ET_ACTIVITY_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		activityList.add(entity);
	}
	
	private void initProductSampleData() {

	    Entity entity;
	    
	    entity = new Entity();
	    entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1));
	    entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebook Basic 15"));
	    entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
	        "Notebook Basic, 1.7GHz - 15 XGA - 1024MB DDR2 SDRAM - 40GB"));
	    entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
	    entity.setId(createId(entity, "ID"));
	    productList.add(entity);

	    entity = new Entity();
	    entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2));
	    entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebook Professional 17"));
	    entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
	        "Notebook Professional, 2.8GHz - 15 XGA - 8GB DDR3 RAM - 500GB"));
	    entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
	    entity.setId(createId(entity, "ID"));
	    productList.add(entity);

	    entity = new Entity();
	    entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3));
	    entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "1UMTS PDA"));
	    entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
	        "Ultrafast 3G UMTS/HSDPA Pocket PC, supports GSM network"));
	    entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
	    entity.setId(createId(entity, "ID"));
	    productList.add(entity);

	    entity = new Entity();
	    entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 4));
	    entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Comfort Easy"));
	    entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
	        "32 GB Digital Assitant with high-resolution color screen"));
	    entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
	    entity.setId(createId(entity, "ID"));
	    productList.add(entity);

	    entity = new Entity();
	    entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 5));
	    entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Ergo Screen"));
	    entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
	        "19 Optimum Resolution 1024 x 768 @ 85Hz, resolution 1280 x 960"));
	    entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
	    entity.setId(createId(entity, "ID"));
	    productList.add(entity);

	    entity = new Entity();
	    entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 6));
	    entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Flat Basic"));
	    entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
	        "Optimum Hi-Resolution max. 1600 x 1200 @ 85Hz, Dot Pitch: 0.24mm"));
	    entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
	    entity.setId(createId(entity, "ID"));
	    productList.add(entity);

	}

	private void initCategorySampleData() {
		Entity entity;
		
		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebooks"));
		entity.setType(DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		categoryList.add(entity);

		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Organizers"));
		entity.setType(DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		categoryList.add(entity);

		entity = new Entity();
		entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3));
		entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Monitors"));
		entity.setType(DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString());
		entity.setId(createId(entity, "ID"));
		categoryList.add(entity);

	}

	private URI createId(Entity entity, String idPropertyName) {
		return createId(entity, idPropertyName, null);
	}

	private URI createId(Entity entity, String idPropertyName, String navigationName) {
		try {
			StringBuilder sb = new StringBuilder(getEntitySetName(entity)).append("(");
			final Property property = entity.getProperty(idPropertyName);
			sb.append(property.asPrimitive()).append(")");
			if(navigationName != null) {
				sb.append("/").append(navigationName);
			}
			return new URI(sb.toString());
		} catch (URISyntaxException e) {
			throw new ODataRuntimeException("Unable to create (Atom) id for entity: " + entity, e);
		}
	}
	
	private String getEntitySetName(Entity entity) {
		if(DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString().equals(entity.getType())){
			return DemoEdmProvider.ES_CATEGORIES_NAME;
		} else if(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString().equals(entity.getType())) {
			return DemoEdmProvider.ES_PRODUCTS_NAME;
		} else if(DemoEdmProvider.ET_PERSON_FQN.getFullQualifiedNameAsString().equals(entity.getType())) {
			return DemoEdmProvider.ES_PEOPLE_NAME;
		}else if(DemoEdmProvider.ET_PROJECT_FQN.getFullQualifiedNameAsString().equals(entity.getType())) {
			return DemoEdmProvider.ES_PROJECTS_NAME;
		}else if(DemoEdmProvider.ET_ACTIVITY_FQN.getFullQualifiedNameAsString().equals(entity.getType())) {
			return DemoEdmProvider.ES_ACTIVITIES_NAME;
		}
		return entity.getType();
	}
	
}