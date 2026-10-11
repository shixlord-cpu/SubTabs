namespace Demo.Orders;

public sealed class OrdersController
{
    private readonly OrdersService _orders;

    public OrdersController(OrdersService orders) => _orders = orders;
}
