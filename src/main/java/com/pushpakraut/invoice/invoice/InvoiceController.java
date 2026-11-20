package com.pushpakraut.invoice.invoice;

import com.pushpakraut.invoice.customer.Customer;
import com.pushpakraut.invoice.customer.CustomerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    public record ItemRequest(@NotBlank @Size(max = 500) String description, @Min(1) @Max(100_000) int quantity,
                              @NotNull @DecimalMin("0.00") @DecimalMax("1000000.00") @Digits(integer = 7, fraction = 2) BigDecimal unitPrice) {}
    public record InvoiceRequest(@NotNull Long customerId, @NotNull LocalDate dueDate,
                                 @NotEmpty @Size(max = 100) List<@Valid ItemRequest> items) {}
    public record StatusRequest(@NotNull Invoice.Status status) {}
    public record InvoiceResponse(Long id, Long customerId, String customerName, LocalDate dueDate,
                                  Invoice.Status status, BigDecimal total, List<Invoice.LineItem> items) {
        static InvoiceResponse of(Invoice i) {
            return new InvoiceResponse(i.getId(), i.getCustomer().getId(), i.getCustomer().getName(),
                    i.getDueDate(), i.getStatus(), i.getTotal(), List.copyOf(i.getItems()));
        }
    }
    public record Summary(long count, BigDecimal outstanding, BigDecimal paid) {}

    private final InvoiceRepository invoices;
    private final CustomerRepository customers;

    public InvoiceController(InvoiceRepository invoices, CustomerRepository customers) {
        this.invoices = invoices;
        this.customers = customers;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<InvoiceResponse> list(@AuthenticationPrincipal String owner,
                                      @RequestParam(required = false) Invoice.Status status) {
        var found = status == null
                ? invoices.findByOwnerEmailOrderByDueDateDesc(owner)
                : invoices.findByOwnerEmailAndStatusOrderByDueDateDesc(owner, status);
        return found.stream().map(InvoiceResponse::of).toList();
    }

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public Summary summary(@AuthenticationPrincipal String owner) {
        var all = invoices.findByOwnerEmailOrderByDueDateDesc(owner);
        BigDecimal paid = all.stream().filter(i -> i.getStatus() == Invoice.Status.PAID)
                .map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = all.stream().filter(i -> i.getStatus() != Invoice.Status.PAID)
                .map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Summary(all.size(), outstanding, paid);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public InvoiceResponse get(@AuthenticationPrincipal String owner, @PathVariable Long id) {
        return InvoiceResponse.of(find(owner, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public InvoiceResponse create(@AuthenticationPrincipal String owner, @Valid @RequestBody InvoiceRequest r) {
        Customer customer = customers.findByIdAndOwnerEmail(r.customerId(), owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown customer"));
        var items = r.items().stream()
                .map(i -> new Invoice.LineItem(i.description(), i.quantity(), i.unitPrice())).toList();
        return InvoiceResponse.of(invoices.save(new Invoice(owner, customer, r.dueDate(), items)));
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public InvoiceResponse updateStatus(@AuthenticationPrincipal String owner, @PathVariable Long id,
                                        @Valid @RequestBody StatusRequest r) {
        Invoice inv = find(owner, id);
        inv.setStatus(r.status());
        return InvoiceResponse.of(inv);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@AuthenticationPrincipal String owner, @PathVariable Long id) {
        invoices.delete(find(owner, id));
    }

    private Invoice find(String owner, Long id) {
        return invoices.findByIdAndOwnerEmail(id, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    }
}
