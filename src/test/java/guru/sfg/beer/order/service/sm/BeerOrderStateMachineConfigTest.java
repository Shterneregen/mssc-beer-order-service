package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class BeerOrderStateMachineConfigTest {

    @Autowired
    private StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> factory;

    @Test
    public void testValidateOrderEvent() {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = factory.getStateMachine(UUID.randomUUID());
        sm.start();
        sm.sendEvent(BeerOrderEventEnum.VALIDATE_ORDER);
        assertEquals(BeerOrderStatusEnum.NEW, sm.getState().getId());
    }

    @Test
    public void testValidationPassedEvent() {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = factory.getStateMachine(UUID.randomUUID());
        sm.start();
        sm.sendEvent(BeerOrderEventEnum.VALIDATION_PASSED);
        assertEquals(BeerOrderStatusEnum.VALIDATED, sm.getState().getId());
    }

    @Test
    public void testValidationFailedEvent() {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = factory.getStateMachine(UUID.randomUUID());
        sm.start();
        sm.sendEvent(BeerOrderEventEnum.VALIDATION_FAILED);
        assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION, sm.getState().getId());
    }
}
