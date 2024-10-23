package io.lettuce.core;

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.RedisClusterClient;

// REMOVE_START
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
// REMOVE_END

public class ConnectClusterTest {
    @Test
    public void connectCluster() {
        RedisURI uri = RedisURI.Builder
                .redis("redis-13891.c34425.eu-west-2-mz.ec2.cloud.rlrcp.com", 13891)
                .withAuthentication("default", "wtpet4pI5EgyJHyldPwR7xM7GaZB0EcG")
                .build();
        RedisClusterClient client = RedisClusterClient.create(uri);
        StatefulRedisClusterConnection<String, String> connection = client.connect();
        RedisAdvancedClusterCommands<String, String> commands = connection.sync();
        // REMOVE_START
        commands.del("foo");
        // REMOVE_END
        
        commands.set("foo", "bar");
        String result = commands.get("foo");
        System.out.println(result); // >>> bar

        connection.close();

        client.shutdown();
        // REMOVE_START
        assertThat(result).isEqualTo("bar");
        // REMOVE_END
    }
}
