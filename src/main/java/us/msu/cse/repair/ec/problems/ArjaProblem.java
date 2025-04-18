package us.msu.cse.repair.ec.problems;

import java.io.*;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.tools.JavaFileObject;

import com.google.gson.Gson;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import jmetal.core.Solution;
import jmetal.encodings.variable.ArrayInt;
import jmetal.encodings.variable.Binary;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import us.msu.cse.repair.core.AbstractRepairProblem;
import us.msu.cse.repair.core.filterrules.MIFilterRule;
import us.msu.cse.repair.core.parser.ModificationPoint;
import us.msu.cse.repair.core.testexecutors.ExternalTestExecutor;
import us.msu.cse.repair.core.testexecutors.ITestExecutor;
import us.msu.cse.repair.core.util.IO;
import us.msu.cse.repair.core.util.Patch;
import us.msu.cse.repair.ec.representation.ArjaSolutionSummary;
import us.msu.cse.repair.ec.representation.ArjaDecisionVariable;
import us.msu.cse.repair.ec.representation.ArrayIntAndBinarySolutionType;
import us.msu.cse.repair.ec.representation.ManipulationSummary;
import us.msu.cse.repair.ec.representation.StatementSummary;

public class ArjaProblem extends AbstractRepairProblem {
	private static final long serialVersionUID = 1L;
	Double weight;

	Integer numberOfObjectives;
	Integer maxNumberOfEdits;
	Double mu;

	String initializationStrategy;

	Boolean miFilterRule;

	String hallOfFameOutPath;
	Set<ArjaSolutionSummary> hallOfFameOut = new LinkedHashSet<>();

	String hallOfFameInPath;
	Set<ArjaSolutionSummary> fameSolutions;
	Set<ArjaDecisionVariable> fameDecisionVariables;

	String fameOutputRoot;
	Set<Patch> famePatches;

	String perfectPath;
	Set<ArjaSolutionSummary> perfectSolutions;
	Set<ArjaDecisionVariable> perfectDecisionVariables;

	Gson gson = new Gson();

	@SuppressWarnings("unchecked")
	public ArjaProblem(Map<String, Object> parameters) throws Exception {
		super(parameters);

		weight = (Double) parameters.get("weight");
		if (weight == null)
			weight = 0.5;

		mu = (Double) parameters.get("mu");
		if (mu == null)
			mu = 0.06;

		numberOfObjectives = (Integer) parameters.get("numberOfObjectives");
		if (numberOfObjectives == null)
			numberOfObjectives = 2;

		initializationStrategy = (String) parameters.get("initializationStrategy");
		if (initializationStrategy == null)
			initializationStrategy = "Prior";

		miFilterRule = (Boolean) parameters.get("miFilterRule");
		if (miFilterRule == null)
			miFilterRule = true;

		maxNumberOfEdits = (Integer) parameters.get("maxNumberOfEdits");

		hallOfFameOutPath = (String) parameters.get("hallOfFameOutPath");

		hallOfFameInPath = (String) parameters.get("hallOfFameInPath");
		if (hallOfFameInPath != null) {
			fameSolutions = Files.readAllLines(Paths.get(hallOfFameInPath))
								.stream()
								.map(line -> gson.fromJson(line, ArjaSolutionSummary.class))
								.collect(Collectors.toSet());

			fameDecisionVariables = new LinkedHashSet<>();
			for (ArjaSolutionSummary solution: fameSolutions) {
				ArjaDecisionVariable var = decisionVar4Solution(solution);
				if (var != null) {
					fameDecisionVariables.add(var);
				}
			}

			System.out.format("Received %d hall-of-fame patches, successfully reconstructed %d\n",
                              fameSolutions.size(), fameDecisionVariables.size());
		}

		fameOutputRoot = (String) parameters.get("fameOutputRoot");
		famePatches = new LinkedHashSet<>();

		perfectPath = (String) parameters.get("perfectPath");
		if (perfectPath != null) {
			perfectSolutions = Files.readAllLines(Paths.get(perfectPath))
                                    .stream()
                                    .map(line -> gson.fromJson(line, ArjaSolutionSummary.class))
                                    .collect(Collectors.toSet());

			perfectDecisionVariables = new LinkedHashSet<>();
			for (ArjaSolutionSummary solution: perfectSolutions) {
				ArjaDecisionVariable var = decisionVar4Solution(solution);
				if (var != null) {
					perfectDecisionVariables.add(var);
				}
			}

			System.out.format("Received %d perfect patches, successfully reconstructed %d\n",
                              perfectSolutions.size(), perfectDecisionVariables.size());

		}

		setProblemParams();
	}

