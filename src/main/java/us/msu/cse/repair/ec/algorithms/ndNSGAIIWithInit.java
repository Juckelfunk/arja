package us.msu.cse.repair.ec.algorithms;

import java.util.List;

import jmetal.core.*;
import jmetal.util.Distance;
import jmetal.util.JMException;
import jmetal.util.Ranking;
import jmetal.util.comparators.CrowdingComparator;
import us.msu.cse.repair.core.AbstractRepairProblem;
import us.msu.cse.repair.core.util.Helper;
import us.msu.cse.repair.ec.problems.ArjaProblem;
import us.msu.cse.repair.ec.representation.ArjaDecisionVariable;
import jmetal.encodings.variable.ArrayInt;
import jmetal.encodings.variable.Binary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Set;

public class ndNSGAIIWithInit extends Algorithm {

    List<Variable[]> initialDecisionVariables;

    private double initRatioOfPerfect;
    private double initRatioOfFame;

    public ndNSGAIIWithInit(Problem problem) {
        super(problem);
        this.initialDecisionVariables = new ArrayList<>();
    }

    public ndNSGAIIWithInit(ArjaProblem problem, double initRatioOfPerfect, double initRatioOfFame) {
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
        arrayInt.array_ = Arrays.copyOf(var.getArray(), size * 3);

        Binary binary = (Binary) decisionVariables[1];
        binary.bits_ = (BitSet) var.getBits().clone();

        return decisionVariables;
    }

    public SolutionSet execute() throws JMException, ClassNotFoundException {
        if (problem_ instanceof ArjaProblem) {
            computeArjaInitialDecisionVariables();
        }

        int populationSize;
        int maxEvaluations;
        int evaluations;

        Integer maxTime;

        SolutionSet population;
        SolutionSet offspringPopulation;
        SolutionSet union;

        Operator mutationOperator;
        Operator crossoverOperator;
        Operator selectionOperator;

        Distance distance = new Distance();

        long initTime = System.currentTimeMillis();

        // Read the parameters
        populationSize = (Integer) getInputParameter("populationSize");
        maxEvaluations = (Integer) getInputParameter("maxEvaluations");

        maxTime = ((Integer) getInputParameter("maxTime"));

        // Initialize the variables
        population = new SolutionSet(populationSize);
        evaluations = 0;

        // Read the operators
        mutationOperator = operators_.get("mutation");
        crossoverOperator = operators_.get("crossover");
        selectionOperator = operators_.get("selection");

        List<List<String>> availableManips = ((AbstractRepairProblem) problem_).getAvailableManipulations();

        // Create the initial solutionSet
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

        // Generations
        while (evaluations < maxEvaluations) {

            // Create the offSpring solutionSet
            offspringPopulation = new SolutionSet(populationSize);
            Solution[] parents = new Solution[2];
            for (int i = 0; i < (populationSize / 2); i++) {
                if (evaluations < maxEvaluations) {
                    // obtain parents
                    parents[0] = (Solution) selectionOperator
                            .execute(population);
                    parents[1] = (Solution) selectionOperator
                            .execute(population);
                    Solution[] offSpring = (Solution[]) crossoverOperator
                            .execute(parents);
                    mutationOperator.execute(offSpring[0]);
                    mutationOperator.execute(offSpring[1]);
                    problem_.evaluate(offSpring[0]);
                    problem_.evaluateConstraints(offSpring[0]);
                    problem_.evaluate(offSpring[1]);
                    problem_.evaluateConstraints(offSpring[1]);
                    offspringPopulation.add(offSpring[0]);
                    offspringPopulation.add(offSpring[1]);
                    evaluations += 2;
                } // if
            } // for

            // Create the solutionSet union of solutionSet and offSpring
            union = ((SolutionSet) population).union(offspringPopulation);


            union = Helper.removeDuplications(union, availableManips);

            if (union.size() < populationSize) {
                int rs = populationSize - union.size();
                for (int k = 0; k < rs; k++) {
                    Solution sol = new Solution(problem_);
                    problem_.evaluate(sol);
                    problem_.evaluateConstraints(sol);
                    evaluations++;
                    union.add(sol);
                }
            }

            // Ranking the union
            Ranking ranking = new Ranking(union);

            int remain = populationSize;
            int index = 0;
            SolutionSet front = null;
            population.clear();

            // Obtain the next front
            front = ranking.getSubfront(index);

            while ((remain > 0) && (remain >= front.size())) {
                // Assign crowding distance to individuals
                distance.crowdingDistanceAssignment(front,
                        problem_.getNumberOfObjectives());
                // Add the individuals of this front
                for (int k = 0; k < front.size(); k++) {
                    population.add(front.get(k));
                } // for

                // Decrement remain
                remain = remain - front.size();

                // Obtain the next front
                index++;
                if (remain > 0) {
                    front = ranking.getSubfront(index);
                } // if
            } // while

            // Remain is less than front(index).size, insert only the best one
            if (remain > 0) { // front contains individuals to insert
                distance.crowdingDistanceAssignment(front,
                        problem_.getNumberOfObjectives());
                front.sort(new CrowdingComparator());
                for (int k = 0; k < remain; k++) {
                    population.add(front.get(k));
                } // for

                remain = 0;
            } // if


            if (maxTime != null) {
                long elapsedTime = System.currentTimeMillis() - initTime;
                if (elapsedTime > maxTime) {
                    break;
                }
            }

        } // while

        // Return the first non-dominated front
        Ranking ranking = new Ranking(population);

        ranking.getSubfront(0).printFeasibleFUN("FUN_NSGAII");
        return ranking.getSubfront(0);
    } // execute
}