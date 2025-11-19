package com.example.interfacesnaturales.comprarvoz;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase estática (singleton simple) para almacenar los items del carrito
 * de forma persistente entre Activities.
 */
public class Cart {
    // Clase interna para representar un ítem con nombre y precio
    public static class CartItem {
        public String name;
        public double price;

        public CartItem(String name, double price) {
            this.name = name;
            this.price = price;
        }

        @Override
        public String toString() {
            return name + " (€" + String.format("%.2f", price) + ")";
        }
    }

    private static final List<CartItem> items = new ArrayList<>();

    public static void addItem(String name, double price) {
        items.add(new CartItem(name, price));
    }

    public static List<CartItem> getItems() {
        return items;
    }

    public static double getTotal() {
        double total = 0;
        for (CartItem item : items) {
            total += item.price;
        }
        return total;
    }

    public static void clearCart() {
        items.clear();
    }
}