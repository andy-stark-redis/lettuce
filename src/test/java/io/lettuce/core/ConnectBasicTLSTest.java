// EXAMPLE: connect_basic_tls
// STEP_START connect_basic_tls
// REMOVE_START
package io.lettuce.core;
// REMOVE_END
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.File;
// REMOVE_START
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
// REMOVE_END

public class ConnectBasicTLSTest {
    @Test
    public void connectBasicTLS() {
        RedisURI uri = RedisURI.Builder
                .redis("<host>", <port>)
                .withAuthentication("default", "<password>")
                .withSsl(true)
                .build();
                
        RedisClient client = RedisClient.create(uri);

        SslOptions sslOptions = SslOptions.builder()
            .jdkSslProvider()
            .truststore(new File(
                "<path_to_truststore.jks_file>"),
                "<password_for_truststore.jks_file>")
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
// STEP_END
