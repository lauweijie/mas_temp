package MAS.Bean;

import MAS.Common.Constants;
import MAS.Common.Utils;
import MAS.Entity.Permission;
import MAS.Entity.Role;
import MAS.Entity.User;
import MAS.Exception.BadPasswordException;
import MAS.Exception.InvalidResetHashException;
import MAS.Exception.NotFoundException;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Stateless(name = "UserEJB")
@LocalBean
public class UserBean {
    @PersistenceContext
    private EntityManager em;

    @EJB
    private MailBean mailBean;

    public UserBean() {
    }

    public long createPermission(String name) {
        Permission permission = new Permission();
        permission.setName(name);
        em.persist(permission);
        em.flush();
        return permission.getId();
    }

    public long createRole(String name) {
        Role role = new Role();
        role.setName(name);
        em.persist(role);
        em.flush();
        return role.getId();
    }

    public long createUser(String username, String firstName, String lastName, String email, String phone) {
        User user = new User();
        user.setUsername(username.toLowerCase());
        user.setSalt(Utils.generateSalt());
        user.setLocked(false);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setDeleted(false);
        user.setResetHash(Utils.generateSalt());
        user.setResetExpiry(Utils.hoursFromNow(72));
        em.persist(user);
        em.flush();

        String msg = "Dear " + firstName + ",\n\n" +
                "Welcome to Merlion Airlines!\n\n" +
                "Please access the link below to activate your account and set your password:\n" +
                Constants.WEB_ROOT + "activate.xhtml?u=" + user.getId() + "&h=" + user.getResetHash() + "\n\n" +
                "For security reasons, the link will expire in 3 days.\n\n" +
                "Yours Sincerely,\n" +
                "Merlion Airlines";
        mailBean.send(email, firstName + " " + lastName, "Merlion Airlines Account Activation", msg);

        return user.getId();
    }

    public void setRoles(long id, List<Long> roleIds) throws NotFoundException {
        User user = em.find(User.class, id);
        if (user == null) throw new NotFoundException();
        Role role;
        ArrayList<Role> roles = new ArrayList<>();
        for(Long roleId : roleIds) {
            role = em.find(Role.class, roleId);
            if (role != null) {
                roles.add(role);
            }
        }
        user.setRoles(roles);
        em.persist(user);
    }

    public boolean isUsernameUnique(String username) {
        return (Long) em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username").setParameter("username", username.toLowerCase()).getSingleResult() == 0;
    }

    public void removeUser(long id) throws NotFoundException {
        User user = em.find(User.class, id);
        if (user == null) throw new NotFoundException();
        user.setDeleted(true);
        user.setRoles(new ArrayList<>());
        em.persist(user);
    }

    public void resetPassword(long id, String resetHash, String newPassword) throws NotFoundException, InvalidResetHashException, BadPasswordException {
        User user = em.find(User.class, id);
        if(user == null)
            throw new NotFoundException();
        if(!user.getResetHash().equals(resetHash) || user.getResetExpiry().after(new Date()))
            throw new InvalidResetHashException();
        if(!Utils.isGoodPassword(newPassword))
            throw new BadPasswordException();
        user.setPasswordHash(Utils.hash(newPassword, user.getSalt()));
        em.persist(user);
    }

    public List<User> getAllUsers() {
        return em.createQuery("SELECT u from User u WHERE NOT u.deleted", User.class).getResultList();
    }

    public void setLocked(long id, boolean isLocked) throws NotFoundException {
        User user = em.find(User.class, id);
        if(user == null) throw new NotFoundException();
        user.setLocked(isLocked);
        em.persist(user);
    }

}
