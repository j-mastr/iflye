package algorithms.gips;

import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.emoflon.gips.core.milp.SolverOutput;
import org.emoflon.gips.core.util.IMeasurement;
import org.emoflon.gips.gipsl.examples.mdvne.MdvneGipsIflyeAdapter;
import org.emoflon.gips.gipsl.examples.mdvne.MdvneGipsLookaheadIflyeAdapter;

import algorithms.AbstractAlgorithm;
import algorithms.AlgorithmConfig;
import algorithms.AlgorithmConfig.Objective;
import facade.ModelFacade;
import model.SubstrateNetwork;
import model.VirtualNetwork;

/**
 * GIPS-based VNE lookahead algorithm implementation.
 *
 * @author Maximilian Kratz {@literal <maximilian.kratz@es.tu-darmstadt.de>}
 */
public class VneGipsLookaheadAlgorithm extends AbstractAlgorithm implements GipsAlgorithm {

	/**
	 * Relative base path of the GIPS MdVNE project.
	 */
	private final static String GIPS_PROJECT_BASE_PATH = "../../gips-examples/org.emoflon.gips.gipsl.examples.mdvne";

	/**
	 * The ID of the virtual network to embed.
	 */
	private String vNetId = null;

	/**
	 * The GIPS MdVNE adapter.
	 */
	private final MdvneGipsLookaheadIflyeAdapter iflyeAdapter;

	/**
	 * The most recent GIPS MdVNE output.
	 */
	private MdvneGipsIflyeAdapter.MdvneIflyeOutput iflyeOutput;

	/**
	 * Initialize the algorithm with the global model facade. GIPS will calculate a
	 * valid embedding for all given virtual networks but only the one whose name
	 * matches the given network ID will be embedded within the model.
	 *
	 * @param sNet   Substrate network to work with.
	 * @param vNets  Set of virtual networks to work with.
	 * @param vNetId ID of the virtual network to embed.
	 */
	public VneGipsLookaheadAlgorithm() {
		this(ModelFacade.getInstance());
	}

	/**
	 * Initialize the algorithm with the given model facade.
	 * 
	 * @param modelFacade Model facade to work with.
	 */
	public VneGipsLookaheadAlgorithm(final ModelFacade modelFacade) {
		super(modelFacade);

		iflyeAdapter = new MdvneGipsLookaheadIflyeAdapter();
	}

	@Override
	public boolean execute() {
		// Check if correct objective is used
		if (AlgorithmConfig.obj != Objective.TOTAL_COMMUNICATION_OBJECTIVE_C) {
			throw new UnsupportedOperationException(
					"The VNE GIPS algorithm can only be used with the total communication cost C.");
		}

		// TODO: Time measurement
		final ResourceSet model = getModelFacade().getResourceSet();
		iflyeOutput = iflyeAdapter.execute(model,
				GIPS_PROJECT_BASE_PATH + "/src-gen/org/emoflon/gips/gipsl/examples/mdvne/api/gips/gips-model.xmi",
				GIPS_PROJECT_BASE_PATH + "/src-gen/org/emoflon/gips/gipsl/examples/mdvne/api/ibex-patterns.xmi",
				GIPS_PROJECT_BASE_PATH + "/src-gen/org/emoflon/gips/gipsl/examples/mdvne/hipe/engine/hipe-network.xmi",
				vNetId);

		final boolean gipsSuccess = this.iflyeOutput.solverOutput().solutionCount() > 0;

		// Workaround to fix the residual bandwidth of other paths possibly affected by
		// virtual link to substrate path embeddings
		getModelFacade().updateAllPathsResidualBandwidth(sNet.getName());
		return gipsSuccess;
	}

	/**
	 * Returns the saved virtual network ID.
	 * 
	 * @return Saved virtual network ID.
	 */
	public String getVNetId() {
		return this.vNetId;
	}

	/**
	 * Sets the saved virtual network ID to the given value.
	 * 
	 * @param vNetId virtual network ID to save.
	 */
	public void setVNetId(final String vNetId) {
		this.vNetId = vNetId;
	}

	/**
	 * Initializes a new instance of the GIPS-based VNE algorithm. Only the virtual
	 * network with the given ID vNetId will be embedded within the model.
	 *
	 * @param sNet   Substrate network to work with.
	 * @param vNets  Set of virtual networks to work with.
	 * @param vNetId ID of the virtual network to embed.
	 * @return Instance of this algorithm implementation.
	 */
	public void prepare(final SubstrateNetwork sNet, final Set<VirtualNetwork> vNets, final String vNetId) {
		if (sNet == null || vNets == null) {
			throw new IllegalArgumentException("One of the provided network objects was null.");
		}

		if (vNets.size() == 0) {
			throw new IllegalArgumentException("Provided set of virtual networks was empty.");
		}

		VneGipsAlgorithmUtils.checkGivenVnets(getModelFacade(), vNets);
		super.prepare(sNet, vNets);
		this.vNetId = vNetId;
	}

	@Override
	public SolverOutput getSolverOutput() {
		return this.iflyeOutput.solverOutput();
	}

	@Override
	public Map<String, String> getMatches() {
		return this.iflyeOutput.matches();
	}

	@Override
	public Map<String, IMeasurement> getMeasurements() {
		return null;
	}

	/**
	 * Resets the algorithm instance.
	 */
	@Override
	public void dispose() {
		iflyeAdapter.resetInit();
	}

	@Override
	public void prepare(final SubstrateNetwork sNet, final Set<VirtualNetwork> vNets) {
		prepare(sNet, vNets, null);
	}

}
