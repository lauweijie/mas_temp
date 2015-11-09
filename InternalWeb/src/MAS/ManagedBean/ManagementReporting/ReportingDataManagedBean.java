package MAS.ManagedBean.ManagementReporting;

import MAS.Bean.*;
import MAS.Entity.BookingClass;
import MAS.Entity.Flight;
import MAS.Exception.NotFoundException;
import MAS.ManagedBean.Auth.AuthManagedBean;
import MAS.ManagedBean.CommonManagedBean;
import com.google.gson.Gson;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@ManagedBean
public class ReportingDataManagedBean {
    @EJB
    private BookingClassBean bookingClassBean;
    @EJB
    private FleetBean fleetBean;
    @EJB
    private RouteBean routeBean;
    @EJB
    private CostsBean costsBean;
    @EJB
    private FlightScheduleBean flightScheduleBean;

    @ManagedProperty(value="#{commonManagedBean}")
    CommonManagedBean commonManagedBean;

    @ManagedProperty(value="#{authManagedBean}")
    private AuthManagedBean authManagedBean;

    @ManagedProperty(value="#{profitabilityReportManagedBean}")
    private ProfitabilityReportManagedBean profitabilityReportManagedBean;

    private class ReportItem {
        public String name;
        public Double value;
    }

    public void search() throws NotFoundException {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String query = params.get("q");

        switch (query) {
            case "topPerformingFlights":
                showTopPerformingFlights();
                return;
            case "topPerformingBookingClass":
                showTopPerformingBookingClasses();
                return;
            default:
                return;
        }
    }

    public void showTopPerformingFlights() throws NotFoundException {
        ArrayList<ReportItem> reportItems = new ArrayList<>();
        List<Flight> flights = flightScheduleBean.getAllFlights();
        for (Flight flight : flights) {
            ReportItem flightItem = new ReportItem();
            flightItem.name = flight.getCode() + " (" + commonManagedBean.formatDate("dd MMM yy, HH:mm", flight.getDepartureTime()) + ")";
            flightItem.value = profitabilityReportManagedBean.getProfitabilityByFlight(flight);
            reportItems.add(flightItem);
        }
        Collections.sort(reportItems, new Comparator<ReportItem>() {
            @Override
            public int compare(ReportItem o1, ReportItem o2) {
                if (o1.value < o2.value)
                    return 1;
                else if (o1.value > o2.value)
                    return -1;
                return 0;
            }
        });
        ArrayList<ReportItem> resultItems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            resultItems.add(reportItems.get(i));
        }

        Gson gson = new Gson();
        String json = gson.toJson(resultItems);

        if(!authManagedBean.isAuthenticated()) {
            json = "[]";
        }

        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentLength(json.length());
        response.setContentType("application/json");

        try {
            response.getOutputStream().write(json.getBytes());
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ctx.responseComplete();
    }

    public void showTopPerformingBookingClasses() throws NotFoundException {
        ArrayList<ReportItem> reportItems = new ArrayList<>();
        List<BookingClass> bookingClasses = bookingClassBean.getAllBookingClasses();
        for (BookingClass bookingClass : bookingClasses) {
            ReportItem bookingClassItem = new ReportItem();
            bookingClassItem.name = bookingClass.getName() + ": " +
                    bookingClass.getFlight().getAircraftAssignment().getRoute().getOrigin().getId() + " - " +
                    bookingClass.getFlight().getAircraftAssignment().getRoute().getDestination().getId() + " (" + bookingClass.getFlight().getCode() + ")";
            bookingClassItem.value = profitabilityReportManagedBean.getProfitabilityByBookingClass(bookingClass.getFlight(), bookingClass);
            reportItems.add(bookingClassItem);
        }
        Collections.sort(reportItems, new Comparator<ReportItem>() {
            @Override
            public int compare(ReportItem o1, ReportItem o2) {
                if (o1.value < o2.value)
                    return 1;
                else if (o1.value > o2.value)
                    return -1;
                return 0;
            }
        });
        ArrayList<ReportItem> resultItems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            resultItems.add(reportItems.get(i));
        }
        Gson gson = new Gson();
        String json = gson.toJson(resultItems);

        if(!authManagedBean.isAuthenticated()) {
            json = "[]";
        }

        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentLength(json.length());
        response.setContentType("application/json");

        try {
            response.getOutputStream().write(json.getBytes());
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ctx.responseComplete();
    }

    public CommonManagedBean getCommonManagedBean() {
        return commonManagedBean;
    }

    public void setCommonManagedBean(CommonManagedBean commonManagedBean) {
        this.commonManagedBean = commonManagedBean;
    }

    public ProfitabilityReportManagedBean getProfitabilityReportManagedBean() {
        return profitabilityReportManagedBean;
    }

    public void setProfitabilityReportManagedBean(ProfitabilityReportManagedBean profitabilityReportManagedBean) {
        this.profitabilityReportManagedBean = profitabilityReportManagedBean;
    }

    public void setAuthManagedBean(AuthManagedBean authManagedBean) {
        this.authManagedBean = authManagedBean;
    }
}