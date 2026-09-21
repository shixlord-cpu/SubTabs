package shop

class OrderService(
    private val orders: MutableList<Order> = mutableListOf()
) {
    fun add(order: Order) {
        orders += order
    }

    fun total(): Double = orders.sumOf { it.amount }
}

data class Order(val id: String, val amount: Double)

fun sampleOrders(): List<Order> = listOf(
    Order("A-1", 12.0),
    Order("A-2", 8.5),
)
