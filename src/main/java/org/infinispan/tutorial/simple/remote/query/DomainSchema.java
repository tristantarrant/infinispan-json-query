package org.infinispan.tutorial.simple.remote.query;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(schemaPackageName = "domain", includeClasses = Person.class)
public interface DomainSchema extends GeneratedSchema {
}
