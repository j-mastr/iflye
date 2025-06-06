package examples.algorithms;

import java.util.Set;

import algorithms.pm.VnePmMdvneAlgorithm;
import examples.AbstractIflyeExample;
import facade.ModelFacade;
import facade.config.ModelFacadeConfig;
import generators.OneTierNetworkGenerator;
import generators.TwoTierNetworkGenerator;
import generators.config.OneTierConfig;
import generators.config.TwoTierConfig;
import metrics.embedding.AcceptedVnrMetric;
import metrics.embedding.AveragePathLengthMetric;
import metrics.embedding.TotalPathCostMetric;
import metrics.manager.GlobalMetricsManager;
import model.SubstrateNetwork;
import model.VirtualNetwork;
import model.VirtualServer;

/**
 * Runnable example for the VNE PM algorithm implementation that demonstrates
 * the repairing of the model consistency after removing an substrate server
 * hosting at least one element of a previously embedded virtual network
 * ungracefully.
 *
 * @author Maximilian Kratz {@literal <maximilian.kratz@es.tu-darmstadt.de>}
 */
public class VnePmMdvneAlgorithmExampleRepairModelServer extends AbstractIflyeExample {

	/**
	 * Main method to start the example. String array of arguments will be ignored.
	 *
	 * @param args Will be ignored.
	 */
	public static void main(final String[] args) {
		// Setup
		ModelFacadeConfig.MIN_PATH_LENGTH = 1;
		ModelFacadeConfig.MAX_PATH_LENGTH = 4;

		GlobalMetricsManager.startRuntime();

		// Substrate network = two tier network
		final OneTierConfig rackConfig = new OneTierConfig(2, 1, false, 1, 1, 1, 10);
		final TwoTierConfig substrateConfig = new TwoTierConfig();
		substrateConfig.setRack(rackConfig);
		substrateConfig.setCoreBandwidth(10);
		substrateConfig.setNumberOfCoreSwitches(1);
		substrateConfig.setNumberOfRacks(6);
		final TwoTierNetworkGenerator subGen = new TwoTierNetworkGenerator(substrateConfig);
		subGen.createNetwork("sub", false);

		for (int i = 1; i <= 3; i++) {
			if (i == 3) {
				ModelFacade.getInstance().removeSubstrateServerFromNetworkSimple(
						((VirtualServer) ModelFacade.getInstance().getServerById("virt_2_srv_1")).getHost().getName());
			}

			// Virtual network = one tier network
			final OneTierConfig virtualConfig = new OneTierConfig(2, 1, false, 1, 1, 1, 1);
			final OneTierNetworkGenerator virtGen = new OneTierNetworkGenerator(virtualConfig);
			virtGen.createNetwork("virt_" + i, true);

			final SubstrateNetwork sNet = (SubstrateNetwork) ModelFacade.getInstance().getNetworkById("sub");
			final VirtualNetwork vNet = (VirtualNetwork) ModelFacade.getInstance().getNetworkById("virt_" + i);

			// Create and execute algorithm
			logger.info("=> Embedding virtual network #" + i);
			final VnePmMdvneAlgorithm algo = new VnePmMdvneAlgorithm();
			algo.prepare(sNet, Set.of(vNet));
			algo.execute();
		}

		GlobalMetricsManager.stopRuntime();

		// Save model to file
		logger.info("=> Execution finished.");
		ModelFacade.getInstance().validateModel();
		ModelFacade.getInstance().persistModel();

		// Time measurements
		logger.info("=> Elapsed time (total): " + GlobalMetricsManager.getRuntime().getValue() / 1_000_000_000
				+ " seconds");
		logger.info(
				"=> Elapsed time (PM): " + GlobalMetricsManager.getRuntime().getPmValue() / 1_000_000_000 + " seconds");
		logger.info("=> Elapsed time (ILP): " + GlobalMetricsManager.getRuntime().getIlpValue() / 1_000_000_000
				+ " seconds");
		logger.info("=> Elapsed time (rest): " + GlobalMetricsManager.getRuntime().getRestValue() / 1_000_000_000
				+ " seconds");

		final SubstrateNetwork sNet = (SubstrateNetwork) ModelFacade.getInstance().getNetworkById("sub");
		final AcceptedVnrMetric acceptedVnrs = new AcceptedVnrMetric(sNet);
		logger.info("=> Accepted VNRs: " + (int) acceptedVnrs.getValue());
		final TotalPathCostMetric totalPathCost = new TotalPathCostMetric(sNet);
		logger.info("=> Total path cost: " + totalPathCost.getValue());
		final AveragePathLengthMetric averagePathLength = new AveragePathLengthMetric(sNet);
		logger.info("=> Average path length: " + averagePathLength.getValue());

		System.exit(0);
	}

}
