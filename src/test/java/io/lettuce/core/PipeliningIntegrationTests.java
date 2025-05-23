package io.lettuce.core;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.internal.Futures;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@SuppressWarnings("rawtypes")
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag(INTEGRATION_TEST)
class PipeliningIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    PipeliningIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        this.client = client;
        this.connection = connection;
    }

    @BeforeEach
    void setUp() {
        this.connection.async().flushall();
    }

    @Test
    void basic() {

        StatefulRedisConnection<String, String> connection = client.connect();
        connection.setAutoFlushCommands(false);

        int iterations = 100;
        List<RedisFuture<?>> futures = triggerSet(connection.async(), iterations);

        verifyNotExecuted(iterations);

        connection.flushCommands();

        Futures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]));

        verifyExecuted(iterations);

        connection.close();
    }

    void verifyExecuted(int iterations) {
        for (int i = 0; i < iterations; i++) {
            assertThat(connection.sync().get(key(i))).as("Key " + key(i) + " must be " + value(i)).isEqualTo(value(i));
        }
    }

    @Test
    void setAutoFlushTrueDoesNotFlush() {

        StatefulRedisConnection<String, String> connection = client.connect();
        connection.setAutoFlushCommands(false);

        int iterations = 100;
        List<RedisFuture<?>> futures = triggerSet(connection.async(), iterations);

        verifyNotExecuted(iterations);

        connection.setAutoFlushCommands(true);

        verifyNotExecuted(iterations);

        connection.flushCommands();
        boolean result = Futures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]));
        assertThat(result).isTrue();

        connection.close();
    }

    void verifyNotExecuted(int iterations) {
        for (int i = 0; i < iterations; i++) {
            assertThat(connection.sync().get(key(i))).as("Key " + key(i) + " must be null").isNull();
        }
    }

    List<RedisFuture<?>> triggerSet(RedisAsyncCommands<String, String> connection, int iterations) {
        List<RedisFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            futures.add(connection.set(key(i), value(i)));
        }
        return futures;
    }

    String value(int i) {
        return value + "-" + i;
    }

    String key(int i) {
        return key + "-" + i;
    }

}
