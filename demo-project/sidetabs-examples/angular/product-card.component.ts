import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-product-card',
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.scss',
})
export class ProductCardComponent {
  @Input() name = 'Mug';
  @Input() price = 12;
  selected = false;

  toggleSelected(): void {
    this.selected = !this.selected;
  }

  label(): string {
    return `${this.name} · ${this.price.toFixed(2)} €`;
  }
}
