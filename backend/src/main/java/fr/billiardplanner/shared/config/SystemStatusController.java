package fr.billiardplanner.shared.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    @GetMapping("/status")
    public SystemStatusResponse status() {
        return new SystemStatusResponse(
            "UP",
            "Billiard Space Planner"
        );
    }
}