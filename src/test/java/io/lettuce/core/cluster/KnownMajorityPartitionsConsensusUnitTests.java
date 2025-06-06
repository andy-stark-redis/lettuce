package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.cluster.PartitionsConsensusTestSupport.createMap;
import static io.lettuce.core.cluster.PartitionsConsensusTestSupport.createNode;
import static io.lettuce.core.cluster.PartitionsConsensusTestSupport.createPartitions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class KnownMajorityPartitionsConsensusUnitTests {

    private RedisClusterNode node1 = createNode(1);

    private RedisClusterNode node2 = createNode(2);

    private RedisClusterNode node3 = createNode(3);

    private RedisClusterNode node4 = createNode(4);

    private RedisClusterNode node5 = createNode(5);

    @Test
    void sameSharedViewShouldDecideForKnownMajority() {

        Partitions current = createPartitions(node1, node2, node3, node4, node5);

        Partitions partitions1 = createPartitions(node1, node2, node3, node4, node5);
        Partitions partitions2 = createPartitions(node1, node2, node3, node4, node5);
        Partitions partitions3 = createPartitions(node1, node2, node3, node4, node5);

        Map<RedisURI, Partitions> map = createMap(partitions1, partitions2, partitions3);

        Partitions result = PartitionsConsensus.KNOWN_MAJORITY.getPartitions(current, map);

        assertThat(Arrays.asList(partitions1, partitions2, partitions3)).contains(result);
    }

    @Test
    void addedNodeViewShouldDecideForKnownMajority() {

        Partitions current = createPartitions(node1, node2, node3, node4);

        Partitions partitions1 = createPartitions(node1, node2, node3, node4, node5);
        Partitions partitions2 = createPartitions(node1, node2, node3, node4, node5);
        Partitions partitions3 = createPartitions(node1, node2, node3, node4, node5);

        Map<RedisURI, Partitions> map = createMap(partitions1, partitions2, partitions3);

        Partitions result = PartitionsConsensus.KNOWN_MAJORITY.getPartitions(current, map);

        assertThat(Arrays.asList(partitions1, partitions2, partitions3)).contains(result);
    }

    @Test
    void removedNodeViewShouldDecideForKnownMajority() {

        Partitions current = createPartitions(node1, node2, node3, node4, node5);

        Partitions partitions1 = createPartitions(node1, node2, node3, node4);
        Partitions partitions2 = createPartitions(node1, node2, node3, node4);
        Partitions partitions3 = createPartitions(node1, node2, node3, node4);

        Map<RedisURI, Partitions> map = createMap(partitions1, partitions2, partitions3);

        Partitions result = PartitionsConsensus.KNOWN_MAJORITY.getPartitions(current, map);

        assertThat(Arrays.asList(partitions1, partitions2, partitions3)).contains(result);
    }

    @Test
    void mixedViewShouldDecideForKnownMajority() {

        Partitions current = createPartitions(node1, node2, node3, node4, node5);

        Partitions partitions1 = createPartitions(node1, node2, node3, node4);
        Partitions partitions2 = createPartitions(node1, node2, node3, node5);
        Partitions partitions3 = createPartitions(node1, node2, node4, node5);

        Map<RedisURI, Partitions> map = createMap(partitions1, partitions2, partitions3);

        Partitions result = PartitionsConsensus.KNOWN_MAJORITY.getPartitions(current, map);

        assertThat(Arrays.asList(partitions1, partitions2, partitions3)).contains(result);
    }

    @Test
    void clusterSplitViewShouldDecideForKnownMajority() {

        Partitions current = createPartitions(node1, node2, node3, node4, node5);

        Partitions partitions1 = createPartitions(node1, node2);
        Partitions partitions2 = createPartitions(node1, node2);
        Partitions partitions3 = createPartitions(node1, node2, node3, node4, node5);

        Map<RedisURI, Partitions> map = createMap(partitions1, partitions2, partitions3);

        Partitions result = PartitionsConsensus.KNOWN_MAJORITY.getPartitions(current, map);

        assertThat(result).isEqualTo(partitions3).isNotEqualTo(partitions1);
    }

    @Test
    void strangeClusterSplitViewShouldDecideForKnownMajority() {

        Partitions current = createPartitions(node1, node2, node3, node4, node5);

        Partitions partitions1 = createPartitions(node1);
        Partitions partitions2 = createPartitions(node2);
        Partitions partitions3 = createPartitions(node3);

        Map<RedisURI, Partitions> map = createMap(partitions1, partitions2, partitions3);

        Partitions result = PartitionsConsensus.KNOWN_MAJORITY.getPartitions(current, map);

        assertThat(Arrays.asList(partitions1, partitions2, partitions3)).contains(result);
    }

}
