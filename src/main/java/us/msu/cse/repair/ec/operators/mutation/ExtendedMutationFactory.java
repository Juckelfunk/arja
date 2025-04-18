package us.msu.cse.repair.ec.operators.mutation;

import java.util.LinkedHashMap;

import jmetal.operators.mutation.Mutation;
import jmetal.operators.mutation.MutationFactory;
import jmetal.util.JMException;

public class ExtendedMutationFactory {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Mutation getMutationOperator(String name, LinkedHashMap parameters) throws JMException {
		if (name.equalsIgnoreCase("BitFlipUniformMutation"))
			return new BitFlipUniformMutation(parameters);
		else if (name.equalsIgnoreCase("GenProgMutation"))
			return new GenProgMutation(parameters);
		else if (name.equalsIgnoreCase("GuidedMutation"))
			return new GuidedMutation(parameters);
		else
			return MutationFactory.getMutationOperator(name, parameters);

	} // getMutationOperator
}
