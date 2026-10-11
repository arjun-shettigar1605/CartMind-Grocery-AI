package com.cartmind.controller;
import com.cartmind.service.SwiggyConnectionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class SwiggyConnectionController {

    private final SwiggyConnectionService connectionService;

    public SwiggyConnectionController(SwiggyConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @GetMapping("/swiggy-status")
    public Map<String, Object> status(Authentication authentication) {
        var status = connectionService.getStatus(authentication.getName());
        return Map.of("connected", status == SwiggyConnectionService.Status.CONNECTED,
                "status", status.name());
    }
}
