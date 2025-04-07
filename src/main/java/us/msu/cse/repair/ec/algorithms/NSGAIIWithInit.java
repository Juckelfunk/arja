package us.msu.cse.repair.ec.algorithms;

import jmetal.core.*;
import jmetal.encodings.variable.ArrayInt;
import jmetal.encodings.variable.Binary;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Distance;
import jmetal.util.JMException;
import jmetal.util.Ranking;
import jmetal.util.comparators.CrowdingComparator;
import us.msu.cse.repair.ec.problems.ArjaProblem;
import us.msu.cse.repair.ec.representation.ArjaDecisionVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

public class NSGAIIWithInit extends Algorithm {
    List<Variable[]> initialDecisionVariables;

    private double initRatioOfPerfect;
    private double initRatioOfFame;

    public NSGAIIWithInit(Problem problem, List<Variable[]> initialDecisionVariables) {
        super(problem);
        this.initialDecisionVariables = initialDecisionVariables;
    }

    public NSGAIIWithInit(Problem problem) {
        super(problem);
        this.initialDecisionVariables = new ArrayList<>();
    }

    public NSGAIIWithInit(ArjaProblem problem, double initRatioOfPerfect, double initRatioOfFame) {
        super(problem);
        this.initRatioOfPerfect = initRatioOfPerfect;
        this.initRatioOfFame = initRatioOfFame;
    }

    private void computeArjaInitialDecisionVariables() {
        int populationSize = (Integer) this.getInputParameter("populationSize");

        ArjaProblem problem = (ArjaProblem) problem_;

        this.initialDecisionVariables = new ArrayList<>();

        Set<ArjaDecisionVariable> perfectDecisionVars = problem.getPerfectDecisionVariables();
        if (initRatioOfPerfect != 0) {
            if (perfectDecisionVars == null) {
                throw new RuntimeException("missing perfect seeds (option perfectPath)");
            }

            int numPerfect = (int) Math.round(initRatioOfPerfect * populationSize);
            int i = 0;
            for (ArjaDecisionVariable var: perfectDecisionVars) {
                initialDecisionVariables.add(wrapArjaDecisionVariable(var));

                i++;
                if (i >= numPerfect) {
                    break;
                }
            }
        }

        Set<ArjaDecisionVariable> fameDecisionVars = problem.getFameDecisionVariables();
        if (initRatioOfFame != 0) {
            if (fameDecisionVars == null) {
                throw new RuntimeException("missing hall of fame seeds (option hallOfFameInPath");
            }

            int numFame = (int) Math.round(initRatioOfFame * populationSize);
            int i = 0;
            for (ArjaDecisionVariable var: fameDecisionVars) {
                initialDecisionVariables.add(wrapArjaDecisionVariable(var));

                i++;
                if (i >= numFame) {
                    break;
                }
            }
        }
    }

    private Variable[] wrapArjaDecisionVariable(ArjaDecisionVariable var) {
        if (!(problem_ instanceof ArjaProblem)) {
            throw new IllegalStateException();
        }

        int size = ((ArjaProblem) problem_).getNumberOfModificationPoints();

        SolutionType solutionType = problem_.getSolutionType();

        Variable[] decisionVariables;
        try {
            decisionVariables = solutionType.createVariables();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        ArrayInt arrayInt = (ArrayInt) decisionVariables[0];
        arrayInt.array_ = Arrays.copyOf(var.getArray(), size * 2);

        Binary binary = (Binary) decisionVariables[1];
        binary.bits_ = (BitSet) var.getBits().clone();

        return decisionVariables;
    }

    public SolutionSet execute() throws JMException, ClassNotFoundException {
        if (problem_ instanceof ArjaProblem) {
            computeArjaInitialDecisionVariables();
        }

        Distance distance = new Distance();
        int populationSize = (Integer)this.getInputParameter("populationSize");
        int maxEvaluations = (Integer)this.getInputParameter("maxEvaluations");
        QualityIndicator indicators = (QualityIndicator)this.getInputParameter("indicators");
        SolutionSet population = new SolutionSet(populationSize);
        int evaluations = 0;
        int requiredEvaluations = 0;
        Operator mutationOperator = (Operator)this.operators_.get("mutation");
        Operator crossoverOperator = (Operator)this.operators_.get("crossover");
        Operator selectionOperator = (Operator)this.operators_.get("selection");

        int initSize = initialDecisionVariables.size();
        for(int i = 0; i < populationSize; ++i) {
            Solution newSolution;
            if (i < initSize) {
                newSolution = new Solution(this.problem_, initialDecisionVariables.get(i));
            } else {
                newSolution = new Solution(this.problem_);
            }
            this.problem_.evaluate(newSolution);
            this.problem_.evaluateConstraints(newSolution);
            ++evaluations;
            population.add(newSolution);
        }

        while(evaluations < maxEvaluations) {
            SolutionSet offspringPopulation = new SolutionSet(populationSize);
            Solution[] parents = new Solution[2];

            for(int i = 0; i < populationSize / 2; ++i) {
                if (evaluations < maxEvaluations) {
                    parents[0] = (Solution)selectionOperator.execute(population);
                    parents[1] = (Solution)selectionOperator.execute(population);
                    Solution[] offSpring = (Solution[])crossoverOperator.execute(parents);
                    mutationOperator.execute(offSpring[0]);
                    mutationOperator.execute(offSpring[1]);
                    this.problem_.evaluate(offSpring[0]);
                    this.problem_.evaluateConstraints(offSpring[0]);
                    this.problem_.evaluate(offSpring[1]);
                    this.problem_.evaluateConstraints(offSpring[1]);
                    offspringPopulation.add(offSpring[0]);
                    offspringPopulation.add(offSpring[1]);
                    evaluations += 2;
                }
            }

            SolutionSet union = population.union(offspringPopulation);
            Ranking ranking = new Ranking(union);
            int remain = populationSize;
            int index = 0;
            SolutionSet front = null;
            population.clear();
            front = ranking.getSubfront(index);

            int k;
            while(remain > 0 && remain >= front.size()) {
                distance.crowdingDistanceAssignment(front, this.problem_.getNumberOfObjectives());

                for(k = 0; k < front.size(); ++k) {
                    population.add(front.get(k));
                }

                remain -= front.size();
                ++index;
                if (remain > 0) {
                    front = ranking.getSubfront(index);
                }
            }

            if (remain > 0) {
                distance.crowdingDistanceAssignment(front, this.problem_.getNumberOfObjectives());
                front.sort(new CrowdingComparator());

                for(k = 0; k < remain; ++k) {
                    population.add(front.get(k));
                }

                remain = 0;
            }

            if (indicators != null && requiredEvaluations == 0) {
                double HV = indicators.getHypervolume(population);
                if (HV >= 0.98 * indicators.getTrueParetoFrontHypervolume()) {
                    requiredEvaluations = evaluations;
                }
            }
        }

        this.setOutputParameter("evaluations", requiredEvaluations);
        Ranking ranking = new Ranking(population);
        ranking.getSubfront(0).printFeasibleFUN("FUN_NSGAII");
        return ranking.getSubfront(0);
    }
}
