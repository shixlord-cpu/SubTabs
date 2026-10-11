package demo.order;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }
}
