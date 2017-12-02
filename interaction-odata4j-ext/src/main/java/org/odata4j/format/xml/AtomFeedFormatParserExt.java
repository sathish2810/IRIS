package org.odata4j.format.xml;

/*
 * #%L
 * interaction-odata4j-ext
 * %%
 * Copyright (C) 2012 - 2013 Temenos Holdings N.V.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.xml.stream.XMLInputFactory;

import org.apache.commons.lang.StringUtils;
import org.core4j.Enumerable;
import org.core4j.Func1;
import org.odata4j.core.OCollection;
import org.odata4j.core.OCollections;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OLink;
import org.odata4j.core.OLinks;
import org.odata4j.core.OObject;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmCollectionType;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmStructuralType;
import org.odata4j.edm.EdmType;
import org.odata4j.format.Entry;
import org.odata4j.internal.FeedCustomizationMapping;
import org.odata4j.internal.InternalUtil;
import org.odata4j.stax2.Attribute2;
import org.odata4j.stax2.QName2;
import org.odata4j.stax2.StartElement2;
import org.odata4j.stax2.XMLEvent2;
import org.odata4j.stax2.XMLEventReader2;
import org.odata4j.stax2.XMLEventWriter2;
import org.odata4j.stax2.XMLFactoryProvider2;
import org.odata4j.stax2.staximpl.StaxXMLInputFactory2Ext;

import com.temenos.interaction.odataext.entity.MetadataOData4j;

public class AtomFeedFormatParserExt extends AtomFeedFormatParser {
    
    private static MetadataOData4j metadataOData4j;
    private static Pattern actionPath = Pattern.compile("(\\/\\w+)$"); 

	public AtomFeedFormatParserExt(MetadataOData4j metadataOData, String entitySetName, OEntityKey entityKey, FeedCustomizationMapping fcMapping) {
		super(metadataOData.getMetadata(), entitySetName, entityKey, fcMapping);
		
		metadataOData4j = metadataOData;
	}
	
	public AtomFeedFormatParserExt(EdmDataServices metadata, String entitySetName, OEntityKey entityKey, FeedCustomizationMapping fcMapping) {
		super(metadata, entitySetName, entityKey, fcMapping);
		
		metadataOData4j = null;
	}
	

	public static class DataServicesAtomEntry extends AtomEntry {
	    public final String etag;
	    public final List<OProperty<?>> properties;

	    private OEntity entity;

	    private DataServicesAtomEntry(String etag, List<OProperty<?>> properties) {
	      this.etag = etag;
	      this.properties = properties;
	    }

	    @Override
	    public String toString() {
	      return InternalUtil.reflectionToString(this);
	    }


	    public OEntity getEntity() {
	      return this.entity;
	    }

	    void setEntity(OEntity entity) {
	      this.entity = entity;
	    }
	  }

	
	@Override
	public AtomFeed parse(Reader reader) {
	    return parseFeed(createXMLEventReader(reader), getEntitySet());
	}
	
	AtomFeed parseFeed(XMLEventReader2 reader, EdmEntitySet entitySet) {

	    AtomFeed feed = new AtomFeed();
	    List<AtomEntry> rt = new ArrayList<AtomEntry>();

	    while (reader.hasNext()) {
	      XMLEvent2 event = reader.nextEvent();

	      if (isStartElement(event, ATOM_ENTRY)) {
	        rt.add(parseEntry(reader, event.asStartElement(), entitySet));
	      } else if (isStartElement(event, ATOM_LINK)) {
	        if ("next".equals(event.asStartElement().getAttributeByName(new QName2("rel")).getValue())) {
	          feed.next = event.asStartElement().getAttributeByName(new QName2("href")).getValue();
	        }
	      } else if (isEndElement(event, ATOM_FEED)) {
	        // return from a sub feed, if we went down the hierarchy
	        break;
	      }

	    }
	    feed.entries = Enumerable.create(rt).cast(Entry.class);

	    return feed;

	  }
	
	private EdmEntitySet getEdmEntitySet(String entitySetName) {
		EdmEntitySet result = null;
		
		if(metadataOData4j == null) {
			result = metadata.getEdmEntitySet(entitySetName);
		} else {
			result = metadataOData4j.getEdmEntitySetByEntitySetName(entitySetName);
		}			
		
		return result;
	}
	
	private EdmEntitySet getEdmEntitySet(EdmEntityType type) {
		EdmEntitySet result = null;
		
		if(metadataOData4j == null) {
			result = metadata.getEdmEntitySet(type);
		} else {
			result = metadataOData4j.getEdmEntitySetByType(type);
		}			
		
		return result;
	}
	
	
	private EdmEntityType findEdmEntityType(String entityTypeName) {
		EdmEntityType result = null;
		
		if(metadataOData4j == null) {
			result = (EdmEntityType)metadata.findEdmEntityType(entityTypeName);
		} else {
			result = (EdmEntityType)metadataOData4j.getEdmEntityTypeByTypeName(entityTypeName);
		}
		
		return result; 		
	}
	
	private AtomEntry parseEntry(XMLEventReader2 reader, StartElement2 entryElement, EdmEntitySet entitySet) {

	    String id = null;
	    String categoryTerm = null;
	    String categoryScheme = null;
	    String title = null;
	    String summary = null;
	    String updated = null;
	    String contentType = null;
	    List<AtomLink> atomLinks = new ArrayList<AtomLink>();

	    String etag = getAttributeValueIfExists(entryElement, M_ETAG);

	    AtomEntry rt = null;

	    while (reader.hasNext()) {
	      XMLEvent2 event = reader.nextEvent();

	      if (event.isEndElement() && event.asEndElement().getName().equals(entryElement.getName())) {
	        rt.id = id; //http://localhost:8810/Oneoff01.svc/Comment(1)
	        rt.title = title;
	        rt.summary = summary;
	        rt.updated = updated;
	        rt.categoryScheme = categoryScheme; //http://schemas.microsoft.com/ado/2007/08/dataservices/scheme
	        rt.categoryTerm = categoryTerm; //NorthwindModel.Customer
	        rt.contentType = contentType;
	        rt.atomLinks = atomLinks;

	        if (rt instanceof DataServicesAtomEntry) {
	          DataServicesAtomEntry dsae = (DataServicesAtomEntry) rt;
	          OEntity entity = entityFromAtomEntry(entitySet, dsae, fcMapping);
	          dsae.setEntity(entity);
	        }
	        return rt;
	      }

	      if (isStartElement(event, ATOM_ID)) {
	        id = reader.getElementText();
	      } else if (isStartElement(event, ATOM_TITLE)) {
	        title = reader.getElementText();
	      } else if (isStartElement(event, ATOM_SUMMARY)) {
	        summary = reader.getElementText();
	      } else if (isStartElement(event, ATOM_UPDATED)) {
	        updated = reader.getElementText();
	      } else if (isStartElement(event, ATOM_CATEGORY)) {
	        categoryTerm = getAttributeValueIfExists(event.asStartElement(), "term");
	        categoryScheme = getAttributeValueIfExists(event.asStartElement(), "scheme");
	        if (categoryTerm != null)
	          entitySet = getEdmEntitySet(findEdmEntityType(categoryTerm));
	      } else if (isStartElement(event, ATOM_LINK)) {
	        AtomLink link = parseAtomLink(reader, event.asStartElement(), entitySet);
	        atomLinks.add(link);
	      } else if (isStartElement(event, M_PROPERTIES)) {
	        rt = parseDSAtomEntry(etag, entitySet.getType(), reader, event);
	      } else if (isStartElement(event, ATOM_CONTENT)) {
	        contentType = getAttributeValueIfExists(event.asStartElement(), "type");
	        if (MediaType.APPLICATION_XML.equals(contentType)) {
	          StartElement2 contentElement = event.asStartElement();
	          StartElement2 valueElement = null;
	          while (reader.hasNext()) {
	            XMLEvent2 event2 = reader.nextEvent();
	            if (valueElement == null && event2.isStartElement()) {
	              valueElement = event2.asStartElement();
	              if (isStartElement(event2, M_PROPERTIES)) {
	                rt = parseDSAtomEntry(etag, entitySet.getType(), reader, event2);
	              } else {
	                BasicAtomEntry bae = new BasicAtomEntry();
	                bae.content = innerText(reader, event2.asStartElement());
	                rt = bae;
	              }
	            }
	            if (event2.isEndElement() && event2.asEndElement().getName().equals(contentElement.getName())) {
	              break;
	            }
	          }
	        } else {
	          BasicAtomEntry bae = new BasicAtomEntry();
	          bae.content = innerText(reader, event.asStartElement());
	          rt = bae;
	        }
	      }
	    }
	    throw new RuntimeException();
	  }
	
	private static String innerText(XMLEventReader2 reader, StartElement2 element) {
	    StringWriter sw = new StringWriter();
	    XMLEventWriter2 writer = XMLFactoryProvider2.getInstance().newXMLOutputFactory2().createXMLEventWriter(sw);
	    while (reader.hasNext()) {

	      XMLEvent2 event = reader.nextEvent();
	      if (event.isEndElement() && event.asEndElement().getName().equals(element.getName())) {

	        return sw.toString();
	      } else {
	        writer.add(event);
	      }

	    }
	    throw new RuntimeException();
	  }
	
	private DataServicesAtomEntry parseDSAtomEntry(String etag, EdmEntityType entityType, XMLEventReader2 reader, XMLEvent2 event) {
	    List<OProperty<?>> properties = Enumerable.create(parseProperties(reader, event.asStartElement(), metadata, entityType)).toList();
	    return new DataServicesAtomEntry(etag, properties);
	  }
	
	private AtomLink parseAtomLink(XMLEventReader2 reader, StartElement2 linkElement, EdmEntitySet entitySet) {
	    AtomLink rt = new AtomLink();
	    rt.relation = getAttributeValueIfExists(linkElement, "rel");
	    rt.type = getAttributeValueIfExists(linkElement, "type");
	    rt.title = getAttributeValueIfExists(linkElement, "title");
	    rt.href = getAttributeValueIfExists(linkElement, "href");
	    rt.inlineContentExpected = false;

	    String navPropertyName = rt.getNavProperty();
	    EdmNavigationProperty navProperty = null;
	    if (entitySet != null && navPropertyName != null)
	      navProperty = entitySet.getType().findNavigationProperty(navPropertyName);
	    EdmEntitySet targetEntitySet = null;
	    if (navProperty != null)
	      targetEntitySet = getEdmEntitySet(navProperty.getToRole().getType());

	    // expected cases:
	    // 1.  </link>                  - no inlined content, i.e. deferred
	    // 2.  <m:inline/></link>       - inlined content but null entity or empty feed
	    // 3.  <m:inline><feed>...</m:inline></link> - inlined content with 1 or more items
	    // 4.  <m:inline><entry>..</m:inline></link> - inlined content 1 an item

	    while (reader.hasNext()) {
	      XMLEvent2 event = reader.nextEvent();

	      if (event.isEndElement() && event.asEndElement().getName().equals(linkElement.getName())) {
	        break;
	      } else if (isStartElement(event, XmlFormatParser.M_INLINE)) {
	        rt.inlineContentExpected = true; // may be null content.
	      } else if (isStartElement(event, ATOM_FEED)) {
	        rt.inlineFeed = parseFeed(reader, targetEntitySet);
	      } else if (isStartElement(event, ATOM_ENTRY)) {
	        rt.inlineEntry = parseEntry(reader, event.asStartElement(), targetEntitySet);
	      }
	    }
	    return rt;
	  }
	
	private OEntity entityFromAtomEntry(
		      EdmEntitySet entitySet,
		      DataServicesAtomEntry dsae,
		      FeedCustomizationMapping mapping) {

		    List<OProperty<?>> props = dsae.properties;
		    if (mapping != null) {
		      Enumerable<OProperty<?>> properties = Enumerable.create(dsae.properties);
		      if (mapping.titlePropName != null)
		        properties = properties.concat(OProperties.string(mapping.titlePropName, dsae.title));
		      if (mapping.summaryPropName != null)
		        properties = properties.concat(OProperties.string(mapping.summaryPropName, dsae.summary));

		      props = properties.toList();
		    }

		    EdmEntityType entityType = entitySet.getType();
		    if (dsae.categoryTerm != null) {
		      // The type of an entity set is polymorphic...
		      entityType = findEdmEntityType(dsae.categoryTerm);
		      if (entityType == null) {
		        throw new RuntimeException("Unable to resolve entity type " + dsae.categoryTerm);
		      }
		    }
		    // favor the key we just parsed.

        OEntityKey key = null;
        if (dsae.id != null) {
            Matcher actionMatcher = actionPath.matcher(dsae.id);
            if (dsae.id.endsWith(")")) {
                key = parseEntityKey(dsae.id);
            } else if (actionMatcher.find() && dsae.id.endsWith("/aapopulate")) {
                // TODO: Hard Coded "/aapopulate" needs to be revisited
                key = parseEntityKey(dsae.id.substring(0, actionMatcher.start()));
            } else {
                key = OEntityKey.infer(entitySet, props);
            }
        }

		    if (key == null) {
		      key = entityKey;
		    }

		    if (key == null)
		      return OEntities.createRequest(
		          entitySet,
		          props,
		          toOLinks(metadata, entitySet, dsae.atomLinks, mapping),
		          dsae.title,
		          dsae.categoryTerm);

		    return OEntities.create(
		        entitySet,
		        entityType,
		        key,
		        dsae.etag,
		        props,
		        toOLinks(metadata, entitySet, dsae.atomLinks, mapping),
		        dsae.title,
		        dsae.categoryTerm);
		  }

	private List<OLink> toOLinks(
		      final EdmDataServices metadata,
		      EdmEntitySet fromRoleEntitySet,
		      List<AtomLink> links,
		      final FeedCustomizationMapping mapping) {
		    List<OLink> rt = new ArrayList<OLink>(links.size());
		    for (final AtomLink link : links) {

		      if (link.relation.startsWith(XmlFormatWriter.related)) {
		        if (link.type.equals(XmlFormatWriter.atom_feed_content_type)) {

		          if (link.inlineContentExpected) {
		            List<OEntity> relatedEntities = null;

		            if (link.inlineFeed != null && link.inlineFeed.entries != null) {

		              // get the entity set belonging to the from role type
		              EdmNavigationProperty navProperty = fromRoleEntitySet != null
		                  ? fromRoleEntitySet.getType().findNavigationProperty(link.getNavProperty())
		                  : null;
		              final EdmEntitySet toRoleEntitySet = metadata != null && navProperty != null
		                  ? getEdmEntitySet(navProperty.getToRole().getType())
		                  : null;

		              // convert the atom feed entries to OEntitys
		              relatedEntities = Enumerable
		                  .create(link.inlineFeed.entries)
		                  .cast(DataServicesAtomEntry.class)
		                  .select(new Func1<DataServicesAtomEntry, OEntity>() {
		                    public OEntity apply(
		                        DataServicesAtomEntry input) {
		                      return entityFromAtomEntry(toRoleEntitySet, input, mapping);
		                    }
		                  }).toList();
		            } // else empty feed.
		            rt.add(OLinks.relatedEntitiesInline(
		                link.relation,
		                link.title,
		                link.href,
		                relatedEntities));
		          } else {
		            // no inlined entities
		            rt.add(OLinks.relatedEntities(link.relation, link.title, link.href));
		          }
		        } else if (link.type.equals(XmlFormatWriter.atom_entry_content_type))
		          if (link.inlineContentExpected) {
		            OEntity relatedEntity = null;
		            if (link.inlineEntry != null) {
		              EdmNavigationProperty navProperty = fromRoleEntitySet != null
		                  ? fromRoleEntitySet.getType().findNavigationProperty(link.getNavProperty())
		                  : null;
		              EdmEntitySet toRoleEntitySet = metadata != null && navProperty != null
		                  ? getEdmEntitySet(navProperty.getToRole().getType())
		                  : null;
		              relatedEntity = entityFromAtomEntry(toRoleEntitySet,
		                  (DataServicesAtomEntry) link.inlineEntry,
		                  mapping);
		            }
		            rt.add(OLinks.relatedEntityInline(link.relation,
		                link.title, link.href, relatedEntity));
		          } else {
		            // no inlined entity
		          rt.add(OLinks.relatedEntity(link.relation, link.title, link.href));
		        }
		      } else {
		          if (!StringUtils.isEmpty(link.relation) && !StringUtils.isEmpty(link.title) && !StringUtils.isEmpty(link.href)) {
		              rt.add(OLinks.relatedEntity(link.relation, link.title, link.href));
		          }
		      }
		    }
		    return rt;
		  }
	private EdmEntitySet getEntitySet() {
	    EdmEntitySet entitySet = null;
	    if (!metadata.getSchemas().isEmpty()) {
	      entitySet = getEdmEntitySet(entitySetName);
	      if (entitySet == null) {
	        // panic! could not determine the entity-set, is it a function?
	        EdmFunctionImport efi = metadata.findEdmFunctionImport(entitySetName);
	        if (efi != null)
	          entitySet = efi.getEntitySet();
	      }
	    }
	    if (entitySet == null)
	      throw new RuntimeException("Could not derive the entity-set " + entitySetName);
	    return entitySet;
	  }
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Iterable<OProperty<?>> parseProperties(XMLEventReader2 reader, StartElement2 propertiesElement, EdmDataServices metadata, EdmStructuralType structuralType) {
		List<OProperty<?>> rt = new ArrayList<OProperty<?>>();

		while (reader.hasNext()) {
			XMLEvent2 event = reader.nextEvent();

			if (event.isEndElement() && event.asEndElement().getName().equals(propertiesElement.getName())) {
				return rt;
			}

			if (event.isStartElement() && event.asStartElement().getName().getNamespaceUri().equals(NS_DATASERVICES)) {

				String name = event.asStartElement().getName().getLocalPart();
				Attribute2 typeAttribute = event.asStartElement().getAttributeByName(M_TYPE);
				Attribute2 nullAttribute = event.asStartElement().getAttributeByName(M_NULL);
				boolean isNull = nullAttribute != null && "true".equals(nullAttribute.getValue());

				OProperty<?> op = null;

				EdmType et = null;
				boolean isCollection = false;
				if (typeAttribute != null) {
					String type = typeAttribute.getValue();
					et = metadata.resolveType(type);
					// Following is to make our parse compatible with Odata4j as well as Odata .NET clients
					// Odata4j 		represent collection as Bag(...)
					// Odata.NET 	represent collection as Collection(...) 
					if( et == null && (type.startsWith( "Bag" ) ||  type.startsWith( "Collection" )) ) {
						isCollection = true;
						type = type.substring( type.indexOf("(" ) + 1, type.length() -1 );
						et = metadata.resolveType( type );
						if(et == null) {
							et = metadataOData4j.getEdmEntityTypeByTypeName(type);
						}
					} else if (et == null) {
						et = metadataOData4j.getEdmEntityTypeByTypeName(type);
					}
					if (et == null) {						
						// property arrived with an unknown type
						throw new RuntimeException("unknown property type: " + type);
					}
				} else if( structuralType instanceof EdmComplexType ) {
					//  Assume for now we're creating a bag
					EdmProperty property = structuralType.findProperty(name);
					if (property != null)
						et = property.getType();	// Simple Property Of Bag
					else 
						et = structuralType;		// This is for <d:element>
				} else {
					EdmProperty property = structuralType.findProperty(name);
					if (property != null) {
						et = property.getType();
					} else {
						property = structuralType.findProperty(structuralType.getName() + "_" + name);
						
						if(property == null) {
							et = EdmSimpleType.STRING; // we must support open types							
						} else {
							et = property.getType();
						}
					}
				}

				if( isCollection )
				{
					//
					//  So we're on the Segments element right now, which means the next event should be the first d:element
					//
					//  Thus loop on the d:element's and for each one recurse again to create the complex object
					//
					OCollection.Builder bagBuilder = OCollections.newBuilder( et );
					Enumerable<OProperty<?>> bagObjects = Enumerable.create(parseProperties(reader, event.asStartElement(), metadata, (EdmComplexType)et ) );

					for( OProperty<?> prop: bagObjects )
					{
						bagBuilder.add( (OObject) OComplexObjects.create( (EdmComplexType) prop.getType(),
								(List<OProperty<?>>) prop.getValue() ) );
					}
					OCollection<? extends OObject> bag = bagBuilder.build();
					op = OProperties.collection( name, new EdmCollectionType( EdmProperty.CollectionKind.List, (EdmComplexType) et ),
							bag);
				} else if (et != null && (!et.isSimple())) {
					EdmStructuralType est = (EdmStructuralType) et;
					op = OProperties.complex(name, (EdmComplexType) et, isNull ? null : Enumerable.create(parseProperties(reader, event.asStartElement(), metadata, est)).toList());
				} else {
					op = OProperties.parseSimple(name, (EdmSimpleType<?>) et, isNull ? null : reader.getElementText());
				}
				rt.add(op);
			}
		}
		throw new RuntimeException();
	}
	
	private XMLEventReader2 createXMLEventReader(Reader reader)
    {
	    XMLInputFactory factory = XMLInputFactory.newInstance();
	    
	    //XXE on its own can be prevented by setting IS_SUPPORT_EXTERNAL_ENTITIES to false but this will not
	    //prevent a billion laugh attack. Setting IS_REPLACING_ENTITY_REFERENCES to false does
	    //not force the implementation to not process internal entity references.
	    //Safest thing to do is to set SUPPORT_DTD to false to prevent both XXE and billion laugh.
	    factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
	    
        XMLEventReader2 xmlEventReader2 =  new StaxXMLInputFactory2Ext(factory).createXMLEventReader(reader);
        return xmlEventReader2;       
    }
}
