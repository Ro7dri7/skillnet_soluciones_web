import {
  Component,
  ElementRef,
  HostListener,
  OnInit,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-searchable-select',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './searchable-select.component.html',
})
export class SearchableSelectComponent implements OnInit {
  readonly label = input.required<string>();
  readonly placeholder = input('Busca o selecciona…');
  readonly options = input.required<readonly string[]>();
  readonly value = input<string>('');

  readonly valueChange = output<string>();

  readonly searchTerm = signal('');
  readonly isOpen = signal(false);
  readonly selected = signal<string | null>(null);

  private readonly dropdownRef = viewChild<ElementRef<HTMLElement>>('dropdown');

  @HostListener('document:mousedown', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const el = this.dropdownRef()?.nativeElement;
    if (el && !el.contains(event.target as Node)) {
      this.isOpen.set(false);
    }
  }

  ngOnInit(): void {
    const initial = this.value();
    if (initial) {
      this.selected.set(initial);
      this.searchTerm.set(initial);
    }
  }

  filteredOptions(): string[] {
    const term = this.searchTerm().toLowerCase();
    if (!term) {
      return [...this.options()];
    }
    return this.options().filter((o) => o.toLowerCase().includes(term));
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.isOpen.set(true);
    if (this.selected() && value !== this.selected()) {
      this.selected.set(null);
    }
  }

  selectOption(option: string): void {
    this.selected.set(option);
    this.searchTerm.set(option);
    this.isOpen.set(false);
    this.valueChange.emit(option);
  }

  toggleOpen(): void {
    this.isOpen.update((v) => !v);
  }
}
