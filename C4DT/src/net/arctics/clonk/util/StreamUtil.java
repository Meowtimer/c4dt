package net.arctics.clonk.util;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.Utilities.as;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.arctics.clonk.Core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;

public class StreamUtil {

	public static String stringFromReader(final Reader reader) {
		final char[] buffer = new char[1024];
		int read;
		final StringBuilder builder = new StringBuilder(1024);
		try {
			while ((read = reader.read(buffer)) > 0)
				builder.append(buffer, 0, read);
		} catch (final IOException e) {
			return "";
		}
		return builder.toString();
	}

	public static String stringFromInputStream(final InputStream stream, final String encoding) throws IOException {
		try (InputStreamReader inputStreamReader = new InputStreamReader(stream, encoding)) {
			return stringFromReader(inputStreamReader);
		}
	}

	public static String stringFromInputStream(final InputStream stream) {
		try {
			return stringFromInputStream(stream, "UTF8"); //$NON-NLS-1$
		} catch (final IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String stringFromURL(final URL url) {
		InputStream stream;
		try {
			stream = url.openStream();
		} catch (final IOException e1) {
			e1.printStackTrace();
			return null;
		}
		try {
			return StreamUtil.stringFromInputStream(stream);
		} finally {
			try {
				stream.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String stringFromFile(final IFile file) {
		InputStream stream;
		try {
			stream = file.getContents();
		} catch (final CoreException e1) {
			e1.printStackTrace();
			return null;
		}
		try {
			return stringFromInputStream(stream);
		} finally {
			try {
				stream.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String stringFromFile(final File file) {
		InputStream stream;
		try {
			stream = new FileInputStream(file);
		} catch (final FileNotFoundException e) {
			return "";
		}
		try {
			return stringFromInputStream(stream);
		} finally {
			try {
				stream.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String stringFromStorage(final IStorage storage) {
		if (storage instanceof IFile)
			return Core.instance().performActionsOnFileDocument(storage, document -> document.get(), false);
		else
			try (InputStream s = storage.getContents()) {
				return stringFromInputStream(s);
			} catch (IOException | CoreException e) {
				e.printStackTrace();
				return "";
			}
	}

	public static String stringFromFileDocument(final IFile file) {
		return Core.instance().performActionsOnFileDocument(file, document -> document.get(), false);
	}

	@FunctionalInterface
	public interface StreamWriteRunnable {
		void run(File file, OutputStream stream, OutputStreamWriter writer) throws IOException;
	}

	public static void writeToFile(final File file, final StreamWriteRunnable runnable) throws IOException {
		final FileOutputStream s = new FileOutputStream(file);
		try {
			final OutputStreamWriter writer = new OutputStreamWriter(s);
			try {
				runnable.run(file, s, writer);
			} finally {
				writer.close();
			}
		} finally {
			s.close();
		}
	}

	public static void transfer(final InputStream source, final OutputStream dest) throws IOException {
		final byte[] buffer = new byte[1024];
		int read;
		while ((read = source.read(buffer)) != -1)
			dest.write(buffer, 0, read);
	}

	public static FilenameFilter patternFilter(final String pattern) {
		return new FilenameFilter() {
			private final Pattern p = StringUtil.patternFromRegExOrWildcard(pattern);
			@Override
			public boolean accept(final File dir, final String name) {
				return p.matcher(name).matches();
			}
		};
	}

	public static <T, C> Stream<C> ofType(Stream<T> stream, Class<C> cls) {
		return stream.map(i -> as(i, cls)).filter(i -> i != null);
	}

	public static byte[] bytesFromURL(URL f) {
		try (InputStream s = f.openStream()) {
			return bytesFromInputStream(s);
		} catch (final IOException e) {
			return null;
		}
	}

	public static byte[] bytesFromInputStream(InputStream s) throws IOException {
		try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			int r;
			final byte[] buf = new byte[1024];
			while ((r = s.read(buf)) > 0)
				out.write(buf, 0, r);
			return out.toByteArray();
		}
	}

	public static byte[] bytesFromFile(File file) {
		try (FileInputStream fs = new FileInputStream(file)) {
			return bytesFromInputStream(fs);
		} catch (final IOException e) {
			return null;
		}
	}

	@SafeVarargs
	public static <T> Stream<? extends T> concatStreams(Stream<? extends T>... streams) {
		return stream(streams).reduce(Stream.empty(), Stream::concat);
	}

	public static ByteArrayInputStream inputStreamFromString(final String text_) throws UnsupportedEncodingException {
		return new ByteArrayInputStream(text_.getBytes("UTF8"));
	}

}
