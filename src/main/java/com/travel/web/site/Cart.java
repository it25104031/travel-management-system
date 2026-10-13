package com.travel.web.site;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The visitor's cart, kept in their HTTP session. Anyone can fill it; signing in is only
 * needed at checkout. Only ids, dates and traveller counts are stored - prices are always
 * read fresh from the package when the cart is shown or checked out.
 */
@Component
@SessionScope
public class Cart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    public static final int MAX_ITEMS = 10;

    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private final String id = UUID.randomUUID().toString().substring(0, 8);
        private final Integer packageId;
        private LocalDate travelDate;
        private int travellers;

        public Item(Integer packageId, LocalDate travelDate, int travellers) {
            this.packageId = packageId;
            this.travelDate = travelDate;
            this.travellers = travellers;
        }

        public String getId() { return id; }
        public Integer getPackageId() { return packageId; }
        public LocalDate getTravelDate() { return travelDate; }
        public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }
        public int getTravellers() { return travellers; }
        public void setTravellers(int travellers) { this.travellers = travellers; }
    }

    private final List<Item> items = new ArrayList<>();

    public synchronized List<Item> getItems() { return List.copyOf(items); }

    public synchronized void add(Item item) {
        if (items.size() >= MAX_ITEMS) throw new IllegalArgumentException("Your cart can hold up to " + MAX_ITEMS + " packages.");
        items.add(item);
    }

    public synchronized Optional<Item> find(String id) {
        return items.stream().filter(i -> i.getId().equals(id)).findFirst();
    }

    public synchronized void remove(String id) { items.removeIf(i -> i.getId().equals(id)); }

    public synchronized void clear() { items.clear(); }

    public synchronized boolean isEmpty() { return items.isEmpty(); }
}
