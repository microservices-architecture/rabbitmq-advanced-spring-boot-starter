/**
 * 
 */
package com.societegenerale.commons.amqp.core.constant;

/**
 * Define the headers for the message in the queue.
 * 
 * @author LoiNX
 *
 */
public enum MessageHeaders {
	X_EXCEPTION_STACKTRACE("x-exception-stacktrace"), X_EXCEPTION_MESSAGE("x-exception-message"),
	X_EXCEPTION_ROOT_CAUSE_MESSAGE("x-exception-root-cause-message"), X_ORIGINAL_EXCHANGE("x-original-exchange"),
	X_ORIGINAL_ROUTINGKEY("x-original-routingKey"), X_ORIGINAL_QUEUE("x-original-queue"),
	X_RECOVER_TIME("x-recover-time"), X_DEAD_DETTER_EXCHANGE("x-dead-letter-exchange"),
	X_DEAD_LETTER_QUEUE("x-dead-letter-queue"), CORRELATION_ID("correlation-id"), APPLICATION_NAME("application-name"),
	EXECUTION_TIME("execution-time"), INFO("info");

	private String value;

	private MessageHeaders(String value) {
		setValue(value);
	}

	private void setValue(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}
}
