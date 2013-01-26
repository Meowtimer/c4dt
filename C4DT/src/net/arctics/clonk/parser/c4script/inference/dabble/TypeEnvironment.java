package net.arctics.clonk.parser.c4script.inference.dabble;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.ScriptProcessor;

public class TypeEnvironment extends ArrayList<ITypeInfo> {
	private static ITypeInfo merge(ITypeInfo left, ITypeInfo right) {
		if (!(left instanceof LinkedTypeInfo) && right instanceof LinkedTypeInfo) {
			ITypeInfo tmp = left;
			left = right;
			right = tmp;
		}
		left.merge(right);
		return left;
	}
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public TypeEnvironment up;
	public TypeEnvironment() { super(5); }
	public TypeEnvironment(TypeEnvironment up) { this(); this.up = up; }
	public TypeEnvironment inject(TypeEnvironment other, boolean ignoreLocals) {
		List<ITypeInfo> merged = null;
		OtherLoop: for (Iterator<ITypeInfo> otherIt = other.iterator(); otherIt.hasNext();) {
			ITypeInfo otherInfo = otherIt.next();
			for (Iterator<ITypeInfo> it = this.iterator(); it.hasNext();) {
				ITypeInfo myInfo = it.next();
				if (myInfo.refersToSameExpression(otherInfo)) {
					if (merged == null)
						merged = new LinkedList<ITypeInfo>();
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
		for (ITypeInfo info : this)
			info.apply(soft, processor);
	}
	public final void injectIntoUpper(boolean ignoreLocals) {
		if (up != null)
			inject(up, ignoreLocals);
	}
}