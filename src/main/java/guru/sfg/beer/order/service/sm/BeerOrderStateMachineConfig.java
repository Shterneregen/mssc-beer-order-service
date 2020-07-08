package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.ALLOCATE_ORDER;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.ALLOCATION_FAILED;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.ALLOCATION_NO_INVENTORY;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.ALLOCATION_SUCCESS;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.BEER_ORDER_PICKED_UP;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.VALIDATE_ORDER;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.VALIDATION_FAILED;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.VALIDATION_PASSED;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.ALLOCATED;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.ALLOCATION_EXCEPTION;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.ALLOCATION_PENDING;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.DELIVERED;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.DELIVERY_EXCEPTION;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.NEW;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.PENDING_INVENTORY;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.PICKED_UP;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.VALIDATED;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.VALIDATION_EXCEPTION;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.VALIDATION_PENDING;

@RequiredArgsConstructor
@Configuration
@EnableStateMachineFactory
public class BeerOrderStateMachineConfig
		extends StateMachineConfigurerAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {

	private final Action<BeerOrderStatusEnum, BeerOrderEventEnum> validateOrderAction;
	private final Action<BeerOrderStatusEnum, BeerOrderEventEnum> allocateOrderAction;
	private final Action<BeerOrderStatusEnum, BeerOrderEventEnum> validateFailureAction;

	@Override
	public void configure(StateMachineStateConfigurer<BeerOrderStatusEnum, BeerOrderEventEnum> states) throws Exception {
		states.withStates()
				.initial(NEW)
				.states(EnumSet.allOf(BeerOrderStatusEnum.class))
				.end(DELIVERED)
				.end(PICKED_UP)
				.end(DELIVERY_EXCEPTION)
				.end(VALIDATED)
				.end(VALIDATION_EXCEPTION);
	}

	@Override
	public void configure(StateMachineTransitionConfigurer<BeerOrderStatusEnum, BeerOrderEventEnum> transitions) throws Exception {
		transitions
				.withExternal().source(NEW).target(VALIDATION_PENDING).event(VALIDATE_ORDER).action(validateOrderAction).and()
				.withExternal().source(VALIDATION_PENDING).target(VALIDATED).event(VALIDATION_PASSED).and()
				.withExternal().source(VALIDATION_PENDING).target(VALIDATION_EXCEPTION).event(VALIDATION_FAILED).action(validateFailureAction).and()
				.withExternal().source(VALIDATED).target(ALLOCATION_PENDING).event(ALLOCATE_ORDER).action(allocateOrderAction).and()
				.withExternal().source(ALLOCATION_PENDING).target(ALLOCATED).event(ALLOCATION_SUCCESS).and()
				.withExternal().source(ALLOCATION_PENDING).target(ALLOCATION_EXCEPTION).event(ALLOCATION_FAILED).and()
				.withExternal().source(ALLOCATION_PENDING).target(PENDING_INVENTORY).event(ALLOCATION_NO_INVENTORY).and()
				.withExternal().source(ALLOCATED).target(BeerOrderStatusEnum.PICKED_UP).event(BEER_ORDER_PICKED_UP);
	}
}
