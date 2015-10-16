package MAS.ManagedBean.DepartureControl;

import MAS.Bean.FlightScheduleBean;
import MAS.Bean.PNRBean;
import MAS.Common.Constants;
import MAS.Common.SeatConfigObject;
import MAS.Common.Utils;
import MAS.Entity.Baggage;
import MAS.Entity.ETicket;
import MAS.Entity.Itinerary;
import MAS.Entity.PNR;
import MAS.Exception.NotFoundException;
import MAS.ManagedBean.Auth.AuthManagedBean;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import java.util.*;

@ManagedBean
@ViewScoped
public class CheckInManagedBean {

    @ManagedProperty(value = "#{authManagedBean}")
    private AuthManagedBean authManagedBean;

    private ETicket primaryETicket;
    private List<ETicket> relatedPassengers;
    private HashMap<Long, Boolean> relatedPassengersCheck;
    private HashMap<Long, Boolean> relatedPassengersCheckDisable;

    private List<ETicket> connections;

    private Integer[] seats;
    private String ffpProgram;
    private String ffpNumber;
    private String finalDestination;

    @EJB
    FlightScheduleBean flightScheduleBean;
    @EJB
    PNRBean pnrBean;

    public void setAuthManagedBean(AuthManagedBean authManagedBean) {
        this.authManagedBean = authManagedBean;
    }

