package io.lettuce.core;
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
                .redis("redis-15313.c34461.eu-west-2-mz.ec2.cloud.rlrcp.com", 15313)
                .withAuthentication("default", "MrlnkBuSZqO0s0vicIkLnqJXetbSTCan")
                .withSsl(true)
                .withVerifyPeer(false)
                .build();
                
        RedisClusterClient client = RedisClusterClient.create(uri);

        SslOptions sslOptions = SslOptions.builder()
            .jdkSslProvider()
            .truststore(new File("/Users/andrew.stark/Documents/Repos/forks/lettuce/src/test/java/io/lettuce/core/truststore.jks"), "secret")
            .keystore(new File("/Users/andrew.stark/Documents/Repos/forks/lettuce/src/test/java/io/lettuce/core/keystore.p12"), "secret".toCharArray())
            .build();

        client.setOptions(ClusterClientOptions.builder()
                .sslOptions(sslOptions)
                .build());

        StatefulRedisClusterConnection<String, String> connection = client.connect();
        RedisClusterCommands<String, String> commands = connection.sync();

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
