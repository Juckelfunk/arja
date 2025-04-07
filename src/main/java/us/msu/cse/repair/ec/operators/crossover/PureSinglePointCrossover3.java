package us.msu.cse.repair.ec.operators.crossover;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import us.msu.cse.repair.ec.representation.ArrayIntAndBinarySolutionType2;
import jmetal.core.Solution;
import jmetal.encodings.variable.ArrayInt;
import jmetal.encodings.variable.Binary;
import jmetal.operators.crossover.Crossover;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

public class PureSinglePointCrossover3 extends Crossover {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final List<?> VALID_TYPES = Arrays.asList(ArrayIntAndBinarySolutionType2.class);

	private Double crossoverProbability_ = null;

	
	// all parts using single crossover point
	public PureSinglePointCrossover3(LinkedHashMap<String, Object> parameters) {
		super(parameters);
		if (parameters.get("probability") != null)
			crossoverProbability_ = (Double) parameters.get("probability");
	} // SinglePointCrossover

	public Solution[] doCrossover(double probability, Solution parent1, Solution parent2) throws JMException {
		Solution[] offSpring = new Solution[2];
		offSpring[0] = new Solution(parent1);
		offSpring[1] = new Solution(parent2);

		if (PseudoRandom.randDouble() < probability) {
			ArrayInt p1_int = (ArrayInt) parent1.getDecisionVariables()[0];
			Binary p1_bin = (Binary) parent1.getDecisionVariables()[1];

			ArrayInt p2_int = (ArrayInt) parent2.getDecisionVariables()[0];
			Binary p2_bin = (Binary) parent2.getDecisionVariables()[1];

			ArrayInt c1_int = (ArrayInt) offSpring[0].getDecisionVariables()[0];
			Binary c1_bin = (Binary) offSpring[0].getDecisionVariables()[1];

			ArrayInt c2_int = (ArrayInt) offSpring[1].getDecisionVariables()[0];
			Binary c2_bin = (Binary) offSpring[1].getDecisionVariables()[1];

			int size = p1_bin.getNumberOfBits();
			
			int k = PseudoRandom.randInt(0, size - 1);

			int crossoverPoint = k;
			for (int i = crossoverPoint; i < size; i++) {
				int valueX1 = p1_int.getValue(i);
				int valueX2 = p2_int.getValue(i);

				c1_int.setValue(i, valueX2);
				c2_int.setValue(i, valueX1);
			} // for

			
			crossoverPoint = size + k;
			for (int i = crossoverPoint; i < 2 * size; i++) {
				int valueX1 = p1_int.getValue(i);
				int valueX2 = p2_int.getValue(i);

				c1_int.setValue(i, valueX2);
				c2_int.setValue(i, valueX1);
			} // for
			
			crossoverPoint = 2 * size + k;
			for (int i = crossoverPoint; i < 3 * size; i++) {
				int valueX1 = p1_int.getValue(i);
				int valueX2 = p2_int.getValue(i);

				c1_int.setValue(i, valueX2);
				c2_int.setValue(i, valueX1);
			} // for

			crossoverPoint = k;
			for (int i = crossoverPoint; i < size; i++) {
				boolean valueX1 = p1_bin.bits_.get(i);
				boolean valueX2 = p2_bin.bits_.get(i);

				c1_bin.setIth(i, valueX2);
				c2_bin.setIth(i, valueX1);
			} // for

		}

		return offSpring;
	}

	@Override
	public Object execute(Object object) throws JMException {
		Solution[] parents = (Solution[]) object;

		if (!(VALID_TYPES.contains(parents[0].getType().getClass())
				&& VALID_TYPES.contains(parents[1].getType().getClass()))) {

			Configuration.logger_.severe("PureSinglePointCrossover.execute: the solutions "
					+ "are not of the right type. The type should be 'ArrayIntAndBinarySolutionType2', but " + parents[0].getType()
					+ " and " + parents[1].getType() + " are obtained");

			Class<?> cls = java.lang.String.class;
			String name = cls.getName();
			throw new JMException("Exception in " + name + ".execute()");
		} // if

		if (parents.length < 2) {
			Configuration.logger_.severe("PureSinglePointCrossover.execute: operator " + "needs two parents");
			Class<?> cls = java.lang.String.class;
			String name = cls.getName();
			throw new JMException("Exception in " + name + ".execute()");
		}

		Solution[] offSpring;
		offSpring = doCrossover(crossoverProbability_, parents[0], parents[1]);

		// -> Update the offSpring solutions
		for (int i = 0; i < offSpring.length; i++) {
			offSpring[i].setCrowdingDistance(0.0);
			offSpring[i].setRank(0);
		}
		return offSpring;
	} // execute

}
