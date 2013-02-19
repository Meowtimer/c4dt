package net.arctics.clonk.parser.c4script.inference.dabble;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.ScriptProcessor;

public class TypeEnvironment extends ArrayList<ITypeVariable> {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private static ITypeVariable merge(ITypeVariable left, ITypeVariable right) {
		if (!(left instanceof LinkedTypeVariables) && right instanceof LinkedTypeVariables) {
			ITypeVariable tmp = left;
			left = right;
			right = tmp;
		}
		left.merge(right);
		return left;
	}
	public TypeEnvironment up;
	public TypeEnvironment() { super(5); }
	public TypeEnvironment(TypeEnvironment up) { this(); this.up = up; }
	public TypeEnvironment inject(TypeEnvironment other, boolean ignoreLocals) {
		List<ITypeVariable> merged = null;
		OtherLoop: for (Iterator<ITypeVariable> otherIt = other.iterator(); otherIt.hasNext();) {
			ITypeVariable otherInfo = otherIt.next();
			for (Iterator<ITypeVariable> it = this.iterator(); it.hasNext();) {
				ITypeVariable myInfo = it.next();
				if (myInfo.refersToSameExpression(otherInfo)) {
					if (merged == null)
						merged = new LinkedList<ITypeVariable>();
					merged.add(merge(myInfo, otherInfo));
					otherIt.remove();
					it.remove();
					continue OtherLoop;
				}
			}
		}
		this.addAll(other);
		if (merged != null)
			this.addAll(merged);
		return this;
	}
	public void apply(ScriptProcessor processor, boolean soft) {
		for (ITypeVariable info : this)
			info.apply(soft, processor);
	}
	public final void injectIntoUpper(boolean ignoreLocals) {
		if (up != null)
			inject(up, ignoreLocals);
	}
}