package org.infinispan.tutorial.simple.remote.query;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;


import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.infinispan.query.remote.client.ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME;

public class ProtobufQuery {

   public static final String PEOPLE_CACHE = "people";

   public static void main(String[] args) throws Exception {
      ConfigurationBuilder builder = new ConfigurationBuilder().uri("hotrod://admin:admin@127.0.0.1:11222");
      // Add the Protobuf serialization context in the client
      builder.addContextInitializer(new DomainSchemaImpl());

      // Use indexed cache
      URI indexedCacheURI = ProtobufQuery.class.getClassLoader().getResource("people.xml").toURI();
      builder.remoteCache(PEOPLE_CACHE).configurationURI(indexedCacheURI);

      // Connect to the server
      RemoteCacheManager client = new RemoteCacheManager(builder.build());

      // Create and add the Protobuf schema in the server
      addPersonSchema(client);

      // Get the people cache, create it if needed with the default configuration
      RemoteCache<String, Person> peopleCache = client.getCache(PEOPLE_CACHE);

      // Create the persons dataset to be stored in the cache
      Map<String, Person> people = new HashMap<>();
      people.put("1", new Person("Oihana", "Rossignol", 2016, "Paris"));
      people.put("2", new Person("Elaia", "Rossignol", 2018, "Paris"));
      people.put("3", new Person("Yago", "Steiner", 2013, "Saint-Mandé"));
      people.put("4", new Person("Alberto", "Steiner", 2016, "Paris"));

      // Put all the values in the cache
      peopleCache.putAll(people);

      // Get a query factory from the cache
      QueryFactory queryFactory = Search.getQueryFactory(peopleCache);

      // Create a query with lastName parameter
      Query query = queryFactory.create("FROM domain.Person p where p.lastName = :lastName");

      // Set the parameter value
      query.setParameter("lastName", "Rossignol");

      // Execute the query
      List<Person> items = query.execute().list();

      // Print the results
      System.out.println(items);

      // Stop the client and release all resources
      client.stop();
   }

   private static void addPersonSchema(RemoteCacheManager cacheManager) {
      // Retrieve metadata cache
      RemoteCache<String, String> metadataCache =
            cacheManager.getCache(PROTOBUF_METADATA_CACHE_NAME);

      // Define the new schema on the server too
      GeneratedSchema schema = new DomainSchemaImpl();
      metadataCache.put(schema.getProtoFileName(), schema.getProtoFile());
   }
}
