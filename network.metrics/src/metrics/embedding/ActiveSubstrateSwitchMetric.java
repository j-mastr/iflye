package metrics.embedding;

import facade.ModelFacade;
import metrics.IMetric;
import model.Node;
import model.SubstrateNetwork;
import model.SubstrateSwitch;

/**
 * Active substrate switches metric. This one equals the number of substrate
 * switches that have one or more embedded virtual networks on them.
 *
 * @author Maximilian Kratz {@literal <maximilian.kratz@es.tu-darmstadt.de>}
 */
public class ActiveSubstrateSwitchMetric implements IMetric {

	/**
	 * Calculated value of this metric.
	 */
	private final int value;

	/**
	 * Creates a new instance of this metric for the provided substrate network.
	 *
	 * @param sNet Substrate network to calculate the metric for.
	 */
	public ActiveSubstrateSwitchMetric(final SubstrateNetwork sNet) {
		int value = 0;

		for (final Node n : ModelFacade.getAllSwitchesOfNetwork(sNet)) {
			final SubstrateSwitch sw = (SubstrateSwitch) n;
			if (!sw.getGuestSwitches().isEmpty()) {
				value++;
			}
		}

		this.value = value;
	}

	@Override
	public double getValue() {
		return this.value;
	}

}
