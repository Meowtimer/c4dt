package net.arctics.clonk.util;

import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * Like IRunnableWithProgress but with a method to obtain the result of the operation
 * @author Madeen
 *
 * @param <ResultType>
 */
public interface IRunnableWithProgressAndResult<ResultType> extends IRunnableWithProgress {
	ResultType getResult();
}
