// EXAMPLE: connect_basic
// STEP_START connect_basic
// REMOVE_START
package io.lettuce.core;
// REMOVE_END
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
// REMOVE_START
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
// REMOVE_END

public class ConnectBasicTest {
    @Test
    public void connectBasic() {
        RedisURI uri = RedisURI.Builder
                .redis("<host>", <port>)
                .withAuthentication("default", "<password>")
                .build();
        RedisClient client = RedisClient.create(uri);
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
