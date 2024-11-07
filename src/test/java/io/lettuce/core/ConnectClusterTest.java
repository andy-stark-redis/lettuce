// EXAMPLE: connect_cluster
// STEP_START connect_cluster
// REMOVE_START
package io.lettuce.core;
// REMOVE_END
import io.lettuce.core.*;
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
                .redis("<host>", <port>)
                .withAuthentication("default", "<password>")
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
// STEP_END
