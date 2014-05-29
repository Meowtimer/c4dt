package net.arctics.clonk.ini;

public class InvalidIniConfigurationException extends Exception {
	private static final long serialVersionUID = 6322932518234920576L;

	public InvalidIniConfigurationException(final String msg) {
		super(msg);
	}
}
