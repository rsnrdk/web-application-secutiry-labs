package rsnrdk.websec.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rsnrdk.websec.async.ItemInsights;
import rsnrdk.websec.async.ItemInsightsService;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemInsightsController {

    private final ItemInsightsService itemInsightsService;

    @GetMapping("/{id}/insights")
    public ItemInsights getInsights(@PathVariable Long id) {
        return itemInsightsService.getInsights(id);
    }
}
