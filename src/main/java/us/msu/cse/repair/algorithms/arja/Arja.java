package us.msu.cse.repair.algorithms.arja;

import us.msu.cse.repair.core.AbstractRepairAlgorithm;
import us.msu.cse.repair.ec.algorithms.NSGAIIWithInit;
import us.msu.cse.repair.ec.problems.ArjaProblem;

public class Arja extends AbstractRepairAlgorithm {
	public Arja(ArjaProblem problem) throws Exception {
		algorithm = new NSGAIIWithInit(problem);
	}

	public Arja(ArjaProblem problem, double initRatioOfPerfect, double initRatioOfFame) {
		algorithm = new NSGAIIWithInit(problem, initRatioOfPerfect, initRatioOfFame);
	}
}
