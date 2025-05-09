package test.algorithms.pm;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import algorithms.AlgorithmConfig;
import algorithms.AlgorithmConfig.Objective;
import algorithms.pm.VnePmMdvneAlgorithm;
import facade.ModelFacade;
import facade.config.ModelFacadeConfig;
import generators.OneTierNetworkGenerator;
import generators.config.OneTierConfig;
import model.SubstrateLink;
import model.SubstrateNetwork;
import model.SubstratePath;
import model.VirtualNetwork;
import test.algorithms.generic.AAlgorithmMultipleVnsTest;

/**
 * Test class for the VNE pattern matching algorithm implementation for
 * minimizing the total path cost metric.
 *
 * @author Maximilian Kratz {@literal <maximilian.kratz@es.tu-darmstadt.de>}
 */
public class VnePmMdvneAlgorithmTotalPathCostTest extends AAlgorithmMultipleVnsTest {

	@Override
	public void initAlgo(final SubstrateNetwork sNet, final Set<VirtualNetwork> vNets) {
		AlgorithmConfig.obj = Objective.TOTAL_PATH_COST;
		algo = new VnePmMdvneAlgorithm();
		algo.prepare(sNet, vNets);
	}

	@AfterEach
	public void resetAlgo() {
		if (algo != null) {
			((VnePmMdvneAlgorithm) algo).dispose();
		}
	}

	@Test
	public void testMultipleNetworksAfterEachOther() {
		twoTierSetupFourServers("sub", 3);
		facade.createAllPathsForNetwork("sub");
		final SubstrateNetwork sNet = (SubstrateNetwork) facade.getNetworkById("sub");

		for (int i = 0; i < 2; i++) {
			final String currVnetId = "virt" + i;
			facade.addNetworkToRoot(currVnetId, true);
			oneTierSetupTwoServers(currVnetId, 1);

			final VirtualNetwork currVnet = (VirtualNetwork) facade.getNetworkById(currVnetId);
			initAlgo(sNet, Set.of(currVnet));
			assertTrue(algo.execute());

			checkAllElementsEmbeddedOnSubstrateNetwork(sNet, Set.of(currVnet));
		}
	}

	@Test
	public void testMaxSizeVnet() {
		oneTierSetupTwoServers("sub", 100);
		facade.createAllPathsForNetwork("sub");
		final SubstrateNetwork sNet = (SubstrateNetwork) facade.getNetworkById("sub");
		oneTierSetupTwoServers("virt", 100);
		final VirtualNetwork currVnet = (VirtualNetwork) facade.getNetworkById("virt");

		initAlgo(sNet, Set.of(currVnet));
		assertTrue(algo.execute());

		checkAllElementsEmbeddedOnSubstrateNetwork(sNet, Set.of(currVnet));
	}

	@Test
	public void testPathLinkBandwidthDecrement() {
		oneTierSetupTwoServers("sub", 100);
		facade.createAllPathsForNetwork("sub");
		final SubstrateNetwork sNet = (SubstrateNetwork) facade.getNetworkById("sub");
		oneTierSetupTwoServers("virt", 100);
		final VirtualNetwork currVnet = (VirtualNetwork) facade.getNetworkById("virt");

		initAlgo(sNet, Set.of(currVnet));
		assertTrue(algo.execute());

		sNet.getPaths().forEach(p -> {
			if (p.getHops() == 1) {
				// Path itself
				final SubstratePath sp = p;
				assertTrue(sp.getBandwidth() != sp.getResidualBandwidth());

				// Links
				sp.getLinks().forEach(l -> {
					final SubstrateLink sl = l;
					assertTrue(sl.getBandwidth() != sl.getResidualBandwidth());
				});
			}
		});
	}

	/*
	 * Negative tests
	 */

	@Test
	public void testNoPathsInNetwork() {
		oneTierSetupTwoServers("sub", 1);
		// Removed path generation on purpose
		final SubstrateNetwork sNet = (SubstrateNetwork) facade.getNetworkById("sub");
		oneTierSetupTwoServers("virt", 1);
		final VirtualNetwork currVnet = (VirtualNetwork) facade.getNetworkById("virt");

		assertThrows(UnsupportedOperationException.class, () -> {
			initAlgo(sNet, Set.of(currVnet));
		});
	}

	@Test
	public void testMinimumPathLength() {
		oneTierSetupTwoServers("sub", 1);
		ModelFacadeConfig.MIN_PATH_LENGTH = 2;
		facade.createAllPathsForNetwork("sub");
		final SubstrateNetwork sNet = (SubstrateNetwork) facade.getNetworkById("sub");
		oneTierSetupTwoServers("virt", 1);
		final VirtualNetwork currVnet = (VirtualNetwork) facade.getNetworkById("virt");

		assertThrows(UnsupportedOperationException.class, () -> {
			initAlgo(sNet, Set.of(currVnet));
		});
	}

