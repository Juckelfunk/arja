package us.msu.cse.repair;

import java.util.LinkedHashMap;

import jmetal.operators.crossover.Crossover;
import jmetal.operators.mutation.Mutation;
import jmetal.operators.selection.Selection;
import jmetal.operators.selection.SelectionFactory;
import us.msu.cse.repair.algorithms.arja.Arja;
import us.msu.cse.repair.core.AbstractRepairAlgorithm;
import us.msu.cse.repair.ec.operators.crossover.ExtendedCrossoverFactory;
import us.msu.cse.repair.ec.operators.mutation.ExtendedMutationFactory;
import us.msu.cse.repair.ec.problems.ArjaProblem;

public class ArjaMain {
	public static void main(String args[]) throws Exception {
		LinkedHashMap<String, String> parameterStrs = Interpreter.getParameterStrings(args);
		LinkedHashMap<String, Object> parameters = Interpreter.getBasicParameterSetting(parameterStrs);

		String ingredientScreenerNameS = parameterStrs.get("ingredientScreenerName");
		if (ingredientScreenerNameS != null) 
			parameters.put("ingredientScreenerName", ingredientScreenerNameS);
		
		
		int populationSize = 40;
		int maxGenerations = 50;
		
		String populationSizeS = parameterStrs.get("populationSize");
		if (populationSizeS != null)
			populationSize = Integer.parseInt(populationSizeS);
		
		String maxGenerationsS = parameterStrs.get("maxGenerations");
		if (maxGenerationsS != null)
			maxGenerations = Integer.parseInt(maxGenerationsS);

		double initRatioOfPerfect = 0;
		double initRatioOfFame = 0;

		String initRatioOfPerfectS = parameterStrs.get("initRatioOfPerfect");
		if (initRatioOfPerfectS != null)
			initRatioOfPerfect = Double.parseDouble(initRatioOfPerfectS);

		String initRatioOfFameS = parameterStrs.get("initRatioOfFame");
		if (initRatioOfFameS != null)
            initRatioOfFame = Double.parseDouble(initRatioOfFameS);

		ArjaProblem problem = new ArjaProblem(parameters);
		AbstractRepairAlgorithm repairAlg = new Arja(problem, initRatioOfPerfect, initRatioOfFame);

		repairAlg.setInputParameter("populationSize", populationSize);
		repairAlg.setInputParameter("maxEvaluations", populationSize * maxGenerations);

		parameters = new LinkedHashMap<String, Object>();

		Crossover crossover;
		Mutation mutation;
		Selection selection;

		parameters = new LinkedHashMap<String, Object>();
		parameters.put("probability", 1.0);
		crossover = ExtendedCrossoverFactory.getCrossoverOperator("HUXSinglePointCrossover", parameters);

		parameters = new LinkedHashMap<String, Object>();
		parameters.put("probability", 1.0 / problem.getNumberOfModificationPoints());
		mutation = ExtendedMutationFactory.getMutationOperator("BitFlipUniformMutation", parameters);

		// Selection Operator
		parameters = null;
		selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters);

		// Add the operators to the algorithm
		repairAlg.addOperator("crossover", crossover);
		repairAlg.addOperator("mutation", mutation);
		repairAlg.addOperator("selection", selection);
		
		repairAlg.execute();
	}
}
