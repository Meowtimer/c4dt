package net.arctics.clonk.index;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.inireader.ScenarioUnit;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

/**
 * A scenario.
 * @author madeen
 *
 */
public class Scenario extends Definition {

	public static final String PROPLIST_NAME = "Scenario";
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private Declaration scenarioPropList; { createScenarioProplist(); }

	private synchronized Declaration createScenarioProplist() {
		if (scenarioPropList == null && engine().settings().supportsGlobalProplists) {
			final ProplistDeclaration type = ProplistDeclaration.newAdHocDeclaration();
			type.setLocation(SourceLocation.ZERO);
			type.setParent(this);
			final Variable v = new Variable(PROPLIST_NAME, type);
			v.setParent(this);
			v.setScope(Scope.STATIC);
			scenarioPropList = v;
		}
		return scenarioPropList;
	}

	public Declaration propList() {
		return scenarioPropList;
	}

	@Override
	public void postLoad(Declaration parent, Index root) {
		createScenarioProplist();
		super.postLoad(parent, root);
	}

	public Scenario(Index index, String name, IContainer container) {
		super(index, ID.get(container != null ? container.getName() : name), name, container);
	}

	public static Scenario get(IContainer folder) {
		final Definition obj = definitionCorrespondingToFolder(folder);
		return obj instanceof Scenario ? (Scenario)obj : null;
	}

	public static Scenario containingScenario(IResource res) {
		if (res == null)
			return null;
		for (IContainer c = res instanceof IContainer ? (IContainer)res : res.getParent(); c != null; c = c.getParent()) {
			final Scenario s = get(c);
			if (s != null)
				return s;
		}
		return null;
	}

	public static Scenario nearestScenario(IResource resource) {
		Scenario scenario;
		for (scenario = null; scenario == null && resource != null; resource = resource.getParent())
			if (resource instanceof IContainer)
				scenario = get((IContainer)resource);
		return scenario;
	}

	public ScenarioUnit scenarioConfiguration() {
		final IFile scenarioFile = as(definitionFolder().findMember(ScenarioUnit.FILENAME), IFile.class);
		return scenarioFile != null ? Structure.pinned(scenarioFile, true, false, ScenarioUnit.class) : null;
	}
	
	@Override
	public Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		if (declarationName.equals(PROPLIST_NAME))
			return scenarioPropList;
		else
			return super.findLocalDeclaration(declarationName, declarationClass);
	}

}
