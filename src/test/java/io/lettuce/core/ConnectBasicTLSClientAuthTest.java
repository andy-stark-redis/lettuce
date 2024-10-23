package io.lettuce.core;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

// REMOVE_START
import static org.assertj.core.api.Assertions.*;

import java.io.File;

import org.junit.Test;
// REMOVE_END

public class ConnectBasicTLSClientAuthTest {
    @Test
    public void connectBasicTLSClientAuth() {
        RedisURI uri = RedisURI.Builder
                .redis("redis-13891.c34425.eu-west-2-mz.ec2.cloud.rlrcp.com", 13891)
                .withAuthentication("default", "wtpet4pI5EgyJHyldPwR7xM7GaZB0EcG")
                .withSsl(true)
                .build();
                
        RedisClient client = RedisClient.create(uri);

        SslOptions sslOptions = SslOptions.builder()
            .jdkSslProvider()
            .truststore(new File("/Users/andrew.stark/Documents/Repos/forks/lettuce/src/test/java/io/lettuce/core/truststore.jks"), "secret")
            .keystore(new File("/Users/andrew.stark/Documents/Repos/forks/lettuce/src/test/java/io/lettuce/core/redis-user-keystore.p12"), "secret".toCharArray())
            .build();

        client.setOptions(ClientOptions.builder()
                .sslOptions(sslOptions)
                .build());

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> commands = connection.sync();

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