	ArjaDecisionVariable decisionVar4Solution(ArjaSolutionSummary solution)  {
		int size = modificationPoints.size();

		BitSet bits = new BitSet(size);
		int[] array = new int[2 * size];

		Set<ManipulationSummary> manipulations = solution.getManipulations();
		for (ManipulationSummary manip: manipulations) {
			List<Integer> gene = gene4Manipulation(manip);

			if (gene.isEmpty()) {
                return null;
			}

			int location = gene.get(0);
			bits.set(location);
			array[location] = gene.get(1);
			array[size + location] = gene.get(2);
		}

		return new ArjaDecisionVariable(bits, array);
	}

	List<Integer> gene4Manipulation(ManipulationSummary manipulation) {
        int locationIndex = -1;
		int manipIndex;
		int ingredientIndex = -1;

		ModificationPoint mp = null;

		StatementSummary locationSummary = manipulation.getLocationSummary();
		int size = modificationPoints.size();
		for (int i = 0; i < size; i++) {
			mp = modificationPoints.get(i);

			StatementSummary summary =  new StatementSummary(mp.getStatement());
			if (locationSummary.equals(summary)) {
				locationIndex = i;
				break;
			}
		}

		if (locationIndex == -1) {
			return new ArrayList<>();
		}

		manipIndex = availableManipulations.get(locationIndex).indexOf(manipulation.getManipName());

		if (manipIndex == -1) {
			return new ArrayList<>();
		}

        List<Statement> ingredients = mp.getIngredients();
		int numIngredients = ingredients.size();
		StatementSummary ingredientSummary = manipulation.getIngredientSummary();
		for (int i = 0; i < numIngredients; i++) {
			StatementSummary summary = new StatementSummary(ingredients.get(i));
			if (ingredientSummary.equals(summary)) {
                ingredientIndex = i;
				break;
			}
		}

		if (ingredientIndex == -1) {
			return new ArrayList<>();
		}

		return Arrays.asList(locationIndex, manipIndex, ingredientIndex);
	}

	public Set<ArjaDecisionVariable> getFameDecisionVariables() {
		return fameDecisionVariables;
	}

	public Set<ArjaDecisionVariable> getPerfectDecisionVariables() {
		return perfectDecisionVariables;
	}

	void setProblemParams() throws JMException {
		numberOfVariables_ = 2;
		numberOfObjectives_ = numberOfObjectives;
		numberOfConstraints_ = 0;
		problemName_ = "ArjaProblem";

		int size = modificationPoints.size();

		double[] prob = new double[size];
		if (initializationStrategy.equalsIgnoreCase("Prior")) {
			for (int i = 0; i < size; i++)
				prob[i] = modificationPoints.get(i).getSuspValue() * mu;
		} else if (initializationStrategy.equalsIgnoreCase("Random")) {
			for (int i = 0; i < size; i++)
				prob[i] = 0.5;
		} else {
			Configuration.logger_.severe("Initialization strategy " + initializationStrategy + " not found");
			throw new JMException("Exception in initialization strategy: " + initializationStrategy);
		}

		solutionType_ = new ArrayIntAndBinarySolutionType(this, size, prob);

		upperLimit_ = new double[2 * size];
		lowerLimit_ = new double[2 * size];
		for (int i = 0; i < size; i++) {
			lowerLimit_[i] = 0;
			upperLimit_[i] = availableManipulations.get(i).size() - 1;
		}

		for (int i = size; i < 2 * size; i++) {
			lowerLimit_[i] = 0;
			upperLimit_[i] = modificationPoints.get(i - size).getIngredients().size() - 1;
		}
	}

