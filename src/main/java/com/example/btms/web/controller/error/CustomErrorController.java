package com.example.btms.web.controller.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Custom Error Controller
 * 
 * Handles HTTP errors gracefully:
 * - 404 Not Found
 * - 500 Internal Server Error
 * - 429 Too Many Requests
 * - Other errors
 * 
 * Reduces console noise from 404s for missing static resources
 * 
 * @author BTMS Team
 * @version 1.0
 */
@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            // Suppress logging for 404s on static resources (reduces noise)
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                String requestUri = uri != null ? uri.toString() : "unknown";

                // Only log 404 for non-static resources
                if (!isStaticResource(requestUri)) {
                    logger.warn("404 Not Found: {}", requestUri);
                }

                model.addAttribute("error", "Page Not Found");
                model.addAttribute("message", "The requested page could not be found.");
                model.addAttribute("status", 404);
                return "error/404";
            }

            if (statusCode == HttpStatus.TOO_MANY_REQUESTS.value()) {
                logger.warn("429 Too Many Requests: {}", uri);
                model.addAttribute("error", "Too Many Requests");
                model.addAttribute("message", "You have exceeded the rate limit. Please try again later.");
                model.addAttribute("status", 429);
                model.addAttribute("retryAfter", 60);
                return "error/429";
            }

            if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                logger.error("500 Internal Server Error: {}", uri);
                model.addAttribute("error", "Internal Server Error");
                model.addAttribute("message", "An unexpected error occurred. Please try again later.");
                model.addAttribute("status", 500);
                return "error/500";
            }
        }

        // Generic error page - use 500 page
        logger.error("Unhandled error: status={}, uri={}", status, uri);
        model.addAttribute("error", "Error");
        model.addAttribute("message", "An error occurred while processing your request.");
        model.addAttribute("status", status != null ? status : "Unknown");
        return "error/500";
    }

    /**
     * Check if URI is a static resource
     */
    private boolean isStaticResource(String uri) {
        return uri != null && (uri.startsWith("/css/") ||
                uri.startsWith("/js/") ||
                uri.startsWith("/images/") ||
                uri.startsWith("/icons/") ||
                uri.startsWith("/sounds/") ||
                uri.startsWith("/fonts/") ||
                uri.endsWith(".css") ||
                uri.endsWith(".js") ||
                uri.endsWith(".jpg") ||
                uri.endsWith(".jpeg") ||
                uri.endsWith(".png") ||
                uri.endsWith(".gif") ||
                uri.endsWith(".svg") ||
                uri.endsWith(".ico") ||
                uri.endsWith(".woff") ||
                uri.endsWith(".woff2") ||
                uri.endsWith(".ttf"));
    }
}
