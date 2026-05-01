package com.hivemind.meeting.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableCassandraRepositories(basePackages = "com.hivemind.meeting.repository")
public class CassandraConfig extends AbstractCassandraConfiguration
{
    @Value("${spring.cassandra.keyspace-name:meeting_keyspace}")
    private String keyspaceName;

    @Value("${spring.cassandra.contact-points:localhost}")
    private String contactPoints;

    @Value("${spring.cassandra.port:9042}")
    private int port;

    @Value("${spring.cassandra.local-datacenter:datacenter1}")
    private String localDatacenter;

    @Override
    protected String getKeyspaceName() { return keyspaceName; }

    @Override
    protected String getContactPoints() { return contactPoints; }

    @Override
    protected int getPort() { return port; }

    @Override
    protected String getLocalDataCenter() { return localDatacenter; }

    @Override
    public SchemaAction getSchemaAction() { return SchemaAction.CREATE_IF_NOT_EXISTS; }

    @Override
    public String[] getEntityBasePackages() { return new String[]{"com.hivemind.meeting.entity"}; }

    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations()
    {
        return Collections.singletonList(
            CreateKeyspaceSpecification.createKeyspace(keyspaceName)
                .ifNotExists()
                .with(KeyspaceOption.DURABLE_WRITES, true)
                .withSimpleReplication(1)
        );
    }
}
