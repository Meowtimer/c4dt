package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.typing.TypingJudgementMode;

import org.eclipse.jface.text.IRegion;

public interface ITypingContext {
	IType typeOf(ASTNode node);
	<T extends IType> T typeOf(ASTNode node, Class<T> cls);
	<T extends AccessDeclaration> Declaration obtainDeclaration(T access);
	void judgement(ASTNode node, IType type, TypingJudgementMode mode);
	void incompatibleTypesMarker(ASTNode node, IRegion region, IType left, IType right);
	boolean isModifiable(ASTNode node);
}
