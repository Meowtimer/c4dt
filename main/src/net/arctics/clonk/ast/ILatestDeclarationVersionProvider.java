package net.arctics.clonk.ast;

/**
 * Implemented by objects that have enough oversight to look for the 'latest' version of some {@link Declaration} object, aka the {@link Declaration} object that has been read from the most recent version of some fragment (file :D, or... location in some file or what have you). 
 * @author madeen
 *
 */
public interface ILatestDeclarationVersionProvider {
	/**
	 * Return the latest {@link Declaration} object that was read from the same fragment as the one passed. 
	 * @param <T>
	 * @param from
	 * @return
	 */
	<T extends Declaration> T latestVersionOf(T from);
}
