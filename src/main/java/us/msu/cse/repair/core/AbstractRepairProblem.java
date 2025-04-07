package us.msu.cse.repair.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.tools.JavaFileObject;

import com.google.gson.Gson;
import com.gzoltar.core.components.Component;
import com.gzoltar.core.components.count.ComponentCount;
import com.gzoltar.core.instr.testing.TestResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import jmetal.core.Problem;
import jmetal.metaheuristics.moead.Utils;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import us.msu.cse.repair.core.compiler.JavaJDKCompiler;
import us.msu.cse.repair.core.coverage.SeedLineGeneratorProcess;
import us.msu.cse.repair.core.coverage.TestFilterProcess;
import us.msu.cse.repair.core.faultlocalizer.*;
import us.msu.cse.repair.core.filterrules.IngredientFilterRule;
import us.msu.cse.repair.core.filterrules.ManipulationFilterRule;
import us.msu.cse.repair.core.instrumentation.TestInstrumenterProcess;
import us.msu.cse.repair.core.manipulation.AbstractManipulation;
import us.msu.cse.repair.core.manipulation.ManipulationFactory;
import us.msu.cse.repair.core.parser.FieldVarDetector;
import us.msu.cse.repair.core.parser.FileASTRequestorImpl;
import us.msu.cse.repair.core.parser.LCNode;
import us.msu.cse.repair.core.parser.LocalVarDetector;
import us.msu.cse.repair.core.parser.MethodDetector;
import us.msu.cse.repair.core.parser.ModificationPoint;
import us.msu.cse.repair.core.parser.SeedStatement;
import us.msu.cse.repair.core.parser.SeedStatementInfo;
import us.msu.cse.repair.core.parser.ingredient.AbstractIngredientScreener;
import us.msu.cse.repair.core.parser.ingredient.IngredientMode;
import us.msu.cse.repair.core.parser.ingredient.IngredientScreenerFactory;
import us.msu.cse.repair.core.testexecutors.ExternalTestExecutor;
import us.msu.cse.repair.core.testexecutors.ExternalTestExecutor2;
import us.msu.cse.repair.core.testexecutors.ITestExecutor;
import us.msu.cse.repair.core.testexecutors.InternalTestExecutor;
import us.msu.cse.repair.core.util.ClassFinder;
import us.msu.cse.repair.core.util.CustomURLClassLoader;
import us.msu.cse.repair.core.util.Helper;
import us.msu.cse.repair.core.util.IO;
import us.msu.cse.repair.core.util.Patch;

public abstract class AbstractRepairProblem extends Problem {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected String[] manipulationNames;

	protected Double percentage;
	protected Double thr;

	protected Integer maxNumberOfModificationPoints;

	protected List<ModificationPoint> modificationPoints;
	protected List<List<String>> availableManipulations;

	protected Map<String, CompilationUnit> sourceASTs;
	protected Map<String, String> sourceContents;

	protected Map<SeedStatement, SeedStatementInfo> seedStatements;
	protected Map<String, ITypeBinding> declaredClasses;
	
	protected Map<IMethodBinding, MethodDeclaration> methodDeclarations;

	protected Map<LCNode, Double> faultyLines;
	protected String faultyLinesInfoPath;

	protected Set<LCNode> seedLines;

	protected Set<String> positiveTests;
	protected Set<String> negativeTests;
	protected Set<String> userPositiveTests;
	protected Set<String> userNegativeTests;

	protected Boolean testFiltered;
	protected String orgPosTestsInfoPath;
	protected String finalTestsInfoPath;
	protected String spectraPath;
	protected Boolean filterTestsOnly;
	protected Boolean spectraOnly;

	protected Map<String, Set<LCNode>> testsCoverage;

	protected String srcJavaDir;

	protected String srcVersion;

	protected String binJavaDir;
	protected String binTestDir;
	protected String binInsTestDir;
	
	
	protected Set<String> dependences;

	protected String additionalTestsInfoPath;
	protected Set<String> additionalTests;

	protected Set<String> allTests;

	protected String externalProjRoot;

	protected String binWorkingRoot;

	protected Set<String> binJavaClasses;
	protected Set<String> binExecuteTestClasses;
	protected String testNamesPath;
	protected String javaClassesInfoPath;
	protected String testClassesInfoPath;

	protected Integer waitTime;


	protected String patchOutputRoot;

	protected String testExecutorName;

	protected String ingredientScreenerName;
	protected IngredientMode ingredientMode;

	protected Boolean ingredientFilterRule;
	protected Boolean manipulationFilterRule;

	protected String oracleLocationsFile;
	protected Map<String, NavigableSet<Integer>> oracleLocations;

