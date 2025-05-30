package us.msu.cse.repair.core.parser.ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;

import us.msu.cse.repair.core.parser.ModificationPoint;
import us.msu.cse.repair.core.parser.SeedStatement;
import us.msu.cse.repair.core.parser.SeedStatementInfo;
import us.msu.cse.repair.core.parser.VarMethodInfoExtractor;

public class DirectIngredientScreener extends AbstractIngredientScreener {
	public DirectIngredientScreener(List<ModificationPoint> modificationPoints,
			Map<SeedStatement, SeedStatementInfo> seedStatements, IngredientMode ingredientMode) {
		super(modificationPoints, seedStatements, ingredientMode);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void screenIngredients(ModificationPoint mp, IngredientMode ingredientMode, boolean includeSelf) {
		// TODO Auto-generated method stub
		List<Statement> ingredients = new ArrayList<Statement>();
		for (Map.Entry<SeedStatement, SeedStatementInfo> entry : seedStatements.entrySet()) {
			if (canPreFiltered(mp, entry, ingredientMode, includeSelf))
				continue;

			Statement seed = entry.getKey().getStatement();
			VarMethodInfoExtractor sie = new VarMethodInfoExtractor(seed);
			sie.extract();

			if (IngredientUtil.isInMethodScope(mp, sie) && IngredientUtil.isInVarScope(mp, sie)) {
				Statement seedCopy = (Statement) ASTNode.copySubtree(mp.getStatement().getAST(), seed);
				ingredients.add(seedCopy);
			}

		}
		mp.setIngredients(ingredients);
	}
	
}
