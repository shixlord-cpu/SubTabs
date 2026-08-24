import { Order } from './order.model';
import { OrderDto } from './order.dto';

export function toOrder(dto: OrderDto): Order {
  return dto;
}
