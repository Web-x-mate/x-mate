package xmate.com.controller.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Inject biến global cho mọi view (brand, version...) */
@ControllerAdvice
public class GlobalModelAdvice {
    @ModelAttribute("brand")
    public String brand() { return "Xmate Admin"; }
}
