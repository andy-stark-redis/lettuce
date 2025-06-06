package io.lettuce.core.support;

import static io.lettuce.core.support.ConnectionWrapping.HasTargetConnection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.commons.pool2.impl.SoftReferenceObjectPool;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.support.ConnectionWrapping.Origin;

/**
 * Connection pool support for {@link GenericObjectPool} and {@link SoftReferenceObjectPool}. Connection pool creation requires
 * a {@link Supplier} that creates Redis connections. The pool can allocate either wrapped or direct connections.
 * <ul>
 * <li>Wrapped instances will return the connection back to the pool when called {@link StatefulConnection#close()}.</li>
 * <li>Regular connections need to be returned to the pool with {@link GenericObjectPool#returnObject(Object)}</li>
 * </ul>
 * <p>
 * Lettuce connections are designed to be thread-safe so one connection can be shared amongst multiple threads and Lettuce
 * connections {@link ClientOptions#isAutoReconnect() auto-reconnect} by default. Connection pooling with Lettuce can be
 * required when you're invoking Redis operations in multiple threads and you use
 * <ul>
 * <li>blocking commands such as {@code BLPOP}.</li>
 * <li>transactions {@code BLPOP}.</li>
 * <li>{@link StatefulConnection#setAutoFlushCommands(boolean) command batching}.</li>
 * </ul>
 *
 * Transactions and command batching affect connection state. Blocking commands won't propagate queued commands to Redis until
 * the blocking command is completed.
 *
 * <h3>Example</h3>
 *
 * <pre class="code">
 * // application initialization
 * RedisClusterClient clusterClient = RedisClusterClient.create(RedisURI.create(host, port));
 * GenericObjectPool&lt;StatefulRedisClusterConnection&lt;String, String&gt;&gt; pool = ConnectionPoolSupport
 *         .createGenericObjectPool(() -&gt; clusterClient.connect(), new GenericObjectPoolConfig());
 *
 * // executing work
 * try (StatefulRedisClusterConnection&lt;String, String&gt; connection = pool.borrowObject()) {
 *     // perform some work
 * }
 *
 * // terminating
 * pool.close();
 * clusterClient.shutdown();
 * </pre>
 *
 * @author Mark Paluch
 * @author dae won
 * @since 4.3
 */
public abstract class ConnectionPoolSupport {

    private ConnectionPoolSupport() {
    }

    /**
     * Creates a new {@link GenericObjectPool} using the {@link Supplier}. Allocated instances are wrapped and must not be
     * returned with {@link ObjectPool#returnObject(Object)}. By default, connections are validated by checking their
     * {@link StatefulConnection#isOpen()} method.
     *
     * @param connectionSupplier must not be {@code null}.
     * @param config must not be {@code null}.
     * @param <T> connection type.
     * @return the connection pool.
     */
    public static <T extends StatefulConnection<?, ?>> GenericObjectPool<T> createGenericObjectPool(
            Supplier<T> connectionSupplier, GenericObjectPoolConfig<T> config) {
        return createGenericObjectPool(connectionSupplier, config, true, (c) -> c.isOpen());
    }

    /**
     * Creates a new {@link GenericObjectPool} using the {@link Supplier}. Allocated instances are wrapped and must not be
     * returned with {@link ObjectPool#returnObject(Object)}.
     *
     * @param connectionSupplier must not be {@code null}.
     * @param config must not be {@code null}.
     * @param validationPredicate a {@link Predicate} to help validate connections
     * @param <T> connection type.
     * @return the connection pool.
     */
    public static <T extends StatefulConnection<?, ?>> GenericObjectPool<T> createGenericObjectPool(
            Supplier<T> connectionSupplier, GenericObjectPoolConfig<T> config, Predicate<T> validationPredicate) {
        return createGenericObjectPool(connectionSupplier, config, true, validationPredicate);
    }

    /**
     * Creates a new {@link GenericObjectPool} using the {@link Supplier}. By default, connections are validated by checking
     * their {@link StatefulConnection#isOpen()} method.
     *
     * @param connectionSupplier must not be {@code null}.
     * @param config must not be {@code null}.
     * @param wrapConnections {@code false} to return direct connections that need to be returned to the pool using
     *        {@link ObjectPool#returnObject(Object)}. {@code true} to return wrapped connections that are returned to the pool
     *        when invoking {@link StatefulConnection#close()}.
     * @param <T> connection type.
     * @return the connection pool.
     */
    @SuppressWarnings("unchecked")
    public static <T extends StatefulConnection<?, ?>> GenericObjectPool<T> createGenericObjectPool(
            Supplier<T> connectionSupplier, GenericObjectPoolConfig<T> config, boolean wrapConnections) {
        return createGenericObjectPool(connectionSupplier, config, wrapConnections, (c) -> c.isOpen());
    }

    /**
     * Creates a new {@link GenericObjectPool} using the {@link Supplier}.
     *
     * @param connectionSupplier must not be {@code null}.
     * @param config must not be {@code null}.
     * @param wrapConnections {@code false} to return direct connections that need to be returned to the pool using
     *        {@link ObjectPool#returnObject(Object)}. {@code true} to return wrapped connections that are returned to the pool
     *        when invoking {@link StatefulConnection#close()}.
     * @param validationPredicate a {@link Predicate} to help validate connections
     * @param <T> connection type.
     * @return the connection pool.
     */
    @SuppressWarnings("unchecked")
    public static <T extends StatefulConnection<?, ?>> GenericObjectPool<T> createGenericObjectPool(
            Supplier<T> connectionSupplier, GenericObjectPoolConfig<T> config, boolean wrapConnections,
            Predicate<T> validationPredicate) {

        LettuceAssert.notNull(connectionSupplier, "Connection supplier must not be null");
        LettuceAssert.notNull(config, "GenericObjectPoolConfig must not be null");
        LettuceAssert.notNull(validationPredicate, "Connection validator must not be null");

        AtomicReference<Origin<T>> poolRef = new AtomicReference<>();

        GenericObjectPool<T> pool = new GenericObjectPool<T>(
                new RedisPooledObjectFactory<>(connectionSupplier, validationPredicate), config) {

            @Override
            public T borrowObject() throws Exception {
                return wrapConnections ? ConnectionWrapping.wrapConnection(super.borrowObject(), poolRef.get())
                        : super.borrowObject();
            }

            @Override
            public void returnObject(T obj) {

                if (wrapConnections && obj instanceof HasTargetConnection) {
                    super.returnObject((T) ((HasTargetConnection) obj).getTargetConnection());
                    return;
                }
                super.returnObject(obj);
            }

        };

        poolRef.set(new ObjectPoolWrapper<>(pool));

        return pool;
    }

