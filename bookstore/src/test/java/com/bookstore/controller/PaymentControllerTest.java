package com.bookstore.controller;

import com.bookstore.dto.OrderDTO;
import com.bookstore.model.User;
import com.bookstore.model.UserType;
import com.bookstore.security.AuthenticationHelper;
import com.bookstore.service.OrderService;
import com.bookstore.service.PaymentService;
import com.bookstore.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private AuthenticationHelper authenticationHelper;

    @MockBean
    private UserService userService;

    @MockBean
    private OrderService orderService;

    @Test
    void regularUserCannotCreateCheckoutForAnotherUsersOrder() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setUserType(UserType.USER);
        OrderDTO order = new OrderDTO();
        order.setId(12L);
        order.setUserId(8L);
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(7L);
        when(userService.findById(7L)).thenReturn(Optional.of(user));
        when(orderService.getOrderById(12L)).thenReturn(order);

        mockMvc.perform(post("/api/payments/create-checkout-session/12"))
                .andExpect(status().isForbidden());

        verify(paymentService, never()).createCheckoutSession(any());
    }

    @Test
    void missingStripeConfigurationReturnsClearServiceUnavailableResponse() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setUserType(UserType.USER);
        OrderDTO order = new OrderDTO();
        order.setId(12L);
        order.setUserId(7L);
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(7L);
        when(userService.findById(7L)).thenReturn(Optional.of(user));
        when(orderService.getOrderById(12L)).thenReturn(order);
        when(paymentService.createCheckoutSession(12L)).thenThrow(
                new IllegalStateException("Stripe is not configured. Set STRIPE_SECRET_KEY."));

        mockMvc.perform(post("/api/payments/create-checkout-session/12"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_CONFIGURED"));
    }
}
