package com.example.orders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(OrderController.class)
public class OrderControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private OrderService orders;

    @Test
    public void answersWithTheOrder() throws Exception {
        mvc.perform(get("/orders/1")).andExpect(status().isOk());
    }
}
