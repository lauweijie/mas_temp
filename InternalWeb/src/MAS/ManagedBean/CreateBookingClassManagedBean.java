package MAS.ManagedBean;

import MAS.Bean.BookingClassBean;
import MAS.Bean.FareRuleBean;
import MAS.Bean.FlightScheduleBean;
import MAS.Entity.FareRule;
import MAS.Entity.Flight;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;
import java.util.List;

@ManagedBean
public class CreateBookingClassManagedBean {
    @EJB
    BookingClassBean bookingClassBean;
    @EJB
    FlightScheduleBean flightScheduleBean;
    @EJB
    FareRuleBean fareRuleBean;

    private List<Flight> flights;
    private List<FareRule> fareRules;

    @ManagedProperty(value="#{authManagedBean}")
    private AuthManagedBean authManagedBean;

    private String name;
    private long fareRuleId;
    private long flightId;
    private int allocation;
    private int travelClass;

    public CreateBookingClassManagedBean() {
        resetFields();
    }

    @PostConstruct
    private void init() {
        flights = flightScheduleBean.getAllFlights();
        fareRules = fareRuleBean.getAllFareRules();
    }

    public void resetFields() {
        name = "";
        fareRuleId = 0;
        flightId = 0;
        allocation = 0;
    }

    public void createBookingClass() {
        try {
            bookingClassBean.createBookingClass(name, allocation, getTravelClass(), fareRuleId, flightId);

            getAuthManagedBean().createAuditLog("Created new booking class: " + getName(), "create_booking_class");

            FacesMessage m = new FacesMessage("Booking class created successfully.");
            m.setSeverity(FacesMessage.SEVERITY_INFO);
            FacesContext.getCurrentInstance().addMessage("status", m);

            resetFields();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAuthManagedBean(AuthManagedBean authManagedBean) {
        this.authManagedBean = authManagedBean;
    }

    public List<Flight> getFlights() {
        return flights;
    }

    public void setFlights(List<Flight> flights) {
        this.flights = flights;
    }

    public List<FareRule> getFareRules() {
        return fareRules;
    }

    public void setFareRules(List<FareRule> fareRules) {
        this.fareRules = fareRules;
    }

    public AuthManagedBean getAuthManagedBean() {
        return authManagedBean;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getFareRuleId() {
        return fareRuleId;
    }

    public void setFareRuleId(long fareRuleId) {
        this.fareRuleId = fareRuleId;
    }

    public long getFlightId() {
        return flightId;
    }

    public void setFlightId(long flightId) {
        this.flightId = flightId;
    }

    public int getAllocation() {
        return allocation;
    }

    public void setAllocation(int allocation) {
        this.allocation = allocation;
    }

    public int getTravelClass() {
        return travelClass;
    }

    public void setTravelClass(int travelClass) {
        this.travelClass = travelClass;
    }
}
