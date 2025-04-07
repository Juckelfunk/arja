package us.msu.cse.repair;

import java.util.LinkedHashMap;

import us.msu.cse.repair.algorithms.kali.Kali;
import us.msu.cse.repair.algorithms.kali.KaliAlg;
import us.msu.cse.repair.core.AbstractRepairAlgorithm;

public class KaliMain {
	public static void main(String args[]) throws Exception {
		LinkedHashMap<String, String> parameterStrs = Interpreter.getParameterStrings(args);
		LinkedHashMap<String, Object> parameters = Interpreter.getBasicParameterSetting(parameterStrs);
	
		Kali problem = new Kali(parameters);
		AbstractRepairAlgorithm repairAlg = new KaliAlg(problem);	
		repairAlg.execute();
	}
}
