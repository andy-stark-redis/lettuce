// EXAMPLE: connect_basic_tls_client_auth
// STEP_START connect_basic_tls_client_auth
// REMOVE_START
package io.lettuce.core;
// REMOVE_END
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
                .redis("<host>", <port>)
                .withAuthentication("default", "<password>")
                .withSsl(true)
                .build();
                
        RedisClient client = RedisClient.create(uri);

        SslOptions sslOptions = SslOptions.builder()
            .jdkSslProvider()
            .truststore(new File("<path_to_truststore.jks_file>"),
                "<password_for_truststore.jks_file>")
            .keystore(new File("<path_to_keystore.p12_file>"),
                "<password_for_keystore.p12_file>".toCharArray())
            .build();

        client.setOptions(ClientOptions.builder()
                .sslOptions(sslOptions)
                .build());

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> commands = connection.sync();
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
