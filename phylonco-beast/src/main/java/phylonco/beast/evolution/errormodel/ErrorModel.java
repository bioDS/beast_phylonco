package phylonco.beast.evolution.errormodel;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.datatype.DataType;

@Description("Error model abstract class")
public abstract class ErrorModel extends CalculationNode {

    final public Input<DataType> datatypeInput = new Input<>("datatype", "the datatype of the alignment, for example nucleotide etc", Input.Validate.REQUIRED);

    final public Input<TaxonSet> excludedTaxaInput = new Input<>("exclude", "list of taxa that is excluded from error model", Input.Validate.OPTIONAL);

    protected DataType.Base datatype;

    private TaxonSet excludedTaxa;

    protected double[][] errorMatrix;
    protected double[][] storedErrorMatrix;

    protected boolean updateMatrix = true;

    /**
     * initialises error model and performs input checking
     * subclasses need to set up the error matrix
     */
    @Override
    public void initAndValidate() {
        datatype = (DataType.Base) (datatypeInput.get());
        if (datatype != null && !canHandleDataType(datatype)) {
            String message = "Error model cannot handle data type " + datatype.getTypeDescription();
            throw new IllegalArgumentException(message);
        }

        // exclude taxa for error model
        if (excludedTaxaInput.get() != null) {
            excludedTaxa = excludedTaxaInput.get();
        }

        // subclasses to set up error matrix
    }

    public TaxonSet getExcludedTaxa() {
        return excludedTaxa;
    }

    /**
     * returns the probability of observed state given the true state based on the error model
     * @param observedState index of observed state
     * @param trueState index of true state
     * @param t branch length
     * @return probability of observed state given the true state
     */
    public abstract double getProbability(int observedState, int trueState, double t);

    /**
     * returns a probability vector of the conditional probability of the observed state given each true state
     * @param observedState index of observed state
     * @param t branch length
     * @return conditional probabilities of the observed state given the true state, for each possible true state
     */
    public abstract double[] getProbabilities(int observedState, double t);

    /**
     * checks whether the error model can handle the input datatype
     * @param datatype the alignment datatype
     * @return true if the error model can handle the input datatype
     */
    public abstract boolean canHandleDataType(DataType datatype);

    /**
     * returns the state partial including ambiguous states, 1.0 if true state is in the set, or 0.0 otherwise
     * @param observedState observed state index
     * @param trueState true state index
     * @return state partial
     */
    public double getStatePartial(int observedState, int trueState) {
        double prob;
        boolean[] states = datatype.getStateSet(observedState);
        if (states[trueState]) {
            prob = 1.0;
        } else {
            prob = 0.0;
        }
        return prob;
    }

    /**
     * return the state partials without errors
     * @param observedState observed state index
     * @return state partial without errors
     */
    public double[] getStatePartials(int observedState) {
        int states = datatype.getStateCount();
        double[] prob = new double[states];
        for (int i = 0; i < states; i++) {
            prob[i] = getStatePartial(observedState, i);
        }
        return prob;
    }

    /**
     * set up the error matrix using the error parameter inputs
     */
    public void setupErrorMatrix() {
        if (errorMatrix == null) {
            errorMatrix = new double[datatype.mapCodeToStateSet.length][datatype.getStateCount()];
        }
        for (int trueState = 0; trueState < datatype.getStateCount(); trueState++) {
            for (int observedState = 0; observedState < datatype.mapCodeToStateSet.length; observedState++) {
                // rows are observed states X, columns are true states Y
                errorMatrix[observedState][trueState] = getProbability(observedState, trueState, 0.0);
            }
        }
    }

    /**
     * returns a boolean indicating whether the error matrix needs to be udpated
     * @return true if the error matrix needs to updated
     */
    public boolean getUpdateFlag() {
        return updateMatrix;
    }

    /**
     * sets the boolean indicating whether the error matrix needs to be udpated
     * @param status matrix update status
     */
    public void setUpdateFlag(boolean status) {
        updateMatrix = status;
    }

    /**
     * CalculationNode implementation follows *
     */
    @Override
    public void store() {
        storedErrorMatrix = errorMatrix;
        super.store();
    }

    /**
     * Restore the additional stored state
     */
    @Override
    public void restore() {
        errorMatrix = storedErrorMatrix;
        super.restore();
    }
    @Override
    public boolean requiresRecalculation() {
        // we only get here if something is dirty
        updateMatrix = true;
        return true;
    }

}
