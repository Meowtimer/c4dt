package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.swt.graphics.Image;

public class ScriptContextInformation implements IContextInformation, IContextInformationExtension {

	private String contextDisplayString;
	private Image image;
	private String informationDisplayString;
	private final int parmIndex;
	private int parmsStart, parmsEnd;
	private SourceLocation[] parameterDisplayStringRanges;
	private Function function;

	public Function function() { return function; }
	public boolean valid(int offset) { return parmIndex != -1 && offset >= parmsStart(); }
	public ScriptContextInformation() { this.parmIndex = -1; }
	@Override
    public String getContextDisplayString() { return contextDisplayString; }
	@Override
    public Image getImage() { return image; }
	@Override
    public String getInformationDisplayString() { return informationDisplayString; }
	@Override
	public int getContextInformationPosition() { return parmsStart(); }
	public boolean hasVarParms() {
		return function.numParameters() > 0 && !function.parameter(function.numParameters()-1).isActualParm();
	}

	@Override
	public String toString() {
		return StringUtil.blockString("", "", ",  ", iterable(parameterDisplayStringRanges));
	}

	public int parmsStart() { return parmsStart; }
	public int currentParameter() { return parmIndex; }
	public int parmsCount() { return parameterDisplayStringRanges.length; }
	public int parmsEnd() { return parmsEnd; }

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ScriptContextInformation) {
			ScriptContextInformation other = (ScriptContextInformation) obj;
			return
				Utilities.eq(getInformationDisplayString(), other.getInformationDisplayString());
		}
		return false;
	}

	public ScriptContextInformation(String contextDisplayString, Image image, Function function, int parmIndex, int parmsStart, int parmsEnd) {
	    super();
	    this.contextDisplayString = contextDisplayString;
	    this.image = image;
	    this.parmIndex = parmIndex;
	    this.function = function;
	    this.parmsStart = parmsStart;
	    this.parmsEnd = parmsEnd;
	    makeDisplayString(function);
    }

	public SourceLocation currentParameterDisplayStringRange() {
		return parameterDisplayStringRanges[Math.min(parameterDisplayStringRanges.length-1, parmIndex)];
	}

	private void makeDisplayString(Function function) {
		final boolean longParameterInfo = ClonkPreferences.toggle(ClonkPreferences.LONG_PARAMETER_INFO, false);
		StringBuilder builder = new StringBuilder();
		builder.append(function.name());
		builder.append(":");
		if (function.numParameters() == 0) {
			int start = builder.length();
			builder.append(" No parameters");
			parameterDisplayStringRanges = new SourceLocation[] { new SourceLocation(start, builder.length()) };
		} else {
			parameterDisplayStringRanges = new SourceLocation[function.numParameters()];
			int estimate = 0;
			for (Variable p : function.parameters()) {
				estimate += p.type().typeName(true).length() + p.name().length();
				if (longParameterInfo && p.userDescription() != null)
					estimate += p.userDescription().length();
			}
			String FIRSTPARM, PARM;
			if (estimate > 60)
				FIRSTPARM = PARM = "\n\t";
			else {
				FIRSTPARM = " ";
				PARM = ", ";
			}
			builder.append(FIRSTPARM);
			for (int i = 0; i < function.numParameters(); i++) {
				if (i > 0)
					builder.append(PARM);
				int parmStart = builder.length();
				Variable par = function.parameter(i);
				IType type = par.type();
				if (type != PrimitiveType.UNKNOWN && type != null) {
					builder.append(type.typeName(true));
					builder.append(' ');
				}
				builder.append(par.name());
				if (longParameterInfo && par.userDescription() != null && !par.userDescription().equals("")) {
					builder.append(" (");
					String desc = par.userDescription().trim();
					int sentenceEnd = desc.indexOf('.');
					if (sentenceEnd != -1)
						desc = desc.substring(0, sentenceEnd);
					builder.append(desc);
					builder.append(")");
				}
				parameterDisplayStringRanges[i] = new SourceLocation(parmStart, builder.length());
			}
		}
		informationDisplayString = builder.toString();
	}

}
