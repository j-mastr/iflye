package algorithms.simple;

import java.util.List;
import java.util.Set;

import algorithms.AbstractAlgorithm;
import facade.ModelFacade;
import model.Link;
import model.Node;
import model.SubstrateNetwork;
import model.SubstrateServer;
import model.VirtualNetwork;
import model.VirtualServer;

/**
 * Super simple Virtual Network Embedding algorithm. It searches for the
 * substrate server with largest residual amount of resources and checks if the
 * whole virtual network could fit onto it. If it does not, the algorithm is
 * unable to embed the request. The resources are added all together.
 *
 * @author Maximilian Kratz {@literal <maximilian.kratz@es.tu-darmstadt.de>}
 */
public class SimpleVne extends AbstractAlgorithm {

	/**
	 * Initialize the algorithm with the global model facade.
	 */
	public SimpleVne() {
		this(ModelFacade.getInstance());
	}

	/**
	 * Initialize the algorithm with the given model facade.
	 * 
	 * @param modelFacade Model facade to work with.
	 */
	public SimpleVne(final ModelFacade modelFacade) {
		super(modelFacade);
	}

	@Override
	public void prepare(final SubstrateNetwork sNet, final Set<VirtualNetwork> vNets) {
		if (vNets.size() != 1) {
			throw new IllegalArgumentException(
					"The simple VNE algorithm is only suited for one virtual network at a time.");
		}

		super.prepare(sNet, vNets);
	}

	@Override
	public boolean execute() {
		final List<Node> subServers = modelFacade.getAllServersOfNetwork(sNet.getName());
		String largestServerId = "";
		long largestServerRes = Long.MAX_VALUE;

		for (Node actNode : subServers) {
			final SubstrateServer actServer = (SubstrateServer) actNode;
			final long resSum = actServer.getResidualCpu() + actServer.getResidualMemory()
					+ actServer.getResidualStorage();
			if (largestServerRes < resSum) {
				largestServerRes = resSum;
				largestServerId = actServer.getName();
			}
		}

		// Check if embedding is possible
		int summedCpu = 0;
		int summedMem = 0;
		int summedStor = 0;

		for (Node actNode : modelFacade.getAllServersOfNetwork(getFirstVnet().getName())) {
			final VirtualServer actServer = (VirtualServer) actNode;
			summedCpu += actServer.getCpu();
			summedMem += actServer.getMemory();
			summedMem += actServer.getStorage();
		}

		final SubstrateServer largestSubServer = (SubstrateServer) modelFacade.getServerById(largestServerId);

		if (!(summedCpu <= largestSubServer.getResidualCpu() && summedMem <= largestSubServer.getResidualMemory()
				&& summedStor <= largestSubServer.getResidualStorage())) {
			logger.info("=> SimpleVne: Embedding not possible due to resource constraints.");
			return false;
		}

		/*
		 * Place embedding on model
		 */

		boolean success = true;

		// Network
		success &= modelFacade.embedNetworkToNetwork(sNet.getName(), getFirstVnet().getName());

		// Servers
		for (Node act : modelFacade.getAllServersOfNetwork(getFirstVnet().getName())) {
			success &= modelFacade.embedServerToServer(largestServerId, act.getName());
		}

		// Switches
		for (Node act : modelFacade.getAllSwitchesOfNetwork(getFirstVnet().getName())) {
			success &= modelFacade.embedSwitchToNode(largestServerId, act.getName());
		}

		// Links
		for (Link act : modelFacade.getAllLinksOfNetwork(getFirstVnet().getName())) {
			success &= modelFacade.embedLinkToServer(largestServerId, act.getName());
		}

		return success;
	}

}
