package org.infinispan.tutorial.simple.remote.query;

import static org.infinispan.query.remote.client.ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.marshall.UTF8StringMarshaller;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

public class JsonQuery {

   public static final String PEOPLE_CACHE = "people";

   public static void main(String[] args) throws Exception {
      ConfigurationBuilder builder = new ConfigurationBuilder().uri("hotrod://admin:admin@127.0.0.1:11222");

      // Use indexed cache
      URI indexedCacheURI = JsonQuery.class.getClassLoader().getResource("people.xml").toURI();
      builder.remoteCache(PEOPLE_CACHE).configurationURI(indexedCacheURI);

      // Connect to the server
      RemoteCacheManager client = new RemoteCacheManager(builder.build());

      // Create and add the Protobuf schema in the server
      addPersonSchema(client);

      // Get the people cache, create it if needed with the default configuration
      RemoteCache<String, String> peopleCache = client.getCache(PEOPLE_CACHE)
            .withDataFormat(DataFormat.builder()
                  .keyType(MediaType.TEXT_PLAIN)
                  .keyMarshaller(new UTF8StringMarshaller())
                  .valueType(MediaType.APPLICATION_JSON)
                  .valueMarshaller(new UTF8StringMarshaller())
                  .build()
            );

      // Create the persons dataset to be stored in the cache
      Map<String, String> people = new HashMap<>();
      people.put("1", """
            {
              "_type":"domain.Person",
              "firstName":"Oihana",
              "lastName":"Rossignol",
              "bornYear": 2016,
              "bornIn": "Paris"
            }
            """);
      people.put("2", """
            {
              "_type":"domain.Person",
              "firstName":"Elaia",
              "lastName":"Rossignol",
              "bornYear": 2018,
              "bornIn": "Paris"
            }
            """);
      people.put("3", """
            {
              "_type":"domain.Person",
              "firstName":"Yago",
              "lastName":"Steiner",
              "bornYear": 2013,
              "bornIn": "Saint-Mandé"
            }
            """);
      people.put("4", """
            {
              "_type":"domain.Person",
              "firstName":"Alberto",
              "lastName":"Steiner",
              "bornYear": 2016,
              "bornIn": "Paris"
            }
            """);

      // Put all the values in the cache
      peopleCache.putAll(people);

      // Get a query factory from the cache
      QueryFactory queryFactory = Search.getQueryFactory(peopleCache);

      // Create a query with lastName parameter
      Query query = queryFactory.create("FROM domain.Person p where p.lastName = :lastName");

      // Set the parameter value
      query.setParameter("lastName", "Rossignol");

      // Execute the query
      List<String> items = query.execute().list();

      // Print the results
      System.out.println(items);

      // Stop the client and release all resources
      client.stop();
   }

   private static void addPersonSchema(RemoteCacheManager cacheManager) throws URISyntaxException, IOException {
      File proto = new File(JsonQuery.class.getResource("/people.proto").toURI());
      // Retrieve metadata cache
      RemoteCache<String, String> metadataCache =
            cacheManager.getCache(PROTOBUF_METADATA_CACHE_NAME);
      metadataCache.put("DomainSchema.proto", Files.readString(proto.toPath()));
   }
}