    /**
     * Creates a new {@link SoftReferenceObjectPool} using the {@link Supplier}. Allocated instances are wrapped and must not be
     * returned with {@link ObjectPool#returnObject(Object)}.
     *
     * @param connectionSupplier must not be {@code null}.
     * @param <T> connection type.
     * @return the connection pool.
     */
    public static <T extends StatefulConnection<?, ?>> SoftReferenceObjectPool<T> createSoftReferenceObjectPool(
            Supplier<T> connectionSupplier) {
        return createSoftReferenceObjectPool(connectionSupplier, true);
    }

    /**
     * Creates a new {@link SoftReferenceObjectPool} using the {@link Supplier}.
     *
     * @param connectionSupplier must not be {@code null}.
     * @param wrapConnections {@code false} to return direct connections that need to be returned to the pool using
     *        {@link ObjectPool#returnObject(Object)}. {@code true} to return wrapped connections that are returned to the pool
     *        when invoking {@link StatefulConnection#close()}.
     * @param <T> connection type.
     * @return the connection pool.
     */
    @SuppressWarnings("unchecked")
    public static <T extends StatefulConnection<?, ?>> SoftReferenceObjectPool<T> createSoftReferenceObjectPool(
            Supplier<T> connectionSupplier, boolean wrapConnections) {
        return createSoftReferenceObjectPool(connectionSupplier, wrapConnections, (c) -> c.isOpen());
    }

    /**
     * Creates a new {@link SoftReferenceObjectPool} using the {@link Supplier}.
     *
     * @param connectionSupplier must not be {@code null}.
     * @param wrapConnections {@code false} to return direct connections that need to be returned to the pool using
     *        {@link ObjectPool#returnObject(Object)}. {@code true} to return wrapped connections that are returned to the pool
     *        when invoking {@link StatefulConnection#close()}.
     * @param validationPredicate a {@link Predicate} to help validate connections
     * @param <T> connection type.
     * @return the connection pool.
     */
    @SuppressWarnings("unchecked")
    public static <T extends StatefulConnection<?, ?>> SoftReferenceObjectPool<T> createSoftReferenceObjectPool(
            Supplier<T> connectionSupplier, boolean wrapConnections, Predicate<T> validationPredicate) {

        LettuceAssert.notNull(connectionSupplier, "Connection supplier must not be null");

        AtomicReference<Origin<T>> poolRef = new AtomicReference<>();

        SoftReferenceObjectPool<T> pool = new SoftReferenceObjectPool<T>(
                new RedisPooledObjectFactory<>(connectionSupplier, validationPredicate)) {

            private final Lock lock = new ReentrantLock();

            @Override
            public T borrowObject() throws Exception {
                lock.lock();
                try {
                    return wrapConnections ? ConnectionWrapping.wrapConnection(super.borrowObject(), poolRef.get())
                            : super.borrowObject();
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public void returnObject(T obj) throws Exception {
                lock.lock();
                try {
                    if (wrapConnections && obj instanceof HasTargetConnection) {
                        super.returnObject((T) ((HasTargetConnection) obj).getTargetConnection());
                        return;
                    }
                    super.returnObject(obj);
                } finally {
                    lock.unlock();
                }
            }

        };
        poolRef.set(new ObjectPoolWrapper<>(pool));

        return pool;
    }

    /**
     * @author Mark Paluch
     * @since 4.3
     */
    private static class RedisPooledObjectFactory<T extends StatefulConnection<?, ?>> extends BasePooledObjectFactory<T> {

        private final Supplier<T> connectionSupplier;

        private final Predicate<T> validationPredicate;

        RedisPooledObjectFactory(Supplier<T> connectionSupplier, Predicate<T> validationPredicate) {
            this.connectionSupplier = connectionSupplier;
            this.validationPredicate = validationPredicate;
        }

        @Override
        public T create() throws Exception {
            return connectionSupplier.get();
        }

        @Override
        public void destroyObject(PooledObject<T> p) throws Exception {
            p.getObject().close();
        }

        @Override
        public PooledObject<T> wrap(T obj) {
            return new DefaultPooledObject<>(obj);
        }

        @Override
        public boolean validateObject(PooledObject<T> p) {
            return this.validationPredicate.test(p.getObject());
        }

    }

    private static class ObjectPoolWrapper<T> implements Origin<T> {

        private static final CompletableFuture<Void> COMPLETED = CompletableFuture.completedFuture(null);

        private final ObjectPool<T> pool;

        ObjectPoolWrapper(ObjectPool<T> pool) {
            this.pool = pool;
        }

        @Override
        public void returnObject(T o) throws Exception {
            pool.returnObject(o);
        }

        @Override
        public CompletableFuture<Void> returnObjectAsync(T o) throws Exception {
            pool.returnObject(o);
            return COMPLETED;
        }

    }

}