	protected Boolean seedLineGenerated;
	
	protected Boolean diffFormat;

	protected String jvmPath;
	protected List<String> compilerOptions;

	protected URL[] progURLs;
	
	protected String gzoltarDataDir;

	protected static int globalID;
	protected Set<Patch> patches;

	protected static long launchTime;
	protected static int evaluations;

	public Boolean useDefects4JInstrumentation;

	protected Double repSim;
	protected Double insRel;
	
	protected static int numberOfTimeouts;
	protected final int maxAllowedWaitTime = 30000;

	private final static List<String> srcVersions = Arrays.asList(
			JavaCore.VERSION_1_1, JavaCore.VERSION_1_2, JavaCore.VERSION_1_3,
			JavaCore.VERSION_1_4, JavaCore.VERSION_1_5, JavaCore.VERSION_1_6,
			JavaCore.VERSION_1_7, JavaCore.VERSION_1_8, JavaCore.VERSION_CLDC_1_1
	);

	@SuppressWarnings("unchecked")
	public AbstractRepairProblem(Map<String, Object> parameters) throws Exception {
		binJavaDir = (String) parameters.get("binJavaDir");
		binTestDir = (String) parameters.get("binTestDir");
		srcJavaDir = (String) parameters.get("srcJavaDir");
		dependences = (Set<String>) parameters.get("dependences");

		srcVersion = (String) parameters.get("srcVersion");
		if (srcVersion == null)
			srcVersion = JavaCore.VERSION_1_7;

		additionalTestsInfoPath = (String) parameters.get("additionalTestsInfoPath");

		binExecuteTestClasses = (Set<String>) parameters.get("tests");
		testNamesPath = (String) parameters.get("testNamesPath");

		percentage = (Double) parameters.get("percentage");

		javaClassesInfoPath = (String) parameters.get("javaClassesInfoPath");
		testClassesInfoPath = (String) parameters.get("testClassesInfoPath");
		
		faultyLinesInfoPath = (String) parameters.get("faultyLinesInfoPath");
	
		gzoltarDataDir = (String) parameters.get("gzoltarDataDir");
		
		String id = Helper.getRandomID();
		
		thr = (Double) parameters.get("thr");
		if (thr == null)
			thr = 0.1;
		
		maxNumberOfModificationPoints = (Integer) parameters.get("maxNumberOfModificationPoints");
		if (maxNumberOfModificationPoints == null)
			maxNumberOfModificationPoints = 40;

		jvmPath = (String) parameters.get("jvmPath");
		if (jvmPath == null)
			jvmPath = System.getProperty("java.home") + "/bin/java";

		externalProjRoot = (String) parameters.get("externalProjRoot");
		if (externalProjRoot == null)
			externalProjRoot = new File("external").getCanonicalPath();

		binWorkingRoot = (String) parameters.get("binWorkingRoot");
		if (binWorkingRoot == null)
			binWorkingRoot = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "working_" + id;
		
		patchOutputRoot = (String) parameters.get("patchOutputRoot");
		if (patchOutputRoot == null)
			patchOutputRoot = "patches_" + id;
		
		binInsTestDir = (String) parameters.get("binInsTestDir");
		if (binInsTestDir == null) 
			binInsTestDir = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "binTest_" + id;
		
		orgPosTestsInfoPath = (String) parameters.get("orgPosTestsInfoPath");
		if (orgPosTestsInfoPath == null)
			orgPosTestsInfoPath = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "orgTests_" + id + ".txt";
		
		finalTestsInfoPath = (String) parameters.get("finalTestsInfoPath");
		if (finalTestsInfoPath == null)
			finalTestsInfoPath = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "finalTests_" + id + ".txt";

		spectraPath = (String) parameters.get("spectraPath");

		filterTestsOnly = (Boolean) parameters.get("filterTestsOnly");
		if (filterTestsOnly == null)
			filterTestsOnly = false;

		spectraOnly = (Boolean) parameters.get("spectraOnly");
		if (spectraOnly == null)
			spectraOnly = false;

		manipulationNames = (String[]) parameters.get("manipulationNames");
		if (manipulationNames == null)
			manipulationNames = new String[] { "Delete", "Replace", "InsertBefore" };

		testExecutorName = (String) parameters.get("testExecutorName");
		if (testExecutorName == null)
			testExecutorName = "ExternalTestExecutor";

		ingredientScreenerName = (String) parameters.get("ingredientScreenerName");
		if (ingredientScreenerName == null)
			ingredientScreenerName = "Direct";

		String modeStr = (String) parameters.get("ingredientMode");
		if (modeStr == null)
			ingredientMode = IngredientMode.Package;
		else
			ingredientMode = IngredientMode.valueOf(modeStr);
		
		diffFormat = (Boolean) parameters.get("diffFormat");
		if (diffFormat == null)
			diffFormat = false;
		

		testFiltered = (Boolean) parameters.get("testFiltered");
		if (testFiltered == null)
			testFiltered = true;

		waitTime = (Integer) parameters.get("waitTime");
		if (waitTime == null)
			waitTime = 6000;

		seedLineGenerated = (Boolean) parameters.get("seedLineGenerated");
		if (seedLineGenerated == null)
			seedLineGenerated = true;

		manipulationFilterRule = (Boolean) parameters.get("manipulationFilterRule");
		if (manipulationFilterRule == null)
			manipulationFilterRule = true;

		ingredientFilterRule = (Boolean) parameters.get("ingredientFilterRule");
		if (ingredientFilterRule == null)
			ingredientFilterRule = true;

		oracleLocationsFile = (String) parameters.get("oracleLocationsFile");
		oracleLocations = new LinkedHashMap<>();
        if (oracleLocationsFile != null) {
			String jsonText = new String(Files.readAllBytes(Paths.get(oracleLocationsFile)), StandardCharsets.UTF_8);
			List<Map<String, Object>> json = new Gson().fromJson(jsonText, List.class);
			for (Map<String, Object> item: json) {
				String className = (String) item.get("className");
				NavigableSet<Integer> instrumentationLines = ((List<Double>) item.get("instrumentationLines"))
						                                                .stream()
						                                                .map(Double::intValue)
						                                                .collect(Collectors.toCollection(TreeSet::new));
				if (!oracleLocations.containsKey(className)) {
					oracleLocations.put(className, instrumentationLines);
				} else {
					oracleLocations.get(className).addAll(instrumentationLines);
				}
			}
		}

		repSim = (Double) parameters.get("repSim");
		if (repSim == null)
			repSim = 0.3;

		insRel = (Double) parameters.get("insRel");
		if (insRel == null)
			insRel = 0.2;

		useDefects4JInstrumentation = (Boolean) parameters.get("useD4JInstr");
		if (useDefects4JInstrumentation == null)
			useDefects4JInstrumentation = false;

		checkParameters();
		invokeModules();

		globalID = 0;
		evaluations = 0;
		launchTime = System.currentTimeMillis();
		patches = new LinkedHashSet<Patch>();
		
		numberOfTimeouts = 0;
	}