	@Test
	public void testNoEmbeddingIfFullOneByOne() {
		oneTierSetupTwoServers("virt", 2);
		twoTierSetupFourServers("sub", 4);
		facade.createAllPathsForNetwork("sub");

		final SubstrateNetwork sNet = (SubstrateNetwork) facade.getNetworkById("sub");

		// First three must succeed
		initAlgo(sNet, Set.of((VirtualNetwork) facade.getNetworkById("virt")));
		assertTrue(algo.execute());

		facade.addNetworkToRoot("virt2", true);
		oneTierSetupTwoServers("virt2", 2);

		initAlgo(sNet, Set.of((VirtualNetwork) facade.getNetworkById("virt2")));
		assertTrue(algo.execute());

		facade.addNetworkToRoot("virt3", true);
		oneTierSetupTwoServers("virt3", 2);

		initAlgo(sNet, Set.of((VirtualNetwork) facade.getNetworkById("virt3")));
		assertTrue(algo.execute());

		facade.addNetworkToRoot("virt4", true);
		oneTierSetupThreeServers("virt4", 2);

		// Last one must not succeed
		initAlgo(sNet, Set.of((VirtualNetwork) facade.getNetworkById("virt4")));
		assertFalse(algo.execute());

		checkAllElementsEmbeddedOnSubstrateNetwork(sNet, Set.of((VirtualNetwork) facade.getNetworkById("virt"),
				(VirtualNetwork) facade.getNetworkById("virt2"), (VirtualNetwork) facade.getNetworkById("virt3")));
		final VirtualNetwork vNet4 = (VirtualNetwork) facade.getNetworkById("virt4");
		assertNull(vNet4.getHost());
	}

	@Test
	public void testPartialEmbeddingIfFullAllAtOnceStaticRejCost() {
		AlgorithmConfig.netRejCostDynamic = false;
		final SubstrateNetwork sNet = (SubstrateNetwork) facade.getNetworkById("sub");
		final List<VirtualNetwork> vNets = rejectionSetup();

		initAlgo(sNet, new HashSet<>(vNets));
		assertFalse(algo.execute());

		// The first three networks must be embedded
		assertNotNull(vNets.get(0).getHost());
		assertNotNull(vNets.get(1).getHost());
		assertNotNull(vNets.get(2).getHost());
		assertNull(vNets.get(3).getHost());
	}

	@Test
	public void testPartialEmbeddingIfFullAllAtOnceDynamicRejCost() {
		AlgorithmConfig.netRejCostDynamic = true;
		final SubstrateNetwork sNet = (SubstrateNetwork) facade.getNetworkById("sub");
		final List<VirtualNetwork> vNets = rejectionSetup();

		initAlgo(sNet, new HashSet<>(vNets));
		assertFalse(algo.execute());

		// The last network must be embedded, because its rejection cost is larger
		assertNotNull(vNets.get(3).getHost());

		// One of the other virtual networks must not be embedded
		int hostIsNullCounter = 0;
		for (int i = 0; i <= 2; i++) {
			if (vNets.get(i).getHost() == null) {
				hostIsNullCounter++;
			}
		}
		assertEquals(1, hostIsNullCounter);
	}

	@Test
	public void testOneTierFull() {
		// Substrate network = one tier network
		final OneTierConfig subConfig = new OneTierConfig(2, 1, false, 4, 4, 4, 10);
		final OneTierNetworkGenerator subGen = new OneTierNetworkGenerator(subConfig);
		subGen.createNetwork("sub", false);

		// Virtual network = one tier network
		final OneTierConfig virtualConfig = new OneTierConfig(2, 1, false, 1, 1, 1, 1);
		final OneTierNetworkGenerator virtGen = new OneTierNetworkGenerator(virtualConfig);
		virtGen.createNetwork("virt", true);

		SubstrateNetwork sNet = (SubstrateNetwork) ModelFacade.getInstance().getNetworkById("sub");
		VirtualNetwork vNet = (VirtualNetwork) ModelFacade.getInstance().getNetworkById("virt");
		initAlgo(sNet, Set.of(vNet));
		assertTrue(algo.execute());

		virtGen.createNetwork("virt2", true);
		sNet = (SubstrateNetwork) ModelFacade.getInstance().getNetworkById("sub");
		vNet = (VirtualNetwork) ModelFacade.getInstance().getNetworkById("virt2");
		initAlgo(sNet, Set.of(vNet));
		assertTrue(algo.execute());

		assertEquals(2, sNet.getGuests().size());
	}

	/*
	 * Utility methods
	 */

	private List<VirtualNetwork> rejectionSetup() {
		facade.addNetworkToRoot("virt2", true);
		facade.addNetworkToRoot("virt3", true);
		facade.addNetworkToRoot("virt4", true);
		oneTierSetupTwoServers("virt", 2);
		oneTierSetupTwoServers("virt2", 2);
		oneTierSetupTwoServers("virt3", 2);
		oneTierSetupThreeServers("virt4", 2);
		twoTierSetupFourServers("sub", 4);
		facade.createAllPathsForNetwork("sub");

		final List<VirtualNetwork> vNets = new LinkedList<>();
		final VirtualNetwork vNet1 = (VirtualNetwork) facade.getNetworkById("virt");
		final VirtualNetwork vNet2 = (VirtualNetwork) facade.getNetworkById("virt2");
		final VirtualNetwork vNet3 = (VirtualNetwork) facade.getNetworkById("virt3");
		final VirtualNetwork vNet4 = (VirtualNetwork) facade.getNetworkById("virt4");

		vNets.add(vNet1);
		vNets.add(vNet2);
		vNets.add(vNet3);
		vNets.add(vNet4);

		return vNets;
	}

}
