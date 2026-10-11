namespace Demo.Orders;

public sealed class OrdersService
{
    private readonly OrdersRepository _repository;

    public OrdersService(OrdersRepository repository) => _repository = repository;
}