	void checkParameters() throws Exception {
		if (binJavaDir == null)
			throw new Exception("The build directory of Java classes is not specified!");
		else if (binTestDir == null)
			throw new Exception("The build directory of test classes is not specified!");
		else if (srcJavaDir == null)
			throw new Exception("The directory of Java source code is not specified!");
		else if (dependences == null)
			throw new Exception("The dependences of the buggy program is not specified!");
		else if (!(new File(jvmPath).exists()))
			throw new Exception("The JVM path does not exist!");
		else if (!(new File(externalProjRoot).exists()))
			throw new Exception("The directory of external project does not exist!");
		else if (!srcVersions.contains(srcVersion))
			throw new Exception("The source version does not exist!");
	}

	protected void invokeModules() throws Exception {
		invokeClassFinder();
		invokeFaultLocalizer();
		invokeSeedLineGenerator();
		invokeASTRequestor();
		invokeLocalVarDetector();
		invokeFieldVarDetector();
		invokeMethodDetector();
		invokeIngredientScreener();
		invokeManipulationInitializer();
		invokeModificationPointsTrimmer();
		invokeTestFilter();

		invokeTestInstrumenter();
		
		invokeCompilerOptionsInitializer();
		invokeProgURLsInitializer();
	}

	protected void invokeTestInstrumenter() throws IOException, InterruptedException {
		System.out.println("Instrumentation of tests starts...");
		if (!testExecutorName.equals("ExternalTestExecutor2"))
			return;

		TestInstrumenterProcess tip = new TestInstrumenterProcess(binInsTestDir, binJavaDir, binTestDir, dependences,
				externalProjRoot, jvmPath);
		tip.instrumentTestClasses();

		System.out.println("Instrumentation of tests is finished!");
	}

	protected void invokeBlocklizer() throws IOException  {
		System.out.println("Blocklizer starts...");
		File srcFile = new File(srcJavaDir);
		Collection<File> javaFiles = FileUtils.listFiles(srcFile, new SuffixFileFilter(".java"),
				TrueFileFilter.INSTANCE);
		String[] sourceFilePaths = new String[javaFiles.size()];

		int i = 0;
		for (File file : javaFiles)
			sourceFilePaths[i++] = file.getCanonicalPath();

		for (String path : sourceFilePaths)
			Helper.blocklizeSource(path);

		System.out.println("Blocklizer is finished!");
	}


