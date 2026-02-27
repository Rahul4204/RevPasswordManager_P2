package com.passwordmanager.app.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "404");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ModelAndView handleInvalidCreds(InvalidCredentialsException ex) {
        logger.warn("Invalid credentials: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "401");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(ValidationException.class)
    public ModelAndView handleValidation(ValidationException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "400");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ModelAndView handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        logger.warn("Not found: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "404");
        mav.addObject("errorMessage", "Page or resource not found.");
        return mav;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleMaxSizeException(MaxUploadSizeExceededException ex) {
        logger.warn("File size exceeded: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "413");
        mav.addObject("errorMessage", "The uploaded file is too large! Please select an image under 5MB.");
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneral(Exception ex) {
        logger.error("Unexpected error", ex);
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "500");
        mav.addObject("errorMessage", "An unexpected error occurred. Please try again.");
        return mav;
    }
}
