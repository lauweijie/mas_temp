package MAS.Bean;

import MAS.Entity.AircraftAssignment;
import MAS.Entity.Flight;
import MAS.Exception.NotFoundException;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;

@Stateless(name = "FlightScheduleEJB")
public class FlightScheduleBean {
    @PersistenceContext
    EntityManager em;

    public FlightScheduleBean() {
    }

    //-----------------Flights---------------------------
    public long createFlight(String code, Date departureTime, Date arrivalTime, long aircraftAssignmentId) throws NotFoundException {
        Flight flight = new Flight();
        flight.setCode(code);
        flight.setArrivalTime(arrivalTime);
        flight.setDepartureTime(departureTime);
        AircraftAssignment aircraftAssignment = em.find(AircraftAssignment.class, aircraftAssignmentId);
        if (aircraftAssignment == null) throw new NotFoundException();
        flight.setAircraftAssignment(aircraftAssignment);
        em.persist(flight);
        em.flush();
        return flight.getId();
    }

    public void changeFlightCode(long id, String code) throws NotFoundException {
        Flight flight = em.find(Flight.class, id);
        if (flight == null) throw new NotFoundException();
        flight.setCode(code);
        em.persist(flight);
    }

    public void changeFlightTimings(long id, Date departureTime, Date arrivalTime) throws NotFoundException {
        Flight flight = em.find(Flight.class, id);
        if (flight == null) throw new NotFoundException();
        flight.setArrivalTime(arrivalTime);
        flight.setDepartureTime(departureTime);
        em.persist(flight);
        em.flush();
    }

    public void removeFlight(long id) throws NotFoundException {
        Flight flight = em.find(Flight.class, id);
        if (flight == null) throw new NotFoundException();
        em.remove(flight);
    }

    public Flight getFlight(long id) throws NotFoundException {
        Flight flight = em.find(Flight.class, id);
        if (flight == null) throw new NotFoundException();
        return flight;
    }

    public List<Flight> getAllFlights() {
        return em.createQuery("SELECT f from Flight f", Flight.class).getResultList();
    }

    public List<Flight> findFlightByAA(long aaId) throws NotFoundException {
        AircraftAssignment aa = em.find(AircraftAssignment.class, aaId);
        if (aa == null) throw new NotFoundException();
        return em.createQuery("SELECT f from Flight f WHERE f.aircraftAssignment = :aa", Flight.class).setParameter("aa", aa).getResultList();
    }

    public String findSeatConfigOfFlight(long flightId) throws NotFoundException {
        Flight flight = em.find(Flight.class, flightId);
        if (flight == null) throw new NotFoundException();
        return flight.getAircraftAssignment().getAircraft().getSeatConfig().getSeatConfig();
    }
}
