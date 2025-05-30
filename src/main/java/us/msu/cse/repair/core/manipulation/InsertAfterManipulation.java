package us.msu.cse.repair.core.manipulation;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import us.msu.cse.repair.core.parser.ModificationPoint;

public class InsertAfterManipulation extends AbstractManipulation {
	public InsertAfterManipulation(ModificationPoint mp, Statement ingredStatement, ASTRewrite rewriter) {
		super(mp, ingredStatement, rewriter);
		// TODO Auto-generated constructor stub
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean manipulate() {
		// TODO Auto-generated method stub
		Statement statement = mp.getStatement();

		if (statement.getParent() instanceof Block) {
			Block block = (Block) statement.getParent();
			ListRewrite lrw = rewriter.getListRewrite(block, Block.STATEMENTS_PROPERTY);
			lrw.insertAfter(ingredStatement, statement, null);
		} else if (statement.getParent() instanceof SwitchStatement) {
			SwitchStatement ss = (SwitchStatement) statement.getParent();
			ListRewrite lrw = rewriter.getListRewrite(ss, SwitchStatement.STATEMENTS_PROPERTY);
			lrw.insertBefore(ingredStatement, statement, null);
		}
		else {
			Statement statementCopy = (Statement) ASTNode.copySubtree(statement.getAST(), statement);
			Statement ingredStatementCopy = (Statement) ASTNode.copySubtree(statement.getAST(), ingredStatement);

			Block newBlock = statement.getAST().newBlock();
			newBlock.statements().add(statementCopy);
			newBlock.statements().add(ingredStatementCopy);
			rewriter.replace(statement, newBlock, null);
		}

		return true;
	}
}