	@Override
	public void evaluate(Solution solution) throws JMException {
		// TODO Auto-generated method stub
		System.out.println("One fitness evaluation starts...");
		
		int[] array = ((ArrayInt) solution.getDecisionVariables()[0]).array_;
		BitSet bits = ((Binary) solution.getDecisionVariables()[1]).bits_;

		int size = modificationPoints.size();
		Map<String, ASTRewrite> astRewriters = new LinkedHashMap<String, ASTRewrite>();

		Map<Integer, Double> selectedMP = new LinkedHashMap<Integer, Double>();

		for (int i = 0; i < size; i++) {
			if (bits.get(i)) {
				double suspValue = modificationPoints.get(i).getSuspValue();
				if (miFilterRule) {
					String manipName = availableManipulations.get(i).get(array[i]);
					ModificationPoint mp = modificationPoints.get(i);

					Statement seed = null;
					if (!mp.getIngredients().isEmpty())
						seed = mp.getIngredients().get(array[i + size]);
					
					int index = MIFilterRule.canFiltered(manipName, seed, modificationPoints.get(i));				 
					if (index == -1)
						selectedMP.put(i, suspValue);
					else if (index < mp.getIngredients().size()) {
						array[i + size] = index;
						selectedMP.put(i, suspValue);
					}
					else 
						bits.set(i, false);
				} else
					selectedMP.put(i, suspValue);
			}
		}

		if (selectedMP.isEmpty()) {
			assignMaxObjectiveValues(solution);
			return;
		}

		int numberOfEdits = selectedMP.size();
		List<Map.Entry<Integer, Double>> list = new ArrayList<Map.Entry<Integer, Double>>(selectedMP.entrySet());

		if (maxNumberOfEdits != null && selectedMP.size() > maxNumberOfEdits) {
			Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
				@Override
				public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
					return o2.getValue().compareTo(o1.getValue());
				}
			});

			numberOfEdits = maxNumberOfEdits;
		}

		for (int i = 0; i < numberOfEdits; i++)
			manipulateOneModificationPoint(list.get(i).getKey(), size, array, astRewriters);

		for (int i = numberOfEdits; i < selectedMP.size(); i++)
			bits.set(list.get(i).getKey(), false);

		Map<String, String> modifiedJavaSources = getModifiedJavaSources(astRewriters);
		Map<String, JavaFileObject> compiledClasses = getCompiledClassesForTestExecution(modifiedJavaSources);

        ValidationStatus status = null;
		Set<String> failedTests = new LinkedHashSet<>();
		if (compiledClasses != null) {
			if (numberOfObjectives == 2 || numberOfObjectives == 3)
				solution.setObjective(0, numberOfEdits);
			try {
				ValidationResult result = invokeTestExecutor(compiledClasses, solution, useDefects4JInstrumentation);
				status = result.status;
				failedTests = result.failedTests;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			assignMaxObjectiveValues(solution);
			System.out.println("Compilation fails!");
		}

		if (status == ValidationStatus.PASS_ALL) {
			save(solution, modifiedJavaSources, compiledClasses, list, numberOfEdits);
		} else if (status == ValidationStatus.PASS_USER) {
			ArjaSolutionSummary summary = new ArjaSolutionSummary(bits, array, this);
			appendToHallOfFameOut(summary, globalID);
			if (fameOutputRoot != null) {
				saveAsFame(solution, modifiedJavaSources, compiledClasses, list, numberOfEdits, failedTests);
			}
		}

		evaluations++;
		System.out.println("One fitness evaluation is finished...");
	}

	void saveAsFame(Solution solution, Map<String, String> modifiedJavaSources, Map<String, JavaFileObject> compiledClasses,
					List<Map.Entry<Integer, Double>> list, int numberOfEdits, Set<String> failedTests) {
		List<Integer> opList = new ArrayList<Integer>();
		List<Integer> locList = new ArrayList<Integer>();
		List<Integer> ingredList = new ArrayList<Integer>();

		int[] var0 = ((ArrayInt) solution.getDecisionVariables()[0]).array_;
		int size = var0.length / 2;

		for (int i = 0; i < numberOfEdits; i++) {
			int loc = list.get(i).getKey();
			int op = var0[loc];
			int ingred = var0[loc + size];
			opList.add(op);
			locList.add(loc);
			ingredList.add(ingred);
		}

		BitSet bits = ((Binary) solution.getDecisionVariables()[1]).bits_;
		ArjaSolutionSummary summary = new ArjaSolutionSummary(bits, var0, this);

		try {
			if (addFamePatch(opList, locList, ingredList)) {
				if (diffFormat) {
					try {
						IO.savePatch(modifiedJavaSources, srcJavaDir, this.fameOutputRoot, globalID);

						PrintStream ps = new PrintStream(
								Files.newOutputStream(
										Paths.get(fameOutputRoot, "Patch_" + globalID, "failed_tests")));
						ps.print(String.join("\n", failedTests));

						ps = new PrintStream(
								Files.newOutputStream(Paths.get(fameOutputRoot, "Patch_" + globalID, "summary")));
						ps.println(gson.toJson(summary));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				saveFamePatch(opList, locList, ingredList);
				globalID++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public boolean addFamePatch(List<Integer> opList, List<Integer> locList, List<Integer> ingredList) {
		Patch patch = new Patch(opList, locList, ingredList, modificationPoints, availableManipulations);
		return famePatches.add(patch);
	}

	public void saveFamePatch(List<Integer> opList, List<Integer> locList, List<Integer> ingredList) throws IOException {
		long estimatedTime = System.currentTimeMillis() - launchTime;
		if (fameOutputRoot != null)
			IO.savePatch(opList, locList, ingredList, modificationPoints, availableManipulations, fameOutputRoot,
					globalID, evaluations, estimatedTime);
	}

	void save(Solution solution, Map<String, String> modifiedJavaSources, Map<String, JavaFileObject> compiledClasses,
			List<Map.Entry<Integer, Double>> list, int numberOfEdits) {
		List<Integer> opList = new ArrayList<Integer>();
		List<Integer> locList = new ArrayList<Integer>();
		List<Integer> ingredList = new ArrayList<Integer>();

		int[] var0 = ((ArrayInt) solution.getDecisionVariables()[0]).array_;
		int size = var0.length / 2;

		for (int i = 0; i < numberOfEdits; i++) {
			int loc = list.get(i).getKey();
			int op = var0[loc];
			int ingred = var0[loc + size];
			opList.add(op);
			locList.add(loc);
			ingredList.add(ingred);
		}

		BitSet bits = ((Binary) solution.getDecisionVariables()[1]).bits_;
		ArjaSolutionSummary summary = new ArjaSolutionSummary(bits, var0, this);

		try {
			if (addTestAdequatePatch(opList, locList, ingredList)) {
				if (diffFormat) {
					try {
						IO.savePatch(modifiedJavaSources, srcJavaDir, this.patchOutputRoot, globalID);

						PrintStream ps = new PrintStream(
								Files.newOutputStream(Paths.get(patchOutputRoot, "Patch_" + globalID, "summary")));
						ps.println(gson.toJson(summary));
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				saveTestAdequatePatch(opList, locList, ingredList);
				globalID++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean manipulateOneModificationPoint(int i, int size, int array[], Map<String, ASTRewrite> astRewriters)
			throws JMException {
		ModificationPoint mp = modificationPoints.get(i);
		String manipName = availableManipulations.get(i).get(array[i]);

		Statement ingredStatement = null;
		if (!mp.getIngredients().isEmpty())
			ingredStatement = mp.getIngredients().get(array[i + size]);

		return manipulateOneModificationPoint(mp, manipName, ingredStatement, astRewriters);
	}

	enum ValidationStatus {PASS_ALL, PASS_USER, FAIL_USER, EXCEPTION}

	public class ValidationResult {
		public ValidationStatus status;
		public Set<String> failedTests;

		public ValidationResult(ValidationStatus status, Set<String> failedTests) {
			this.status = status;
			this.failedTests = failedTests;
		}
	}

	ValidationResult invokeTestExecutor(Map<String, JavaFileObject> compiledClasses, Solution solution,
							   boolean enableDefects4jInstrumentation) throws Exception {
		Set<String> samplePosTests = getSamplePositiveTests();
		ITestExecutor testExecutor = getTestExecutor(compiledClasses, samplePosTests);

		if (enableDefects4jInstrumentation) {
			((ExternalTestExecutor) testExecutor).enableDefects4jInstrumentation();
		}

		boolean status = testExecutor.runTests();

		if (status && percentage != null && percentage < 1) {
			testExecutor = getTestExecutor(compiledClasses, positiveTests);

			if (enableDefects4jInstrumentation) {
				((ExternalTestExecutor) testExecutor).enableDefects4jInstrumentation();
			}

			status = testExecutor.runTests();
		}

		if (testExecutor.isTimeout()) {
			assignMaxObjectiveValues(solution);
			numberOfTimeouts++;
			System.out.println("Timeout occurs!");
			adjustWaitTime();
			return new ValidationResult(ValidationStatus.EXCEPTION, testExecutor.getFailedTests().keySet());
		}
		else if (testExecutor.isIOExceptional()) {
			assignMaxObjectiveValues(solution);
			System.out.println("IO Exception occurs!");
			adjustWaitTime();
			return new ValidationResult(ValidationStatus.EXCEPTION, testExecutor.getFailedTests().keySet());
		} else {
			double ratioOfFailuresInPositive = testExecutor.getRatioOfFailuresInPositive();
			double ratioOfFailuresInNegative = testExecutor.getRatioOfFailuresInNegative();
			double fitness = weight * testExecutor.getRatioOfFailuresInPositive()
					+ testExecutor.getRatioOfFailuresInNegative();
			
			System.out.println("Number of failed tests: "
					+ (testExecutor.getFailureCountInNegative() + testExecutor.getFailureCountInPositive()));
			System.out.println("Weighted failure rate: " + fitness);
			
			if (numberOfObjectives == 1 || numberOfObjectives == 2) 
				solution.setObjective(numberOfObjectives - 1, fitness);
			else {
				solution.setObjective(1, ratioOfFailuresInPositive);
				solution.setObjective(2, ratioOfFailuresInNegative);
			}

            adjustWaitTime();

			Set<String> failures = testExecutor.getFailedTests().keySet();
			if (failures.isEmpty()) {
				return new ValidationResult(ValidationStatus.PASS_ALL, failures);
			}
			if (failures.stream().noneMatch(this::isUserTest)) {
				return new ValidationResult(ValidationStatus.PASS_USER, failures);
			}
			return new ValidationResult(ValidationStatus.FAIL_USER, failures);
		}
	}

	public void appendToHallOfFameOut(ArjaSolutionSummary summary, int globalId) {
        hallOfFameOut.add(summary);
		if (hallOfFameOutPath != null) {
			try {
				PrintStream ps = new PrintStream(new FileOutputStream(hallOfFameOutPath, true));
				ps.format("{\"globalID\": %d, \"summary\": %s}\n", globalID, gson.toJson(summary));
				System.out.println("Patch " + globalID + " added to hall of fame");
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

	ValidationResult invokeTestExecutor(Map<String, JavaFileObject> compiledClasses, Solution solution) throws Exception {
		return invokeTestExecutor(compiledClasses, solution, false);
	}

	void assignMaxObjectiveValues(Solution solution) {
		for (int i = 0; i < solution.getNumberOfObjectives(); i++)
			solution.setObjective(i, Double.MAX_VALUE);
	}

	public Boolean getMiFilterRule() {
		return miFilterRule;
	}

	public Integer getMaxNumberOfEdits() {
		return maxNumberOfEdits;
	}

	public Double getWeight() {
		return weight;
	}

	public String getInitializationStrategy() {
		return initializationStrategy;
	}

	public Double getMu() {
		return mu;
	}

	public String getFameOutputRoot() {
		return fameOutputRoot;
	}
}
