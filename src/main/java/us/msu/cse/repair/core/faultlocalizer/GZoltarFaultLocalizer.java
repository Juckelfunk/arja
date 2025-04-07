package us.msu.cse.repair.core.faultlocalizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.gzoltar.core.GZoltar;
import com.gzoltar.core.components.Statement;
import com.gzoltar.core.instr.testing.TestResult;
import com.gzoltar.core.components.Component;
import com.gzoltar.core.components.count.ComponentCount;

import us.msu.cse.repair.core.parser.LCNode;

public class GZoltarFaultLocalizer implements IFaultLocalizer {
	Set<String> positiveTestMethods;
	Set<String> negativeTestMethods;

	Map<LCNode, Double> faultyLines;

	List<TestResult> testResults;
	Map<String, Set<LCNode>> testsCoverage;

	public GZoltarFaultLocalizer(Set<String> binJavaClasses, Set<String> binExecuteTestClasses, String binJavaDir,
			String binTestDir, Set<String> dependences) throws FileNotFoundException, IOException {
		String projLoc = new File("").getAbsolutePath();
		GZoltar gz = new GZoltar(projLoc);

		gz.getClasspaths().add(binJavaDir);
		gz.getClasspaths().add(binTestDir);

		if (dependences != null)
			gz.getClasspaths().addAll(dependences);

		for (String testClass : binExecuteTestClasses)
			gz.addTestToExecute(testClass);

		for (String javaClass : binJavaClasses)
			gz.addClassToInstrument(javaClass);

		gz.run();

		positiveTestMethods = new LinkedHashSet<String>();
		negativeTestMethods = new LinkedHashSet<String>();

		testResults = gz.getTestResults().stream()
                                         .filter(tr -> !tr.getName().startsWith("junit.framework"))
									     .collect(Collectors.toList());

		for (TestResult tr : testResults) {
			String testName = tr.getName();
			if (tr.wasSuccessful())
				positiveTestMethods.add(testName);
			else {
                negativeTestMethods.add(testName);
			}
		}

		faultyLines = new LinkedHashMap<LCNode, Double>();
		for (Statement gzoltarStatement : gz.getSuspiciousStatements()) {
			String className = gzoltarStatement.getMethod().getParent().getLabel();
			int lineNumber = gzoltarStatement.getLineNumber();

			double suspValue = gzoltarStatement.getSuspiciousness();

			LCNode lcNode = new LCNode(className, lineNumber);
			faultyLines.put(lcNode, suspValue);
		}
	}

	@Override
	public Map<LCNode, Double> searchSuspicious(double thr) {
		// TODO Auto-generated method stub
		Map<LCNode, Double> partFaultyLines = new LinkedHashMap<LCNode, Double>();
		for (Map.Entry<LCNode, Double> entry : faultyLines.entrySet()) {
			if (entry.getValue() >= thr)
				partFaultyLines.put(entry.getKey(), entry.getValue());
		}
		return partFaultyLines;
	}

	@Override
	public Set<String> getPositiveTests() {
		// TODO Auto-generated method stub
		return this.positiveTestMethods;
	}

	@Override
	public Set<String> getNegativeTests() {
		// TODO Auto-generated method stub
		return this.negativeTestMethods;
	}

	@Override
	public Map<String, Set<LCNode>> getTestsCoverage() {
		if (testsCoverage == null) {
			testsCoverage = new LinkedHashMap<>();
			for (TestResult tr: testResults) {
				String test = tr.getName();

//				if (testsCoverage.containsKey(test)) {
//					throw new RuntimeException(String.format("repeated test in spectrum: %s", test));
//				}
				Set<LCNode> coveredNodes = new LinkedHashSet<>();

				for (ComponentCount cc : tr.getCoveredComponents()) {
					Component component = cc.getComponent();
					if (component instanceof com.gzoltar.core.components.Statement) {

						com.gzoltar.core.components.Statement s = (com.gzoltar.core.components.Statement) component;
						String className = s.getMethod().getParent().getLabel();
						int lineNumber = s.getLineNumber();

						coveredNodes.add(new LCNode(className, lineNumber));
					}
				}

				testsCoverage.put(test, coveredNodes);
			}
		}
		return testsCoverage;
	}

	public List<TestResult> getTestResults() {
		return testResults;
	}
}
