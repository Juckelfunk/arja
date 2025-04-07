package us.msu.cse.repair;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.RandomGenerator;

public class Interpreter {
	
	public static LinkedHashMap<String, Object> getBasicParameterSetting(Map<String, String> parameterStrs) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<String, Object>();
	
		String binJavaDir = parameterStrs.get("binJavaDir");
		parameters.put("binJavaDir", binJavaDir);
		
		String binTestDir = parameterStrs.get("binTestDir");
		parameters.put("binTestDir", binTestDir);
		
		String srcJavaDir = parameterStrs.get("srcJavaDir");
		parameters.put("srcJavaDir", srcJavaDir);

		String srcVersion = parameterStrs.get("srcVersion");
		parameters.put("srcVersion", srcVersion);
		
		String dependencesS = parameterStrs.get("dependences");	
		if (dependencesS != null) {
			Set<String> dependences = new LinkedHashSet<String>();
			String strs[] = dependencesS.split(":");
			for (String st : strs)
				dependences.add(st.trim());
			parameters.put("dependences", dependences);	
		}

		String testFilteredS = parameterStrs.get("testFiltered");
		if (testFilteredS != null) {
			parameters.put("testFiltered", Boolean.valueOf(testFilteredS));
		}

		String testsS = parameterStrs.get("tests");	
		if (testsS != null) {
			Set<String> tests = new LinkedHashSet<String>();
			String strs[] = testsS.split(":");
			for (String st : strs)
				tests.add(st.trim());
			parameters.put("tests", tests);	
		}	

		String testNamesPath = parameterStrs.get("testNamesPath");
		parameters.put("testNamesPath", testNamesPath);

		String orgPosTestsInfoPath = parameterStrs.get("orgPosTestsInfoPath");
		parameters.put("orgPosTestsInfoPath", orgPosTestsInfoPath);

		String spectraPath = parameterStrs.get("spectraPath");
		parameters.put("spectraPath", spectraPath);

		String finalTestsInfoPath = parameterStrs.get("finalTestsInfoPath");
		parameters.put("finalTestsInfoPath", finalTestsInfoPath);

		String filterTestsOnlyS = parameterStrs.get("filterTestsOnly");
		parameters.put("filterTestsOnly", Boolean.valueOf(filterTestsOnlyS));

		String spectraOnlyS = parameterStrs.get("spectraOnly");
		parameters.put("spectraOnly", Boolean.valueOf(spectraOnlyS));

		String oracleLocationsFile = parameterStrs.get("oracleLocationsFile");
		parameters.put("oracleLocationsFile", oracleLocationsFile);

		String additionalTestsInfoPath = parameterStrs.get("additionalTestsInfoPath");
		parameters.put("additionalTestsInfoPath", additionalTestsInfoPath);

		String hallOfFameOutPath = parameterStrs.get("hallOfFameOutPath");
		parameters.put("hallOfFameOutPath", hallOfFameOutPath);

		String hallOfFameInPath = parameterStrs.get("hallOfFameInPath");
		parameters.put("hallOfFameInPath", hallOfFameInPath);

		String perfectPath = parameterStrs.get("perfectPath");
		parameters.put("perfectPath", perfectPath);

		String thrS = parameterStrs.get("thr");
		if (thrS != null) {
			double thr = Double.parseDouble(thrS);
			parameters.put("thr", thr);
		}

		String external = parameterStrs.get("externalProjRoot");
		if (external != null) {
			parameters.put("externalProjRoot", external);
		}
		
		String maxNumberOfModificationPointsS = parameterStrs.get("maxNumberOfModificationPoints");
		if (maxNumberOfModificationPointsS != null) {
			int maxNumberOfModificationPoints = Integer.parseInt(maxNumberOfModificationPointsS);
			parameters.put("maxNumberOfModificationPoints", maxNumberOfModificationPoints);	
		}
		
		String jvmPathS = parameterStrs.get("jvmPath");
		if (jvmPathS != null)
			parameters.put("jvmPath", jvmPathS);
		
		
		String binWorkingRootS = parameterStrs.get("binWorkingRoot");
		if (binWorkingRootS != null)
			parameters.put("binWorkingRoot", binWorkingRootS);
		
		String testExecutorNameS = parameterStrs.get("testExecutorName");
		if (testExecutorNameS != null)
			parameters.put("testExecutorName", testExecutorNameS);
		
		String waitTimeS = parameterStrs.get("waitTime");
		if (waitTimeS != null) {
			int waitTime = Integer.parseInt(waitTimeS);
			parameters.put("waitTime", waitTime);
		}   
		
		String patchOutputRootS = parameterStrs.get("patchOutputRoot");
		if (patchOutputRootS != null)
			parameters.put("patchOutputRoot", patchOutputRootS);

		String fameOutputRootS = parameterStrs.get("fameOutputRoot");
		if (fameOutputRootS != null)
			parameters.put("fameOutputRoot", fameOutputRootS);

		String gzoltarDataDirS = (String) parameterStrs.get("gzoltarDataDir");
		if (gzoltarDataDirS != null)
			parameters.put("gzoltarDataDir", gzoltarDataDirS);
			
		String ingredientModeS = parameterStrs.get("ingredientMode");
		if (ingredientModeS != null)
			parameters.put("ingredientMode", ingredientModeS);
		
		String diffFormatS = parameterStrs.get("diffFormat");
		if (diffFormatS != null) {
			boolean diffFormat = Boolean.parseBoolean(diffFormatS);
			parameters.put("diffFormat", diffFormat);
		}

		String weightS = parameterStrs.get("weight");
		if (weightS != null) {
			parameters.put("weight", Double.parseDouble(weightS));
		}

		String seed_str = parameterStrs.get("seed");
		double seed = 0.0;
		if (seed_str != null) {
			int tmp = Integer.parseInt(seed_str);
			// the seed of RandomGenerator should fall in [0, 1)
			seed = (tmp / (Integer.MAX_VALUE + 1.0) + 1.0) / 2;
			assert seed >= 0;
			assert seed < 1;
		}
		System.out.format("Using converted seed %.18f\n", seed);
		try {
			PseudoRandom.randDouble();

			Field random_1 = PseudoRandom.class.getDeclaredField("random_");
			random_1.setAccessible(true);
			Object random_ = random_1.get(null);
			Field seed_field = random_.getClass().getDeclaredField("seed");
			seed_field.setAccessible(true);
			seed_field.set(random_, seed);

			Method randomize = RandomGenerator.class.getDeclaredMethod("randomize");
			randomize.setAccessible(true);
			randomize.invoke(random_);
		} catch (Exception e) {
			e.printStackTrace();
		}

		String useD4JInstrS = parameterStrs.get("useD4JInstr");
		if (useD4JInstrS != null) {
			boolean useD4JInstr = Boolean.parseBoolean(useD4JInstrS);
			parameters.put("useD4JInstr", useD4JInstr);
		}


		return parameters;
	}
	
	public static LinkedHashMap<String, String> getParameterStrings(String args[]) throws JMException {
		LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-D")) {
				if (i + 1 >= args.length)
					throw new JMException("The command is invalid!");
				parameters.put(args[i].trim().substring(2), args[i + 1].trim());
			}
		}
		
		return parameters;
	}
}
