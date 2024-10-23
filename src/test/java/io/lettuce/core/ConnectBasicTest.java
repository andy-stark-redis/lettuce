package io.lettuce.core;
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
                .redis("redis-14669.c338.eu-west-2-1.ec2.redns.redis-cloud.com", 14669)
                .withAuthentication("default", "jj7hRGi1K22vop5IDFvAf8oyeeF98s4h")
                .build();
        RedisClient client = RedisClient.create(uri);
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
