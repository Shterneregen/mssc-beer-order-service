package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.VALIDATE_ORDER;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.VALIDATION_FAILED;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.VALIDATION_PASSED;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.DELIVERED;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.DELIVERY_EXCEPTION;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.NEW;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.PICKED_UP;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.VALIDATED;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.VALIDATION_EXCEPTION;

@Configuration
@EnableStateMachineFactory
public class BeerOrderStateMachineConfig
        extends StateMachineConfigurerAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {

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
                .withExternal().source(NEW).target(NEW).event(VALIDATE_ORDER).and()
                .withExternal().source(NEW).target(VALIDATED).event(VALIDATION_PASSED).and()
                .withExternal().source(NEW).target(VALIDATION_EXCEPTION).event(VALIDATION_FAILED);
    }
}
