package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.ValidateOrderRequest;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationListener {

	private final JmsTemplate jmsTemplate;

	@JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
	public void list(Message msg) {
		boolean isValid = true;

		ValidateOrderRequest request = (ValidateOrderRequest) msg.getPayload();
		log.debug("Validate order test listener for order [{}]", request.getBeerOrderDto().getId());

		// condition to fail validation
		if ("fail-validation".equals(request.getBeerOrderDto().getCustomerRef())) {
			log.debug("Order is not valid");
			isValid = false;
		}

		jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
				ValidateOrderResult.builder()
						.isValid(isValid)
						.orderId(request.getBeerOrderDto().getId())
						.build());
	}
}
