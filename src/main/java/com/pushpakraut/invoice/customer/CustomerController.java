package com.pushpakraut.invoice.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.pushpakraut.invoice.invoice.InvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    public record CustomerRequest(@NotBlank @Size(max = 200) String name, @Email @Size(max = 254) String email) {}
    public record CustomerResponse(Long id, String name, String email) {
        static CustomerResponse of(Customer c) { return new CustomerResponse(c.getId(), c.getName(), c.getEmail()); }
    }

    private final CustomerRepository repo;
    private final InvoiceRepository invoices;

    public CustomerController(CustomerRepository repo, InvoiceRepository invoices) {
        this.repo = repo;
        this.invoices = invoices;
    }

    @GetMapping
    public List<CustomerResponse> list(@AuthenticationPrincipal String owner) {
        return repo.findByOwnerEmailOrderByNameAsc(owner).stream().map(CustomerResponse::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@AuthenticationPrincipal String owner, @Valid @RequestBody CustomerRequest r) {
        return CustomerResponse.of(repo.save(new Customer(owner, r.name(), r.email())));
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@AuthenticationPrincipal String owner, @PathVariable Long id,
                                   @Valid @RequestBody CustomerRequest r) {
        Customer c = find(owner, id);
        c.setName(r.name());
        c.setEmail(r.email());
        return CustomerResponse.of(repo.save(c));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String owner, @PathVariable Long id) {
        Customer c = find(owner, id);
        if (invoices.existsByCustomer_Id(c.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer has invoices. Delete them first.");
        }
        repo.delete(c);
    }

    private Customer find(String owner, Long id) {
        // Scoped by owner so one user can never read another user's data
        return repo.findByIdAndOwnerEmail(id, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }
}