	protected void invokeClassFinder() throws ClassNotFoundException, IOException {
		ClassFinder finder = new ClassFinder(binJavaDir, binTestDir, dependences);
		binJavaClasses = finder.findBinJavaClasses();
		
		if (binExecuteTestClasses == null) {
			if (testNamesPath != null) {
				binExecuteTestClasses = FileUtils.readLines(new File(testNamesPath))
						                         .stream()
						                         .filter(s -> !s.isEmpty())
						                         .collect(Collectors.toSet());
			} else {
				binExecuteTestClasses = finder.findBinExecuteTestClasses();
			}
		}

		additionalTests = new LinkedHashSet<>();

		if (additionalTestsInfoPath != null) {
			FileInputStream fstream = new FileInputStream(additionalTestsInfoPath);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

			String strLine;
			while ((strLine = br.readLine()) != null)
				additionalTests.add(strLine.trim());
			br.close();

			System.out.format("loaded %d additional test cases\n", additionalTests.size());
		}

		allTests = new LinkedHashSet<>(binExecuteTestClasses);
		allTests.addAll(additionalTests);

		if (javaClassesInfoPath != null)
			FileUtils.writeLines(new File(javaClassesInfoPath), binJavaClasses);
		if (testClassesInfoPath != null)
			FileUtils.writeLines(new File(testClassesInfoPath), binExecuteTestClasses);
	}

	protected void invokeFaultLocalizer() throws FileNotFoundException, IOException {
		System.out.println("Fault localization starts...");

		IFaultLocalizer faultLocalizer;

		if (gzoltarDataDir == null)
			faultLocalizer = new GZoltarFaultLocalizer(binJavaClasses, allTests, binJavaDir, binTestDir,
					dependences);
		else
			faultLocalizer = new GZoltarFaultLocalizer2(gzoltarDataDir);

		faultyLines = faultLocalizer.searchSuspicious(thr);

		positiveTests = faultLocalizer.getPositiveTests();
		negativeTests = faultLocalizer.getNegativeTests();

		userPositiveTests = new LinkedHashSet<>(positiveTests);
		userPositiveTests.retainAll(binExecuteTestClasses);
		userNegativeTests = new LinkedHashSet<>(negativeTests);
		userNegativeTests.retainAll(binExecuteTestClasses);

		if (orgPosTestsInfoPath != null)
			FileUtils.writeLines(new File(orgPosTestsInfoPath), positiveTests);

		testsCoverage = faultLocalizer.getTestsCoverage();

        if (spectraPath != null) {
			StringBuilder builder = new StringBuilder();

			for (Entry<String, Set<LCNode>> entry: testsCoverage.entrySet()) {
				String test = entry.getKey();
				Set<LCNode> nodes = entry.getValue();

				builder.append(test).append(",")
					   .append(positiveTests.contains(test) ? "PASS" : "FAIL");

				for (LCNode node: nodes) {
					builder.append(",").append(node.getClassName()).append(":").append(node.getLineNumber());
				}

				builder.append("\n");
			}

			try (PrintWriter writer = new PrintWriter(spectraPath)) {
				writer.print(builder.toString().trim());
			}
		}

		System.out.println("Number of positive tests: " + positiveTests.size());
		System.out.println("Number of negative tests: " + negativeTests.size());
		System.out.println("Fault localization is finished!");

		if (spectraOnly) {
			System.exit(0);
		}
	}

