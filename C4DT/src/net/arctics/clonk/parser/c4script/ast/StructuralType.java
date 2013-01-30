package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.ArrayUtil.iterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IRefinedPrimitiveType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.util.StringUtil;

/**
 * Type used to describe that some typed element needs to refer to an object that supports a set of functions.
 * @author madeen
 *
 */
public class StructuralType implements IType, IRefinedPrimitiveType {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private final Set<String> functions = new HashSet<String>();

	public Set<String> functions() { return functions; }
	public void addFunctions(Collection<String> functions) { this.functions.addAll(functions); }

	public StructuralType(String supportsFunction) {
		functions.add(supportsFunction);
	}

	public StructuralType(StructuralType a, StructuralType b) {
		functions.addAll(a.functions());
		functions.addAll(b.functions());
	}

	@Override
	public Iterator<IType> iterator() {
		return iterable(PrimitiveType.PROPLIST, PrimitiveType.OBJECT, PrimitiveType.ID, this).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		boolean anyDefinitions = false;
		boolean primitives = false;
		for (IType t : other)
			if (t instanceof Definition) {
				anyDefinitions = true;
				Definition d = (Definition)t;
				if (satisfiedBy(d))
					return true;
			}
			else if (t instanceof PrimitiveType) switch ((PrimitiveType)t) {
			case ANY: case UNKNOWN: case OBJECT:
				primitives = true;
				break;
			default:
				continue;
			}
		return anyDefinitions ? false : primitives;
	}

	public boolean satisfiedBy(Definition d) {
		boolean satisfies = true;
		for (String f : functions) {
			Function fun = d.findFunction(f);
			if (fun == null) {
				satisfies = false;
				break;
			}
		}
		return satisfies;
	}

	@Override
	public String typeName(boolean special) {
		List<String> x = new ArrayList<String>(functions.size()+1);
		x.addAll(functions);
		x.add("...");
		return StringUtil.blockString("{", "}", ", ", x);
	}

	@Override
	public String toString() {
		return typeName(true);
	}

	@Override
	public IType simpleType() {
		return PrimitiveType.OBJECT;
	}


	@Override
	public PrimitiveType primitiveType() {
		return PrimitiveType.OBJECT;
	}

	@Override
	public void setTypeDescription(String description) {}

	/*
	@Override
	public IType resolve(ProblemReportingContext context, IType callerType) {
		final List<Definition> implementors = new ArrayList<>(10);
		final Sink<Definition> defSink = new Sink<Definition>() {
			@Override
			public void receivedObject(Definition item) {
				if (satisfiedBy(item))
					implementors.add(item);
			}
		};
		context.script().index().forAllRelevantIndexes(new Sink<Index>() {
			@Override
			public void receivedObject(Index item) {
				item.allDefinitions(defSink);
			}
		});
		return TypeChoice.make(implementors);
	}*/

}
