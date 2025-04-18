package us.msu.cse.repair.core.manipulation;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import us.msu.cse.repair.core.parser.ModificationPoint;

public class ReplaceManipulation extends AbstractManipulation {

	public ReplaceManipulation(ModificationPoint mp, Statement ingredStatement, ASTRewrite rewriter) {
		super(mp, ingredStatement, rewriter);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean manipulate() {
		// TODO Auto-generated method stub
		Statement statement = mp.getStatement();
		rewriter.replace(statement, ingredStatement, null);
		return true;
	}

}
