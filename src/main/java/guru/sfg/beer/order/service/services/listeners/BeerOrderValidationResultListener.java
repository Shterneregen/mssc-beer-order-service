package guru.sfg.beer.order.service.services.listeners;

import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import static guru.sfg.beer.order.service.config.JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = VALIDATE_ORDER_RESPONSE_QUEUE)
    public void listenOrderValidationResult(ValidateOrderResult validateOrderResult) {
        log.debug("Validate order result [{}] for order [{}]",
                validateOrderResult.isValid(), validateOrderResult.getId());
        beerOrderManager.processValidationResult(validateOrderResult.getId(), validateOrderResult.isValid());
    }
}
