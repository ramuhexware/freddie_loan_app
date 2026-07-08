package com.freddieapp.strutsportal.action;

import com.opensymphony.xwork2.ActionSupport;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.struts2.ServletActionContext;

/**
 * Simple login / logout action.
 * In production, integrate with LDAP or OAuth2.
 * For demonstration, uses hardcoded credentials.
 */
@Slf4j
@Getter
@Setter
public class LoginAction extends ActionSupport {

    private String username;
    private String password;

    public String execute() {
        // Simplified auth check — replace with real LDAP/OAuth in production
        if ("admin".equalsIgnoreCase(username) && "freddie123".equals(password)) {
            HttpSession session = ServletActionContext.getRequest().getSession(true);
            session.setAttribute("loggedInUser", username);
            session.setAttribute("userRole", "ADMIN");
            log.info("[LoginAction] User '{}' logged in", username);
            return SUCCESS;
        }
        if ("officer".equalsIgnoreCase(username) && "freddie123".equals(password)) {
            HttpSession session = ServletActionContext.getRequest().getSession(true);
            session.setAttribute("loggedInUser", username);
            session.setAttribute("userRole", "LOAN_OFFICER");
            log.info("[LoginAction] User '{}' logged in", username);
            return SUCCESS;
        }
        addActionError("Invalid username or password");
        return INPUT;
    }

    public String logout() {
        HttpSession session = ServletActionContext.getRequest().getSession(false);
        if (session != null) {
            String user = (String) session.getAttribute("loggedInUser");
            session.invalidate();
            log.info("[LoginAction] User '{}' logged out", user);
        }
        return SUCCESS;
    }
}
