package us.msu.cse.repair.core.parser.ingredient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;

import us.msu.cse.repair.core.parser.MethodInfo;
import us.msu.cse.repair.core.parser.ModificationPoint;
import us.msu.cse.repair.core.parser.SeedStatement;
import us.msu.cse.repair.core.parser.SeedStatementInfo;
import us.msu.cse.repair.core.parser.VarMethodInfoExtractor;
import us.msu.cse.repair.core.util.visitors.MethodConvASTVisitor;

public class MethodTypeMatchIngredientScreener extends AbstractIngredientScreener {

	public MethodTypeMatchIngredientScreener(List<ModificationPoint> modificationPoints,
			Map<SeedStatement, SeedStatementInfo> seedStatements, IngredientMode ingredientMode) {
		super(modificationPoints, seedStatements, ingredientMode);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void screenIngredients(ModificationPoint mp, IngredientMode ingredientMode, boolean includeSelf) {
		// TODO Auto-generated method stub
		Set<SeedStatement> ingredientSet = new LinkedHashSet<SeedStatement>();
		for (Map.Entry<SeedStatement, SeedStatementInfo> entry : seedStatements.entrySet()) {
			if (canPreFiltered(mp, entry, ingredientMode, includeSelf))
				continue;

			Statement seed = entry.getKey().getStatement();
			seed = getMethodTypeMatchedStatement(seed, mp);
			if (seed != null) {
				ingredientSet.add(new SeedStatement(seed));
			}
		}

		List<Statement> ingredients = new ArrayList<Statement>();
		for (SeedStatement seedStatement : ingredientSet)
			ingredients.add(seedStatement.getStatement());

		mp.setIngredients(ingredients);
	}

	Statement getMethodTypeMatchedStatement(Statement seed, ModificationPoint mp) {
		VarMethodInfoExtractor sie = new VarMethodInfoExtractor(seed);
		sie.extract();

		Map<String, MethodInfo> thisMethods = sie.getThisMethods();
		Map<String, MethodInfo> superMethods = sie.getSuperMethods();
		Map<String, MethodInfo> methods = sie.getMethods();

		Map<String, String> methodMatchMap = new LinkedHashMap<String, String>();
		Map<String, String> thisMethodMatchMap = new LinkedHashMap<String, String>();
		Map<String, String> superMethodMatchMap = new LinkedHashMap<String, String>();

		Map<String, MethodInfo> declaredMethods = new LinkedHashMap<String, MethodInfo>(mp.getDeclaredMethods());
		Map<String, MethodInfo> inheritedMethods = new LinkedHashMap<String, MethodInfo>(mp.getInheritedMethods());
		Map<String, MethodInfo> outerMethods = new LinkedHashMap<String, MethodInfo>(mp.getOuterMethods());

		if (IngredientUtil.isInVarScope(mp, sie)) {
			Statement seedCopy = (Statement) ASTNode.copySubtree(mp.getStatement().getAST(), seed);
			
			IngredientUtil.findSuperMethods(superMethods, inheritedMethods);
			IngredientUtil.findThisMethods(thisMethods, declaredMethods, inheritedMethods);
			IngredientUtil.findMethods(methods, declaredMethods, inheritedMethods, outerMethods);

			if (!IngredientUtil.doSuperMethodMatch(superMethodMatchMap, superMethods, inheritedMethods))
				return null;
			else if (!IngredientUtil.doThisMethodMatch(thisMethodMatchMap, thisMethods, declaredMethods,
					inheritedMethods))
				return null;
			else if (!IngredientUtil.doMethodMatch(methodMatchMap, methods, declaredMethods, inheritedMethods,
					outerMethods))
				return null;
			else if (methodMatchMap.size() > 0 || thisMethodMatchMap.size() > 0 || superMethodMatchMap.size() > 0) {	
				MethodConvASTVisitor visitor = new MethodConvASTVisitor(methodMatchMap, thisMethodMatchMap,
						superMethodMatchMap, sie.getMethodIDs(), sie.getSuperMethodIDs());
				seedCopy.accept(visitor);
			}
			
			return seedCopy;
		} else
			return null;
	}

}
