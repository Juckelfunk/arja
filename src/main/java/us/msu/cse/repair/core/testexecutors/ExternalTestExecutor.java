package us.msu.cse.repair.core.testexecutors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import us.msu.cse.repair.core.util.ProcessWithTimeout;
import us.msu.cse.repair.core.util.StreamReaderThread;

public class ExternalTestExecutor implements ITestExecutor {
	String binJavaDir;
	String binTestDir;
	Set<String> dependences;

	String binWorkingDir;

	String externalProjRoot;

	Set<String> positiveTests;
	Set<String> negativeTests;
	String finalTestsInfoPath;

	String jvmPath;

	int waitTime;
	boolean isIOExceptional;
	boolean isTimeout;

	int failuresInPositive;
	int failuresInNegative;

	Set<String> failedTests;

	boolean defects4jInstrumentation = false;

	final int MAX = 300;

	public ExternalTestExecutor(Set<String> positiveTests, Set<String> negativeTests, String finalTestsInfoPath,
			String binJavaDir, String binTestDir, Set<String> dependences, String binWorkingDir,
			String externalProjRoot, String jvmPath, int waitTime) {
		this.positiveTests = positiveTests;
		this.negativeTests = negativeTests;
		this.finalTestsInfoPath = finalTestsInfoPath;

		this.binJavaDir = binJavaDir;
		this.binTestDir = binTestDir;
		this.dependences = dependences;
		this.externalProjRoot = externalProjRoot;

		this.binWorkingDir = binWorkingDir;

		this.jvmPath = jvmPath;

		this.waitTime = waitTime;

		this.failuresInPositive = 0;
		this.failuresInNegative = 0;
		
		this.isTimeout = false;
		this.isIOExceptional = false;
	}

	@Override
	public boolean runTests() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		List<String> params = new ArrayList<String>();
		params.add(jvmPath);
		params.add("-Djava.awt.headless=true");
		params.add("-cp");

		String cpStr = "";
		cpStr += (binWorkingDir + File.pathSeparator);
		cpStr += (binJavaDir + File.pathSeparator);
		cpStr += (binTestDir + File.pathSeparator);
		cpStr += new File(externalProjRoot, "bin").getCanonicalPath();
		if (dependences != null) {
			for (String dp : dependences)
				cpStr += (File.pathSeparator + dp);
		}
		cpStr += (File.pathSeparator + new File(externalProjRoot, "lib/junit-4.12.jar").getCanonicalPath());
		cpStr += (File.pathSeparator + new File(externalProjRoot, "lib/hamcrest-core-1.3.jar").getCanonicalPath());
		params.add(cpStr);

		if (isDefects4jInstrumentationEnabled()) {
			params.add("-Ddefects4j.instrumentation.enabled=true");
		}

		params.add("us.msu.cse.repair.external.junit.JUnitTestRunner");

		if (finalTestsInfoPath != null)
			params.add("@" + finalTestsInfoPath);
		else {
			String testStrs = "";
			for (String test : positiveTests)
				testStrs += (test + File.pathSeparator);
			for (String test : negativeTests)
				testStrs += (test + File.pathSeparator);
			params.add(testStrs);
		}

		ProcessBuilder builder = new ProcessBuilder(params);
		builder.redirectOutput();
		builder.redirectErrorStream(true);
		builder.directory();
		builder.environment().put("TZ", "America/Los_Angeles");

		Process process = builder.start();

		StreamReaderThread streamReaderThread = new StreamReaderThread(process.getInputStream());
		streamReaderThread.start();

		ProcessWithTimeout processWithTimeout = new ProcessWithTimeout(process);
		int exitCode = processWithTimeout.waitForProcess(waitTime);

		streamReaderThread.join();
		
/*		for (String st : streamReaderThread.getOutput())
			System.out.println(st);*/
		
		if (exitCode != 0) {
			isTimeout = true;
			return false;
		}

		if (streamReaderThread.isIOExceptional()) {
			isIOExceptional = true;
			return false;
		}

		List<String> output = streamReaderThread.getOutput();

		failedTests = new LinkedHashSet<String>();
		for (String str : output) {
			if (str.startsWith("FailedTest"))
				failedTests.add(str.split(":")[1].trim());
		}

		System.out.format("FailedTest: %s\n", String.join(",", failedTests));

		for (String test : failedTests) {
			if (negativeTests.contains(test))
				failuresInNegative++;
			else
				failuresInPositive++;
		}
		return failedTests.isEmpty();
	}

	@Override
	public int getFailureCountInPositive() {
		// TODO Auto-generated method stub
		return this.failuresInPositive;
	}

	@Override
	public int getFailureCountInNegative() {
		// TODO Auto-generated method stub
		return this.failuresInNegative;
	}

	@Override
	public double getRatioOfFailuresInPositive() {
		// TODO Auto-generated method stub
		if (!positiveTests.isEmpty())
			return (double) failuresInPositive / positiveTests.size();
		else
			return 0;
	}

	@Override
	public double getRatioOfFailuresInNegative() {
		// TODO Auto-generated method stub
		if (!negativeTests.isEmpty())
			return (double) failuresInNegative / negativeTests.size();
		else
			return 0;
	}

	@Override
	public boolean isIOExceptional() {
		// TODO Auto-generated method stub
		return this.isIOExceptional;
	}
	
	@Override
	public boolean isTimeout() {
		// TODO Auto-generated method stub
		return this.isTimeout;
	}

	@Override
	public Map<String, Double> getFailedTests() {
		// TODO Auto-generated method stub
		Map<String, Double> map = new LinkedHashMap<>();
		for (String test : failedTests) 
			map.put(test, 1.0);
		return map;
	}

	public void enableDefects4jInstrumentation() {
		defects4jInstrumentation = true;
	}

	public void disableDefects4jInstrumentation() {
		defects4jInstrumentation = false;
	}

	public boolean isDefects4jInstrumentationEnabled() {
		return defects4jInstrumentation;
	}
}
