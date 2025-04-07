package us.msu.cse.repair.core.faultlocalizer;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import us.msu.cse.repair.core.parser.LCNode;

public class GZoltarFaultLocalizer2 implements IFaultLocalizer {
	Set<String> positiveTestMethods;
	Set<String> negativeTestMethods;

	Map<LCNode, Double> faultyLines;
	Map<String, Set<LCNode>> testsCoverage;

	public GZoltarFaultLocalizer2(String gzoltarDataDir) throws IOException {
		testsCoverage = new LinkedHashMap<>();
		positiveTestMethods = new LinkedHashSet<String>();
		negativeTestMethods = new LinkedHashSet<String>();
		File testFile = new File(gzoltarDataDir, "tests");
		List<String> allTestMethods = FileUtils.readLines(testFile, "UTF-8");
		for (int i = 1; i < allTestMethods.size(); i++) {
			String info[] = allTestMethods.get(i).trim().split(",");
			String test = info[0].trim();
			if (info[1].trim().equals("PASS"))
				positiveTestMethods.add(test);
			else
				negativeTestMethods.add(test);

			if (testsCoverage.containsKey(test)) {
				throw new RuntimeException(String.format("%s appeared more than once in %s",
                                                         test, testFile.getAbsolutePath()));
			}
			Set<LCNode> coveredNodes = new LinkedHashSet<>();
			for (int j = 2; j < info.length; j++) {
				String[] tmp = info[j].trim().split(":");
				coveredNodes.add(new LCNode(tmp[0], Integer.parseInt(tmp[1])));
			}
			testsCoverage.put(test, coveredNodes);
		}

		faultyLines = new LinkedHashMap<LCNode, Double>();
		File spectraFile = new File(gzoltarDataDir, "spectra");
		List<String> fLines = FileUtils.readLines(spectraFile, "UTF-8");
		for (int i = 1; i < fLines.size(); i++) {
			String line = fLines.get(i).trim();

			int startIndex = line.indexOf('<');
			int endIndex = line.indexOf('{');
			String className = line.substring(startIndex + 1, endIndex);

			String[] info = line.split("#")[1].split(",");
			int lineNumber = Integer.parseInt(info[0].trim());
			double suspValue = Double.parseDouble(info[1].trim());

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
		return testsCoverage;
	}
}
