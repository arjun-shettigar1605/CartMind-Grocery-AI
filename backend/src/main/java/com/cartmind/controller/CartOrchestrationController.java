package com.cartmind.controller;
import com.cartmind.mcp.SwiggyMcpService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.cartmind.dto.AddressDto;
import com.cartmind.dto.CartPreviewDto;
import com.cartmind.dto.CartPreviewRequest;
import com.cartmind.dto.CartSyncRequest;
import com.cartmind.dto.CartSyncResultDto;
import org.springframework.security.core.Authentication;


@RestController
@RequestMapping("/cart")
public class CartOrchestrationController {

    private final SwiggyMcpService swiggyMcpService;

    public CartOrchestrationController(SwiggyMcpService swiggyMcpService) {
        this.swiggyMcpService = swiggyMcpService;
    }

    @GetMapping("/addresses")
    public List<AddressDto> addresses(Authentication auth) {
        return swiggyMcpService.getAddresses(auth.getName());
    }

    @PostMapping("/preview")
    public CartPreviewDto preview(@RequestBody CartPreviewRequest request, Authentication auth) {
        return swiggyMcpService.previewCart(auth.getName(), request);
    }

    @PostMapping("/sync")
    public CartSyncResultDto sync(@RequestBody CartSyncRequest request, Authentication auth) {
        return swiggyMcpService.syncCart(auth.getName(), request);
    }
}