package com.stock.trade.util;

import com.stock.trade.config.KisProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UrlHelper {

    private final KisProperties kisProperties;

    public String getBaseUrl() {
        return kisProperties.getBaseUrl();
    }
}
