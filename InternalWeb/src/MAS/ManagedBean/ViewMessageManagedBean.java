package MAS.ManagedBean;

import MAS.Bean.MessageBean;
import MAS.Entity.Message;
import MAS.Exception.NotFoundException;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;
import java.util.Map;

@ManagedBean
public class ViewMessageManagedBean {
    @EJB
    private MessageBean messageBean;
    @ManagedProperty(value="#{authManagedBean}")
    private AuthManagedBean authManagedBean;

    private Message message;

    @PostConstruct
    public void init() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        try {
            long id = Long.parseLong(params.get("id"));
            message = messageBean.getMessage(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public void setAuthManagedBean(AuthManagedBean authManagedBean) {
        this.authManagedBean = authManagedBean;
    }
}
