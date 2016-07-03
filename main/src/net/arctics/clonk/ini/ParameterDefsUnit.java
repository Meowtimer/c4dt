package net.arctics.clonk.ini;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.StructureVariable;

public class ParameterDefsUnit extends IniUnit implements IGlobalVariablesContributor {
	public static final String CONFIG_NAME = "ParameterDefs.txt";
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private List<Variable> scenparVariables;
	@Override
	protected String configurationName() { return CONFIG_NAME; } //$NON-NLS-1$
	public List<Variable> scenparVariables() { return defaulting(scenparVariables, Collections.<Variable>emptyList()); }
	public ParameterDefsUnit(final Object input) { super(input); }
	@Override
	public void startParsing() {
		super.startParsing();
	}
	/**
	 * Creates global const variables for definitions in the PlayerControls.txt file so they can be used in scripts.
	 */
	@Override
	protected void endParsing() {
		final Stream<IniEntry> names = sections()
			.filter(s -> s.name().equals("ParameterDef"))
			.map(s -> as(s.map().get("ID"), IniEntry.class))
			.filter(n -> n != null && n.stringValue() != null);

		scenparVariables = names.map(name -> {
			final Variable var = new StructureVariable("SCENPAR_" + name.stringValue(), PrimitiveType.INT); //$NON-NLS-1$
			var.setScope(Scope.CONST);
			var.setParent(this);
			var.setLocation(name);
			return var;
		}).collect(Collectors.toList());
		super.endParsing();
	}
	@Override
	public String nameEntryName(final IniSection section) {
		if (section != null && section.parentSection() != null) {
			final IniSection psec = section.parentSection();
			switch (psec.name()) {
			case "ControlDefs":
				return "Identifier";
			case "ControlSets":
				return "Name";
			case "ControlSet":
				return "Control";
			}
		}
		return super.nameEntryName(section);
	}
	@Override
	public Declaration findLocalDeclaration(final String declarationName, final Class<? extends Declaration> declarationClass) {
		if (declarationClass == Variable.class && declarationName.startsWith("CON_"))
			for (final Variable var : scenparVariables())
				if (var.name().equals(declarationName))
					return var;
		return super.findLocalDeclaration(declarationName, declarationClass);
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Declaration> T latestVersionOf(final T of) {
		final T r = super.latestVersionOf(of);
		if (r != null)
			return r;
		if (of instanceof Variable)
			for (final Variable c : scenparVariables)
				if (of.name().equals(c.name()))
					return (T) c;
		return null;
	}
	@Override
	public Collection<Variable> globalVariables() { return scenparVariables(); }
}
