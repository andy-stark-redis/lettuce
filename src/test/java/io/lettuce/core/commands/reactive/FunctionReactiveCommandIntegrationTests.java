package io.lettuce.core.commands.reactive;

import javax.inject.Inject;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.FunctionCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Tag;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class FunctionReactiveCommandIntegrationTests extends FunctionCommandIntegrationTests {

    @Inject
    FunctionReactiveCommandIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        super(client, ReactiveSyncInvocationHandler.sync(connection));
    }

}
