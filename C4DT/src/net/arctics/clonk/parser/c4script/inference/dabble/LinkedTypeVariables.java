package net.arctics.clonk.parser.c4script.inference.dabble;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.ArrayUtil.iterable;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.Visitor;
import net.arctics.clonk.util.StringUtil;

public class LinkedTypeVariables implements ITypeVariable {

	private ITypeVariable[] linkedTypeInfos;
	private IType unified;

	public LinkedTypeVariables(ITypeVariable ati, ITypeVariable bti) {
		boolean ma = ati instanceof LinkedTypeVariables;
		boolean mb = bti instanceof LinkedTypeVariables;
		if (!(ma || mb))
			linkedTypeInfos = new ITypeVariable[] {ati, bti};
		else {
			ITypeVariable[] _a = ma ? ((LinkedTypeVariables)ati).linkedTypeInfos : new ITypeVariable[] {ati};
			ITypeVariable[] _b = mb ? ((LinkedTypeVariables)bti).linkedTypeInfos : new ITypeVariable[] {bti};
			linkedTypeInfos = concat(_a, _b);
		}
		IType unified = null;
		for (ITypeVariable l : linkedTypeInfos)
			unified = TypeUnification.unify(unified, l.get());
		this.unified = unified;
		for (ITypeVariable l : linkedTypeInfos)
			l.set(unified);
	}

	@Override
	public IType get() {
		return unified;
	}

	@Override
	public void set(IType type) {
		unified = type;
		for (ITypeVariable l : linkedTypeInfos)
			l.set(type);
	}

	@Override
	public boolean hint(IType type) {
		boolean r = true;
		for (ITypeVariable l : linkedTypeInfos)
			r &= l.hint(type);
		return r;
	}

	@Override
	public boolean binds(ASTNode expr, Visitor visitor) {
		for (ITypeVariable l : linkedTypeInfos)
			if (l.binds(expr, visitor))
				return true;
		return false;
	}

	@Override
	public boolean same(ITypeVariable other) {
		for (ITypeVariable o : other instanceof LinkedTypeVariables ? ((LinkedTypeVariables)other).linkedTypeInfos : new ITypeVariable[] {other})
			for (ITypeVariable l : linkedTypeInfos)
				if (l.same(o))
					return true;
		return false;
	}

	@Override
	public void apply(boolean soft, Visitor visitor) {
		for (ITypeVariable l : linkedTypeInfos)
			l.apply(soft, visitor);
	}

	@Override
	public void merge(ITypeVariable other) {
		unified = TypeUnification.unify(unified, other.get());
		boolean append = true;
		for (ITypeVariable l : linkedTypeInfos) {
			l.set(unified);
			if (l.same(other))
				append = false;
		}
		if (append)
			linkedTypeInfos = concat(other, linkedTypeInfos);
	}

	@Override
	public String toString() {
		return StringUtil.blockString("[", "]", ", ", iterable(linkedTypeInfos)) + ": " + unified.typeName(true);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public Declaration declaration(Visitor visitor) {
		return null;
	}

}
