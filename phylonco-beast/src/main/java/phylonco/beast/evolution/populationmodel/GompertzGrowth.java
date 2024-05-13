package phylonco.beast.evolution.populationmodel;

import beast.base.core.*;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.coalescent.PopulationFunction;
import beast.base.inference.operator.UpDownOperator;
import beast.base.inference.parameter.RealParameter;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.IterativeLegendreGaussIntegrator;
import org.apache.commons.math3.exception.TooManyEvaluationsException;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;


@Description("Coalescent intervals for a gompertz growing population.")
public class GompertzGrowth extends PopulationFunction.Abstract implements Loggable, PopFuncWithUpDownOp {
    final public Input<Function> f0Input = new Input<>("f0",
            "Initial proportion of the carrying capacity.", Input.Validate.REQUIRED);
    final public Input<Function> bInput = new Input<>("b",
            "Initial growth rate of tumor growth. Should be greater than 0.", Input.Validate.REQUIRED);
//    final public Input<Function> NInfinityInput = new Input<>("NInfinity",
//            "Carrying capacity of the population.", Input.Validate.REQUIRED);
    final public Input<Function> N0Input = new Input<>("N0",
        "Initial population size.", Input.Validate.REQUIRED);

    public GompertzGrowth() {
        // Example of setting up inputs with default values
        //            f0Input.setValue(new RealParameter("0.5"), this);
        //            bInput.setValue(new RealParameter("0.1"), this);
        //            NInfinityInput.setValue(new RealParameter("1000"), this);
    }

    @Override
    public void initAndValidate() {
        if (f0Input.get() != null && f0Input.get() instanceof RealParameter) {
            RealParameter f0Param = (RealParameter) f0Input.get();
            f0Param.setBounds(Math.max(0.0, f0Param.getLower()), f0Param.getUpper());
        }

        if (bInput.get() != null && bInput.get() instanceof RealParameter) {
            RealParameter bParam = (RealParameter) bInput.get();
            bParam.setBounds(0.0, Double.POSITIVE_INFINITY);  // b should be positive for Gompertz growth
        }

//        if (NInfinityInput.get() != null && NInfinityInput.get() instanceof RealParameter) {
//            RealParameter NInfinityParam = (RealParameter) NInfinityInput.get();
//            NInfinityParam.setBounds(Math.max(0.0, NInfinityParam.getLower()), NInfinityParam.getUpper());
//        }

        if (N0Input.get() != null && N0Input.get() instanceof RealParameter) {
            RealParameter N0Param = (RealParameter) N0Input.get();
            N0Param.setBounds(Math.max(0.0, N0Param.getLower()), N0Param.getUpper());
        }

        // Compute N0 from f0 and NInfinity if they are not null and are RealParameters
//        if (f0Input.get() != null && NInfinityInput.get() != null &&
//                f0Input.get() instanceof RealParameter && NInfinityInput.get() instanceof RealParameter) {
//            double f0 = ((RealParameter) f0Input.get()).getValue();
//            double NInfinity = ((RealParameter) NInfinityInput.get()).getValue();
//        }
    }

    public double getNInfinity() {
        return getN0() / getF0();
    }

    public double getF0() {
        return f0Input.get().getArrayValue();
    }

    public final double getGrowthRateB() {
        return bInput.get().getArrayValue();
    }

//    public double getNInfinity() {
//        return NInfinityInput.get().getArrayValue();
//    }
    public double getN0() {
        return N0Input.get().getArrayValue();
    }

    @Override
    public List<String> getParameterIds() {
        List<String> ids = new ArrayList<>();
        // add f0 and b parameter ids
        if (N0Input.get() instanceof BEASTInterface)
            ids.add(((BEASTInterface)N0Input.get()).getID());
        return ids;
    }

    @Override
    public double getPopSize(double t) {
        double f0 = getF0();
        double b = getGrowthRateB();
        double NInfinity = getNInfinity();
        double N0 = getN0();




        double popSize = N0 * Math.exp(Math.log(NInfinity / N0) * (1 - Math.exp(b * t)));

        return popSize;
    }

    @Override
    public double getIntensity(double t) {
        if (t == 0) return 0;
        UnivariateFunction function = time -> {
            double popSize = getPopSize(time);
            return 1 / Math.max(popSize, 1e-20);
        };
        IterativeLegendreGaussIntegrator integrator = new IterativeLegendreGaussIntegrator(5, 1.0e-12, 1.0e-8, 2, 10000);
        // default intensity if fails
        double intensity = 0;
        try {
            int maxEval = 100000; // trying maxEval = 100000 to test speed
            // previous maxEval default = Integer.MAX_VALUE;
            intensity = integrator.integrate(maxEval, function, 0, t);
        } catch (TooManyEvaluationsException ex) {
            // System.err.println("f0 = " + getF0() + ", b=" + getGrowthRateB() + " NInfinity=" + getNInfinity());
            return intensity;
        }
        return intensity;
    }

    @Override
    public double getInverseIntensity(double x) {
        return 0;
    }

    @Override
    public void init(PrintStream printStream) {

    }

    @Override
    public void log(long step, PrintStream printStream) {
    }

    @Override
    public void close(PrintStream printStream) {

    }

    @Override
    public UpDownOperator getUpDownOperator(Tree tree) {

        UpDownOperator upDownOperator = new UpDownOperator();

        String idStr = getID() + "Up" + tree.getID() + "DownOperator";
        upDownOperator.setID(idStr);

        upDownOperator.setInputValue("scaleFactor", 0.75);
        upDownOperator.setInputValue("weight", 3.0);

        upDownOperator.setInputValue("up", this.f0Input.get());
        upDownOperator.setInputValue("up", this.bInput.get());
        upDownOperator.setInputValue("down", tree);
        upDownOperator.initAndValidate();
        return upDownOperator;

    }
}