    @PostConstruct
    public void init() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        try {
            primaryETicket = flightScheduleBean.getETicket(Long.parseLong(params.get("eticket")));
            relatedPassengers = flightScheduleBean.getRelatedETickets(primaryETicket);
            relatedPassengersCheck = new HashMap<>();
            relatedPassengersCheckDisable = new HashMap<>();
            for (ETicket eTicket : relatedPassengers) {
                relatedPassengersCheck.put(eTicket.getId(), eTicket.getId().equals(primaryETicket.getId()));
                relatedPassengersCheckDisable.put(eTicket.getId(), eTicket.getId().equals(primaryETicket.getId()));
            }

            // @TODO: Populate seat array with allocated seat in ticket

            connections = getPossibleConnections(primaryETicket);
            seats = new Integer[connections.size()];
            finalDestination = connections.get(connections.size() - 1).getFlight().getAircraftAssignment().getRoute().getDestination().getId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            String ffp = pnrBean.getPassengerSpecialServiceRequest(primaryETicket.getPnr(), pnrBean.getPassengerNumber(primaryETicket.getPnr(), primaryETicket.getPassengerName()), Constants.SSR_ACTION_CODE_FFP).getValue();
            String[] parts = ffp.split("/");
            if (parts.length != 2) throw new Exception();
            if (!Arrays.asList(Constants.FFP_ALLIANCE_LIST_CODE).contains(parts[0])) throw new NotFoundException();
            ffpProgram = parts[0];
            ffpNumber = parts[1];
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public List<ETicket> getLinkedETickets(ETicket eTicket) {
        ArrayList<ETicket> linkedETickets = new ArrayList<>();
        linkedETickets.add(eTicket);
        while (eTicket.getNextConnection() != null) {
            eTicket = eTicket.getNextConnection();
            linkedETickets.add(eTicket);
        }
        return linkedETickets;
    }

    public LinkedHashMap<String, Integer> availableSeatsName(ETicket eTicket) {
        SeatConfigObject seatConfigObject = SeatConfigObject.getInstance(eTicket.getFlight().getAircraftAssignment().getAircraft().getSeatConfig().getSeatConfig());
        seatConfigObject.addTakenSeats(flightScheduleBean.getSeatsTakenForFlight(eTicket.getFlight()));
        return seatConfigObject.getAvailableSeatsNameForTravelClass(eTicket.getTravelClass());
    }

    public String getNiceSeatName(ETicket eTicket) throws NotFoundException {
        SeatConfigObject seatConfigObject = SeatConfigObject.getInstance(eTicket.getFlight().getAircraftAssignment().getAircraft().getSeatConfig().getSeatConfig());
        return seatConfigObject.convertIntToString(eTicket.getSeatNumber());
    }

    public void checkIn() {
        int index = 0;
        List<ETicket> selectedConnections = getSelectedConnections();
        for (ETicket eTicket : selectedConnections) {
            if (!flightScheduleBean.isSeatAvailable(eTicket.getFlight(), seats[index++])) {
                authManagedBean.createAuditLog("Checked in passenger: " + eTicket.getPassengerName(), "check_in");
                FacesMessage m = new FacesMessage("The seat selected on flight " + eTicket.getFlight().getCode() + " is no longer available.");
                m.setSeverity(FacesMessage.SEVERITY_ERROR);
                FacesContext.getCurrentInstance().addMessage("check-in-status", m);
                return;
            }
        }
        index = 0;
        for (ETicket eTicket : selectedConnections) {
            // @TODO: Double check if seat is still available, if not show error message

            eTicket.setSeatNumber(seats[index]);
            if (!ffpNumber.trim().equals("")) {
                eTicket.setFfpNumber(ffpProgram + "/" + ffpNumber.trim());
            }
            eTicket.setCheckedIn(true);
            try {
                eTicket.setNextConnection(selectedConnections.get(index + 1));
            } catch (IndexOutOfBoundsException e) {}
            try {
                flightScheduleBean.updateETicket(eTicket);
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            index++;
        }
    }

    public void addBaggageToETicket(double weight) {
        Baggage baggage = new Baggage();
        baggage.setWeight(weight);
        List<Baggage> baggageList = primaryETicket.getBaggages();
        baggageList.add(baggage);
    }

    public LinkedHashMap<String, String> getFFPAllianceList() {
        LinkedHashMap<String, String> ffpAllianceList = new LinkedHashMap<>();
        for (int i = 0; i < Constants.FFP_ALLIANCE_LIST_CODE.length; i++) {
            ffpAllianceList.put(Constants.FFP_ALLIANCE_LIST_NAME[i], Constants.FFP_ALLIANCE_LIST_CODE[i]);
        }
        return ffpAllianceList;
    }

    public ETicket getPrimaryETicket() {
        return primaryETicket;
    }

    public HashMap<Long, Boolean> getRelatedPassengersCheck() {
        return relatedPassengersCheck;
    }

    public void setRelatedPassengersCheck(HashMap<Long, Boolean> relatedPassengersCheck) {
        this.relatedPassengersCheck = relatedPassengersCheck;
    }

    public List<ETicket> getRelatedPassengers() {
        return relatedPassengers;
    }

    public void setRelatedPassengers(List<ETicket> relatedPassengers) {
        this.relatedPassengers = relatedPassengers;
    }

    public HashMap<Long, Boolean> getRelatedPassengersCheckDisable() {
        return relatedPassengersCheckDisable;
    }

    public void setRelatedPassengersCheckDisable(HashMap<Long, Boolean> relatedPassengersCheckDisable) {
        this.relatedPassengersCheckDisable = relatedPassengersCheckDisable;
    }

    public void setPrimaryETicket(ETicket primaryETicket) {
        this.primaryETicket = primaryETicket;
    }

    public List<ETicket> getSelectedConnections() {
        List<ETicket> selectedConnections = new ArrayList<>();
        for (ETicket eTicket : connections) {
            selectedConnections.add(eTicket);
            if (eTicket.getFlight().getAircraftAssignment().getRoute().getDestination().getId().equals(finalDestination)) {
                break;
            }
        }
        return selectedConnections;
    }

    public List<ETicket> getPossibleConnections(ETicket eTicket) {
        ArrayList<ETicket> connections = new ArrayList<>(Arrays.asList(eTicket));
        PNR pnr = eTicket.getPnr();
        Date arrivalTime = eTicket.getFlight().getArrivalTime();
        String destination = eTicket.getFlight().getAircraftAssignment().getRoute().getDestination().getId();
        List<Itinerary> itineraries = pnr.getItineraries();

        HashSet<String> visited = new HashSet<>();
        for (ETicket connection : connections) {
            visited.add(connection.getFlight().getAircraftAssignment().getRoute().getOrigin().getId());
            visited.add(connection.getFlight().getAircraftAssignment().getRoute().getDestination().getId());
        }

        for (Itinerary itinerary : itineraries) {
            if (itinerary.getDepartureDate().after(arrivalTime) && itinerary.getDepartureDate().before(Utils.minutesLater(arrivalTime, Constants.MAX_CONNECTION_TIME_MINUTES)) && itinerary.getOrigin().equals(destination) && !visited.contains(itinerary.getDestination())) {
                try {
                    int itineraryNumber = pnrBean.getItineraryNumber(pnr, itinerary.getFlightCode());
                    int passengerNumber = pnrBean.getPassengerNumber(pnr, eTicket.getPassengerName());
                    String eTicketNumber = pnrBean.getPassengerSpecialServiceRequest(pnr, passengerNumber, itineraryNumber ,Constants.SSR_ACTION_CODE_TICKET_NUMBER).getValue();
                    ETicket connection = flightScheduleBean.getETicket(Long.parseLong(eTicketNumber));
                    connections.addAll(getPossibleConnections(connection));
                    return connections;
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return connections;
    }

    public String getFfpProgram() {
        return ffpProgram;
    }

    public void setFfpProgram(String ffpProgram) {
        this.ffpProgram = ffpProgram;
    }

    public String getFfpNumber() {
        return ffpNumber;
    }

    public void setFfpNumber(String ffpNumber) {
        this.ffpNumber = ffpNumber;
    }

    public String getFinalDestination() {
        return finalDestination;
    }

    public void setFinalDestination(String finalDestination) {
        this.finalDestination = finalDestination;
    }

    public List<ETicket> getConnections() {
        return connections;
    }

    public void setConnections(List<ETicket> connections) {
        this.connections = connections;
    }

    public Integer[] getSeats() {
        return seats;
    }

    public void setSeats(Integer[] seats) {
        this.seats = seats;
    }
}