	void invokeSeedLineGenerator() throws IOException, InterruptedException {
		if (seedLineGenerated) {
			SeedLineGeneratorProcess slgp = new SeedLineGeneratorProcess(binJavaClasses, javaClassesInfoPath,
					allTests, testClassesInfoPath, binJavaDir, binTestDir, dependences, externalProjRoot,
					jvmPath);
			seedLines = slgp.getSeedLines();
		} else
			seedLines = null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void invokeASTRequestor() throws IOException {
		System.out.println("AST parsing starts...");
		
		modificationPoints = new ArrayList<ModificationPoint>();
		seedStatements = new LinkedHashMap<SeedStatement, SeedStatementInfo>();
		sourceASTs = new LinkedHashMap<String, CompilationUnit>();
		sourceContents = new LinkedHashMap<String, String>();
		declaredClasses = new LinkedHashMap<String, ITypeBinding>();
		
		methodDeclarations = new LinkedHashMap<IMethodBinding, MethodDeclaration>();

		FileASTRequestorImpl requestor = new FileASTRequestorImpl(faultyLines, seedLines, modificationPoints,
				seedStatements, sourceASTs, sourceContents, declaredClasses, methodDeclarations);

		ASTParser parser = ASTParser.newParser(AST.JLS8);
		String[] classpathEntries = null;
		if (dependences != null)
			classpathEntries = dependences.toArray(new String[dependences.size()]);

		parser.setEnvironment(classpathEntries, new String[] { srcJavaDir }, null, true);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);

		Map options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(srcVersion, options);
		parser.setCompilerOptions(options);

		File srcFile = new File(srcJavaDir);
		Collection<File> javaFiles = FileUtils.listFiles(srcFile, new SuffixFileFilter(".java"),
				TrueFileFilter.INSTANCE);
		String[] sourceFilePaths = new String[javaFiles.size()];

		int i = 0;
		for (File file : javaFiles)
			sourceFilePaths[i++] = file.getCanonicalPath();

		parser.createASTs(sourceFilePaths, null, new String[] { "UTF-8" }, requestor, null);

		if (maxNumberOfModificationPoints != null && modificationPoints.size() > maxNumberOfModificationPoints) {
			Collections.sort(modificationPoints, new Comparator<ModificationPoint>() {
				@Override
				public int compare(ModificationPoint o1, ModificationPoint o2) {
					Double d1 = new Double(o1.getSuspValue());
					Double d2 = new Double(o2.getSuspValue());
					return d2.compareTo(d1);
				}
			});

			shuffleModificationPoints(maxNumberOfModificationPoints);
		}
		else {
			shuffleModificationPoints(modificationPoints.size());
		}
		
		System.out.println("AST parsing is finished!");
	}
	
	private void shuffleModificationPoints(int size) {
		List<ModificationPoint> temp = new ArrayList<ModificationPoint>();
		int[] permutation = new int[size];
		Utils.randomPermutation(permutation, size);

		for (int i = 0; i < size; i++) {
			ModificationPoint mp = modificationPoints.get(permutation[i]);
			temp.add(mp);
		}
		modificationPoints = temp;
	}

	protected void invokeLocalVarDetector() {
		System.out.println("Detection of local variables starts...");
		LocalVarDetector lvd = new LocalVarDetector(modificationPoints);
		lvd.detect();
		System.out.println("Detection of local variables is finished!");
	}

	protected void invokeMethodDetector() throws ClassNotFoundException, IOException {
		System.out.println("Detection of methods starts...");
		MethodDetector md = new MethodDetector(modificationPoints, declaredClasses, dependences);
		md.detect();
		System.out.println("Detection of methods is finished!");
	}

	protected void invokeFieldVarDetector() throws ClassNotFoundException, IOException {
		System.out.println("Detection of fields starts...");
		FieldVarDetector fvd = new FieldVarDetector(modificationPoints, declaredClasses, dependences);
		fvd.detect();
		System.out.println("Detection of fields is finished!");
	}

	protected void invokeIngredientScreener() throws JMException {
		System.out.println("Ingredient screener starts...");
		AbstractIngredientScreener ingredientScreener = IngredientScreenerFactory
				.getIngredientScreener(ingredientScreenerName, modificationPoints, seedStatements, ingredientMode);
		ingredientScreener.screen();

		if (ingredientFilterRule) {
			for (ModificationPoint mp : modificationPoints) {
				Iterator<Statement> iterator = mp.getIngredients().iterator();
				while (iterator.hasNext()) {
					Statement seed = iterator.next();
					if (IngredientFilterRule.canFiltered(seed, mp))
						iterator.remove();
					else if (containsInstrumentationLine(seed)) {
						iterator.remove();
					}
				}
			}
		}
		System.out.println("Ingredient screener is finished!");
	}

	protected boolean containsInstrumentationLine(ASTNode seed) {
        CompilationUnit cu = (CompilationUnit) seed.getRoot();
		int startPos = seed.getStartPosition();
		int endPos = startPos + seed.getLength() - 1;
		int startLine = cu.getLineNumber(startPos);
		int endLine = cu.getLineNumber(endPos);

		AbstractTypeDeclaration declaration = Helper.getAbstractTypeDeclaration(seed);
		ITypeBinding binding = declaration.resolveBinding();
		String className = binding.getBinaryName();

		return containsInstrumentationLine(className, startLine, endLine);
	}

	protected boolean containsInstrumentationLine(String className, int startLine, int endLine) {
		for (Entry<String, NavigableSet<Integer>> entry: oracleLocations.entrySet()) {
			NavigableSet<Integer> instrLines = entry.getValue();
			NavigableSet<Integer> intersection = instrLines.subSet(startLine, true, endLine, true);
			if (intersection.isEmpty()) {
				continue;
			}

			if (className.equals(entry.getKey())) {
				return true;
			}
		}
		return false;
	}

