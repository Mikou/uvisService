package dk.uvis.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;

public class DemoEdmProvider extends CsdlAbstractEdmProvider {

	// Service Namespace
	public static final String NAMESPACE = "OData.Uvis";

	// EDM Container
	public static final String CONTAINER_NAME = "Container";
	public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

	// Entity Types Names
	public static final String ET_PRODUCT_NAME = "Product";
	public static final FullQualifiedName ET_PRODUCT_FQN = new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME);
	
	public static final String ET_PERSON_NAME = "Person";
	public static final FullQualifiedName ET_PERSON_FQN = new FullQualifiedName(NAMESPACE, ET_PERSON_NAME);

	public static final String ET_PROJECT_NAME = "Project";
	public static final FullQualifiedName ET_PROJECT_FQN = new FullQualifiedName(NAMESPACE, ET_PROJECT_NAME);
	
	public static final String ET_ACTIVITY_NAME = "Activity";
	public static final FullQualifiedName ET_ACTIVITY_FQN = new FullQualifiedName(NAMESPACE, ET_ACTIVITY_NAME);
	
	public static final String ET_CATEGORY_NAME = "Category";
	public static final FullQualifiedName ET_CATEGORY_FQN = new FullQualifiedName(NAMESPACE, ET_CATEGORY_NAME);

	// Entity Set Names
	public static final String ES_PRODUCTS_NAME = "Products";
	public static final String ES_PEOPLE_NAME = "People";
	public static final String ES_PROJECTS_NAME = "Projects";
	public static final String ES_ACTIVITIES_NAME = "Activities";
	public static final String ES_CATEGORIES_NAME = "Categories";

	public static final String NAV_TO_PRODUCT = ES_PRODUCTS_NAME;
	public static final String NAV_TO_CATEGORY = ES_CATEGORIES_NAME;
	
	public static final String NAV_TO_ACTIVITY = ES_ACTIVITIES_NAME;
	public static final String NAV_TO_PERSON = ES_PEOPLE_NAME;
	public static final String NAV_TO_PROJECT = ES_PROJECTS_NAME;
	
	

	@Override
	public CsdlEntityType getEntityType(final FullQualifiedName entityTypeName) throws ODataException {

		CsdlEntityType entityType = null;

		// this method is called for one of the EntityTypes that are configured in the Schema
		if (entityTypeName.equals(ET_PRODUCT_FQN)) {
			// create EntityType properties
			CsdlProperty id = new CsdlProperty().setName("ID")
					.setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
			CsdlProperty name = new CsdlProperty().setName("Name")
					.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty description = new CsdlProperty().setName("Description")
					.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

			// create CsdlPropertyRef for Key element
			CsdlPropertyRef propertyRef = new CsdlPropertyRef();
			propertyRef.setName("ID");

			// navigation property: many-to-one, null not allowed (product must have a category)
			CsdlNavigationProperty navProp = new CsdlNavigationProperty()
					.setName("Category")
					.setType(ET_CATEGORY_FQN)
					.setNullable(false)
					.setPartner("Products");
			List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
			navPropList.add(navProp);

			// configure EntityType
			entityType = new CsdlEntityType();
			entityType.setName(ET_PRODUCT_NAME);
			entityType.setProperties(Arrays.asList(id, name, description));
			entityType.setKey(Collections.singletonList(propertyRef));
			entityType.setNavigationProperties(navPropList);

		} else if (entityTypeName.equals(ET_CATEGORY_FQN)) {
			// create EntityType properties
			CsdlProperty id = new CsdlProperty()
					.setName("ID")
					.setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
			CsdlProperty name = new CsdlProperty()
					.setName("Name")
					.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

			// create PropertyRef for Key element
			CsdlPropertyRef propertyRef = new CsdlPropertyRef();
			propertyRef.setName("ID");

			// navigation property: one-to-many
			CsdlNavigationProperty navProp = new CsdlNavigationProperty()
					.setName("Products")
					.setType(ET_PRODUCT_FQN)
					.setCollection(true)
					.setPartner("Category");
			List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
			navPropList.add(navProp);

			// configure EntityType
			entityType = new CsdlEntityType();
			entityType.setName(ET_CATEGORY_NAME);
			entityType.setProperties(Arrays.asList(id, name));
			entityType.setKey(Arrays.asList(propertyRef));
			entityType.setNavigationProperties(navPropList);
		}  else if (entityTypeName.equals(ET_ACTIVITY_FQN)){
			// create EntityType properties
			CsdlProperty id = new CsdlProperty().setName("ID")
					.setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
			CsdlProperty name = new CsdlProperty().setName("Name")
					.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty start = new CsdlProperty().setName("Start")
					.setType(EdmPrimitiveTypeKind.Date.getFullQualifiedName());
			CsdlProperty end = new CsdlProperty().setName("End")
					.setType(EdmPrimitiveTypeKind.Date.getFullQualifiedName());

			// create CsdlPropertyRef for Key element
			CsdlPropertyRef propertyRef = new CsdlPropertyRef();
			propertyRef.setName("ID");

			entityType = new CsdlEntityType();
			entityType.setName(ET_ACTIVITY_NAME);
			entityType.setProperties(Arrays.asList(id, name, start, end));
			entityType.setKey(Collections.singletonList(propertyRef));
			CsdlNavigationProperty navProp = null;
			List<CsdlNavigationProperty> navPropList = null;
			// navigation property: many-to-one, null not allowed (product must have a category)
			navProp = new CsdlNavigationProperty()
					.setName("Project")
					.setType(ET_PROJECT_FQN)
					.setNullable(false)
					.setPartner("Activity");
			navPropList = new ArrayList<CsdlNavigationProperty>();
			navPropList.add(navProp);
			// navigation property: many-to-one, null not allowed (product must have a category)
			navProp = new CsdlNavigationProperty()
					.setName("Person")
					.setType(ET_PERSON_FQN)
					.setNullable(false)
					.setPartner("Activity");
			//navPropList = new ArrayList<CsdlNavigationProperty>();
			navPropList.add(navProp);
			entityType.setNavigationProperties(navPropList);

			// configure EntityType

			//entityType.setNavigationProperties(navPropList);
		
		} else if (entityTypeName.equals(ET_PERSON_FQN)){
			// create EntityType properties
			CsdlProperty id = new CsdlProperty().setName("ID")
					.setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
			CsdlProperty name = new CsdlProperty().setName("Name")
					.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty age = new CsdlProperty().setName("Age")
					.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

			// create CsdlPropertyRef for Key element
			CsdlPropertyRef propertyRef = new CsdlPropertyRef();
			propertyRef.setName("ID");
			// navigation property: one-to-many
			CsdlNavigationProperty navProp = new CsdlNavigationProperty()
					.setName("Activities")
					.setType(ET_ACTIVITY_FQN)
					.setCollection(true)
					.setPartner("Person");
			List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
			navPropList.add(navProp);

			// configure EntityType
			entityType = new CsdlEntityType();
			entityType.setName(ET_PERSON_NAME);
			entityType.setProperties(Arrays.asList(id, name, age));
			entityType.setKey(Collections.singletonList(propertyRef));
			entityType.setNavigationProperties(navPropList);
			
		} else if (entityTypeName.equals(ET_PROJECT_FQN)){
			// create EntityType properties
			CsdlProperty id = new CsdlProperty().setName("ID")
					.setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
			CsdlProperty name = new CsdlProperty().setName("Name")
					.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty description = new CsdlProperty().setName("Description")
					.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

			// create CsdlPropertyRef for Key element
			CsdlPropertyRef propertyRef = new CsdlPropertyRef();
			propertyRef.setName("ID");

			// navigation property: one-to-many
			CsdlNavigationProperty navProp = new CsdlNavigationProperty()
					.setName("Activities")
					.setType(ET_ACTIVITY_FQN)
					.setCollection(true)
					.setPartner("Project");
			List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
			navPropList.add(navProp);

			// configure EntityType
			entityType = new CsdlEntityType();
			entityType.setName(ET_PROJECT_NAME);
			entityType.setProperties(Arrays.asList(id, name, description));
			entityType.setKey(Collections.singletonList(propertyRef));
			entityType.setNavigationProperties(navPropList);
			
		}
		return entityType;
	}

	@Override
	public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, String entitySetName)
			throws ODataException {
		CsdlEntitySet entitySet = null;

		if (entityContainer.equals(CONTAINER)) {
			if (entitySetName.equals(ES_PRODUCTS_NAME)) {
				entitySet = new CsdlEntitySet();
				entitySet.setName(ES_PRODUCTS_NAME);
				entitySet.setType(ET_PRODUCT_FQN);

				//navigation
				CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
				navPropBinding.setTarget("Categories");
				navPropBinding.setPath("Category");
				List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
				navPropBindingList.add(navPropBinding);
				entitySet.setNavigationPropertyBindings(navPropBindingList);

			} else if(entitySetName.equals(ES_CATEGORIES_NAME)){
				entitySet = new CsdlEntitySet();
				entitySet.setName(ES_CATEGORIES_NAME);
				entitySet.setType(ET_CATEGORY_FQN);
				
				// navigation
				CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
				navPropBinding.setTarget("Products");
				navPropBinding.setPath("Products");
				List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
				navPropBindingList.add(navPropBinding);
				entitySet.setNavigationPropertyBindings(navPropBindingList);
			} else if(entitySetName.equals(ES_PEOPLE_NAME)){
				entitySet = new CsdlEntitySet();
				entitySet.setName(ES_PEOPLE_NAME);
				entitySet.setType(ET_PERSON_FQN);
				
				// navigation
				CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
				navPropBinding.setTarget("Activities");
				navPropBinding.setPath("Activities");
				List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
				navPropBindingList.add(navPropBinding);
				entitySet.setNavigationPropertyBindings(navPropBindingList);

			} else if(entitySetName.equals(ES_PROJECTS_NAME)){
				entitySet = new CsdlEntitySet();
				entitySet.setName(ES_PROJECTS_NAME);
				entitySet.setType(ET_PROJECT_FQN);
				
				// navigation
				CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
				navPropBinding.setTarget("Activities");
				navPropBinding.setPath("Activities");
				List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
				navPropBindingList.add(navPropBinding);
				entitySet.setNavigationPropertyBindings(navPropBindingList);
				
			}else if(entitySetName.equals(ES_ACTIVITIES_NAME)){
				entitySet = new CsdlEntitySet();
				entitySet.setName(ES_ACTIVITIES_NAME);
				entitySet.setType(ET_ACTIVITY_FQN);
				
				//navigation
				CsdlNavigationPropertyBinding navPropBinding = null;
				List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
				
				navPropBinding = new CsdlNavigationPropertyBinding();
				navPropBinding.setTarget("People");
				navPropBinding.setPath("Person");
				navPropBindingList.add(navPropBinding);
				
				navPropBinding = new CsdlNavigationPropertyBinding();
				navPropBinding.setTarget("Projects");
				navPropBinding.setPath("Project");
				navPropBindingList.add(navPropBinding);
				
				entitySet.setNavigationPropertyBindings(navPropBindingList);
			}
		}

		return entitySet;
	}

	@Override
	public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) {
	
		// This method is invoked when displaying the service document at e.g.
		// http://localhost:8080/DemoService/DemoService.svc
		if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
			CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
			entityContainerInfo.setContainerName(CONTAINER);
			return entityContainerInfo;
		}
	
		return null;
	}

	@Override
	public List<CsdlSchema> getSchemas() throws ODataException {

		// create Schema
		CsdlSchema schema = new CsdlSchema();
		schema.setNamespace(NAMESPACE);

		// add EntityTypes
		List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
		entityTypes.add(getEntityType(ET_PRODUCT_FQN));
		entityTypes.add(getEntityType(ET_CATEGORY_FQN));
		entityTypes.add(getEntityType(ET_PERSON_FQN));
		entityTypes.add(getEntityType(ET_PROJECT_FQN));
		entityTypes.add(getEntityType(ET_ACTIVITY_FQN));
		schema.setEntityTypes(entityTypes);

		// add EntityContainer
		schema.setEntityContainer(getEntityContainer());

		// finally
		List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
		schemas.add(schema);

		return schemas;
	}

	@Override
	public CsdlEntityContainer getEntityContainer() throws ODataException {
	
		// create EntitySets
		List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
		entitySets.add(getEntitySet(CONTAINER, ES_PRODUCTS_NAME));
		entitySets.add(getEntitySet(CONTAINER, ES_CATEGORIES_NAME));
		entitySets.add(getEntitySet(CONTAINER, ES_PEOPLE_NAME));
		entitySets.add(getEntitySet(CONTAINER, ES_PROJECTS_NAME));
		entitySets.add(getEntitySet(CONTAINER, ES_ACTIVITIES_NAME));
		
		// create EntityContainer
		CsdlEntityContainer entityContainer = new CsdlEntityContainer();
		entityContainer.setName(CONTAINER_NAME);
		entityContainer.setEntitySets(entitySets);
	
		return entityContainer;
	}

}