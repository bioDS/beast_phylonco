package beast.evolution.substitutionmodel;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.NucleotideDiploid16;

@Description("GT16 diploid substitution model from CellPhy paper")
public class GT16 extends GeneralSubstitutionModel {
    final public Input<RealParameter> rateACInput = new Input<>("rateAC", "the rate of A to C",  Input.Validate.REQUIRED);
    final public Input<RealParameter> rateAGInput = new Input<>("rateAG", "the rate of A to G",  Input.Validate.REQUIRED);
    final public Input<RealParameter> rateATInput = new Input<>("rateAT", "the rate of A to T",  Input.Validate.REQUIRED);
    final public Input<RealParameter> rateCGInput = new Input<>("rateCG", "the rate of C to G",  Input.Validate.REQUIRED);
    final public Input<RealParameter> rateCTInput = new Input<>("rateCT", "the rate of C to T",  Input.Validate.REQUIRED);
    final public Input<RealParameter> rateGTInput = new Input<>("rateGT", "the rate of G to T",  Input.Validate.REQUIRED);


    private RealParameter rateAC; // rate AC
    private RealParameter rateAG; // rate AG
    private RealParameter rateAT; // rate AT
    private RealParameter rateCG; // rate CG
    private RealParameter rateCT; // rate CT
    private RealParameter rateGT; // rate GT

    public GT16() {
        ratesInput.setRule(Input.Validate.OPTIONAL);
    }

    @Override
    public void initAndValidate() {
        if (ratesInput.get() != null) {
            throw new IllegalArgumentException("the rates attribute should not be used. Use the individual rates rateAC, rateCG, etc, instead.");
        }

        frequencies = frequenciesInput.get();

        rateAC = rateACInput.get();
        rateAG = rateAGInput.get();
        rateAT = rateATInput.get();
        rateCG = rateCGInput.get();
        rateCT = rateCTInput.get();
        rateGT = rateGTInput.get();

        updateMatrix = true;
        nrOfStates = 16;
        rateMatrix = new double[nrOfStates][nrOfStates];
        try {
            eigenSystem = createEigenSystem();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setupRelativeRates() { }

    @Override
    protected void setupRateMatrix() {
        setupFrequencies();
        setupRateMatrixUnnormalized();
        normalize();
    }

    @Override
    protected void setupRateMatrixUnnormalized() {
        setupRateMatrixUnnormalized(rateAC.getValue(), rateAG.getValue(), rateAT.getValue(),
                rateCG.getValue(), rateCT.getValue(), rateGT.getValue());
    }

    // instantaneous matrix Q
    private void setupRateMatrixUnnormalized(double rateAC, double rateAG, double rateAT,
                                             double rateCG, double rateCT, double rateGT) {
        double[] pi = frequencies.getFreqs();
        rateMatrix = new double[nrOfStates][nrOfStates];
        int bases = 4;
        for (int i = 0; i < nrOfStates; i++) {
            for (int j = 0; j < nrOfStates; j++) {
                double result = 0.0;
                int fromFirst = i / bases; // first allele in from state
                int fromSecond = i % bases; // second allele in from state
                int toFirst = j / bases; // first allele in to state
                int toSecond = j % bases; // second allele in to state
                if (i != j && (fromFirst == toFirst || fromSecond == toSecond)) {
                        int first, second;
                        if (fromFirst == toFirst) {
                            first = Math.min(fromSecond, toSecond);
                            second = Math.max(fromSecond, toSecond);
                        } else {
                            first = Math.min(fromFirst, toFirst);
                            second = Math.max(fromFirst, toFirst);
                        }
                        int orderedPair = first * 10 + second;
                        switch (orderedPair) {
                            case 1: // 01 - AC
                                result = rateAC; // A -> C or C -> A
                                break;
                            case 2: // 02 - AG
                                result = rateAG; // A -> G or G -> A
                                break;
                            case 3: // 03 - AT
                                result = rateAT; // A -> T or T -> A
                                break;
                            case 12: // 12 - CG
                                result = rateCG; // C -> G or G -> C
                                break;
                            case 13: // 13 - CT
                                result = rateCT; // C -> T or T -> C
                                break;
                            case 23: // 23 - GT
                                result = rateGT; // G -> T or T -> G
                                break;
                            default:
                                result = 0.0;
                                break;
                        }
                } else {
                    result = 0.0; // not reachable in single mutation or diagonal
                }
                rateMatrix[i][j] = result;
            }
        }
        // calculate diagonal entries
        setupDiagonal(rateMatrix);
    }

    private void setupDiagonal(double[][] rateMatrix) {
        for (int i = 0; i < nrOfStates; i++) {
            double sum = 0;
            for (int j = 0; j < nrOfStates; j++) {
                if (i != j)
                    sum += rateMatrix[i][j];
            }
            rateMatrix[i][i] = -sum;
        }
    }

    private void normalize() {
        double[] frequencies = getFrequencies();
        double f = 0.0;
        for (int i = 0; i < nrOfStates; i++) {
            f += frequencies[i] * -rateMatrix[i][i];
        }
        f = 1 / f;
        for (int i = 0; i < nrOfStates; i++) {
            for (int j = 0; j < nrOfStates; j++) {
                rateMatrix[i][j] = f * rateMatrix[i][j];
            }
        }
    }

    protected void setupFrequencies() {
        frequencies.initAndValidate();
    }

    @Override
    public int getStateCount() {
        return nrOfStates;
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof NucleotideDiploid16;
    }

}
