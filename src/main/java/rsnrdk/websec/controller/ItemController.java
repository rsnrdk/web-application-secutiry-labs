package rsnrdk.websec.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rsnrdk.websec.dto.ItemDto;
import rsnrdk.websec.service.ItemService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public List<ItemDto> getAll() {
        return itemService.getAllItems();
    }

    @GetMapping("/{id}")
    public ItemDto getItem(@PathVariable Long id) {
        return itemService.getItem(id);
    }

    @PostMapping
    public ItemDto createItem(@RequestBody ItemDto dto) {
        return itemService.createItem(dto);
    }

    @PutMapping
    public ItemDto updateItem(@RequestBody ItemDto dto) {
        return itemService.updateItem(dto);
    }

    @DeleteMapping("/{id}")
    public void deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
    }
}