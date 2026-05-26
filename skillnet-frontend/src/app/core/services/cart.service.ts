import { Injectable, computed, effect, signal } from '@angular/core';

import { CartItem } from '../models/cart-item.model';

import { MarketplaceCourse } from '../../features/marketplace/models/marketplace-course.model';



const CART_STORAGE_KEY = 'skillnet_cart';



function loadStoredCart(): CartItem[] {

  if (typeof localStorage === 'undefined') {

    return [];

  }

  try {

    const raw = localStorage.getItem(CART_STORAGE_KEY);

    if (!raw) {

      return [];

    }

    const parsed = JSON.parse(raw) as CartItem[];

    return Array.isArray(parsed) ? parsed : [];

  } catch {

    return [];

  }

}



@Injectable({ providedIn: 'root' })

export class CartService {

  private readonly _cartItems = signal<CartItem[]>(loadStoredCart());



  readonly cartItems = this._cartItems.asReadonly();



  readonly cartCount = computed(() => this._cartItems().length);



  readonly cartTotal = computed(() =>

    this._cartItems().reduce((sum, item) => sum + item.price, 0),

  );



  constructor() {

    effect(() => {

      if (typeof localStorage === 'undefined') {

        return;

      }

      localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(this._cartItems()));

    });

  }



  isInCart(courseId: number): boolean {

    return this._cartItems().some((item) => item.id === courseId);

  }



  addToCart(course: MarketplaceCourse | CartItem): void {

    const item = this.normalizeItem(course);

    if (this.isInCart(item.id)) {

      return;

    }

    this._cartItems.update((items) => [...items, item]);

  }



  removeFromCart(courseId: number): void {

    this._cartItems.update((items) => items.filter((item) => item.id !== courseId));

  }



  toggleCart(course: MarketplaceCourse | CartItem): void {

    const item = this.normalizeItem(course);

    if (this.isInCart(item.id)) {

      this.removeFromCart(item.id);

    } else {

      this.addToCart(course);

    }

  }



  clearCart(): void {

    this._cartItems.set([]);

  }



  private normalizeItem(course: MarketplaceCourse | CartItem): CartItem {

    if ('instructor' in course && typeof course.instructor === 'string') {

      return course as CartItem;

    }

    const marketplace = course as MarketplaceCourse;

    return {

      id: marketplace.id,

      title: marketplace.title,

      price: marketplace.price,

      imageUrl: marketplace.imageUrl,

      instructor: marketplace.professorName,

    };

  }

}


