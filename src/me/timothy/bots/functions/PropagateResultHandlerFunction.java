package me.timothy.bots.functions;

import me.timothy.bots.memory.PropagateResult;

public interface PropagateResultHandlerFunction {
	/**
	 * Do the things that the result says to do.
	 * 
	 * @param result the result
	 * @return if any reddit requests were done
	 */
	public boolean handleResult(PropagateResult result);
}
