package com.pushpakraut.invoice.customer;

import jakarta.persistence.*;

@Entity
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false)
    private String name;

    private String email;

    protected Customer() {}

    public Customer(String ownerEmail, String name, String email) {
        this.ownerEmail = ownerEmail;
        this.name = name;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
}
