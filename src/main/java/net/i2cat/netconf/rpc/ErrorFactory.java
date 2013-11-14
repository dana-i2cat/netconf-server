package net.i2cat.netconf.rpc;

/**
 * Factory class to create {@link Error}'s for Netconf {@link Reply}'s
 * 
 * @author Julio Carlos Barrera
 * 
 */
public class ErrorFactory {

	/**
	 * Constructs a valid error using parameters
	 * 
	 * @param type
	 *            {@link ErrorType}
	 * @param tag
	 *            {@link ErrorTag}
	 * @param severity
	 *            {@link ErrorSeverity}
	 * @param appTag
	 *            Application tag (optional)
	 * @param path
	 *            Error path (optional)
	 * @param message
	 *            Error message (optional)
	 * @param info
	 *            Error info (optional)
	 * @return
	 */
	public static Error newError(ErrorType type, ErrorTag tag, ErrorSeverity severity, String appTag, String path, String message, String info) {
		Error error = new Error();
		error.setType(type);
		error.setTag(tag);
		error.setSeverity(severity);
		error.setAppTag(appTag);
		error.setPath(path);
		error.setMessage(message);
		error.setInfo(info);
		return error;
	}

}