	protected void invokeManipulationInitializer() {
		System.out.println("Initialization of manipulations starts...");
		availableManipulations = new ArrayList<List<String>>(modificationPoints.size());

		for (int i = 0; i < modificationPoints.size(); i++) {
			ModificationPoint mp = modificationPoints.get(i);
			List<String> list = new ArrayList<String>();
			for (int j = 0; j < manipulationNames.length; j++) {
				String manipulationName = manipulationNames[j];

				if (manipulationFilterRule) {
					if (!ManipulationFilterRule.canFiltered2(manipulationName, mp)
                        && !changesInstrumentation(manipulationName, mp))
						list.add(manipulationName);
				} else
					list.add(manipulationName);
			}
			availableManipulations.add(list);
		}
		System.out.println("Initialization of manipulations is finished!");
	}

	protected boolean changesInstrumentation(String manipName, ModificationPoint mp) {
		Statement statement = mp.getStatement();

		if (!containsInstrumentationLine(statement)) {
			return false;
		}

		if (manipName.equalsIgnoreCase("replace") || manipName.equalsIgnoreCase("delete")) {
			return true;
		} else if (manipName.equalsIgnoreCase("insertBefore")) {
			CompilationUnit cu = (CompilationUnit) statement.getRoot();
			int startPos = statement.getStartPosition();
			int startLine = cu.getLineNumber(startPos);
			int line = startLine - 1;

			TypeDeclaration declaration = Helper.getTypeDeclaration(statement);
			ITypeBinding binding = declaration.resolveBinding();
			String className = binding.getBinaryName();

			return containsInstrumentationLine(className, line, line);
		} else {
			throw new RuntimeException("Unexpected manipulation name: " + manipName);
		}
	}

	protected void invokeTestFilter() throws IOException, InterruptedException {
		if (testFiltered) {
			System.out.println("Filtering of the tests starts...");
			Set<LCNode> fLines;
			if (maxNumberOfModificationPoints != null) {
				fLines = new LinkedHashSet<LCNode>();
				for (ModificationPoint mp : modificationPoints)
					fLines.add(mp.getLCNode());
			} else
				fLines = faultyLines.keySet();

			if (faultyLinesInfoPath != null) {
				List<String> lines = new ArrayList<String>();
				for (LCNode node : fLines)
					lines.add(node.toString());
				FileUtils.writeLines(new File(faultyLinesInfoPath), lines);
			}


			positiveTests = positiveTests.stream().filter(t -> testsCoverage.get(t).stream().anyMatch(fLines::contains))
					                    .collect(Collectors.toSet());
			System.out.println("Filtering of the tests is finished!");
		} else {
			System.out.println("Filtering of the tests is skipped!");
		}

		if (finalTestsInfoPath != null) {
			List<String> finalTests = new ArrayList<String>();
			finalTests.addAll(positiveTests);
			finalTests.addAll(negativeTests);
			FileUtils.writeLines(new File(finalTestsInfoPath), finalTests);
		}
		
		System.out.println("Number of positive tests considered: " + positiveTests.size() );

		if (filterTestsOnly) {
			System.exit(0);
		}
	}

	protected void invokeCompilerOptionsInitializer() {
		compilerOptions = new ArrayList<String>();
		compilerOptions.add("-nowarn");
		compilerOptions.add("-source");
		compilerOptions.add("1.7");
		compilerOptions.add("-cp");
		String cpStr = binJavaDir;

		if (dependences != null) {
			for (String str : dependences)
				cpStr += (File.pathSeparator + str);
		}

		compilerOptions.add(cpStr);
	}

	protected void invokeProgURLsInitializer() throws MalformedURLException {
		List<String> tempList = new ArrayList<String>();
		tempList.add(binJavaDir);
		tempList.add(binTestDir);
		if (dependences != null)
			tempList.addAll(dependences);
		progURLs = Helper.getURLs(tempList);
	}

	protected void invokeModificationPointsTrimmer() {
		int i = 0;
		while (i < modificationPoints.size()) {
			ModificationPoint mp = modificationPoints.get(i);
			List<String> manips = availableManipulations.get(i);

			if (mp.getIngredients().isEmpty()) {
				Iterator<String> iter = manips.iterator();
				while (iter.hasNext()) {
					String manipName = iter.next();
					if (!manipName.equalsIgnoreCase("Delete"))
						iter.remove();
				}
			}

			if (manips.isEmpty()) {
				modificationPoints.remove(i);
				availableManipulations.remove(i);
			} else
				i++;
		}
	}

