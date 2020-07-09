package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.brewery.model.BeerOrderDto;

import java.util.UUID;

public interface BeerOrderManager {

	BeerOrder newBeerOrder(BeerOrder beerOrder);

	void processValidationResult(UUID beerOrderId, Boolean isValid);

	void processValidationPassed(UUID beerOrderId);

	void beerOrderAllocationPassed(BeerOrderDto beerOrder);

	void beerOrderAllocationPendingInventory(BeerOrderDto beerOrder);

	void beerOrderAllocationFailed(BeerOrderDto beerOrder);

	void beerOrderPickedUp(UUID id);

	void cancelOrder(UUID id);
}
