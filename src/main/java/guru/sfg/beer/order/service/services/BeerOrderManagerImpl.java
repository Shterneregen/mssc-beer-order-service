package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStateChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;

import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.ALLOCATE_ORDER;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.ALLOCATION_FAILED;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.ALLOCATION_NO_INVENTORY;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.ALLOCATION_SUCCESS;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.VALIDATE_ORDER;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.VALIDATION_FAILED;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.VALIDATION_PASSED;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.NEW;

@Slf4j
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {

	public static final String ORDER_ID_HEADER = "beer_order_id_header";

	@Autowired
	private StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
	@Autowired
	private BeerOrderRepository beerOrderRepository;
	@Autowired
	private BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;
	@Autowired
	private EntityManager entityManager;

	@Transactional
	@Override
	public BeerOrder newBeerOrder(BeerOrder beerOrder) {
		log.debug("Creating new order...");
		beerOrder.setId(null);
		beerOrder.setOrderStatus(NEW);

		BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
		log.debug("New order id: [{}]", savedBeerOrder.getId());
		sendBeerOrderEvent(savedBeerOrder, VALIDATE_ORDER);
		return savedBeerOrder;
	}

	@Transactional
	@Override
	public void processValidationResult(UUID beerOrderId, Boolean isValid) {
		log.debug("Process Validation Result [{}] Valid [{}]", beerOrderId, isValid);
		entityManager.flush();

		Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);

		beerOrderOptional.ifPresentOrElse(beerOrder -> {
			if (isValid) {
				log.debug("Order is valid");
				sendBeerOrderEvent(beerOrder, VALIDATION_PASSED);
//				BeerOrder validatedOrder = beerOrderRepository.findById(beerOrderId).get();
//				sendBeerOrderEvent(validatedOrder, ALLOCATE_ORDER);
			} else {
				log.debug("Order is not valid");
				sendBeerOrderEvent(beerOrder, VALIDATION_FAILED);
			}
		}, () -> log.error("Order Not Found. Id: {}", beerOrderId));
	}

	@Transactional
	@Override
	public void processValidationPassed(UUID beerOrderId) {
		log.debug("Process Validation passed [{}]", beerOrderId);
		Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);
		beerOrderOptional.ifPresentOrElse(beerOrder -> {
			sendBeerOrderEvent(beerOrder, ALLOCATE_ORDER);
		}, () -> log.error("Order Not Found. Id: {}", beerOrderId));
	}

	@Override
	public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
		log.debug("Beer order allocation passed [{}]", beerOrderDto.getId());
		Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());

		beerOrderOptional.ifPresentOrElse(beerOrder -> {
			sendBeerOrderEvent(beerOrder, ALLOCATION_SUCCESS);
			updateAllocatedQty(beerOrderDto);
		}, () -> log.error("Order Id Not Found: {}", beerOrderDto.getId()));
	}

	@Override
	public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
		log.debug("Beer order allocation pending inventory [{}]", beerOrderDto.getId());
		Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());

		beerOrderOptional.ifPresentOrElse(beerOrder -> {
			sendBeerOrderEvent(beerOrder, ALLOCATION_NO_INVENTORY);

			updateAllocatedQty(beerOrderDto);
		}, () -> log.error("Order Id Not Found: {}", beerOrderDto.getId()));
	}

	private void updateAllocatedQty(BeerOrderDto beerOrderDto) {
		log.debug("Updating allocated quantity [{}]", beerOrderDto.getId());
		Optional<BeerOrder> allocatedOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());

		allocatedOrderOptional.ifPresentOrElse(allocatedOrder -> {
			allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
				beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
					if (beerOrderLine.getId().equals(beerOrderLineDto.getId())) {
						beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
					}
				});
			});

			beerOrderRepository.saveAndFlush(allocatedOrder);
		}, () -> log.error("Order Not Found. Id: {}", beerOrderDto.getId()));
	}

	@Override
	public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
		log.debug("Beer order allocation failed [{}]", beerOrderDto.getId());
		Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());

		beerOrderOptional.ifPresentOrElse(beerOrder -> {
			sendBeerOrderEvent(beerOrder, ALLOCATION_FAILED);
		}, () -> log.error("Order Not Found. Id: {}", beerOrderDto.getId()));

	}

	@Override
	public void beerOrderPickedUp(UUID id) {
		log.debug("Beer order picked up [{}]", id);
		Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(id);

		beerOrderOptional.ifPresentOrElse(beerOrder -> {
			//do process
			sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.BEER_ORDER_PICKED_UP);
		}, () -> log.error("Order Not Found. Id: {}", id));
	}

	@Override
	public void cancelOrder(UUID id) {
		beerOrderRepository.findById(id).ifPresentOrElse(beerOrder -> {
			sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.CANCEL_ORDER);
		}, () -> log.error("Order Not Found. Id: " + id));
	}

	private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum) {
		log.debug("Sending event [{}] for order [{}]", eventEnum, beerOrder.getId());
		StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(beerOrder);
		Message msg = MessageBuilder.withPayload(eventEnum)
				.setHeader(ORDER_ID_HEADER, beerOrder.getId().toString())
				.build();
		sm.sendEvent(msg);
	}

	private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
		StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine(beerOrder.getId());

		sm.stop();
		sm.getStateMachineAccessor().doWithAllRegions(sma -> {
			sma.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
			sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
		});
		sm.start();

		return sm;
	}
}
