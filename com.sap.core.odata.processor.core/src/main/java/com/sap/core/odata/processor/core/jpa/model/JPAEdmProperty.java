/*******************************************************************************
 * Copyright 2013 SAP AG
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.sap.core.odata.processor.core.jpa.model;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.SingularAttribute;

import com.sap.core.odata.api.edm.EdmFacets;
import com.sap.core.odata.api.edm.EdmSimpleTypeKind;
import com.sap.core.odata.api.edm.FullQualifiedName;
import com.sap.core.odata.api.edm.provider.ComplexProperty;
import com.sap.core.odata.api.edm.provider.ComplexType;
import com.sap.core.odata.api.edm.provider.Facets;
import com.sap.core.odata.api.edm.provider.Property;
import com.sap.core.odata.api.edm.provider.SimpleProperty;
import com.sap.core.odata.processor.api.jpa.access.JPAEdmBuilder;
import com.sap.core.odata.processor.api.jpa.exception.ODataJPAModelException;
import com.sap.core.odata.processor.api.jpa.exception.ODataJPARuntimeException;
import com.sap.core.odata.processor.api.jpa.model.JPAEdmAssociationEndView;
import com.sap.core.odata.processor.api.jpa.model.JPAEdmAssociationView;
import com.sap.core.odata.processor.api.jpa.model.JPAEdmComplexPropertyView;
import com.sap.core.odata.processor.api.jpa.model.JPAEdmComplexTypeView;
import com.sap.core.odata.processor.api.jpa.model.JPAEdmEntityTypeView;
import com.sap.core.odata.processor.api.jpa.model.JPAEdmKeyView;
import com.sap.core.odata.processor.api.jpa.model.JPAEdmNavigationPropertyView;
import com.sap.core.odata.processor.api.jpa.model.JPAEdmPropertyView;
import com.sap.core.odata.processor.api.jpa.model.JPAEdmReferentialConstraintView;
import com.sap.core.odata.processor.api.jpa.model.JPAEdmSchemaView;
import com.sap.core.odata.processor.core.jpa.access.model.JPAEdmNameBuilder;
import com.sap.core.odata.processor.core.jpa.access.model.JPATypeConvertor;

public class JPAEdmProperty extends JPAEdmBaseViewImpl implements
    JPAEdmPropertyView, JPAEdmComplexPropertyView {

  private JPAEdmSchemaView schemaView;
  private JPAEdmEntityTypeView entityTypeView;
  private JPAEdmComplexTypeView complexTypeView;
  private JPAEdmNavigationPropertyView navigationPropertyView = null;

  private JPAEdmKeyView keyView;
  private List<Property> properties;
  private SimpleProperty currentSimpleProperty = null;
  private ComplexProperty currentComplexProperty = null;
  private Attribute<?, ?> currentAttribute;
  private boolean isBuildModeComplexType;

  public JPAEdmProperty(final JPAEdmSchemaView view) {
    super(view);
    schemaView = view;
    entityTypeView = schemaView.getJPAEdmEntityContainerView()
        .getJPAEdmEntitySetView().getJPAEdmEntityTypeView();
    complexTypeView = schemaView.getJPAEdmComplexTypeView();
    navigationPropertyView = new JPAEdmNavigationProperty(schemaView);
    isBuildModeComplexType = false;
  }

  public JPAEdmProperty(final JPAEdmSchemaView schemaView,
      final JPAEdmComplexTypeView view) {
    super(view);
    this.schemaView = schemaView;
    complexTypeView = view;
    isBuildModeComplexType = true;
  }

  @Override
  public JPAEdmBuilder getBuilder() {
    if (builder == null) {
      builder = new JPAEdmPropertyBuilder();
    }

    return builder;
  }

  @Override
  public List<Property> getEdmPropertyList() {
    return properties;
  }

  @Override
  public JPAEdmKeyView getJPAEdmKeyView() {
    return keyView;
  }

  @Override
  public SimpleProperty getEdmSimpleProperty() {
    return currentSimpleProperty;
  }

  @Override
  public Attribute<?, ?> getJPAAttribute() {
    return currentAttribute;
  }

  @Override
  public ComplexProperty getEdmComplexProperty() {
    return currentComplexProperty;
  }

  @Override
  public JPAEdmNavigationPropertyView getJPAEdmNavigationPropertyView()
  {
    return navigationPropertyView;
  }

  private class JPAEdmPropertyBuilder implements JPAEdmBuilder {
    /*
     * 
     * Each call to build method creates a new EDM Property List. 
     * The Property List can be created either by an Entity type or
     * ComplexType. The flag isBuildModeComplexType tells if the
     * Properties are built for complex type or for Entity Type.
     * 
     * While Building Properties Associations are built. However
     * the associations thus built does not contain Referential
     * constraint. Associations thus built only contains
     * information about Referential constraints. Adding of
     * referential constraints to Associations is the taken care
     * by Schema.
     * 
     * Building Properties is divided into four parts
     * 	A) Building Simple Properties
     * 	B) Building Complex Properties
     * 	C) Building Associations
     * 	D) Building Navigation Properties
     *  
     * ************************************************************
     * 					Build EDM Schema - STEPS
     * ************************************************************
     * A) 	Building Simple Properties:
     * 
     * 	1) 	Fetch JPA Attribute List from 
     * 			A) Complex Type
     * 			B) Entity Type
     * 	  	depending on isBuildModeComplexType.
     * B)	Building Complex Properties
     * C)	Building Associations
     * D)	Building Navigation Properties
    	
     * ************************************************************
     * 					Build EDM Schema - STEPS
     * ************************************************************
     *
     */
    @Override
    public void build() throws ODataJPAModelException, ODataJPARuntimeException {

      JPAEdmBuilder keyViewBuilder = null;

      properties = new ArrayList<Property>();

      Set<?> jpaAttributes = null;

      if (isBuildModeComplexType) {
        jpaAttributes = complexTypeView.getJPAEmbeddableType()
            .getAttributes();
      } else {

        jpaAttributes = entityTypeView.getJPAEntityType()
            .getAttributes();
      }

      for (Object jpaAttribute : jpaAttributes) {
        currentAttribute = (Attribute<?, ?>) jpaAttribute;

        PersistentAttributeType attributeType = currentAttribute
            .getPersistentAttributeType();

        switch (attributeType) {
        case BASIC:

          currentSimpleProperty = new SimpleProperty();
          JPAEdmNameBuilder
              .build((JPAEdmPropertyView) JPAEdmProperty.this, isBuildModeComplexType);

          EdmSimpleTypeKind simpleTypeKind = JPATypeConvertor
              .convertToEdmSimpleType(currentAttribute
                  .getJavaType(), currentAttribute);

          currentSimpleProperty.setType(simpleTypeKind);
          currentSimpleProperty
              .setFacets(setFacets(currentAttribute));

          properties.add(currentSimpleProperty);

          if (((SingularAttribute<?, ?>) currentAttribute).isId()) {
            if (keyView == null) {
              keyView = new JPAEdmKey(JPAEdmProperty.this);
              keyViewBuilder = keyView.getBuilder();
            }

            keyViewBuilder.build();
          }

          break;
        case EMBEDDED:
          ComplexType complexType = complexTypeView
              .searchEdmComplexType(currentAttribute.getJavaType().getName());

          if (complexType == null) {
            JPAEdmComplexTypeView complexTypeViewLocal = new JPAEdmComplexType(
                schemaView, currentAttribute);
            complexTypeViewLocal.getBuilder().build();
            complexType = complexTypeViewLocal.getEdmComplexType();
            complexTypeView.addJPAEdmCompleTypeView(complexTypeViewLocal);

          }

          if (isBuildModeComplexType == false
              && entityTypeView.getJPAEntityType().getIdType()
                  .getJavaType()
                  .equals(currentAttribute.getJavaType())) {

            if (keyView == null) {
              keyView = new JPAEdmKey(complexTypeView,
                  JPAEdmProperty.this);
            }
            keyView.getBuilder().build();
            complexTypeView.expandEdmComplexType(complexType, properties, currentAttribute.getName());
          }
          else {
            currentComplexProperty = new ComplexProperty();
            if (isBuildModeComplexType) {
              JPAEdmNameBuilder
                  .build((JPAEdmComplexPropertyView) JPAEdmProperty.this,
                      complexTypeView.getJPAEmbeddableType().getJavaType().getSimpleName());
            } else {
              JPAEdmNameBuilder
                  .build((JPAEdmComplexPropertyView) JPAEdmProperty.this,
                      JPAEdmProperty.this);
            }
            currentComplexProperty.setType(new FullQualifiedName(
                schemaView.getEdmSchema().getNamespace(),
                complexType.getName()));
            currentComplexProperty
                .setFacets(setFacets(currentAttribute));
            properties.add(currentComplexProperty);
            List<String> nonKeyComplexTypes = schemaView.getNonKeyComplexTypeList();
            if (!nonKeyComplexTypes.contains(currentComplexProperty.getType().getName()))
            {
              schemaView.addNonKeyComplexName(currentComplexProperty.getType().getName());
            }
          }

          break;
        case MANY_TO_MANY:
        case ONE_TO_MANY:
        case ONE_TO_ONE:
        case MANY_TO_ONE:

          JPAEdmAssociationEndView associationEndView = new JPAEdmAssociationEnd(entityTypeView, JPAEdmProperty.this);
          associationEndView.getBuilder().build();

          JPAEdmAssociationView associationView = schemaView.getJPAEdmAssociationView();
          if (associationView.searchAssociation(associationEndView) == null) {
            JPAEdmAssociationView associationViewLocal = new JPAEdmAssociation(associationEndView, entityTypeView, JPAEdmProperty.this);
            associationViewLocal.getBuilder().build();
            associationView.addJPAEdmAssociationView(associationViewLocal);
          }

          JPAEdmReferentialConstraintView refConstraintView = new JPAEdmReferentialConstraint(
              associationView, entityTypeView, JPAEdmProperty.this);
          refConstraintView.getBuilder().build();

          if (refConstraintView.isExists()) {
            associationView.addJPAEdmRefConstraintView(refConstraintView);
          }

          if (navigationPropertyView == null)
          {
            navigationPropertyView = new JPAEdmNavigationProperty(schemaView);
          }
          JPAEdmNavigationPropertyView localNavigationPropertyView = new JPAEdmNavigationProperty(associationView, JPAEdmProperty.this);
          localNavigationPropertyView.getBuilder().build();
          navigationPropertyView.addJPAEdmNavigationPropertyView(localNavigationPropertyView);
          break;
        default:
          break;
        }
      }

    }

    private EdmFacets setFacets(final Attribute<?, ?> jpaAttribute)
        throws ODataJPAModelException, ODataJPARuntimeException {

      Facets facets = new Facets();
      if (jpaAttribute.getJavaMember() instanceof AnnotatedElement) {
        Column column = ((AnnotatedElement) jpaAttribute
            .getJavaMember()).getAnnotation(Column.class);
        if (column != null) {
          EdmSimpleTypeKind attrEmdType = JPATypeConvertor
              .convertToEdmSimpleType(jpaAttribute.getJavaType(), jpaAttribute);
          if (column.nullable()) {
            facets.setNullable(true);
          } else {
            facets.setNullable(false);
          }
          if (column.length() != 0
              && attrEmdType.equals(EdmSimpleTypeKind.String)) {
            facets.setMaxLength(column.length());
          }
          if (column.precision() != 0
              && attrEmdType.equals(EdmSimpleTypeKind.Double)) {
            facets.setPrecision(column.precision());
          }
        }
        return facets;
      }
      return facets;
    }
  }

  @Override
  public JPAEdmEntityTypeView getJPAEdmEntityTypeView() {
    return entityTypeView;
  }

  @Override
  public JPAEdmComplexTypeView getJPAEdmComplexTypeView() {
    return complexTypeView;
  }
}
