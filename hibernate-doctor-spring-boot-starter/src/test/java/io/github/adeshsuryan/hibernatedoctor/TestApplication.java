package io.github.adeshsuryan.hibernatedoctor;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@SpringBootApplication
class TestApplication {
}

@Entity
@Table(name = "customer")
class Customer {

    @Id
    private Integer id;
    private String name;

    Customer() {
    }

    Customer(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

@Entity
@Table(name = "purchase_order")
class PurchaseOrder {

    @Id
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;

    PurchaseOrder() {
    }

    PurchaseOrder(Integer id, Customer customer) {
        this.id = id;
        this.customer = customer;
    }

    public Customer getCustomer() {
        return customer;
    }
}

@Repository
interface OrderRepository extends JpaRepository<PurchaseOrder, Integer> {
}

@Repository
interface CustomerRepository extends JpaRepository<Customer, Integer> {
}

@Service
class OrderService {

    private final OrderRepository orders;

    OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    /** Deliberate N+1: one findAll, then a lazy load per row. */
    @Transactional(readOnly = true)
    public List<String> customerNames() {
        return orders.findAll().stream()
                .map(o -> o.getCustomer().getName())
                .toList();
    }

    /** Clean path: a single aggregate query. */
    @Transactional(readOnly = true)
    public long count() {
        return orders.count();
    }
}
