package net.arctics.clonk;

import static java.util.Arrays.stream;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Main entry class without static dependencies which populates a new {@link URLClassLoader} with {@link URL}s to Eclipe's jars
 * and then boots up the actual {@link CLI} class using it.
 * @author madeen
 */
public class CLIEntry {
	
	final static URL[] urlArrayTemplate = new URL[0];
	
	interface Throwy<I, O> {
		O apply(I input) throws Throwable; 
	}
	
	static <I, O> Function<I, O> mungeException(Throwy<I, O> throwy) {
		return input -> {
			try {
				return throwy.apply(input);
			} catch (final Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		};
	}
	
	static File findEclipsePluginsFolder(String eclipseHome) {
		if (eclipseHome == null) {
			throw new IllegalArgumentException("Missing ECLIPSE_HOME");
		}
		
		class Walk {
			Stream<File> walk(File file) {
				return Stream.concat(
					stream(new File[] { file }),
					file.isDirectory() ? stream(file.listFiles()).flatMap(this::walk) : stream(new File[0])
				);
			}
		}
		
		return new Walk().walk(new File(eclipseHome.replaceFirst("^~", System.getProperty("user.home"))))
			.filter(file -> file.isDirectory() && file.getName().equals("plugins"))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Cannot find Eclipse plugins folder"));
	}
	
	public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException {
		final File pluginsFolder = findEclipsePluginsFolder(
			System.getenv().get("ECLIPSE_HOME")
		);
		
		final URL[] jarls = Stream.concat(
			stream(new URL[] {
				CLIEntry.class.getClassLoader().getResource(".")
			}),
			stream(pluginsFolder.listFiles(file -> file.getName().endsWith(".jar")))
				.map(File::toURI)
				.map(mungeException(URI::toURL))
		).toArray(length -> new URL[length]);
		
		final ClassLoader parentClassLoader =
			//Thread.currentThread().getContextClassLoader()
			null
			;
		
		try (final URLClassLoader loader = new URLClassLoader(jarls, parentClassLoader)) {
			final Class<?> actualMainClass = loader.loadClass("net.arctics.clonk.CLI");
			final Method main = actualMainClass.getMethod("main", new Class<?>[] { String[].class });
			final String[] actualArgs = Stream.concat(
				stream(new String[] {
					"--engine",
					"OpenClonk",
					"--engineConfigurationRoot",
					CLIEntry.class.getClassLoader().getResource("./res/engines").toExternalForm().replace("file:", ""),
				}),
				stream(args)
			).toArray(length -> new String[length]);
			main.invoke(null, new Object[] { actualArgs });
		}
	}
}
