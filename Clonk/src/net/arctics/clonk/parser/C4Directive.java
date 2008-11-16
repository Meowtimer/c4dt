package net.arctics.clonk.parser;

public class C4Directive extends C4Field {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public enum C4DirectiveType {
		STRICT,
		INCLUDE,
		APPENDTO;
		
		public static C4DirectiveType makeType(String arg) {
			if (arg.equalsIgnoreCase("strict")) return C4DirectiveType.STRICT;
			if (arg.equalsIgnoreCase("include")) return C4DirectiveType.INCLUDE;
			if (arg.equalsIgnoreCase("appendto")) return C4DirectiveType.APPENDTO;
			return null;
		}
	}
	
	private C4DirectiveType type;
	private String content;
	
	public C4Directive(C4DirectiveType type, String content) {
		this.content = content;
		this.type = type;
		switch (type) {
		case INCLUDE: case APPENDTO:
			System.out.println(content);
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
	
	
}
