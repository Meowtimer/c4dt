package net.arctics.clonk.parser.c4script.inference.dabble;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.ArrayUtil.iterable;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.ScriptProcessor;
import net.arctics.clonk.util.StringUtil;

public class LinkedTypeInfo implements ITypeInfo {

	private ITypeInfo[] linkedTypeInfos;
	private IType unified;

	public LinkedTypeInfo(ITypeInfo ati, ITypeInfo bti) {
		boolean ma = ati instanceof LinkedTypeInfo;
		boolean mb = bti instanceof LinkedTypeInfo;
		if (!(ma || mb))
			linkedTypeInfos = new ITypeInfo[] {ati, bti};
		else {
			ITypeInfo[] _a = ma ? ((LinkedTypeInfo)ati).linkedTypeInfos : new ITypeInfo[] {ati};
			ITypeInfo[] _b = mb ? ((LinkedTypeInfo)bti).linkedTypeInfos : new ITypeInfo[] {bti};
			linkedTypeInfos = concat(_a, _b);
		}
		IType unified = null;
		for (ITypeInfo l : linkedTypeInfos)
			unified = TypeUnification.unify(unified, l.type());
		this.unified = unified;
		for (ITypeInfo l : linkedTypeInfos)
			l.storeType(unified);
	}

	@Override
	public IType type() {
		return unified;
	}

	@Override
	public void storeType(IType type) {
		unified = type;
		for (ITypeInfo l : linkedTypeInfos)
			l.storeType(type);
	}

	@Override
	public boolean hint(IType type) {
		boolean r = true;
		for (ITypeInfo l : linkedTypeInfos)
			r &= l.hint(type);
		return r;
	}

	@Override
	public boolean storesTypeInformationFor(ASTNode expr, ScriptProcessor processor) {
		for (ITypeInfo l : linkedTypeInfos)
			if (l.storesTypeInformationFor(expr, processor))
				return true;
		return false;
	}

	@Override
	public boolean refersToSameExpression(ITypeInfo other) {
		for (ITypeInfo o : other instanceof LinkedTypeInfo ? ((LinkedTypeInfo)other).linkedTypeInfos : new ITypeInfo[] {other})
			for (ITypeInfo l : linkedTypeInfos)
				if (l.refersToSameExpression(o))
					return true;
		return false;
	}

	@Override
	public void apply(boolean soft, ScriptProcessor processor) {
		for (ITypeInfo l : linkedTypeInfos)
			l.apply(soft, processor);
	}

	@Override
	public void merge(ITypeInfo other) {
		unified = TypeUnification.unify(unified, other.type());
		boolean append = true;
		for (ITypeInfo l : linkedTypeInfos) {
			l.storeType(unified);
			if (l.refersToSameExpression(other))
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
	public Declaration declaration(ScriptProcessor processor) {
		return null;
	}

}
