package exceptions;

/**
 * An exception thrown when the robot tries to deliver more items than its tube capacity without refilling.
 */
public class ExcessiveDeliveryException extends Throwable {
	/**
	 *
	 * @serial auto-generated
	 */
	private static final long serialVersionUID = 142582747286413119L;

	public ExcessiveDeliveryException() {
		super("Attempting to deliver more than 2 items in a single trip!!");
	}
}
