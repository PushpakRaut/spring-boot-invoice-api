package com.pushpakraut.invoice.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByOwnerEmailOrderByDueDateDesc(String ownerEmail);
    List<Invoice> findByOwnerEmailAndStatusOrderByDueDateDesc(String ownerEmail, Invoice.Status status);
    Optional<Invoice> findByIdAndOwnerEmail(Long id, String ownerEmail);
    boolean existsByCustomer_Id(Long customerId);
}
