package com.example.orders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PricingServiceTest {
    @Mock private TaxTable taxTable;
    @Mock private DiscountRules discountRules;
    @Mock private CurrencyConverter currencyConverter;
    @Mock private PriceList priceList;
    @Mock private Rounding rounding;
    @Mock private AuditLog auditLog;
    @InjectMocks private PricingService pricing;

    @Test
    public void appliesTheDiscount() {
        when(priceList.priceOf("item-1")).thenReturn(1000);
        pricing.price("item-1");
        verify(discountRules).apply(1000);
    }

    @Test
    public void writesAnAuditEntry() {
        pricing.price("item-1");
        verify(auditLog).record("priced item-1");
    }
}
