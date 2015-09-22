package MAS.ManagedBean;

import MAS.Bean.AuditLogBean;
import MAS.Bean.UserBean;
import MAS.Common.Constants;
import MAS.Common.Permissions;
import MAS.Entity.Permission;
import MAS.Entity.Role;
import MAS.Entity.User;
import MAS.Exception.InvalidLoginException;
import MAS.Exception.NotFoundException;
import MAS.Structure.MainMenu;
import MAS.Structure.MenuEntry;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@ManagedBean
@SessionScoped
public class AuthManagedBean {
    @EJB
    private UserBean userBean;

    @EJB
    private AuditLogBean auditLogBean;

    private long userId;
    private boolean authenticated = false;
    private Set<String> permissions;
    private MainMenu mainMenu;

    public void populatePermissions() {
        permissions = new HashSet<>();
        if (!authenticated) return;
        try {
            for (Role role : userBean.getUser(userId).getRoles()) {
                for (Permission permission : role.getPermissions()) {
                    permissions.add(permission.getName());
                }
            }
        } catch (NotFoundException e) {
        }
    }

    public boolean login(String username, String password) {
        try {
            userId = userBean.login(username, password);
            authenticated = true;
            populatePermissions();
            generateMenu(permissions);

            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            createAuditLog("Logged in from " + request.getRemoteAddr(), "login");

            return true;
        } catch (InvalidLoginException e) {
            return false;
        }
    }

    public void logout() {
        userId = -1;
        authenticated = false;
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        forwardToLogin();
    }

    public void requiresAuth() {
        if (!authenticated) {
            forwardToLogin();
        }
    }

    public void forwardToLogin() {
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(Constants.WEB_ROOT + "/Auth");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createAuditLog(String description, String category) {
        try {
            auditLogBean.createAuditLog(userId, description, category);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    public void generateMenu(Set<String> permissions) {
        mainMenu = new MainMenu();
        List<MenuEntry> menuEntries = mainMenu.getEntries();
        Iterator<MenuEntry> it = menuEntries.iterator();
        while (it.hasNext()) {
            MenuEntry menuEntry = it.next();
            menuEntry.stripInaccessibleChildren(permissions);
            if(menuEntry.getChildren().size() == 0) {
                it.remove();
            }
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Set<String> getPermissions() {
        return this.permissions;
    }


    public MainMenu getMainMenu() {
        return mainMenu;
    }
}