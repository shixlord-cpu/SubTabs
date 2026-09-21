import { Component } from '@angular/core';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  title = 'Catalog';
  compact = false;

  toggleCompact(): void {
    this.compact = !this.compact;
  }
  heading(): string {
    return this.compact ? this.title : `${this.title} overview`;
  }
}
