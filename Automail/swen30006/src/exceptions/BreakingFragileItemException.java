package exceptions;

/**
 * An exception throws when robot wrongly handle fragile item
 */
public class BreakingFragileItemException extends Exception {
	/**
	 *
	 * @serial auto-generated
	 */
	private static final long serialVersionUID = -4678468475075922472L;

	public BreakingFragileItemException() {
		super("Breaking Fragile Item!!");
	}
}
