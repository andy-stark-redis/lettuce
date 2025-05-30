package io.lettuce.core;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.Transports;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;
import io.netty.util.internal.SystemPropertyUtil;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class UnixDomainSocketIntegrationTests {

    private static final String MASTER_ID = "mymaster";

    private static RedisClient sentinelClient;

    private Logger log = LogManager.getLogger(getClass());

    private String key = "key";

    private String value = "value";

    @BeforeAll
    static void setupClient() {
        sentinelClient = getRedisSentinelClient();
    }

    @AfterAll
    static void shutdownClient() {
        FastShutdown.shutdown(sentinelClient);
    }

    @Test
    void standalone_RedisClientWithSocket() throws Exception {

        assumeTestSupported();

        RedisURI redisURI = getSocketRedisUri();

        RedisClient redisClient = RedisClient.create(TestClientResources.get(), redisURI);

        StatefulRedisConnection<String, String> connection = redisClient.connect();
        someRedisAction(connection.sync());
        connection.close();

        FastShutdown.shutdown(redisClient);
    }

    @Test
    void standalone_ConnectToSocket() throws Exception {

        assumeTestSupported();

        RedisURI redisURI = getSocketRedisUri();

        RedisClient redisClient = RedisClient.create(TestClientResources.get());

        StatefulRedisConnection<String, String> connection = redisClient.connect(redisURI);

        someRedisAction(connection.sync());
        connection.close();

        FastShutdown.shutdown(redisClient);
    }

    @Test
    void sentinel_RedisClientWithSocket() throws Exception {

        assumeTestSupported();

        RedisURI uri = new RedisURI();
        uri.getSentinels().add(getSentinelSocketRedisUri());
        uri.setSentinelMasterId("mymaster");

        RedisClient redisClient = RedisClient.create(TestClientResources.get(), uri);

        StatefulRedisConnection<String, String> connection = redisClient.connect();

        someRedisAction(connection.sync());

        connection.close();

        StatefulRedisSentinelConnection<String, String> sentinelConnection = redisClient.connectSentinel();

        assertThat(sentinelConnection.sync().ping()).isEqualTo("PONG");
        sentinelConnection.close();

        FastShutdown.shutdown(redisClient);
    }

    @Test
    void sentinel_ConnectToSocket() throws Exception {

        assumeTestSupported();

        RedisURI uri = new RedisURI();
        uri.getSentinels().add(getSentinelSocketRedisUri());
        uri.setSentinelMasterId("mymaster");

        RedisClient redisClient = RedisClient.create(TestClientResources.get());

        StatefulRedisConnection<String, String> connection = redisClient.connect(uri);

        someRedisAction(connection.sync());

        connection.close();

        StatefulRedisSentinelConnection<String, String> sentinelConnection = redisClient.connectSentinel(uri);

        assertThat(sentinelConnection.sync().ping()).isEqualTo("PONG");
        sentinelConnection.close();

        FastShutdown.shutdown(redisClient);
    }

    @Test
    void sentinel_socket_and_inet() throws Exception {

        assumeTestSupported();

        RedisURI uri = new RedisURI();
        uri.getSentinels().add(getSentinelSocketRedisUri());
        uri.getSentinels().add(RedisURI.create(RedisURI.URI_SCHEME_REDIS + "://" + TestSettings.host() + ":26379"));
        uri.setSentinelMasterId(MASTER_ID);

        RedisClient redisClient = RedisClient.create(TestClientResources.get(), uri);

        StatefulRedisSentinelConnection<String, String> sentinelConnection = redisClient
                .connectSentinel(getSentinelSocketRedisUri());
        log.info("Masters: " + sentinelConnection.sync().masters());

        try {
            redisClient.connect();
            fail("Missing validation exception");
        } catch (RedisConnectionException e) {
            assertThat(e).hasMessageContaining("You cannot mix unix domain socket and IP socket URI's");
        } finally {
            FastShutdown.shutdown(redisClient);
        }

    }

    private void someRedisAction(RedisCommands<String, String> connection) {
        connection.set(key, value);
        String result = connection.get(key);

        assertThat(result).isEqualTo(value);
    }

    private static RedisClient getRedisSentinelClient() {
        return RedisClient.create(TestClientResources.get(), RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).build());
    }

    private void assumeTestSupported() {
        String osName = SystemPropertyUtil.get("os.name").toLowerCase(Locale.UK).trim();
        assumeTrue(Transports.NativeTransports.isDomainSocketSupported(),
                "Only supported on Linux/OSX, your os is " + osName + " with epoll/kqueue support.");
    }

    private static RedisURI getSocketRedisUri() throws IOException {
        File file = new File(TestSettings.socket()).getCanonicalFile();
        return RedisURI.create(RedisURI.URI_SCHEME_REDIS_SOCKET + "://" + file.getCanonicalPath());
    }

    private static RedisURI getSentinelSocketRedisUri() throws IOException {
        File file = new File(TestSettings.sentinelSocket()).getCanonicalFile();
        return RedisURI.create(RedisURI.URI_SCHEME_REDIS_SOCKET + "://" + file.getCanonicalPath());
    }

}
