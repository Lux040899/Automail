package exceptions;

/**
 * An exception thrown when a mail that is already delivered attempts to be delivered again.
 */
public class MailAlreadyDeliveredException extends Throwable    {
    /**
     *
     * @serial auto-generated
     */
    private static final long serialVersionUID = 5930069006714908031L;

    public MailAlreadyDeliveredException() {
        super("This mail has already been delivered!");
    }
}
