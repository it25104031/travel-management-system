package com.travel.web;

import com.travel.module1_auth.domain.SiteCustomerEntity;
import com.travel.module1_auth.service.SiteCustomerService;
import com.travel.web.site.Cart;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class SiteModelAdvice {

    private final SiteInfo site;
    private final SiteCustomerService customerService;
    private final Cart cart;

    public SiteModelAdvice(SiteInfo site, SiteCustomerService customerService, Cart cart) {
        this.site = site;
        this.customerService = customerService;
        this.cart = cart;
    }

    @ModelAttribute("site")
    public SiteInfo site() { return site; }

    @ModelAttribute("member")
    public SiteCustomerEntity member() { return customerService.current().orElse(null); }

    @ModelAttribute("cartCount")
    public int cartCount() {
        try {
            return cart.getItems().size();
        } catch (Exception e) {
            return 0;
        }
    }
}
