package MAS.Bean;

import MAS.Common.Utils;
import MAS.Entity.Customer;
import MAS.Exception.NotFoundException;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

@Stateless(name = "CustomerEJB")
@LocalBean
public class CustomerBean {
    @PersistenceContext
    private EntityManager em;

    public CustomerBean() {
    }

    public Customer createCustomer(Customer customer, String password) {
        customer.setSalt(Utils.generateSalt());
        customer.setPasswordHash(Utils.hash(password, customer.getSalt()));
        customer.setLocked(false);

        em.persist(customer);
        em.flush();

        return customer;
    }

    public Customer getCustomer(long id) throws NotFoundException {
        Customer customer = em.find(Customer.class, id);
        if (customer == null) throw new NotFoundException();
        return customer;
    }

    public void updateCustomer(Customer customer) throws NotFoundException {
        if (customer.getId() == null || em.find(Customer.class, customer.getId()) == null) throw new NotFoundException();
        em.merge(customer);
    }

}
