/*
 * Copyright 2017-2018, Société Générale All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.societegenerale.commons.amqp.core.recoverer;

import static com.societegenerale.commons.amqp.core.constant.MessageHeaders.CORRELATION_ID;
import static com.societegenerale.commons.amqp.core.constant.MessageHeaders.X_DEAD_DETTER_EXCHANGE;
import static com.societegenerale.commons.amqp.core.constant.MessageHeaders.X_DEAD_LETTER_QUEUE;
import static com.societegenerale.commons.amqp.core.constant.MessageHeaders.X_EXCEPTION_MESSAGE;
import static com.societegenerale.commons.amqp.core.constant.MessageHeaders.X_EXCEPTION_ROOT_CAUSE_MESSAGE;
import static com.societegenerale.commons.amqp.core.constant.MessageHeaders.X_EXCEPTION_STACKTRACE;
import static com.societegenerale.commons.amqp.core.constant.MessageHeaders.X_ORIGINAL_EXCHANGE;
import static com.societegenerale.commons.amqp.core.constant.MessageHeaders.X_ORIGINAL_QUEUE;
import static com.societegenerale.commons.amqp.core.constant.MessageHeaders.X_ORIGINAL_ROUTINGKEY;
import static com.societegenerale.commons.amqp.core.constant.MessageHeaders.X_RECOVER_TIME;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.beans.factory.annotation.Autowired;

import com.societegenerale.commons.amqp.core.config.RabbitConfig;
import com.societegenerale.commons.amqp.core.recoverer.handler.MessageExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Anand Manissery on 7/13/2017.
 */
@Slf4j
public class DeadLetterMessageRecoverer implements MessageRecoverer {

	@Autowired
	private AmqpTemplate errorTemplate;

	@Autowired
	private RabbitConfig rabbitmqProperties;

	@Autowired(required = false)
	private List<MessageExceptionHandler> messageExceptionHandlers = new ArrayList<>();

	@Override
	public void recover(final Message message, final Throwable cause) {
		Map<String, Object> headers = message.getMessageProperties().getHeaders();
		headers.put(X_EXCEPTION_STACKTRACE.value(), ExceptionUtils.getFullStackTrace(cause));
		headers.put(X_EXCEPTION_MESSAGE.value(), ExceptionUtils.getMessage(cause));
		headers.put(X_EXCEPTION_ROOT_CAUSE_MESSAGE.value(), ExceptionUtils.getRootCauseMessage(cause));
		headers.put(X_ORIGINAL_EXCHANGE.value(), message.getMessageProperties().getReceivedExchange());
		headers.put(X_ORIGINAL_ROUTINGKEY.value(), message.getMessageProperties().getReceivedRoutingKey());
		headers.put(X_ORIGINAL_QUEUE.value(), message.getMessageProperties().getConsumerQueue());
		headers.put(X_RECOVER_TIME.value(), Instant.now());
		String deadLetterExchangeName = rabbitmqProperties.getDeadLetterConfig().getDeadLetterExchange().getName();
		String deadLetterRoutingKey = rabbitmqProperties.getDeadLetterConfig()
				.createDeadLetterQueueName(message.getMessageProperties().getConsumerQueue());
		headers.put(X_DEAD_DETTER_EXCHANGE.value(), deadLetterExchangeName);
		headers.put(X_DEAD_LETTER_QUEUE.value(), deadLetterRoutingKey);
		if (headers.containsKey(CORRELATION_ID.value())) {
			message.getMessageProperties().setCorrelationId((String) headers.get(CORRELATION_ID.value()));
		}

		headers.putAll(loadAdditionalHeaders(message, cause));

		for (MessageExceptionHandler messageExceptionHandler : messageExceptionHandlers) {
			try {
				messageExceptionHandler.handle(message, cause);
			} catch (Exception e) {
				// To catch any exception in the MessageExceptionHandler to avoid the
				// interruption in other MessageExceptionHandlers
				log.error("Exception occurred while processing '{}' message exception handler.",
						messageExceptionHandler, e);
			}
		}

		this.errorTemplate.send(deadLetterExchangeName, deadLetterRoutingKey, message);

		log.warn("Republishing failed message to exchange '{}', routing key '{}', message {{}} , cause {}",
				deadLetterExchangeName, deadLetterRoutingKey, message, cause);

	}

	/**
	 * This is a dummy implementation that doesn't do anything.. If you extend this
	 * class, you can simply override this method to provide your own additional
	 * headers
	 * 
	 * @param message
	 * @param cause
	 * @return
	 */
	protected Map<String, Object> loadAdditionalHeaders(Message message, Throwable cause) {
		log.info("No additional headers added for message {}, cause {}", message,
				cause == null ? null : cause.getMessage());
		return new HashMap<>();
	}

}
