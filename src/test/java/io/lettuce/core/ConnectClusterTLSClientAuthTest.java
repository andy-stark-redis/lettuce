// EXAMPLE: connect_cluster_tls_client_auth
// STEP_START connect_cluster_tls_client_auth
// REMOVE_START
package io.lettuce.core;
// REMOVE_END
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.ClusterClientOptions;
import java.io.File;
// REMOVE_START
import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
// REMOVE_END

public class ConnectClusterTLSClientAuthTest {
    @Test
    public void connectClusterTLSClientAuth() {
        RedisURI uri = RedisURI.Builder
                .redis("<host>", <port>)
                .withAuthentication("default", "<password>")
                .withSsl(true)
                .withVerifyPeer(false)
                .build();
                
        RedisClusterClient client = RedisClusterClient.create(uri);

        SslOptions sslOptions = SslOptions.builder()
            .jdkSslProvider()
            .truststore(new File("<path_to_truststore.jks_file>"),
                "<password_for_truststore.jks_file>")
            .keystore(new File("<path_to_keystore.p12_file>"),
                "<password_for_keystore.p12_file>".toCharArray())
            .build();

        client.setOptions(ClusterClientOptions.builder()
                .sslOptions(sslOptions)
                .build());

        StatefulRedisClusterConnection<String, String> connection = client.connect();
        RedisClusterCommands<String, String> commands = connection.sync();
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
//STEP_END
