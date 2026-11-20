package com.pushpakraut.invoice.invoice;

import com.pushpakraut.invoice.customer.Customer;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Invoice {
    public enum Status { DRAFT, SENT, PAID }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ownerEmail;

    @ManyToOne(optional = false)
    private Customer customer;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @ElementCollection
    @CollectionTable(name = "invoice_items")
    private List<LineItem> items = new ArrayList<>();

    @Embeddable
    public record LineItem(String description, int quantity, BigDecimal unitPrice) {
        public BigDecimal total() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
    }

    protected Invoice() {}

    public Invoice(String ownerEmail, Customer customer, LocalDate dueDate, List<LineItem> items) {
        this.ownerEmail = ownerEmail;
        this.customer = customer;
        this.dueDate = dueDate;
        this.items = new ArrayList<>(items);
    }

    public BigDecimal getTotal() {
        return items.stream().map(LineItem::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }
    public String getOwnerEmail() { return ownerEmail; }
    public Customer getCustomer() { return customer; }
    public LocalDate getDueDate() { return dueDate; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<LineItem> getItems() { return items; }
}
