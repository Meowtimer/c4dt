package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.swt.graphics.Image;

public class C4ScriptContextInformation implements IContextInformation, IContextInformationExtension {

	private String contextDisplayString;
	private Image image;
	private String informationDisplayString;
	private final int parmIndex;
	private int parmsStart, parmsEnd;
	private SourceLocation[] parameterDisplayStringRanges;
	private Function function;
	
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
		if (obj instanceof C4ScriptContextInformation) {
			C4ScriptContextInformation info = (C4ScriptContextInformation) obj;
			return parmsStart() == info.parmsStart(); // similar enough :o
		}
		return false;
	}
	
	public C4ScriptContextInformation(String contextDisplayString, Image image, Function function, int parmIndex, int parmsStart, int parmsEnd) {
	    super();
	    System.out.println(String.format("New context information %d", parmIndex));
	    this.contextDisplayString = contextDisplayString;
	    this.image = image;
	    this.parmIndex = parmIndex;
	    this.function = function;
	    this.parmsStart = parmsStart;
	    this.parmsEnd = parmsEnd;
	    makeDisplayString(function);
    }
	
	public SourceLocation currentParameterDisplayStringRange() {
		return parameterDisplayStringRanges[parmIndex];
	}

	private void makeDisplayString(Function function) {
		StringBuilder builder = new StringBuilder();
		builder.append(function.name());
		builder.append("(");
		if (function.numParameters() == 0) {
			parameterDisplayStringRanges = new SourceLocation[] { new SourceLocation(builder.length(), builder.length()+"No parameters".length()) };
			builder.append("No parameters");
		} else {
			parameterDisplayStringRanges = new SourceLocation[function.numParameters()];
			for (int i = 0; i < function.numParameters(); i++) {
				if (i > 0)
					builder.append(", ");
				int parmStart = builder.length();
				Variable par = function.parameter(i);
				IType type = par.type();
				if (type != PrimitiveType.UNKNOWN && type != null) {
					builder.append(type.typeName(true));
					builder.append(' ');
				}
				builder.append(par.name());
				parameterDisplayStringRanges[i] = new SourceLocation(parmStart, builder.length());
			}
		}
		builder.append(")");
		informationDisplayString = builder.toString();
	}

	public boolean valid(int offset) {
		return parmIndex != -1 && offset >= parmsStart();
	}
	
	public C4ScriptContextInformation() {
		this.parmIndex = -1;
	}

	@Override
    public String getContextDisplayString() {
	    return contextDisplayString;
    }

	@Override
    public Image getImage() {
	    return image;
    }

	@Override
    public String getInformationDisplayString() {
	    return informationDisplayString;
    }

	@Override
	public int getContextInformationPosition() {
		return parmsStart();
	}

	public boolean hasVarParms() {
		return function.numParameters() > 0 && !function.parameter(function.numParameters()-1).isActualParm();
	}
	
}
