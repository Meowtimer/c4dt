package net.arctics.clonk.parser;

import java.io.Serializable;

public class C4Directive implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public enum C4DirectiveType {
		STRICT,
		INCLUDE,
		APPENDTO;
		
		public static C4DirectiveType makeType(String arg) {
			if (arg.equals("strict")) return C4DirectiveType.STRICT;
			if (arg.equals("include")) return C4DirectiveType.INCLUDE;
			if (arg.equals("appendto")) return C4DirectiveType.APPENDTO;
			return null;
		}
		
		@Override
		public String toString() {
			return super.name().toLowerCase();
		}
	}
	
	private C4DirectiveType type;
	private String content;
	
	public C4Directive(C4DirectiveType type, String content) {
		this.content = content;
		this.type = type;
		switch (type) {
		case INCLUDE: case APPENDTO:
			if (content != null)
				this.content = content.substring(0, Math.min(4, content.length()));
		}
	}
	
	public C4Directive(String type, String content) {
		this(C4DirectiveType.makeType(type),content);
	}
	
	/**
	 * @return the type
	 */
	public C4DirectiveType getType() {
		return type;
	}
	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}
	
	@Override
	public String toString() {
		if (content != "" && content != null)
			return "#" + type.toString() + " " + content;
		return "#" + type.toString();
	}
	
}