	public Map<String, String> getModifiedJavaSources(Map<String, ASTRewrite> astRewriters) {
		Map<String, String> javaSources = new LinkedHashMap<String, String>();

		for (Entry<String, ASTRewrite> entry : astRewriters.entrySet()) {
			String sourceFilePath = entry.getKey();
			String content = sourceContents.get(sourceFilePath);

			Document doc = new Document(content);
			TextEdit edits = entry.getValue().rewriteAST(doc, null);

			try {
				edits.apply(doc);
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			javaSources.put(sourceFilePath, doc.get());
		}
		return javaSources;
	}

	protected boolean manipulateOneModificationPoint(ModificationPoint mp, String manipName, Statement ingredStatement,
			Map<String, ASTRewrite> astRewriters) throws JMException {
		String sourceFilePath = mp.getSourceFilePath();
		ASTRewrite rewriter;
		if (astRewriters.containsKey(sourceFilePath))
			rewriter = astRewriters.get(sourceFilePath);
		else {
			CompilationUnit unit = sourceASTs.get(sourceFilePath);
			rewriter = ASTRewrite.create(unit.getAST());
			astRewriters.put(sourceFilePath, rewriter);
		}

		AbstractManipulation manipulation = ManipulationFactory.getManipulation(manipName, mp, ingredStatement,
				rewriter);
		return manipulation.manipulate();
	}

	public Map<String, JavaFileObject> getCompiledClassesForTestExecution(Map<String, String> javaSources) {
		JavaJDKCompiler compiler = new JavaJDKCompiler(ClassLoader.getSystemClassLoader(), compilerOptions);
		try {
			boolean isCompiled = compiler.compile(javaSources);
			if (isCompiled)
				return compiler.getClassLoader().getCompiledClasses();
			else
				return null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public ITestExecutor getTestExecutor(Map<String, JavaFileObject> compiledClasses, Set<String> executePosTests)
			throws JMException, IOException {
		if (testExecutorName.equalsIgnoreCase("ExternalTestExecutor")) {
			File binWorkingDirFile = new File(binWorkingRoot, "bin_" + (globalID++));
			IO.saveCompiledClasses(compiledClasses, binWorkingDirFile);
			String binWorkingDir = binWorkingDirFile.getCanonicalPath();
			String tempPath;
			if (executePosTests.equals(positiveTests)) {
				tempPath = finalTestsInfoPath;
			} else {
				File tempDir = new File(System.getProperty("java.io.tmpdir"));
				File tempFile = File.createTempFile("arja-executePosTests-", ".txt", tempDir);
				Files.write(tempFile.toPath(), executePosTests, StandardCharsets.UTF_8);
				tempPath = tempFile.getPath();
			}
			return new ExternalTestExecutor(executePosTests, negativeTests, tempPath, binJavaDir, binTestDir,
					dependences, binWorkingDir, externalProjRoot, jvmPath, waitTime);

		}
		else if (testExecutorName.equalsIgnoreCase("ExternalTestExecutor2")) {
			File binWorkingDirFile = new File(binWorkingRoot, "bin_" + (globalID++));
			IO.saveCompiledClasses(compiledClasses, binWorkingDirFile);
			String binWorkingDir = binWorkingDirFile.getCanonicalPath();
			String tempPath;
			if (executePosTests.equals(positiveTests)) {
				tempPath = finalTestsInfoPath;
			} else {
				File tempDir = new File(System.getProperty("java.io.tmpdir"));
				File tempFile = File.createTempFile("arja-executePosTests-", ".txt", tempDir);
				Files.write(tempFile.toPath(), executePosTests, StandardCharsets.UTF_8);
				tempPath = tempFile.getPath();
			}
			return new ExternalTestExecutor2(executePosTests, negativeTests, tempPath, binJavaDir, binInsTestDir,
					dependences, binWorkingDir, externalProjRoot, jvmPath, waitTime);

		} else if (testExecutorName.equalsIgnoreCase("InternalTestExecutor")) {
			CustomURLClassLoader urlClassLoader = new CustomURLClassLoader(progURLs, compiledClasses);
			return new InternalTestExecutor(executePosTests, negativeTests, urlClassLoader, waitTime);
		} else {
			Configuration.logger_.severe("test executor name '" + testExecutorName + "' not found ");
			throw new JMException("Exception in getTestExecutor()");
		}
	}

	public Set<String> getSamplePositiveTests() {
		if (percentage == null || percentage == 1)
			return positiveTests;
		else {
			int num = (int) (positiveTests.size() * percentage);
			List<String> tempList = new ArrayList<String>(positiveTests);
			Collections.shuffle(tempList);
			Set<String> samplePositiveTests = new LinkedHashSet<String>();
			for (int i = 0; i < num; i++)
				samplePositiveTests.add(tempList.get(i));
			return samplePositiveTests;
		}
	}

	public List<List<String>> getAvailableManipulations() {
		return this.availableManipulations;
	}

	public List<ModificationPoint> getModificationPoints() {
		return this.modificationPoints;
	}

	public String[] getManipulationNames() {
		return this.manipulationNames;
	}

	public Map<String, CompilationUnit> getSourceASTs() {
		return this.sourceASTs;
	}

	public Map<String, String> getSourceContents() {
		return this.sourceContents;
	}

	public Set<String> getNegativeTests() {
		return this.negativeTests;
	}

	public Set<String> getPositiveTests() {
		return this.positiveTests;
	}

	public Double getPercentage() {
		return this.percentage;
	}

	public void saveTestAdequatePatch(List<Integer> opList, List<Integer> locList, List<Integer> ingredList)
			throws IOException {
		long estimatedTime = System.currentTimeMillis() - launchTime;
		if (patchOutputRoot != null)
			IO.savePatch(opList, locList, ingredList, modificationPoints, availableManipulations, patchOutputRoot,
					globalID, evaluations, estimatedTime);
	}

	public boolean addTestAdequatePatch(List<Integer> opList, List<Integer> locList, List<Integer> ingredList) {
		Patch patch = new Patch(opList, locList, ingredList, modificationPoints, availableManipulations);
		return patches.add(patch);
	}
	
	
	public void saveTestAdequatePatch(List<Integer> opList, List<Integer> locList, List<Integer> ingredList,
			Map<String, String> modifiedJavaSources) throws IOException {
		if (addTestAdequatePatch(opList, locList, ingredList)) {
			if (diffFormat) {
				try {
					IO.savePatch(modifiedJavaSources, srcJavaDir, this.patchOutputRoot, globalID);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} else
				saveTestAdequatePatch(opList, locList, ingredList);
			globalID++;
		}
	}

	public void saveTestAdequatePatch(List<Integer> opList, List<Integer> locList, List<Integer> ingredList,
									  Map<String, String> modifiedJavaSources, String summary) throws IOException {
		if (diffFormat) {
			try {
				IO.savePatch(modifiedJavaSources, srcJavaDir, this.patchOutputRoot, globalID);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		PrintStream ps = new PrintStream(
				Files.newOutputStream(Paths.get(patchOutputRoot, "Patch_" + globalID, "summary")));
		ps.println(summary);

		saveTestAdequatePatch(opList, locList, ingredList);

		globalID++;
	}

	public String getSrcJavaDir() {
		return this.srcJavaDir;
	}

	public String getBinJavaDir() {
		return this.binJavaDir;
	}

	public String getBinTestDir() {
		return this.binTestDir;
	}

	public Set<String> getDependences() {
		return this.dependences;
	}

	public int getNumberOfModificationPoints() {
		return modificationPoints.size();
	}

	public Set<Patch> getPatches() {
		return this.patches;
	}

	public void clearPatches() {
		patches.clear();
	}

	public void resetPatchOutputRoot(String patchOutputRoot) {
		this.patchOutputRoot = patchOutputRoot;
	}


	public void resetBinWorkingRoot(String binWorkingRoot) {
		this.binWorkingRoot = binWorkingRoot;
	}

	public static void resetGlobalID(int id) {
		globalID = id;
	}

	public static void increaseGlobalID() {
		globalID++;
	}

	public static void resetLaunchTime(long time) {
		launchTime = time;
	}

	public static long getLaunchTime() {
		return launchTime;
	}

	public static void resetEvaluations(int evals) {
		evaluations = evals;
	}

	public static int getEvaluations() {
		return evaluations;
	}
	
	public String getBinWorkingRoot() {
		return binWorkingRoot;
	}
	
	public String getOrgPosTestsInfoPath() {
		return orgPosTestsInfoPath;
	}
	
	public String getFinalTestsInfoPath() {
		return finalTestsInfoPath;
	}

	public Boolean getDiffFormat() {
		return diffFormat;
	}

	public String getPatchOutputRoot() {
		return patchOutputRoot;
	}

	public static int getGlobalID() {
		return globalID;
	}

	public boolean isUserTest(String testName) {
		return userPositiveTests.contains(testName) || userNegativeTests.contains(testName);
	}

	
	public String getBinInsTestDir() {
		return binInsTestDir;
	}
	
	
	public void adjustWaitTime() {
		if (evaluations % 40 == 0) {
			double r = numberOfTimeouts / 40.0;
			
			if (r > 0.3) {
				waitTime = (int)(waitTime * 1.5);
				if (waitTime > maxAllowedWaitTime)
					waitTime = maxAllowedWaitTime;
			}
			numberOfTimeouts = 0;
		}
	}
}
