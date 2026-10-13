package com.travel.common;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Runs a controller action and turns business-rule failures into flash messages. */
public final class FlashMessages {

    private FlashMessages() { }

    public static void run(RedirectAttributes flash, String successMessage, Runnable action) {
        try {
            action.run();
            if (successMessage != null) flash.addFlashAttribute("success", successMessage);
        } catch (ValidationException e) {
            flash.addFlashAttribute("errors", e.getErrors());
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("errors", List.of(e.getMessage()));
        }
    }
}
