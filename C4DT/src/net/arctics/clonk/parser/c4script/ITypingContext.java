package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;

import org.eclipse.jface.text.IRegion;

public interface ITypingContext {
	IType queryTypeOfExpression(ASTNode exprElm, IType defaultType);
	void storeType(ASTNode exprElm, IType type);
	IType typeOf(ASTNode node);
	<T extends IType> T typeOf(ASTNode node, Class<T> cls);
	boolean validForType(ASTNode node, IType type);
	<T extends AccessDeclaration> Declaration obtainDeclaration(T access);
	void assignment(ASTNode leftSide, ASTNode rightSide);
	void typingJudgement(ASTNode node, IType type, TypingJudgementMode mode);
	void incompatibleTypes(ASTNode node, IRegion region, IType left, IType right);
	boolean isModifiable(ASTNode node);
}
