package no.ntnu.okse.web.controller;

import no.ntnu.okse.web.WebSecurityConfig;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 08/05/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */
@Controller
public class PasswordChangeController {

    // URL routes
    private static final String CHANGE_PASSWORD = "/auth/password";

    // Log4j logger
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PasswordChangeController.class.getName());

    /**
     * This method returns the changePassword view if the user is logged in (index page if not)
     * @return The view to render
     */
    @RequestMapping(method = RequestMethod.GET, value = CHANGE_PASSWORD)
    public String changePasswordGet() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof AnonymousAuthenticationToken) {
            // The user is not logged in
            return "fragments/indexNotLoggedIn";
        }

        return "fragments/changePassword";
    }

    /**
     * This method changes the users password if it's valid
     * @param firstPW First password entered by the user
     * @param secondPW Second password entered by the user
     * @param model A model to add attributes to for rendering
     * @param redirectAttributes A model to add attributes which are redirect to another view
     * @return The view to render
     */
    @RequestMapping(method = RequestMethod.POST, value = CHANGE_PASSWORD)
    public String changePasswordPost(@RequestParam(value = "password1") String firstPW,
                                     @RequestParam(value = "password2") String secondPW, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Changing password");
        if (! firstPW.equals(secondPW)) {
            model.addAttribute("errorMessage", "Passwords must be equal!");
            // add message, not same
            log.debug("Unable to change password, not equal");
            return "fragments/changePassword";
        }

        if (! WebSecurityConfig.changeUserPassword(firstPW)) {
            // add message, unable
            model.addAttribute("errorMessage", "Unable to change password!");
            log.debug("Unable to change password.");
            return "fragments/changePassword";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Password changed!");
        log.debug("Password changed");
        return "redirect:/";
    }


}
